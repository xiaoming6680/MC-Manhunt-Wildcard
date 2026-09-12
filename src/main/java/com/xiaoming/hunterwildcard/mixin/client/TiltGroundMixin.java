package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.WorldTiltClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "On ground" for the local player means "pressed against something in the direction gravity pulls":
 * the move was cut short along the gravity vector. That is what lets jumping, friction and sprinting work
 * while standing on a wall.
 */
@Mixin(Entity.class)
public abstract class TiltGroundMixin {
    @Unique
    private Vec3d hunterwildcard$moveStart;

    @Inject(method = "move", at = @At("HEAD"))
    private void hunterwildcard$rememberStart(MovementType type, Vec3d movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        hunterwildcard$moveStart = type == MovementType.SELF && WorldTiltClient.isLocalPlayer(self) && WorldTiltClient.isFrameActive()
                ? self.getEntityPos()
                : null;
    }

    @Inject(method = "move", at = @At("TAIL"))
    private void hunterwildcard$groundAlongGravity(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (hunterwildcard$moveStart == null) {
            return;
        }
        Entity self = (Entity) (Object) this;
        Vec3d gravity = WorldTiltClient.currentGravity();
        double intended = movement.dotProduct(gravity);
        double actual = self.getEntityPos().subtract(hunterwildcard$moveStart).dotProduct(gravity);
        hunterwildcard$moveStart = null;
        self.setOnGround(intended > 1.0E-4 && actual < intended - 1.0E-4);
        self.fallDistance = 0.0F;
    }
}
