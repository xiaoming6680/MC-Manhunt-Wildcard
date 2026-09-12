package com.xiaoming.hunterwildcard.mixin.client;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Each player's replicated gravity rotates their model around the same feet origin as their collision box.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class TiltModelMixin {
    @Inject(method = "setupTransforms", at = @At("HEAD"))
    private void hunterwildcard$tiltPlayerModel(LivingEntityRenderState state, MatrixStack matrices, float bodyYaw, float scale, CallbackInfo ci) {
        var world=net.minecraft.client.MinecraftClient.getInstance().world;
        if(world==null||!(state instanceof PlayerEntityRenderState player))return;
        var entity=world.getEntityById(player.id);
        if(entity==null||!com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.active(entity))return;
        matrices.multiply(com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.rotation(entity));
    }
    @Inject(method="updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
    private void hunterwildcard$localPose(net.minecraft.entity.LivingEntity entity,LivingEntityRenderState state,float tick,CallbackInfo ci) {
        if(!com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.active(entity))return;
        float[] angles=com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.localAngles(entity);
        state.bodyYaw=angles[0];state.relativeHeadYaw=0;state.pitch=angles[1];
    }
}
