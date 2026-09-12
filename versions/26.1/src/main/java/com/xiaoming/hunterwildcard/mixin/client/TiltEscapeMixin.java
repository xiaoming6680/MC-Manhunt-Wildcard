package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla's four upright sample points can lie inside the wall underneath rotated feet. */
@Mixin(LocalPlayer.class)
public abstract class TiltEscapeMixin {
    @Inject(method="moveTowardsClosestSpace",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$escape(double x,double z,CallbackInfo ci) {
        LocalPlayer player=(LocalPlayer)(Object)this;
        if(!TiltFrame.active(player))return;
        ci.cancel();
        var body=player.getBoundingBox().deflate(1.0E-7);
        if(player.level().noCollision(player,body))return;
        for(double distance=.1;distance<=1.01;distance+=.1)for(Direction direction:Direction.values()) {
            var axis=TiltFrame.vector(direction);
            if(player.level().noCollision(player,body.move(axis.scale(distance)))) {
                var velocity=player.getDeltaMovement();
                player.setDeltaMovement(velocity.subtract(axis.scale(velocity.dot(axis))).add(axis.scale(.1)));
                return;
            }
        }
    }
}
