package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.WorldTiltClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player models stand along the tilted gravity while World Tilt is active. Every participant is tilted at the
 * same time, so the local frame is a fair approximation for everyone; the model pivots around the hitbox
 * centre so it stays roughly where the collision box is.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class TiltModelMixin {
    @Inject(method = "setupTransforms", at = @At("HEAD"))
    private void hunterwildcard$tiltPlayerModel(LivingEntityRenderState state, MatrixStack matrices, float bodyYaw, float scale, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState) || !WorldTiltClient.isFrameActive()) {
            return;
        }
        float pivot = state.height / 2.0F;
        matrices.translate(0.0F, pivot, 0.0F);
        matrices.multiply(WorldTiltClient.frame());
        matrices.translate(0.0F, -pivot, 0.0F);
    }
}
