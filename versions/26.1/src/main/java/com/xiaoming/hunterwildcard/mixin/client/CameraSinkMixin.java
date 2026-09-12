package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.BackroomsClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sinks the camera through the floor on the way into the Backrooms; the body never moves. */
@Mixin(Camera.class)
public abstract class CameraSinkMixin {
    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    public abstract Vec3 position();

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void hunterwildcard$applyBackroomsOffset(float tickProgress, CallbackInfo ci) {
        double offset = BackroomsClient.cameraOffset(tickProgress);
        if (offset != 0.0D) {
            setPosition(position().add(0.0D, offset, 0.0D));
        }
    }
}
