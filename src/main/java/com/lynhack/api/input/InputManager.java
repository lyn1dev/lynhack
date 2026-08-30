package com.lynhack.api.input;

import com.lynhack.api.event.FrameListener;
import com.lynhack.api.event.FrameUpdateEvent;
import com.lynhack.api.util.GaussianRandomUtil;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
     * Instantly presses a key on behalf of a requester and initializes its Gaussian hold threshold.
     */
    public void simulateKeyPress(Object requester, int keycode) {
        if (keycode <= 0) return;

        Set<Object> requesters = activeKeyRequesters.computeIfAbsent(keycode, k -> ConcurrentHashMap.newKeySet());
        boolean wasEmpty = requesters.isEmpty();
        requesters.add(requester);

        if (wasEmpty) {
            long now = Util.getMeasuringTimeMs();
            keyPressTimestamps.put(keycode, now);
            targetHoldDurations.put(keycode, GaussianRandomUtil.nextKeyHoldDuration());
            setPhysicalKeyState(keycode, true);
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
        return blockedPhysicalKeys.contains(keycode);
    }

    public boolean isSimulatedPressed(int keycode) {
        Set<Object> reqs = activeKeyRequesters.get(keycode);
        return reqs != null && !reqs.isEmpty();
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
