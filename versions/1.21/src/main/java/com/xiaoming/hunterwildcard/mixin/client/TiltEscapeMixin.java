package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla's four upright sample points can lie inside the wall underneath rotated feet. */
@Mixin(ClientPlayerEntity.class)
public abstract class TiltEscapeMixin {
    @Inject(method="pushOutOfBlocks",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$escape(double x,double z,CallbackInfo ci) {
        ClientPlayerEntity player=(ClientPlayerEntity)(Object)this;
        if(!TiltFrame.active(player))return;
        ci.cancel();
        var body=player.getBoundingBox().contract(1.0E-7);
        if(player.getEntityWorld().isSpaceEmpty(player,body))return;
        for(double distance=.1;distance<=1.01;distance+=.1)for(Direction direction:Direction.values()) {
            var axis=TiltFrame.vector(direction);
            if(player.getEntityWorld().isSpaceEmpty(player,body.offset(axis.multiply(distance)))) {
                var velocity=player.getVelocity();
                player.setVelocity(velocity.subtract(axis.multiply(velocity.dotProduct(axis))).add(axis.multiply(.1)));
                return;
            }
        }
    }
}
