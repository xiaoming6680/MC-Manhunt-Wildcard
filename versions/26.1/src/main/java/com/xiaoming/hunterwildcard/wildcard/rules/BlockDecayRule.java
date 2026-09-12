package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BlockDecayRule implements WildcardRule {
    private final List<DecayEntry> entries = new ArrayList<>();
    private int ticks;

    @Override
    public void onStart(GameContext context) {
        entries.clear();
        ticks = 0;
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        Iterator<DecayEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            DecayEntry entry = iterator.next();
            if (ticks < entry.expireTick()) {
                continue;
            }

            ServerLevel world = context.getServer().getLevel(entry.worldKey());
            if (world != null && world.getBlockState(entry.pos()).equals(entry.state())) {
                world.removeBlock(entry.pos(), false);
            }
            iterator.remove();
        }
    }

    @Override
    public void onBlockPlaced(GameContext context, ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
        if (isExcluded(state)) {
            return;
        }

        entries.add(new DecayEntry(world.dimension(), pos.immutable(), state, ticks + context.getConfig().getBlockDecayTicks()));
    }

    @Override
    public void onStop(GameContext context) {
        entries.clear();
    }

    private boolean isExcluded(BlockState state) {
        Block block = state.getBlock();
        return state.hasBlockEntity()
                || state.is(Blocks.BEDROCK)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.FURNACE)
                || state.is(Blocks.BLAST_FURNACE)
                || block instanceof ChestBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof BedBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof CraftingTableBlock;
    }

    private record DecayEntry(ResourceKey<Level> worldKey, BlockPos pos, BlockState state, int expireTick) {
    }
}
