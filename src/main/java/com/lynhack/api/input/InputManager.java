package com.lynhack.api.input;

import com.lynhack.api.event.FrameListener;
import com.lynhack.api.event.FrameUpdateEvent;
import com.lynhack.api.util.GaussianRandomUtil;
import com.lynhack.debug.DebugTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative Closet Input Dispatcher.
 * 
 * Strictly simulates physical keyboard and mouse inputs through Minecraft's native
 * KeyBinding pipeline with Gaussian hold duration ($40ms - 100ms, \mu=70ms, \sigma=10ms$).
 * Never bypasses client-side physics or injects synthetic interaction packets directly.
 */
public class InputManager implements FrameListener {
    private static final InputManager INSTANCE = new InputManager();

    public static InputManager getInstance() {
        return INSTANCE;
    }

    public record PendingRelease(Object requester, int keycode, long targetReleaseTimeMs) implements Comparable<PendingRelease> {
        @Override
        public int compareTo(PendingRelease other) {
            return Long.compare(this.targetReleaseTimeMs, other.targetReleaseTimeMs);
        }
    }

    private static final int ATTACK_KEY_CODE = -100; // Special code for attack key binding

    // Key -> Set of requesting modules/objects
    private final Map<Integer, Set<Object>> activeKeyRequesters = new ConcurrentHashMap<>();
    // Key -> Timestamp when key was initially pressed down
    private final Map<Integer, Long> keyPressTimestamps = new ConcurrentHashMap<>();
    // Key -> Target minimum hold duration for the current press cycle
    private final Map<Integer, Long> targetHoldDurations = new ConcurrentHashMap<>();
    // Monotonic PriorityQueue for delayed releases (drained strictly on main render thread)
    private final PriorityQueue<PendingRelease> pendingReleases = new PriorityQueue<>(Comparator.comparingLong(PendingRelease::targetReleaseTimeMs));
    // Physical keys to block from vanilla Minecraft
    private final Set<Integer> blockedPhysicalKeys = ConcurrentHashMap.newKeySet();

    private InputManager() {}

    /**
     * Main-thread frame loop: drains expired key releases using O(1) peek checks.
     */
    @Override
    public void onFrame(FrameUpdateEvent event) {
        long now = Util.getMeasuringTimeMs();

        synchronized (pendingReleases) {
            while (!pendingReleases.isEmpty() && pendingReleases.peek().targetReleaseTimeMs() <= now) {
                PendingRelease release = pendingReleases.poll();
                internalRelease(release.requester(), release.keycode());
            }
        }
    }

