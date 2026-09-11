package com.xiaoming.hunterwildcard.client;

import com.xiaoming.hunterwildcard.wildcard.rules.WorldTiltRule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Client half of the World Tilt wildcard: pulls the local player sideways every tick (mirroring the server) and
 * tells the camera which way gravity currently points so the view rolls to match, easing in and out.
 */
public final class WorldTiltClient {
    private static boolean active;
    private static double dirX;
    private static double dirZ;
    private static float fromBlend;
    private static float toBlend;
    private static long transitionStartMs = -1L;
    private static long transitionMs;

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
        if (active) {
            WorldTiltRule.applyGravity(player, dirX, dirZ);
        } else if (player.hasNoGravity() && toBlend == 0.0F) {
            // The server clears the flag through the data tracker too; this just keeps single-player snappy.
            player.setNoGravity(false);
        }
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
}
