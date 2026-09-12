package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InGameOverlayRenderer.class)
public abstract class TiltOverlayMixin {
    @Inject(method="getInWallBlockState",at=@At("HEAD"),cancellable=true)
    private static void hunterwildcard$eyeSamples(PlayerEntity player,CallbackInfoReturnable<BlockState> cir) {
        if(!TiltFrame.active(player))return;
        cir.setReturnValue(null);
        for(int i=0;i<8;i++) {
            var offset=new Vec3d(((i&1)-.5)*player.getWidth()*.8,
                    (((i>>1)&1)-.5)*.1*player.getScale(),(((i>>2)&1)-.5)*player.getWidth()*.8);
            var pos=BlockPos.ofFloored(player.getEyePos().add(TiltFrame.world(TiltFrame.direction(player),offset)));
            var state=player.getWorld().getBlockState(pos);
            if(state.getRenderType()!=BlockRenderType.INVISIBLE && state.shouldBlockVision(player.getWorld(),pos)) {
                cir.setReturnValue(state);return;
            }
        }
    }
}
