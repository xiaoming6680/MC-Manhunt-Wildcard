package com.xiaoming.hunterwildcard.backrooms;

/**
 * Every block-placement decision the Backrooms makes, as a pure function of seed and position.
 * <p>
 * Nothing here may read a neighbouring chunk: chunks generate off-thread in arbitrary order, so every
 * cell hashes its own coordinates and decides alone. Ported from The Fourth Frequency.
 * <p>
 * Exits are scattered "hole cells": the whole 4x4 interior of a cell is false floor. They always sit
 * inside a corridor cell (never on a wall line), so a hole is a clean square you can see once you are
 * in that cell, and there is no big clearing to run to.
 */
public final class BackroomsMazePolicy {
    private static final int WALL_CHANCE_256 = 90;
    private static final int LIGHT_CHANCE_256 = 155;
    /** About one exit cell in 500 (a 22x22-cell patch, ~110 blocks across, holds one on average). */
    private static final int HOLE_CHANCE_65536 = 130;
    /**
     * Connectivity without corridors: a cell never carries both its north and its west wall, so from
     * any cell you can always step north or west, and every such path eventually merges (the
     * binary-tree maze argument). No trunk corridors, so there is no sightline that runs past render
     * distance and hands hunters a free look down a hallway.
     */
    private static final long TIE_SALT = 0x6E4A_91D3_2C7F_B085L;

    private static final long WEST_SALT = 0x5F1E_2B77_A1C3_0D45L;
    private static final long NORTH_SALT = 0x27B4_9E01_6C8F_3AA9L;
    private static final long LIGHT_SALT = 0x71C0_45D2_9E3B_86F7L;
    private static final long HOLE_SALT = 0x3D07_B1F4_5E29_A6C3L;

    private static final int[][] SEARCH_ORDER = {
            {0, 0}, {1, 0}, {0, 1}, {-1, 0}, {0, -1},
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};

    private BackroomsMazePolicy() {
    }

    /** Nearest cell (this one first) whose interior is real floor. */
    public static int[] standableCell(long seed, int cellX, int cellZ) {
        for (int radius = 0; radius < 6; radius++) {
            for (int[] step : SEARCH_ORDER) {
                int candidateX = cellX + step[0] * radius;
                int candidateZ = cellZ + step[1] * radius;
                if (!holeCell(seed, candidateX, candidateZ)) {
                    return new int[] {candidateX, candidateZ};
                }
            }
        }
        return new int[] {cellX, cellZ};
    }

    /** Block coordinates of a cell's centre, which is never on a wall line. */
    public static int[] cellCentre(int cellX, int cellZ) {
        return new int[] {
                cellX * BackroomsLayout.CELL_SIZE + BackroomsLayout.CELL_SIZE / 2,
                cellZ * BackroomsLayout.CELL_SIZE + BackroomsLayout.CELL_SIZE / 2};
    }

    public static boolean wall(long seed, int x, int z) {
        int localX = BackroomsLayout.local(x);
        int localZ = BackroomsLayout.local(z);
        if (localX != 0 && localZ != 0) {
            return false;
        }
        int cellX = BackroomsLayout.cell(x);
        int cellZ = BackroomsLayout.cell(z);
        if (localX == 0 && localZ == 0) {
            return true;
        }
        return localX == 0 ? westWall(seed, cellX, cellZ) : northWall(seed, cellX, cellZ);
    }

    /** True for every interior block of a hole cell; wall lines are never false floor. */
    public static boolean falseFloor(long seed, int x, int z) {
        if (BackroomsLayout.local(x) == 0 || BackroomsLayout.local(z) == 0) {
            return false;
        }
        return holeCell(seed, BackroomsLayout.cell(x), BackroomsLayout.cell(z));
    }

    public static boolean holeCell(long seed, int cellX, int cellZ) {
        return ((mix(seed, cellX, cellZ, HOLE_SALT) >>> 32) & 0xFFFFL) < HOLE_CHANCE_65536;
    }

    public static boolean ceilingLight(long seed, int x, int z) {
        if (!ceilingFixture(seed, x, z)) {
            return false;
        }
        int cellX = BackroomsLayout.cell(x);
        int cellZ = BackroomsLayout.cell(z);
        int district = (int) Math.floorMod(mix(seed, Math.floorDiv(cellX, 8), Math.floorDiv(cellZ, 8), LIGHT_SALT ^ 0x43ADL), 3);
        int chance = district == 0 ? 110 : district == 1 ? LIGHT_CHANCE_256 : 210;
        return below(mix(seed, cellX, cellZ, LIGHT_SALT), chance);
    }

    public static boolean ceilingFixture(long seed, int x, int z) {
        int localZ = BackroomsLayout.local(z);
        return BackroomsLayout.local(x) == BackroomsLayout.LIGHT_LOCAL_X
                && (localZ == BackroomsLayout.LIGHT_LOCAL_Z || localZ == BackroomsLayout.LIGHT_LOCAL_Z + 1);
    }

    private static boolean westWall(long seed, int cellX, int cellZ) {
        boolean west = below(mix(seed, cellX, cellZ, WEST_SALT), WALL_CHANCE_256);
        boolean north = below(mix(seed, cellX, cellZ, NORTH_SALT), WALL_CHANCE_256);
        if (west && north) {
            return keepWestOnTie(seed, cellX, cellZ);
        }
        return west;
    }

    private static boolean northWall(long seed, int cellX, int cellZ) {
        boolean west = below(mix(seed, cellX, cellZ, WEST_SALT), WALL_CHANCE_256);
        boolean north = below(mix(seed, cellX, cellZ, NORTH_SALT), WALL_CHANCE_256);
        if (west && north) {
            return !keepWestOnTie(seed, cellX, cellZ);
        }
        return north;
    }

    private static boolean keepWestOnTie(long seed, int cellX, int cellZ) {
        return (mix(seed, cellX, cellZ, TIE_SALT) & 1L) == 0L;
    }

    private static boolean below(long hash, int chanceOf256) {
        return ((hash >>> 40) & 0xFFL) < chanceOf256;
    }

    private static long mix(long seed, int cellX, int cellZ, long salt) {
        long h = seed ^ salt;
        h ^= (long) cellX * 0x9E3779B97F4A7C15L;
        h ^= (long) cellZ * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }
}
