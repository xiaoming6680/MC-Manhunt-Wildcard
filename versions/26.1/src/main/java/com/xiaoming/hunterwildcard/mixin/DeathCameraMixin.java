package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.respawn.DeathCameraAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class DeathCameraMixin implements DeathCameraAccess {
    @Shadow private Entity camera;

    @Override public void hunterwildcard$setDeathCamera(Entity target) { camera = target; }

    @Inject(method = "tick", at = @At("HEAD"))
    private void hunterwildcard$validateCameraBeforeFollowing(CallbackInfo ci) {
        GameManager.getInstance().beforeDeathSpectatorTick((ServerPlayer)(Object)this);
    }

    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"))
    private void hunterwildcard$detachBeforeTargetTeleport(TeleportTransition target, CallbackInfoReturnable<ServerPlayer> cir) {
        GameManager.getInstance().beforeDeathTargetTeleport((ServerPlayer)(Object)this);
    }
}
