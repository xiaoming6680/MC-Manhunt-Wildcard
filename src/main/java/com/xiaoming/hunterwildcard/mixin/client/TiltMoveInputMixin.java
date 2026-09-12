package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.WorldTiltClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** WASD for the local player moves along the tilted ground instead of the world's horizontal plane. */
@Mixin(Entity.class)
public abstract class TiltMoveInputMixin {
    @Inject(method = "updateVelocity", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$moveInGravityFrame(float speed, Vec3d movementInput, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (WorldTiltClient.isLocalPlayer(self) && WorldTiltClient.isFrameActive()) {
            self.setVelocity(self.getVelocity().add(WorldTiltClient.movementDelta(speed, movementInput)));
            ci.cancel();
        }
    }
}
