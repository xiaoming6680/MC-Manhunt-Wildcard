package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemPlaceMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void hunterwildcard$afterPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()
                || !(context.getLevel() instanceof ServerLevel world)
                || !(context.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        if (!state.isAir()) {
            GameManager.getInstance().handleBlockPlaced(player, world, pos, state);
        }
    }
}
