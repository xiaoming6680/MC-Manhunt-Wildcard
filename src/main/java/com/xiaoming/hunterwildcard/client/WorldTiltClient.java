package com.xiaoming.hunterwildcard.client;

import com.xiaoming.hunterwildcard.wildcard.rules.WorldTiltRule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Client half of the World Tilt wildcard. Gravity is described by a unit vector that eases from straight down
 * to a horizontal direction over the transition; everything else derives from it:
 * <ul>
 *   <li>a rotation "frame" that maps the player's local space (down = -Y) onto world space,</li>
 *   <li>mouse look happens in local yaw/pitch and is projected through the frame to world yaw/pitch,</li>
 *   <li>the camera, movement input, jumping, ground detection and the player model all use the frame.</li>
 * </ul>
 * The hitbox stays world-upright (vanilla has no rotated collision), so "standing" on a wall means being pressed
 * against it; that is the compromise that keeps this a mod feature instead of a whole gravity API.
 */
public final class WorldTiltClient {
    private static final float MOUSE_SENSITIVITY = 0.15F;
    private static final Vector3f WORLD_DOWN = new Vector3f(0.0F, -1.0F, 0.0F);

    private static boolean active;
    private static double dirX;
    private static double dirZ;
    private static float fromBlend;
    private static float toBlend;
    private static long transitionStartMs = -1L;
    private static long transitionMs;
    private static float localYaw;
    private static float localPitch;

    private WorldTiltClient() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(WorldTiltClient::tick);
    }

    public static void set(boolean nowActive, int transitionTicks, double gravityX, double gravityZ) {
        dirX = gravityX;
        dirZ = gravityZ;
        float target = nowActive ? 1.0F : 0.0F;
        if (target == toBlend && transitionStartMs >= 0L) {
            active = nowActive;
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (!isFrameActive() && player != null) {
            // Entering the tilted frame: start from wherever the player is looking right now.
            localYaw = player.getYaw();
            localPitch = player.getPitch();
        }
        fromBlend = currentBlend();
        toBlend = target;
        transitionStartMs = System.currentTimeMillis();
        transitionMs = Math.max(1L, transitionTicks * 50L);
        active = nowActive;
    }

    public static void reset() {
        active = false;
        fromBlend = 0.0F;
        toBlend = 0.0F;
        transitionStartMs = -1L;
        transitionMs = 0L;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.hasNoGravity()) {
            client.player.setNoGravity(false);
        }
    }

    private static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        if (isFrameActive()) {
            applyLocalLook(player);
        }
        if (active) {
            if (!player.hasNoGravity()) {
                player.setNoGravity(true);
            }
            player.fallDistance = 0.0F;
            Vec3d gravity = currentGravity();
            Vec3d velocity = player.getVelocity();
            if (velocity.dotProduct(gravity) < WorldTiltRule.TERMINAL_SPEED) {
                player.setVelocity(velocity.add(gravity.multiply(WorldTiltRule.GRAVITY_PER_TICK)));
            }
        } else if (player.hasNoGravity() && !isFrameActive()) {
            player.setNoGravity(false);
        }
    }

    public static boolean isLocalPlayer(Entity entity) {
        return entity != null && entity == MinecraftClient.getInstance().player;
    }

    /** True while gravity is anything other than straight down (including both transitions). */
    public static boolean isFrameActive() {
        return currentBlend() > 1.0E-4F;
    }

    /** 0 = normal gravity, 1 = fully sideways; eased between the two while transitioning. */
    public static float currentBlend() {
        if (transitionStartMs < 0L) {
            return toBlend;
        }
        float t = MathHelper.clamp((System.currentTimeMillis() - transitionStartMs) / (float) transitionMs, 0.0F, 1.0F);
        float eased = t < 0.5F ? 2.0F * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 2) / 2.0F;
        return fromBlend + (toBlend - fromBlend) * eased;
    }

    /** Direction gravity pulls right now in world space (unit length); straight down when inactive. */
    public static Vec3d currentGravity() {
        float blend = currentBlend();
        if (blend <= 0.0F) {
            return new Vec3d(0.0, -1.0, 0.0);
        }
        Vec3d mixed = new Vec3d(dirX * blend, -(1.0F - blend), dirZ * blend);
        return mixed.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, -1.0, 0.0) : mixed.normalize();
    }

    /** Rotation taking local space (down = -Y) to world space for the current gravity; the shortest arc, so no twist. */
    public static Quaternionf frame() {
        Vec3d gravity = currentGravity();
        return new Quaternionf().rotationTo(WORLD_DOWN, new Vector3f((float) gravity.x, (float) gravity.y, (float) gravity.z));
    }

    public static float localYaw() {
        return localYaw;
    }

    public static float localPitch() {
        return localPitch;
    }

    /** Mouse movement edits the local angles; world yaw/pitch follow so aiming and raycasts stay honest. */
    public static void handleMouse(Entity player, double cursorDeltaX, double cursorDeltaY) {
        localYaw += (float) cursorDeltaX * MOUSE_SENSITIVITY;
        localPitch = MathHelper.clamp(localPitch + (float) cursorDeltaY * MOUSE_SENSITIVITY, -90.0F, 90.0F);
        applyLocalLook(player);
    }

    private static void applyLocalLook(Entity player) {
        Vec3d look = toWorld(lookVector(localPitch, localYaw));
        float worldPitch = (float) Math.toDegrees(-Math.asin(MathHelper.clamp(look.y, -1.0, 1.0)));
        float worldYaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        player.setYaw(worldYaw);
        player.setPitch(worldPitch);
        player.lastYaw = worldYaw;
        player.lastPitch = worldPitch;
    }

    /** Vanilla's movement-input rotation done in local yaw, then projected through the frame. */
    public static Vec3d movementDelta(float speed, Vec3d input) {
        double lengthSquared = input.lengthSquared();
        if (lengthSquared < 1.0E-7) {
            return Vec3d.ZERO;
        }
        Vec3d scaled = (lengthSquared > 1.0 ? input.normalize() : input).multiply(speed);
        float sin = MathHelper.sin(localYaw * (float) (Math.PI / 180.0));
        float cos = MathHelper.cos(localYaw * (float) (Math.PI / 180.0));
        Vec3d local = new Vec3d(scaled.x * cos - scaled.z * sin, scaled.y, scaled.z * cos + scaled.x * sin);
        return toWorld(local);
    }

    /** Replaces the vertical component of a jump with one along local "up". */
    public static Vec3d jumpVelocity(Vec3d current, float jumpStrength, boolean sprinting) {
        Vec3d up = currentGravity().multiply(-1.0);
        Vec3d withoutUp = current.subtract(up.multiply(current.dotProduct(up)));
        Vec3d result = withoutUp.add(up.multiply(jumpStrength));
        if (sprinting) {
            result = result.add(toWorld(lookVector(0.0F, localYaw)).multiply(0.2));
        }
        return result;
    }

    public static Vec3d toWorld(Vec3d local) {
        Vector3f v = new Vector3f((float) local.x, (float) local.y, (float) local.z);
        frame().transform(v);
        return new Vec3d(v.x, v.y, v.z);
    }

    private static Vec3d lookVector(float pitch, float yaw) {
        float pitchRad = pitch * (float) (Math.PI / 180.0);
        float yawRad = -yaw * (float) (Math.PI / 180.0);
        float cosYaw = MathHelper.cos(yawRad);
        float sinYaw = MathHelper.sin(yawRad);
        float cosPitch = MathHelper.cos(pitchRad);
        float sinPitch = MathHelper.sin(pitchRad);
        return new Vec3d(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }
}
