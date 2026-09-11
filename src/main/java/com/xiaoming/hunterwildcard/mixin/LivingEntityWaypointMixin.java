package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.waypoint.ServerWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * The vanilla locator bar asks every waypoint source for a tracker per receiver. Returning no tracker
 * hides the source from that receiver, which is how a round limits the bar to your own side.
 */
@Mixin(LivingEntity.class)
public class LivingEntityWaypointMixin {
    @Inject(method = "createTracker", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$hideOtherSide(ServerPlayerEntity receiver, CallbackInfoReturnable<Optional<ServerWaypoint.WaypointTracker>> cir) {
        LivingEntity source = (LivingEntity) (Object) this;
        if (!GameManager.getInstance().canSeeWaypoint(receiver, source)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
