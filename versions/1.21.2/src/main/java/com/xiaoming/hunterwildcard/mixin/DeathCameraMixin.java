package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.respawn.DeathCameraAccess;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.TeleportTarget;

@Mixin(ServerPlayerEntity.class)
public abstract class DeathCameraMixin implements DeathCameraAccess {
    @Shadow private Entity cameraEntity;

    @Override public void hunterwildcard$setDeathCamera(Entity target) { cameraEntity = target; }

    @Inject(method = "tick", at = @At("HEAD"))
    private void hunterwildcard$validateCameraBeforeFollowing(CallbackInfo ci) {
        GameManager.getInstance().beforeDeathSpectatorTick((ServerPlayerEntity)(Object)this);
    }

    @Inject(method = "teleportTo(Lnet/minecraft/world/TeleportTarget;)Lnet/minecraft/server/network/ServerPlayerEntity;", at = @At("HEAD"))
    private void hunterwildcard$detachBeforeTargetTeleport(TeleportTarget target, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        GameManager.getInstance().beforeDeathTargetTeleport((ServerPlayerEntity)(Object)this);
    }
}
