package com.xiaoming.hunterwildcard.mixin.client;
import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Older renderers read the entity directly; restore its world-space angles after drawing. */
@Mixin(LivingEntityRenderer.class)
public abstract class TiltModelMixin {
    @Unique private float[] hunterwildcard$savedAngles;
    @Inject(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at=@At("HEAD"))
    private void hunterwildcard$localPose(LivingEntity entity, float yaw, float tick, MatrixStack matrices, VertexConsumerProvider vertices, int light, CallbackInfo ci) {
        hunterwildcard$savedAngles = null;
        if (!TiltFrame.active(entity)) return;
        hunterwildcard$savedAngles = new float[]{entity.bodyYaw, entity.prevBodyYaw, entity.headYaw, entity.prevHeadYaw, entity.getPitch(), entity.prevPitch};
        float[] angles = TiltFrame.localAngles(entity);
        entity.bodyYaw = entity.prevBodyYaw = entity.headYaw = entity.prevHeadYaw = angles[0];
        entity.setPitch(angles[1]); entity.prevPitch = angles[1];
    }
    @Inject(method="setupTransforms", at=@At("HEAD"))
    private void hunterwildcard$rotate(LivingEntity entity, MatrixStack matrices, float animation, float bodyYaw, float tick, float scale, CallbackInfo ci) {
        if (TiltFrame.active(entity)) matrices.multiply(TiltFrame.rotation(entity));
    }
    @Inject(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at=@At("RETURN"))
    private void hunterwildcard$restorePose(LivingEntity entity, float yaw, float tick, MatrixStack matrices, VertexConsumerProvider vertices, int light, CallbackInfo ci) {
        float[] saved = hunterwildcard$savedAngles;
        if (saved == null) return;
        entity.bodyYaw=saved[0]; entity.prevBodyYaw=saved[1]; entity.headYaw=saved[2]; entity.prevHeadYaw=saved[3];
        entity.setPitch(saved[4]); entity.prevPitch=saved[5]; hunterwildcard$savedAngles=null;
    }
}