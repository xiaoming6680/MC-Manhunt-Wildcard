package com.xiaoming.hunterwildcard.backrooms;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * Six blocks of fixed geometry per column, no noise, no features, no mobs.
 * A chunk is 1536 writes decided by a few hash lookups and never reads a neighbour.
 */
public final class BackroomsChunkGenerator extends ChunkGenerator {
    public static final MapCodec<BackroomsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
            Codec.LONG.optionalFieldOf("seed", BackroomsLayout.DEFAULT_SEED).forGetter(generator -> generator.seed)
    ).apply(instance, BackroomsChunkGenerator::new));

    private final long seed;

    public BackroomsChunkGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource);
        this.seed = seed;
    }

    public long getSeed() {
        return seed;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState noiseConfig, StructureManager structureAccessor, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int originX = chunkPos.getMinBlockX();
        int originZ = chunkPos.getMinBlockZ();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockState wall = BackroomsBlocks.WALL.defaultBlockState();
        for (int offsetX = 0; offsetX < 16; offsetX++) {
            for (int offsetZ = 0; offsetZ < 16; offsetZ++) {
                int x = originX + offsetX;
                int z = originZ + offsetZ;
                place(chunk, cursor, offsetX, BackroomsLayout.FLOOR_Y, offsetZ, floorState(x, z), oceanFloor, worldSurface);
                if (BackroomsMazePolicy.wall(seed, x, z)) {
                    for (int y = BackroomsLayout.INTERIOR_BOTTOM_Y; y <= BackroomsLayout.INTERIOR_TOP_Y; y++) {
                        place(chunk, cursor, offsetX, y, offsetZ, wallBand(wall, y), oceanFloor, worldSurface);
                    }
                }
                place(chunk, cursor, offsetX, BackroomsLayout.CEILING_Y, offsetZ, ceilingState(x, z), oceanFloor, worldSurface);
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    private static void place(ChunkAccess chunk, BlockPos.MutableBlockPos cursor, int offsetX, int y, int offsetZ, BlockState state, Heightmap oceanFloor, Heightmap worldSurface) {
        chunk.setBlockState(cursor.set(offsetX, y, offsetZ), state);
        oceanFloor.update(offsetX, y, offsetZ, state);
        worldSurface.update(offsetX, y, offsetZ, state);
    }

    @Override
    public void applyCarvers(WorldGenRegion chunkRegion, long seed, RandomState noiseConfig, BiomeManager biomeAccess, StructureManager structureAccessor, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState noiseConfig, ChunkAccess chunk) {
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureManager structureAccessor) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return BackroomsLayout.WORLD_HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return BackroomsLayout.MIN_Y;
    }

    @Override
    public int getMinY() {
        return BackroomsLayout.MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world, RandomState noiseConfig) {
        if (BackroomsMazePolicy.falseFloor(seed, x, z)) {
            return BackroomsLayout.MIN_Y;
        }
        return BackroomsMazePolicy.wall(seed, x, z) ? BackroomsLayout.CEILING_Y + 1 : BackroomsLayout.FLOOR_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState noiseConfig) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState[] column = new BlockState[BackroomsLayout.WORLD_HEIGHT];
        boolean wall = BackroomsMazePolicy.wall(seed, x, z);
        for (int index = 0; index < column.length; index++) {
            int y = BackroomsLayout.MIN_Y + index;
            if (y == BackroomsLayout.FLOOR_Y) {
                column[index] = floorState(x, z);
            } else if (y == BackroomsLayout.CEILING_Y) {
                column[index] = ceilingState(x, z);
            } else if (wall && y >= BackroomsLayout.INTERIOR_BOTTOM_Y && y <= BackroomsLayout.INTERIOR_TOP_Y) {
                column[index] = wallBand(BackroomsBlocks.WALL.defaultBlockState(), y);
            } else {
                column[index] = air;
            }
        }
        return new NoiseColumn(BackroomsLayout.MIN_Y, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> text, RandomState noiseConfig, BlockPos pos) {
        text.add("Backrooms cell " + BackroomsLayout.cell(pos.getX()) + ", " + BackroomsLayout.cell(pos.getZ()));
    }

    private BlockState ceilingState(int x, int z) {
        if (BackroomsMazePolicy.ceilingLight(seed, x, z)) {
            return BackroomsBlocks.LIGHT.defaultBlockState();
        }
        return BackroomsMazePolicy.ceilingFixture(seed, x, z) ? BackroomsBlocks.LIGHT_OFF.defaultBlockState() : BackroomsBlocks.CEILING.defaultBlockState();
    }

    private BlockState floorState(int x, int z) {
        return BackroomsMazePolicy.falseFloor(seed, x, z) ? BackroomsBlocks.FALSE_FLOOR.defaultBlockState() : BackroomsBlocks.FLOOR.defaultBlockState();
    }

    private static BlockState wallBand(BlockState state, int y) {
        int band = y == BackroomsLayout.INTERIOR_BOTTOM_Y ? 0 : y == BackroomsLayout.INTERIOR_TOP_Y ? 2 : 1;
        return state.setValue(BackroomsBlocks.WallBlock.BAND, band);
    }
}
