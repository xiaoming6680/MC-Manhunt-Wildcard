package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.BackroomsClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sinks the camera through the floor on the way into the Backrooms; the body never moves. */
@Mixin(Camera.class)
public abstract class CameraSinkMixin {
    @Shadow
    protected abstract void setPos(Vec3d pos);

    @Shadow
    public abstract Vec3d getCameraPos();

    @Inject(method = "update", at = @At("TAIL"))
    private void hunterwildcard$applyBackroomsOffset(BlockView world, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        double offset = BackroomsClient.cameraOffset(tickProgress);
        if (offset != 0.0D) {
            setPos(getCameraPos().add(0.0D, offset, 0.0D));
        }
    }
}
