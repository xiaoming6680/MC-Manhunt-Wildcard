package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.WorldTiltClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rolls the camera so the World Tilt gravity vector points straight down on screen, whatever way the player
 * is looking. With normal gravity the roll is zero and vanilla behaviour is untouched.
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
    private void hunterwildcard$alignWithGravity(float yaw, float pitch, CallbackInfo ci) {
        if (WorldTiltClient.currentBlend() <= 0.0F) {
            return;
        }
        Vec3d gravity = WorldTiltClient.currentGravity();
        // Express gravity in the un-rolled camera frame, then roll so it points to screen -Y.
        Vector3f local = new Vector3f((float) gravity.x, (float) gravity.y, (float) gravity.z);
        rotation.conjugate(new Quaternionf()).transform(local);
        float roll = (float) Math.atan2(local.x, -local.y);
        if (Math.abs(roll) < 1.0E-4F) {
            return;
        }
        rotation.rotationYXZ(-yaw * DEGREES_TO_RADIANS, pitch * DEGREES_TO_RADIANS, roll);
        horizontalPlane.set(0.0F, 0.0F, 1.0F).rotate(rotation);
        verticalPlane.set(0.0F, 1.0F, 0.0F).rotate(rotation);
        diagonalPlane.set(1.0F, 0.0F, 0.0F).rotate(rotation);
    }
}
