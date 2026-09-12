package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
public abstract class TiltOverlayMixin {
    @Inject(method="getViewBlockingState",at=@At("HEAD"),cancellable=true)
    private static void hunterwildcard$eyeSamples(Player player,CallbackInfoReturnable<BlockState> cir) {
        if(!TiltFrame.active(player))return;
        cir.setReturnValue(null);
        for(int i=0;i<8;i++) {
            var offset=new Vec3(((i&1)-.5)*player.getBbWidth()*.8,
                    (((i>>1)&1)-.5)*.1*player.getScale(),(((i>>2)&1)-.5)*player.getBbWidth()*.8);
            var pos=BlockPos.containing(player.getEyePosition().add(TiltFrame.world(TiltFrame.direction(player),offset)));
            var state=player.level().getBlockState(pos);
            if(state.getRenderShape()!=RenderShape.INVISIBLE && state.isViewBlocking(player.level(),pos)) {
                cir.setReturnValue(state);return;
            }
        }
    }
}
