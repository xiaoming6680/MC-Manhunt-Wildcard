package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.*;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.data.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(PlayerEntity.class)
public abstract class TiltPlayerFrameMixin implements TiltTrackedGravity {
    @Inject(method="travel",at=@At("HEAD"))
    private void hunterwildcard$swimSteering(net.minecraft.util.math.Vec3d input,CallbackInfo ci) {
        PlayerEntity player=(PlayerEntity)(Object)this;
        if(!TiltFrame.active(player)||player.hasVehicle()||!player.isSwimming())return;
        var d=TiltFrame.direction(player);
        double lookUp=TiltFrame.local(d,player.getRotationVector()).y;
        double response=lookUp<-.2?.085:.06;
        var ahead=player.getPos().add(TiltFrame.world(d,new net.minecraft.util.math.Vec3d(0,.9,0)));
        if(lookUp<=0||((LivingEntityJumpAccessor)player).hunterwildcard$isJumping()||!player.getEntityWorld().getFluidState(net.minecraft.util.math.BlockPos.ofFloored(ahead)).isEmpty()) {
            var velocity=TiltFrame.local(d,player.getVelocity());
            player.setVelocity(TiltFrame.world(d,velocity.add(0,(lookUp-velocity.y)*response,0)));
        }
    }
    @WrapOperation(method="travel",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerEntity;isSwimming()Z"))
    private boolean hunterwildcard$skipWorldVerticalSwim(PlayerEntity player,Operation<Boolean> original) {
        return !TiltFrame.active(player)&&original.call(player);
    }
    @Unique private static final TrackedData<Integer> HUNTERWILDCARD_GRAVITY=
            DataTracker.registerData(PlayerEntity.class,TrackedDataHandlerRegistry.INTEGER);
    @Inject(method="initDataTracker",at=@At("TAIL"))
    private void hunterwildcard$track(DataTracker.Builder builder,CallbackInfo ci) {
        builder.add(HUNTERWILDCARD_GRAVITY,Direction.DOWN.ordinal());
    }
    @Override public Direction hunterwildcard$gravity() {
        var tracker=((PlayerEntity)(Object)this).getDataTracker();
        return tracker==null ? Direction.DOWN : Direction.values()[tracker.get(HUNTERWILDCARD_GRAVITY)];
    }
    @Override public void hunterwildcard$gravity(Direction direction) {
        ((PlayerEntity)(Object)this).getDataTracker().set(HUNTERWILDCARD_GRAVITY,direction.ordinal());
    }
    @Inject(method="canChangeIntoPose",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$poseSpace(EntityPose pose,CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player=(PlayerEntity)(Object)this;
        if(TiltFrame.active(player)) cir.setReturnValue(player.getEntityWorld().isSpaceEmpty(player,
                TiltFrame.box(player,player.getDimensions(pose),player.getPos()).contract(1.0E-7)));
    }
    @Inject(method="adjustMovementForSneaking",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$sneakEdges(net.minecraft.util.math.Vec3d movement,net.minecraft.entity.MovementType type,CallbackInfoReturnable<net.minecraft.util.math.Vec3d> cir) {
        PlayerEntity player=(PlayerEntity)(Object)this;
        if(!TiltFrame.active(player))return;
        cir.setReturnValue(movement);
        if(!player.isSneaking()||!player.isOnGround()||player.getAbilities().flying)return;
        var d=TiltFrame.direction(player);
        var local=TiltFrame.local(d,movement);
        double x=local.x,z=local.z,step=player.getStepHeight();
        while(x!=0&&player.getEntityWorld().isSpaceEmpty(player,player.getBoundingBox().offset(TiltFrame.world(d,new net.minecraft.util.math.Vec3d(x,-step,0)))))x=Math.abs(x)<.05?0:x-Math.copySign(.05,x);
        while(z!=0&&player.getEntityWorld().isSpaceEmpty(player,player.getBoundingBox().offset(TiltFrame.world(d,new net.minecraft.util.math.Vec3d(0,-step,z)))))z=Math.abs(z)<.05?0:z-Math.copySign(.05,z);
        while(x!=0&&z!=0&&player.getEntityWorld().isSpaceEmpty(player,player.getBoundingBox().offset(TiltFrame.world(d,new net.minecraft.util.math.Vec3d(x,-step,z))))) {
            x=Math.abs(x)<.05?0:x-Math.copySign(.05,x);z=Math.abs(z)<.05?0:z-Math.copySign(.05,z);
        }
        cir.setReturnValue(TiltFrame.world(d,new net.minecraft.util.math.Vec3d(x,local.y,z)));
    }
}
