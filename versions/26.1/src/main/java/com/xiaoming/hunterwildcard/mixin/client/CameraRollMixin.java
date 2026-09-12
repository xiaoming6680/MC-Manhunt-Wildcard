package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.WorldTiltClient;
import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While World Tilt is active the camera is built from the player's local yaw/pitch inside the gravity frame,
 * so the horizon follows the new "down" and the mouse behaves exactly as usual relative to it.
 */
@Mixin(Camera.class)
public abstract class CameraRollMixin {
    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    @Final
    private Vector3f forwards;

    @Shadow
    @Final
    private Vector3f up;

    @Shadow
    @Final
    private Vector3f left;

    @Inject(method = "setRotation(FF)V", at = @At("TAIL"))
    private void hunterwildcard$useGravityFrame(float yaw, float pitch, CallbackInfo ci) {
        if (!TiltFrame.active(((Camera)(Object)this).entity())) {
            return;
        }
        boolean inverse = Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - ((Camera)(Object)this).entity().getYRot())) > 90.0F;
        var entity=((Camera)(Object)this).entity();
        float[] angles=TiltFrame.localAngles(entity);
        rotation.set(TiltFrame.rotation(entity)).rotateYXZ((float)Math.PI-(angles[0]+(inverse?180:0))*((float)Math.PI/180),-angles[1]*(inverse?-1:1)*((float)Math.PI/180),0);
        forwards.set(0.0F, 0.0F, -1.0F).rotate(rotation);
        up.set(0.0F, 1.0F, 0.0F).rotate(rotation);
        left.set(-1.0F, 0.0F, 0.0F).rotate(rotation);
    }
    @org.spongepowered.asm.mixin.injection.ModifyArgs(method="alignWithEntity",at=@At(value="INVOKE",target="Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void hunterwildcard$eyeOrigin(org.spongepowered.asm.mixin.injection.invoke.arg.Args args,
            float tick) {
        var entity = ((Camera)(Object)this).entity();
        if(!TiltFrame.active(entity))return;
        var eye=entity.getEyePosition(tick);
        args.set(0,eye.x);args.set(1,eye.y);args.set(2,eye.z);
    }
}
