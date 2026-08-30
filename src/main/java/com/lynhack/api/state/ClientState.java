package com.lynhack.api.state;

import com.lynhack.api.event.EventBus;
import com.lynhack.api.event.FrameUpdateEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class ClientState {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Render timing
    public static float tickDelta = 0.0f;
    public static long frameTimeNanos = 0L;

    // Player position & motion
    public static double x, y, z;
    public static double prevX, prevY, prevZ;
    public static double interpolatedX, interpolatedY, interpolatedZ;
    public static double velocityX, velocityY, velocityZ;
    public static double speed2D;
    public static double forwardVelocity, strafeVelocity;
    public static boolean isOnGround;
    public static double fallDistance;

    // Player orientation
    public static float yaw, pitch, headYaw;
    public static Direction facing = Direction.NORTH;

    // Player environmental status
    public static boolean inWater, inLava, isSubmerged;
    public static boolean isSneaking, isSprinting, isGliding, isRiding;
    public static float health, maxHealth;
    public static int foodLevel;
    public static float saturation;
    public static int air;

    // Network shadow state foundation (for future anti-cheat prediction models)
    public static double shadowX, shadowY, shadowZ;
    public static boolean isDesynced = false;

    // Lazy raycasting cache (strictly reset at frame start)
    private static EntityHitResult cachedEntityHitResult = null;
    private static BlockHitResult cachedBlockHitResult = null;
    private static boolean hasComputedEntityRaycast = false;
    private static boolean hasComputedBlockRaycast = false;
    private static double lastEntityMaxDistance = 0.0;
    private static double lastBlockMaxDistance = 0.0;

    private ClientState() {}

    /**
     * Called per frame at the beginning of rendering.
     * Wipes lazy raycast caches, extracts 1.21+ RenderTickCounter, captures per-frame snapshot,
     * and dispatches FrameUpdateEvent to EventBus.
     */
    public static void update() {
        // 1. Strict cache reset at frame start
        cachedEntityHitResult = null;
        cachedBlockHitResult = null;
        hasComputedEntityRaycast = false;
        hasComputedBlockRaycast = false;

        // 2. Extract tickDelta from 1.21+ RenderTickCounter
        if (mc.getRenderTickCounter() != null) {
            tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        } else {
            tickDelta = 1.0f;
        }
        frameTimeNanos = System.nanoTime();

        ClientPlayerEntity player = mc.player;
        if (player == null) {
            EventBus.getInstance().postFrame(new FrameUpdateEvent(tickDelta, frameTimeNanos));
            return;
        }

        // 3. Capture motion & position snapshot
        x = player.getX();
        y = player.getY();
        z = player.getZ();
        prevX = player.lastRenderX;
        prevY = player.lastRenderY;
        prevZ = player.lastRenderZ;
        interpolatedX = prevX + (x - prevX) * tickDelta;
        interpolatedY = prevY + (y - prevY) * tickDelta;
        interpolatedZ = prevZ + (z - prevZ) * tickDelta;

        Vec3d vel = player.getVelocity();
        velocityX = vel.x;
        velocityY = vel.y;
        velocityZ = vel.z;
        speed2D = Math.hypot(velocityX, velocityZ);
        isOnGround = player.isOnGround();
        fallDistance = player.fallDistance;

        // Calculate directional velocities relative to player yaw
        double yawRad = Math.toRadians(player.getYaw());
        forwardVelocity = velocityX * -Math.sin(yawRad) + velocityZ * Math.cos(yawRad);
        strafeVelocity = velocityX * Math.cos(yawRad) - velocityZ * -Math.sin(yawRad);

        // 4. Capture orientation
        yaw = player.getYaw();
        pitch = player.getPitch();
        headYaw = player.getHeadYaw();
        facing = player.getHorizontalFacing();

        // 5. Capture status
        inWater = player.isTouchingWater();
        inLava = player.isInLava();
        isSubmerged = player.isSubmergedInWater();
        isSneaking = player.isSneaking();
        isSprinting = player.isSprinting();
        isGliding = player.isGliding();
        isRiding = player.hasVehicle();

        health = player.getHealth();
        maxHealth = player.getMaxHealth();
        if (player.getHungerManager() != null) {
            foodLevel = player.getHungerManager().getFoodLevel();
            saturation = player.getHungerManager().getSaturationLevel();
        }
        air = player.getAir();

        // 6. Network shadow baseline
        shadowX = x;
        shadowY = y;
        shadowZ = z;

        // 7. Dispatch FrameUpdateEvent via zero-reflection fast path
        EventBus.getInstance().postFrame(new FrameUpdateEvent(tickDelta, frameTimeNanos));
    }

    /**
     * Lazy-evaluated frame-perfect entity raycast.
     * Only calculates on first call in a frame; subsequent calls in the same frame return cached result.
     */
    public static EntityHitResult getTargetedEntity(double maxDistance) {
        if (hasComputedEntityRaycast && maxDistance <= lastEntityMaxDistance) {
            return cachedEntityHitResult;
        }

        Entity camera = mc.getCameraEntity();
        ClientWorld world = mc.world;
        if (camera == null || world == null) {
            return null;
        }

        Vec3d cameraPos = camera.getCameraPosVec(tickDelta);
        Vec3d rotVec = camera.getRotationVec(tickDelta);
        Vec3d reachVec = cameraPos.add(rotVec.multiply(maxDistance));
        Box box = camera.getBoundingBox().stretch(rotVec.multiply(maxDistance)).expand(1.0, 1.0, 1.0);

        cachedEntityHitResult = ProjectileUtil.raycast(
                camera,
                cameraPos,
                reachVec,
                box,
                entity -> !entity.isSpectator() && entity.canHit(),
                maxDistance * maxDistance
        );

        hasComputedEntityRaycast = true;
        lastEntityMaxDistance = maxDistance;
        return cachedEntityHitResult;
    }

    /**
     * Lazy-evaluated frame-perfect block raycast.
     * Only calculates on first call in a frame; subsequent calls return cached result.
     */
    public static BlockHitResult getTargetedBlock(double maxDistance) {
        if (hasComputedBlockRaycast && maxDistance <= lastBlockMaxDistance) {
            return cachedBlockHitResult;
        }

        Entity camera = mc.getCameraEntity();
        ClientWorld world = mc.world;
        if (camera == null || world == null) {
            return null;
        }

        Vec3d cameraPos = camera.getCameraPosVec(tickDelta);
        Vec3d rotVec = camera.getRotationVec(tickDelta);
        Vec3d reachVec = cameraPos.add(rotVec.multiply(maxDistance));

        cachedBlockHitResult = world.raycast(new RaycastContext(
                cameraPos,
                reachVec,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                camera
        ));

        hasComputedBlockRaycast = true;
        lastBlockMaxDistance = maxDistance;
        return cachedBlockHitResult;
    }
}
