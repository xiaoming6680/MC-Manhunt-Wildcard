package com.xiaoming.hunterwildcard.prepare;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;

public class HunterBoundaryManager {
    private static final int PARTICLE_INTERVAL_TICKS = 10;
    private static final int BOUNDARY_PARTICLE_POINTS = 32;

    private final Map<UUID, BoundaryPosition> centers = new HashMap<>();
    private final Map<UUID, BoundaryPosition> lastLegalPositions = new HashMap<>();
    private int visualTicks;

    public void start(GameContext context) {
        clear();
        if (!context.getConfig().hunterPrepareBoundaryEnabled) {
            return;
        }

        for (ServerPlayer hunter : context.getHunters()) {
            recordCenter(hunter);
            hunter.sendSystemMessage(HunterWildcardText.translatable("msg.prepare.boundary_created"), false);
        }
    }

    public void tick(GameContext context) {
        if (!context.getConfig().hunterPrepareBoundaryEnabled) {
            return;
        }

        double radius = Math.max(1, context.getConfig().hunterPrepareBoundaryRadius);
        double radiusSquared = radius * radius;
        double warnDistance = Math.max(0, context.getConfig().hunterPrepareBoundaryWarnDistance);
        visualTicks--;
        boolean showBoundary = visualTicks <= 0;

        for (ServerPlayer hunter : context.getHunters()) {
            UUID uuid = hunter.getUUID();
            BoundaryPosition center = centers.computeIfAbsent(uuid, ignored -> createPosition(hunter));
            if (showBoundary) {
                spawnBoundaryParticles(hunter, center, radius);
            }

            if (isInside(hunter, center, radiusSquared)) {
                lastLegalPositions.put(uuid, createPosition(hunter));
                maybeWarnNearEdge(hunter, center, radius, warnDistance);
                continue;
            }

            BoundaryPosition target = lastLegalPositions.getOrDefault(uuid, center);
            teleport(hunter, context, target);
            hunter.sendSystemMessage(HunterWildcardText.translatable("msg.prepare.boundary_blocked"), true);
        }

        if (showBoundary) {
            visualTicks = PARTICLE_INTERVAL_TICKS;
        }
    }

    public void remove(ServerPlayer player) {
        centers.remove(player.getUUID());
        lastLegalPositions.remove(player.getUUID());
    }

    public void clear() {
        centers.clear();
        lastLegalPositions.clear();
        visualTicks = 0;
    }

    private void recordCenter(ServerPlayer hunter) {
        BoundaryPosition position = createPosition(hunter);
        centers.put(hunter.getUUID(), position);
        lastLegalPositions.put(hunter.getUUID(), position);
    }

    private boolean isInside(ServerPlayer hunter, BoundaryPosition center, double radiusSquared) {
        if (!hunter.level().dimension().equals(center.worldKey)) {
            return false;
        }

        double dx = hunter.getX() - center.x;
        double dz = hunter.getZ() - center.z;
        return dx * dx + dz * dz <= radiusSquared;
    }

    private void maybeWarnNearEdge(ServerPlayer hunter, BoundaryPosition center, double radius, double warnDistance) {
        if (warnDistance <= 0) {
            return;
        }

        double dx = hunter.getX() - center.x;
        double dz = hunter.getZ() - center.z;
        double remaining = radius - Math.sqrt(dx * dx + dz * dz);
        if (remaining <= warnDistance) {
            hunter.sendSystemMessage(HunterWildcardText.translatable("msg.prepare.boundary_remaining", Math.max(0, (int) Math.floor(remaining))), true);
        }
    }

    private void spawnBoundaryParticles(ServerPlayer hunter, BoundaryPosition center, double radius) {
        if (!hunter.level().dimension().equals(center.worldKey)) {
            return;
        }

        ServerLevel world = hunter.level();
        double y = hunter.getY() + 0.15;
        int points = radius <= 12.0 ? 20 : BOUNDARY_PARTICLE_POINTS;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.02, 0.0, 0.0);
        }
    }

    private void teleport(ServerPlayer hunter, GameContext context, BoundaryPosition target) {
        ServerLevel world = context.getServer().getLevel(target.worldKey);
        if (world == null) {
            world = hunter.level();
        }

        hunter.teleportTo(world, target.x, target.y, target.z, Set.<Relative>of(), target.yaw, target.pitch, true);
        lastLegalPositions.put(hunter.getUUID(), new BoundaryPosition(world.dimension(), target.x, target.y, target.z, target.yaw, target.pitch));
    }

    private BoundaryPosition createPosition(ServerPlayer player) {
        return new BoundaryPosition(
                player.level().dimension(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }

    private record BoundaryPosition(ResourceKey<Level> worldKey, double x, double y, double z, float yaw, float pitch) {
    }
}
