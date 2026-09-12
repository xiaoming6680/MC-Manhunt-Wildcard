package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Breaking a block also clears the 3x3x3 volume in front of it along the player's look direction
 * (the broken block's layer plus two more), with normal drops. Only blocks the held tool can actually
 * harvest are taken, and anything unbreakable or with a block entity (chests, spawners...) is left alone.
 */
public class ChainMiningRule implements WildcardRule {
    private static final int DEPTH = 3;

    @Override
    public void onBlockBroken(GameContext context, ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
        Direction facing = Direction.getFacing(player.getRotationVec(1.0F));
        BlockPos center = pos.offset(facing);
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
        BlockPos forward = center.offset(facing, depth);
        return switch (axis) {
            case X -> forward.add(0, a, b);
            case Y -> forward.add(a, 0, b);
            case Z -> forward.add(a, b, 0);
        };
    }

    private static void breakIfLegal(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() && state.getCollisionShape(world, pos).isEmpty()) {
            return;
        }
        if (state.getHardness(world, pos) < 0.0F || state.hasBlockEntity()) {
            return;
        }
        if (!player.canHarvest(state) || world.isOutOfHeightLimit(pos)) {
            return;
        }
        if (!player.isCreative()) {
            Block.dropStacks(state, world, pos, null, player, player.getMainHandStack());
        }
        world.breakBlock(pos, false, player, 512);
    }
}
