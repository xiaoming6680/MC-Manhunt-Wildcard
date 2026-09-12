package com.xiaoming.hunterwildcard.backrooms;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int originX = chunkPos.getStartX();
        int originZ = chunkPos.getStartZ();
        Heightmap oceanFloor = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        BlockState wall = BackroomsBlocks.WALL.getDefaultState();
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

    private static void place(Chunk chunk, BlockPos.Mutable cursor, int offsetX, int y, int offsetZ, BlockState state, Heightmap oceanFloor, Heightmap worldSurface) {
        chunk.setBlockState(cursor.set(offsetX, y, offsetZ), state, false);
        oceanFloor.trackUpdate(offsetX, y, offsetZ, state);
        worldSurface.trackUpdate(offsetX, y, offsetZ, state);
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, net.minecraft.world.gen.GenerationStep.Carver carver) {
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
    }

    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
    }

    @Override
    public void populateEntities(ChunkRegion region) {
    }

    @Override
    public int getWorldHeight() {
        return BackroomsLayout.WORLD_HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return BackroomsLayout.MIN_Y;
    }

    @Override
    public int getMinimumY() {
        return BackroomsLayout.MIN_Y;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        if (BackroomsMazePolicy.falseFloor(seed, x, z)) {
            return BackroomsLayout.MIN_Y;
        }
        return BackroomsMazePolicy.wall(seed, x, z) ? BackroomsLayout.CEILING_Y + 1 : BackroomsLayout.FLOOR_Y + 1;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState[] column = new BlockState[BackroomsLayout.WORLD_HEIGHT];
        boolean wall = BackroomsMazePolicy.wall(seed, x, z);
        for (int index = 0; index < column.length; index++) {
            int y = BackroomsLayout.MIN_Y + index;
            if (y == BackroomsLayout.FLOOR_Y) {
                column[index] = floorState(x, z);
            } else if (y == BackroomsLayout.CEILING_Y) {
                column[index] = ceilingState(x, z);
            } else if (wall && y >= BackroomsLayout.INTERIOR_BOTTOM_Y && y <= BackroomsLayout.INTERIOR_TOP_Y) {
                column[index] = wallBand(BackroomsBlocks.WALL.getDefaultState(), y);
            } else {
                column[index] = air;
            }
        }
        return new VerticalBlockSample(BackroomsLayout.MIN_Y, column);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Backrooms cell " + BackroomsLayout.cell(pos.getX()) + ", " + BackroomsLayout.cell(pos.getZ()));
    }

    private BlockState ceilingState(int x, int z) {
        if (BackroomsMazePolicy.ceilingLight(seed, x, z)) {
            return BackroomsBlocks.LIGHT.getDefaultState();
        }
        return BackroomsMazePolicy.ceilingFixture(seed, x, z) ? BackroomsBlocks.LIGHT_OFF.getDefaultState() : BackroomsBlocks.CEILING.getDefaultState();
    }

    private BlockState floorState(int x, int z) {
        return BackroomsMazePolicy.falseFloor(seed, x, z) ? BackroomsBlocks.FALSE_FLOOR.getDefaultState() : BackroomsBlocks.FLOOR.getDefaultState();
    }

    private static BlockState wallBand(BlockState state, int y) {
        int band = y == BackroomsLayout.INTERIOR_BOTTOM_Y ? 0 : y == BackroomsLayout.INTERIOR_TOP_Y ? 2 : 1;
        return state.with(BackroomsBlocks.WallBlock.BAND, band);
    }
}
