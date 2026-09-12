package com.xiaoming.hunterwildcard.backrooms;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Indestructible surfaces of the Backrooms. The false floor looks like floor but has no collision. */
public final class BackroomsBlocks {
    public static final Block WALL = registerSurface("backrooms_wall", true, true);
    public static final Block FLOOR = registerSurface("backrooms_floor", true, false);
    public static final Block FALSE_FLOOR = registerSurface("backrooms_false_floor", false, false);
    public static final Block CEILING = register("backrooms_ceiling", settings(0), Block::new);
    public static final Block LIGHT = register("backrooms_light", settings(15), Block::new);
    public static final Block LIGHT_OFF = register("backrooms_light_off", settings(0), Block::new);

    private BackroomsBlocks() {
    }

    /** Touching the class registers the blocks; call once from mod init. */
    public static void register() {
        HunterWildcardMod.LOGGER.info("Backrooms blocks registered.");
    }

    private static BlockBehaviour.Properties settings(int luminance) {
        return BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000.0F).noLootTable().lightLevel(state -> luminance);
    }

    private static Block registerSurface(String path, boolean solid, boolean wall) {
        BlockBehaviour.Properties settings = BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000.0F).noLootTable();
        if (!solid) {
            // noCollision needs solid() beside it, or the false floor stops occluding and leaks light,
            // which is exactly the tell it must not have.
            settings = settings.noCollision().forceSolidOn();
        }
        if (!wall) {
            settings = settings.sound(SoundType.WOOL);
        }
        return register(path, settings, wall ? WallBlock::new : Block::new);
    }

    private static <T extends Block> T register(String path, BlockBehaviour.Properties settings, Function<BlockBehaviour.Properties, T> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(HunterWildcardMod.MOD_ID, path));
        T block = factory.apply(settings.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    /** Wallpaper with skirting (band 0) and ceiling trim (band 2). */
    public static final class WallBlock extends Block {
        public static final IntegerProperty BAND = IntegerProperty.create("band", 0, 2);

        public WallBlock(BlockBehaviour.Properties settings) {
            super(settings);
            registerDefaultState(stateDefinition.any().setValue(BAND, 1));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BAND);
        }
    }
}
