package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.WorldTiltClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Jumping pushes the local player away from the tilted ground rather than straight up. */
@Mixin(LivingEntity.class)
public abstract class TiltJumpMixin {
    @Shadow
    protected abstract float getJumpVelocity();

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$jumpInGravityFrame(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (WorldTiltClient.isLocalPlayer(self) && WorldTiltClient.isFrameActive()) {
            self.setVelocity(WorldTiltClient.jumpVelocity(self.getVelocity(), getJumpVelocity(), self.isSprinting()));
            self.velocityDirty = true;
            ci.cancel();
        }
    }
}
