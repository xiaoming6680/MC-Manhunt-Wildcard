package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.WorldTiltClient;
import net.minecraft.client.render.Camera;
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
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    @Final
    private Vector3f horizontalPlane;

    @Shadow
    @Final
    private Vector3f verticalPlane;

    @Shadow
    @Final
    private Vector3f diagonalPlane;

    @Inject(method = "setRotation(FF)V", at = @At("TAIL"))
    private void hunterwildcard$useGravityFrame(float yaw, float pitch, CallbackInfo ci) {
        if (!WorldTiltClient.isFrameActive()) {
            return;
        }
        rotation.set(WorldTiltClient.frame())
                .rotateYXZ(-WorldTiltClient.localYaw() * DEGREES_TO_RADIANS, WorldTiltClient.localPitch() * DEGREES_TO_RADIANS, 0.0F);
        horizontalPlane.set(0.0F, 0.0F, 1.0F).rotate(rotation);
        verticalPlane.set(0.0F, 1.0F, 0.0F).rotate(rotation);
        diagonalPlane.set(1.0F, 0.0F, 0.0F).rotate(rotation);
    }
}
