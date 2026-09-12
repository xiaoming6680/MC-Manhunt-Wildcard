package com.xiaoming.hunterwildcard.respawn;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

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
    public static BlockPos find(ServerLevel world, Vec3 origin, int minDistance, int maxDistance,
                                List<ServerPlayer> avoid, double avoidDistance, Random random) {
        BlockPos best = null;
        double bestClearance = -1.0D;
        double avoidSquared = avoidDistance * avoidDistance;
        for (int i = 0; i < CANDIDATES; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = minDistance + random.nextDouble() * Math.max(0, maxDistance - minDistance);
            int x = (int) Math.round(origin.x + Math.cos(angle) * distance);
            int z = (int) Math.round(origin.z + Math.sin(angle) * distance);
            if (!world.getWorldBorder().isWithinBounds(x, z)) {
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

    private static double clearanceSquared(ServerLevel world, BlockPos spot, List<ServerPlayer> avoid) {
        double nearest = Double.MAX_VALUE;
        for (ServerPlayer player : avoid) {
            if (player.level() != world) {
                continue;
            }
            double dx = player.getX() - (spot.getX() + 0.5D);
            double dz = player.getZ() - (spot.getZ() + 0.5D);
            nearest = Math.min(nearest, dx * dx + dz * dz);
        }
        return nearest;
    }

    private static BlockPos standableSpot(ServerLevel world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        if (!world.dimensionType().hasCeiling()) {
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            return isStandable(world, pos) ? pos : null;
        }

        int top = Math.min(world.getMinY() + world.getHeight() - 8, CEILING_SCAN_TOP);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, top, z);
        while (cursor.getY() > world.getMinY() + 4) {
            if (isStandable(world, cursor)) {
                return cursor.immutable();
            }
            cursor.move(0, -1, 0);
        }
        return null;
    }

    private static boolean isStandable(ServerLevel world, BlockPos feet) {
        if (feet.getY() <= world.getMinY() || feet.getY() >= world.getMinY() + world.getHeight() - 2) {
            return false;
        }
        BlockPos ground = feet.below();
        if (!world.getBlockState(ground).isRedstoneConductor(world, ground) || !world.getFluidState(ground).isEmpty()) {
            return false;
        }
        return world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                && world.getFluidState(feet).isEmpty()
                && world.getBlockState(feet.above()).getCollisionShape(world, feet.above()).isEmpty()
                && world.getFluidState(feet.above()).isEmpty();
    }
}
