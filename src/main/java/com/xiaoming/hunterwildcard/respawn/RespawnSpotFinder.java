package com.xiaoming.hunterwildcard.respawn;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Random;

/**
 * Picks a random standable surface spot in a ring around a point, preferring spots far from a set of
 * players (the hunters, when a runner respawns). Chunks are loaded synchronously, so the candidate
 * count is kept small.
 */
public final class RespawnSpotFinder {
    private static final int CANDIDATES = 14;
    private static final int CEILING_SCAN_TOP = 120;

    private RespawnSpotFinder() {
    }

    /**
     * @return a spot at least {@code avoidDistance} from every avoided player when one exists within the
     *         candidate budget, otherwise the candidate farthest from them, or null when nothing is standable.
     */
    public static BlockPos find(ServerWorld world, Vec3d origin, int minDistance, int maxDistance,
                                List<ServerPlayerEntity> avoid, double avoidDistance, Random random) {
        BlockPos best = null;
        double bestClearance = -1.0D;
        double avoidSquared = avoidDistance * avoidDistance;
        for (int i = 0; i < CANDIDATES; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = minDistance + random.nextDouble() * Math.max(0, maxDistance - minDistance);
            int x = (int) Math.round(origin.x + Math.cos(angle) * distance);
            int z = (int) Math.round(origin.z + Math.sin(angle) * distance);
            if (!world.getWorldBorder().contains(x, z)) {
                continue;
            }
            BlockPos spot = standableSpot(world, x, z);
            if (spot == null) {
                continue;
            }
            double clearance = clearanceSquared(world, spot, avoid);
            if (clearance >= avoidSquared) {
                return spot;
            }
            if (clearance > bestClearance) {
                bestClearance = clearance;
                best = spot;
            }
        }
        return best;
    }

    private static double clearanceSquared(ServerWorld world, BlockPos spot, List<ServerPlayerEntity> avoid) {
        double nearest = Double.MAX_VALUE;
        for (ServerPlayerEntity player : avoid) {
            if (player.getEntityWorld() != world) {
                continue;
            }
            double dx = player.getX() - (spot.getX() + 0.5D);
            double dz = player.getZ() - (spot.getZ() + 0.5D);
            nearest = Math.min(nearest, dx * dx + dz * dz);
        }
        return nearest;
    }

    private static BlockPos standableSpot(ServerWorld world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        if (!world.getDimension().hasCeiling()) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            return isStandable(world, pos) ? pos : null;
        }

        int top = Math.min(world.getBottomY() + world.getHeight() - 8, CEILING_SCAN_TOP);
        BlockPos.Mutable cursor = new BlockPos.Mutable(x, top, z);
        while (cursor.getY() > world.getBottomY() + 4) {
            if (isStandable(world, cursor)) {
                return cursor.toImmutable();
            }
            cursor.move(0, -1, 0);
        }
        return null;
    }

    private static boolean isStandable(ServerWorld world, BlockPos feet) {
        if (feet.getY() <= world.getBottomY() || feet.getY() >= world.getBottomY() + world.getHeight() - 2) {
            return false;
        }
        BlockPos ground = feet.down();
        if (!world.getBlockState(ground).isSolidBlock(world, ground) || !world.getFluidState(ground).isEmpty()) {
            return false;
        }
        return world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                && world.getFluidState(feet).isEmpty()
                && world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty()
                && world.getFluidState(feet.up()).isEmpty();
    }
}
