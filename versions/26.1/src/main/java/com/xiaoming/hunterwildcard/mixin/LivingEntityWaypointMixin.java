package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.WaypointTransmitter;

/**
 * The vanilla locator bar asks every waypoint source for a tracker per receiver. Returning no tracker
 * hides the source from that receiver, which is how a round limits the bar to your own side.
 */
@Mixin(LivingEntity.class)
public class LivingEntityWaypointMixin {
    @Inject(method = "makeWaypointConnectionWith", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$hideOtherSide(ServerPlayer receiver, CallbackInfoReturnable<Optional<WaypointTransmitter.Connection>> cir) {
        LivingEntity source = (LivingEntity) (Object) this;
        if (!GameManager.getInstance().canSeeWaypoint(receiver, source)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