    /**
     * Simulates an authentic attack input (Left Mouse Button click) via Minecraft's native KeyBinding.
     * Generates a Gaussian hold duration ($40ms - 100ms$) and releases cleanly without direct packet injection.
     */
    public void simulateAttack(Object requester, String details) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null || mc.options.attackKey == null) return;

        KeyBinding attackKey = mc.options.attackKey;
        long holdDuration = GaussianRandomUtil.nextKeyHoldDuration();
        long targetReleaseTime = Util.getMeasuringTimeMs() + holdDuration;

        // Press native attack key and increment click counter
        attackKey.setPressed(true);
        KeyBinding.onKeyPressed(attackKey.getDefaultKey());

        synchronized (pendingReleases) {
            pendingReleases.add(new PendingRelease(requester, ATTACK_KEY_CODE, targetReleaseTime));
        }

        String reqName = requester instanceof com.lynhack.api.module.Module m ? m.getName() : requester.getClass().getSimpleName();
        DebugTracker.getInstance().recordSimulatedPress(GLFW.GLFW_MOUSE_BUTTON_LEFT, "LMB (Attack)", reqName, details, holdDuration);
    }

    /**
     * Instantly presses a key on behalf of a requester and initializes its Gaussian hold threshold.
     */
    public void simulateKeyPress(Object requester, int keycode) {
        if (keycode <= 0) return;

        Set<Object> requesters = activeKeyRequesters.computeIfAbsent(keycode, k -> ConcurrentHashMap.newKeySet());
        boolean wasEmpty = requesters.isEmpty();
        requesters.add(requester);

        if (wasEmpty) {
            long now = Util.getMeasuringTimeMs();
            long holdDuration = GaussianRandomUtil.nextKeyHoldDuration();
            keyPressTimestamps.put(keycode, now);
            targetHoldDurations.put(keycode, holdDuration);
            setPhysicalKeyState(keycode, true);

            String reqName = requester instanceof com.lynhack.api.module.Module m ? m.getName() : requester.getClass().getSimpleName();
            DebugTracker.getInstance().recordSimulatedPress(keycode, getKeyName(keycode), reqName, "", holdDuration);
        }
    }

    /**
     * Requests key release. If the key has not met its Gaussian hold duration, defers release to the priority queue.
     */
    public void simulateKeyRelease(Object requester, int keycode) {
        if (keycode <= 0) return;

        long now = Util.getMeasuringTimeMs();
        long pressTime = keyPressTimestamps.getOrDefault(keycode, now);
        long minDuration = targetHoldDurations.getOrDefault(keycode, 40L);
        long targetReleaseTime = pressTime + minDuration;

        if (now < targetReleaseTime) {
            synchronized (pendingReleases) {
                pendingReleases.add(new PendingRelease(requester, keycode, targetReleaseTime));
            }
        } else {
            internalRelease(requester, keycode);
        }
    }

    /**
     * Simulates a single humanized key tap (presses immediately, enqueues release at now + gaussian hold duration).
     */
    public void simulateKeyTap(Object requester, int keycode) {
        if (keycode <= 0) return;

        simulateKeyPress(requester, keycode);
        long holdDuration = targetHoldDurations.getOrDefault(keycode, GaussianRandomUtil.nextKeyHoldDuration());
        long targetReleaseTime = Util.getMeasuringTimeMs() + holdDuration;

        synchronized (pendingReleases) {
            pendingReleases.add(new PendingRelease(requester, keycode, targetReleaseTime));
        }
    }

    private void internalRelease(Object requester, int keycode) {
        if (keycode == ATTACK_KEY_CODE) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options != null && mc.options.attackKey != null) {
                mc.options.attackKey.setPressed(false);
            }
            return;
        }

        Set<Object> requesters = activeKeyRequesters.get(keycode);
        if (requesters != null) {
            requesters.remove(requester);
            if (requesters.isEmpty()) {
                activeKeyRequesters.remove(keycode);
                keyPressTimestamps.remove(keycode);
                targetHoldDurations.remove(keycode);
                setPhysicalKeyState(keycode, false);
            }
        }
    }

    /**
     * Releases all keys requested by a specific module/object (e.g. when disabled).
     */
    public void releaseAll(Object requester) {
        synchronized (pendingReleases) {
            pendingReleases.removeIf(p -> p.requester().equals(requester));
        }

        for (Map.Entry<Integer, Set<Object>> entry : activeKeyRequesters.entrySet()) {
            if (entry.getValue().remove(requester) && entry.getValue().isEmpty()) {
                int keycode = entry.getKey();
                keyPressTimestamps.remove(keycode);
                targetHoldDurations.remove(keycode);
                setPhysicalKeyState(keycode, false);
            }
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null && mc.options.attackKey != null) {
            mc.options.attackKey.setPressed(false);
        }
    }

    /**
     * Emergency flush: cancels all pending releases, unpresses all simulated keys, and clears blocks.
     */
    public void flushAll() {
        synchronized (pendingReleases) {
            pendingReleases.clear();
        }

        for (Integer keycode : activeKeyRequesters.keySet()) {
            setPhysicalKeyState(keycode, false);
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null && mc.options.attackKey != null) {
            mc.options.attackKey.setPressed(false);
        }

        activeKeyRequesters.clear();
        keyPressTimestamps.clear();
        targetHoldDurations.clear();
        blockedPhysicalKeys.clear();
    }

    public void blockPhysicalKey(int keycode) {
        blockedPhysicalKeys.add(keycode);
    }

    public void unblockPhysicalKey(int keycode) {
        blockedPhysicalKeys.remove(keycode);
    }

    public boolean shouldBlockPhysicalKey(int keycode) {
        boolean blocked = blockedPhysicalKeys.contains(keycode);
        if (blocked) {
            DebugTracker.getInstance().recordPhysicalBlock(keycode, getKeyName(keycode));
        }
        return blocked;
    }

    public boolean isSimulatedPressed(int keycode) {
        Set<Object> reqs = activeKeyRequesters.get(keycode);
        return reqs != null && !reqs.isEmpty();
    }

    public static String getKeyName(int keycode) {
        String name = GLFW.glfwGetKeyName(keycode, 0);
        if (name != null && !name.isEmpty()) return name.toUpperCase();
        return switch (keycode) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_W -> "W";
            case GLFW.GLFW_KEY_A -> "A";
            case GLFW.GLFW_KEY_S -> "S";
            case GLFW.GLFW_KEY_D -> "D";
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "LMB (Attack)";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "RMB (Use)";
            default -> "KEY_" + keycode;
        };
    }

    private void setPhysicalKeyState(int keycode, boolean pressed) {
        try {
            InputUtil.Key key = InputUtil.fromKeyCode(new KeyInput(keycode, 0, 0));
            KeyBinding.setKeyPressed(key, pressed);
        } catch (Exception ignored) {
            // Ignore invalid key codes or off-world contexts safely
        }
    }
}
