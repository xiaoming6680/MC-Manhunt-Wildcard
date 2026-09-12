package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.*;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(Player.class)
public abstract class TiltPlayerFrameMixin implements TiltTrackedGravity {
    @Inject(method="travel",at=@At("HEAD"))
    private void hunterwildcard$swimSteering(net.minecraft.world.phys.Vec3 input,CallbackInfo ci) {
        Player player=(Player)(Object)this;
        if(!TiltFrame.active(player)||player.isPassenger()||!player.isSwimming())return;
        var d=TiltFrame.direction(player);
        double lookUp=TiltFrame.local(d,player.getLookAngle()).y;
        double response=lookUp<-.2?.085:.06;
        var ahead=player.position().add(TiltFrame.world(d,new net.minecraft.world.phys.Vec3(0,.9,0)));
        if(lookUp<=0||player.isJumping()||!player.level().getFluidState(net.minecraft.core.BlockPos.containing(ahead)).isEmpty()) {
            var velocity=TiltFrame.local(d,player.getDeltaMovement());
            player.setDeltaMovement(TiltFrame.world(d,velocity.add(0,(lookUp-velocity.y)*response,0)));
        }
    }
    @WrapOperation(method="travel",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;isSwimming()Z"))
    private boolean hunterwildcard$skipWorldVerticalSwim(Player player,Operation<Boolean> original) {
        return !TiltFrame.active(player)&&original.call(player);
    }
    @Unique private static final EntityDataAccessor<Integer> HUNTERWILDCARD_GRAVITY=
            SynchedEntityData.defineId(Player.class,EntityDataSerializers.INT);
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void hunterwildcard$track(SynchedEntityData.Builder builder,CallbackInfo ci) {
        builder.define(HUNTERWILDCARD_GRAVITY,Direction.DOWN.ordinal());
    }
    @Override public Direction hunterwildcard$gravity() {
        var tracker=((Player)(Object)this).getEntityData();
        return tracker==null ? Direction.DOWN : Direction.values()[tracker.get(HUNTERWILDCARD_GRAVITY)];
    }
    @Override public void hunterwildcard$gravity(Direction direction) {
        ((Player)(Object)this).getEntityData().set(HUNTERWILDCARD_GRAVITY,direction.ordinal());
    }
    @Inject(method="canPlayerFitWithinBlocksAndEntitiesWhen",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$poseSpace(Pose pose,CallbackInfoReturnable<Boolean> cir) {
        Player player=(Player)(Object)this;
        if(TiltFrame.active(player)) cir.setReturnValue(player.level().noCollision(player,
                TiltFrame.box(player,player.getDimensions(pose),player.position()).deflate(1.0E-7)));
    }
    @Inject(method="maybeBackOffFromEdge",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$sneakEdges(net.minecraft.world.phys.Vec3 movement,net.minecraft.world.entity.MoverType type,CallbackInfoReturnable<net.minecraft.world.phys.Vec3> cir) {
        Player player=(Player)(Object)this;
        if(!TiltFrame.active(player))return;
        cir.setReturnValue(movement);
        if(!player.isShiftKeyDown()||!player.onGround()||player.getAbilities().flying)return;
        var d=TiltFrame.direction(player);
        var local=TiltFrame.local(d,movement);
        double x=local.x,z=local.z,step=player.maxUpStep();
        while(x!=0&&player.level().noCollision(player,player.getBoundingBox().move(TiltFrame.world(d,new net.minecraft.world.phys.Vec3(x,-step,0)))))x=Math.abs(x)<.05?0:x-Math.copySign(.05,x);
        while(z!=0&&player.level().noCollision(player,player.getBoundingBox().move(TiltFrame.world(d,new net.minecraft.world.phys.Vec3(0,-step,z)))))z=Math.abs(z)<.05?0:z-Math.copySign(.05,z);
        while(x!=0&&z!=0&&player.level().noCollision(player,player.getBoundingBox().move(TiltFrame.world(d,new net.minecraft.world.phys.Vec3(x,-step,z))))) {
            x=Math.abs(x)<.05?0:x-Math.copySign(.05,x);z=Math.abs(z)<.05?0:z-Math.copySign(.05,z);
        }
        cir.setReturnValue(TiltFrame.world(d,new net.minecraft.world.phys.Vec3(x,local.y,z)));
    }
}
