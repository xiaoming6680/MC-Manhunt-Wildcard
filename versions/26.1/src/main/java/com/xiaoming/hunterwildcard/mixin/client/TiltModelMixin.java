package com.xiaoming.hunterwildcard.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Each player's replicated gravity rotates their model around the same feet origin as their collision box.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class TiltModelMixin {
    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void hunterwildcard$tiltPlayerModel(LivingEntityRenderState state, PoseStack matrices, float bodyYaw, float scale, CallbackInfo ci) {
        var world=net.minecraft.client.Minecraft.getInstance().level;
        if(world==null||!(state instanceof AvatarRenderState player))return;
        var entity=world.getEntity(player.id);
        if(entity==null||!com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.active(entity))return;
        matrices.mulPose(com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.rotation(entity));
    }
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
    private void hunterwildcard$localPose(net.minecraft.world.entity.LivingEntity entity,LivingEntityRenderState state,float tick,CallbackInfo ci) {
        if(!com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.active(entity))return;
        float[] angles=com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.localAngles(entity);
        state.bodyRot=angles[0];state.yRot=0;state.xRot=angles[1];
    }
}
