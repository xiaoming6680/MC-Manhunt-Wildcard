package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Breaking a block also clears the 3x3x3 volume in front of it along the player's look direction
 * (the broken block's layer plus two more), with normal drops. Only blocks the held tool can actually
 * harvest are taken, and anything unbreakable or with a block entity (chests, spawners...) is left alone.
 */
public class ChainMiningRule implements WildcardRule {
    private static final int DEPTH = 3;

    @Override
    public void onBlockBroken(GameContext context, ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
        Direction facing = Direction.getApproximateNearest(player.getViewVector(1.0F));
        BlockPos center = pos.relative(facing);
        Direction.Axis axis = facing.getAxis();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                for (int depth = -1; depth < DEPTH - 1; depth++) {
                    BlockPos target = offset(center, axis, facing, a, b, depth);
                    if (target.equals(pos)) {
                        continue;
                    }
                    breakIfLegal(player, world, target);
                }
            }
        }
    }

    private static BlockPos offset(BlockPos center, Direction.Axis axis, Direction facing, int a, int b, int depth) {
        BlockPos forward = center.relative(facing, depth);
        return switch (axis) {
            case X -> forward.offset(0, a, b);
            case Y -> forward.offset(a, 0, b);
            case Z -> forward.offset(a, b, 0);
        };
    }

    private static void breakIfLegal(ServerPlayer player, ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() && state.getCollisionShape(world, pos).isEmpty()) {
            return;
        }
        if (state.getDestroySpeed(world, pos) < 0.0F || state.hasBlockEntity()) {
            return;
        }
        if (!player.hasCorrectToolForDrops(state) || world.isOutsideBuildHeight(pos)) {
            return;
        }
        if (!player.isCreative()) {
            Block.dropResources(state, world, pos, null, player, player.getMainHandItem());
        }
        world.destroyBlock(pos, false, player, 512);
    }
}
