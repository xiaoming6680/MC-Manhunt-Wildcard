package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.WorldTiltClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mouse look for the local player goes through the World Tilt gravity frame while it is active. */
@Mixin(Entity.class)
public class TiltLookMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$lookInGravityFrame(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (WorldTiltClient.isLocalPlayer(self) && WorldTiltClient.isFrameActive()) {
            WorldTiltClient.handleMouse(self, cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
