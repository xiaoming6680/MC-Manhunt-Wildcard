package com.xiaoming.hunterwildcard.backrooms;

/**
 * Every block-placement decision the Backrooms makes, as a pure function of seed and position.
 * <p>
 * Nothing here may read a neighbouring chunk: chunks generate off-thread in arbitrary order, so every
 * cell hashes its own coordinates and decides alone. Ported from The Fourth Frequency.
 */
public final class BackroomsMazePolicy {
    private static final int WALL_CHANCE_256 = 90;
    private static final int LIGHT_CHANCE_256 = 155;
    /**
     * Connectivity without corridors: a cell never carries both its north and its west wall, so from
     * any cell you can always step north or west, and every such path eventually merges (the
     * binary-tree maze argument). No trunk corridors, so there is no sightline that runs past render
     * distance and hands hunters a free look down a hallway.
     */
    private static final long TIE_SALT = 0x6E4A_91D3_2C7F_B085L;
    private static final int EXIT_SQUARE_CELLS = 32;
    private static final int EXIT_REGION_CELLS = 3;
    private static final int SOFT_FLOOR_CHANCE_65536 = 25;
    private static final int SOFT_FLOOR_PATCH = 4;

    private static final long WEST_SALT = 0x5F1E_2B77_A1C3_0D45L;
    private static final long NORTH_SALT = 0x27B4_9E01_6C8F_3AA9L;
    private static final long LIGHT_SALT = 0x71C0_45D2_9E3B_86F7L;
    private static final long EXIT_SALT = 0x1A93_C7E5_0B62_4D18L;
    private static final long SOFT_SALT = 0x3D07_B1F4_5E29_A6C3L;

    private static final int[][] SEARCH_ORDER = {
            {0, 0}, {1, 0}, {0, 1}, {-1, 0}, {0, -1},
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};

    private BackroomsMazePolicy() {
    }

    /** Nearest cell (this one first) whose centre is real floor: not an exit region, not a soft patch. */
    public static int[] standableCell(long seed, int cellX, int cellZ) {
        for (int radius = 0; radius < 6; radius++) {
            for (int[] step : SEARCH_ORDER) {
                int candidateX = cellX + step[0] * radius;
                int candidateZ = cellZ + step[1] * radius;
                if (withinExitRegion(seed, candidateX, candidateZ)) {
                    continue;
                }
                int blockX = candidateX * BackroomsLayout.CELL_SIZE + BackroomsLayout.CELL_SIZE / 2;
                int blockZ = candidateZ * BackroomsLayout.CELL_SIZE + BackroomsLayout.CELL_SIZE / 2;
                if (!falseFloor(seed, blockX, blockZ)) {
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
        if (exitClearing(seed, x, z)) {
            return false;
        }
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

    public static boolean exitClearing(long seed, int x, int z) {
        int[] region = exitRegionOrigin(seed, BackroomsLayout.cell(x), BackroomsLayout.cell(z));
        if (region == null) {
            return false;
        }
        int minX = region[0] * BackroomsLayout.CELL_SIZE;
        int minZ = region[1] * BackroomsLayout.CELL_SIZE;
        int span = EXIT_REGION_CELLS * BackroomsLayout.CELL_SIZE;
        return x >= minX && x <= minX + span && z >= minZ && z <= minZ + span;
    }

    private static boolean scatteredSoftFloor(long seed, int x, int z) {
        if (BackroomsLayout.local(x) == 0 || BackroomsLayout.local(z) == 0) {
            return false;
        }
        int patchX = Math.floorDiv(x, SOFT_FLOOR_PATCH);
        int patchZ = Math.floorDiv(z, SOFT_FLOOR_PATCH);
        return ((mix(seed, patchX, patchZ, SOFT_SALT) >>> 32) & 0xFFFFL) < SOFT_FLOOR_CHANCE_65536;
    }

    public static boolean falseFloor(long seed, int x, int z) {
        return primaryExitFloor(seed, x, z) || scatteredSoftFloor(seed, x, z);
    }

    public static boolean primaryExitFloor(long seed, int x, int z) {
        int[] region = exitRegionOrigin(seed, BackroomsLayout.cell(x), BackroomsLayout.cell(z));
        if (region == null) {
            return false;
        }
        int minX = region[0] * BackroomsLayout.CELL_SIZE;
        int minZ = region[1] * BackroomsLayout.CELL_SIZE;
        int span = EXIT_REGION_CELLS * BackroomsLayout.CELL_SIZE;
        return x >= minX && x < minX + span && z >= minZ && z < minZ + span;
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

    public static int[] exitRegionOrigin(long seed, int cellX, int cellZ) {
        int squareX = Math.floorDiv(cellX, EXIT_SQUARE_CELLS);
        int squareZ = Math.floorDiv(cellZ, EXIT_SQUARE_CELLS);
        long h = mix(seed, squareX, squareZ, EXIT_SALT);
        int span = EXIT_SQUARE_CELLS - EXIT_REGION_CELLS;
        int originX = squareX * EXIT_SQUARE_CELLS + (int) ((h >>> 8) & 0xFFFFL) % span;
        int originZ = squareZ * EXIT_SQUARE_CELLS + (int) ((h >>> 32) & 0xFFFFL) % span;
        if (cellX < originX || cellX > originX + EXIT_REGION_CELLS || cellZ < originZ || cellZ > originZ + EXIT_REGION_CELLS) {
            return null;
        }
        return new int[] {originX, originZ};
    }

    public static boolean withinExitRegion(long seed, int cellX, int cellZ) {
        return exitRegionOrigin(seed, cellX, cellZ) != null;
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
