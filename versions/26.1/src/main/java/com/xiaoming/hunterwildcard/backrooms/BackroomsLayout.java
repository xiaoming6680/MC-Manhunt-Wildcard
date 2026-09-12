package com.xiaoming.hunterwildcard.backrooms;

/**
 * Fixed vertical geometry of the Backrooms dimension and the cell grid its maze is cut from.
 * Ported from The Fourth Frequency's "unrendered layer".
 * <p>
 * The floor sits on the very bottom of the world on purpose: a hole in it is a hole in the world,
 * so falling through one needs no trigger block. {@link #FLOOR_Y} must stay at {@link #MIN_Y}.
 */
public final class BackroomsLayout {
    public static final long DEFAULT_SEED = 0x4241_434B_524F_4F4DL;

    /** Multiple of sixteen, as every dimension height must be. Only y=0..5 is ever written. */
    public static final int WORLD_HEIGHT = 16;
    public static final int MIN_Y = 0;
    public static final int FLOOR_Y = 0;
    public static final int CEILING_Y = 5;
    public static final int INTERIOR_BOTTOM_Y = FLOOR_Y + 1;
    public static final int INTERIOR_TOP_Y = CEILING_Y - 1;

    /** Blocks per maze cell: one wall line plus a four-block gap. */
    public static final int CELL_SIZE = 5;
    public static final int LIGHT_LOCAL_X = 2;
    public static final int LIGHT_LOCAL_Z = 2;

    private BackroomsLayout() {
    }

    public static int cell(int blockCoordinate) {
        return Math.floorDiv(blockCoordinate, CELL_SIZE);
    }

    public static int local(int blockCoordinate) {
        return Math.floorMod(blockCoordinate, CELL_SIZE);
    }

    /** Lowest y a player can occupy while still counting as inside the layer. */
    public static int voidThreshold() {
        return MIN_Y - 8;
    }
}
