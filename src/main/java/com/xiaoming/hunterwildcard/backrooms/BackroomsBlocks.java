package com.xiaoming.hunterwildcard.backrooms;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.Identifier;

import java.util.function.Function;

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

    private static AbstractBlock.Settings settings(int luminance) {
        return AbstractBlock.Settings.create().strength(-1.0F, 3_600_000.0F).dropsNothing().luminance(state -> luminance);
    }

    private static Block registerSurface(String path, boolean solid, boolean wall) {
        AbstractBlock.Settings settings = AbstractBlock.Settings.create().strength(-1.0F, 3_600_000.0F).dropsNothing();
        if (!solid) {
            // noCollision needs solid() beside it, or the false floor stops occluding and leaks light,
            // which is exactly the tell it must not have.
            settings = settings.noCollision().solid();
        }
        if (!wall) {
            settings = settings.sounds(BlockSoundGroup.WOOL);
        }
        return register(path, settings, wall ? WallBlock::new : Block::new);
    }

    private static <T extends Block> T register(String path, AbstractBlock.Settings settings, Function<AbstractBlock.Settings, T> factory) {
        RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(HunterWildcardMod.MOD_ID, path));
        T block = factory.apply(settings.registryKey(key));
        return Registry.register(Registries.BLOCK, key, block);
    }

    /** Wallpaper with skirting (band 0) and ceiling trim (band 2). */
    public static final class WallBlock extends Block {
        public static final IntProperty BAND = IntProperty.of("band", 0, 2);

        public WallBlock(AbstractBlock.Settings settings) {
            super(settings);
            setDefaultState(stateManager.getDefaultState().with(BAND, 1));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
            builder.add(BAND);
        }
    }
}
