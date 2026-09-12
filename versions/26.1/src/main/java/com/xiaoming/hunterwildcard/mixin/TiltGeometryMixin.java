package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(Entity.class)
public abstract class TiltGeometryMixin {
    @Unique private Vec3 hunterwildcard$moveStart;
    @Inject(method="checkSupportingBlock",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$standingBlock(boolean grounded,Vec3 movement,CallbackInfo ci) {
        Entity self=(Entity)(Object)this;
        if(!TiltFrame.active(self))return;
        ci.cancel();
        self.mainSupportingBlockPos=grounded?TiltFrame.supportingBlock(self):java.util.Optional.empty();
    }
    @WrapOperation(method="isInWall",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/AABB;ofSize(Lnet/minecraft/world/phys/Vec3;DDD)Lnet/minecraft/world/phys/AABB;"))
    private AABB hunterwildcard$headBox(Vec3 center,double x,double y,double z,Operation<AABB> original) {
        Entity self=(Entity)(Object)this;
        if(!TiltFrame.active(self))return original.call(center,x,y,z);
        return TiltFrame.rotateBox(TiltFrame.direction(self),AABB.ofSize(Vec3.ZERO,x,y,z),false).move(center);
    }
    @WrapOperation(method="move",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;updateEntityMovementAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"))
    private void hunterwildcard$deferLanding(net.minecraft.world.level.block.Block block,net.minecraft.world.level.BlockGetter world,Entity entity,Operation<Void> original) {
        // Invoke once below, with gravity-local velocity; horizontal wall bumps aren't landings.
        if(!TiltFrame.active(entity))original.call(block,world,entity);
    }
    @Inject(method="makeBoundingBox(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$box(Vec3 pos,CallbackInfoReturnable<AABB> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) cir.setReturnValue(TiltFrame.box(self,self.getDimensions(self.getPose()),pos));
    }
    @Inject(method="onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V",at=@At("TAIL"))
    private void hunterwildcard$syncBox(EntityDataAccessor<?> data,CallbackInfo ci) {
        Entity self=(Entity)(Object)this;
        if(self instanceof com.xiaoming.hunterwildcard.wildcard.rules.TiltTrackedGravity)
            self.setBoundingBox(TiltFrame.box(self,self.getDimensions(self.getPose()),self.position()));
    }
    @Inject(method="refreshDimensions",at=@At("TAIL"))
    private void hunterwildcard$resizeBox(CallbackInfo ci) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) self.setBoundingBox(TiltFrame.box(self,self.getDimensions(self.getPose()),self.position()));
    }
    @Inject(method="getEyePosition()Lnet/minecraft/world/phys/Vec3;",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$eyes(CallbackInfoReturnable<Vec3> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) cir.setReturnValue(TiltFrame.eye(self,self.position()));
    }
    @Inject(method="getEyeY",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$eyeY(CallbackInfoReturnable<Double> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self))cir.setReturnValue(TiltFrame.eye(self,self.position()).y);
    }
    @Inject(method="getEyePosition(F)Lnet/minecraft/world/phys/Vec3;",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$camera(float tick,CallbackInfoReturnable<Vec3> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) cir.setReturnValue(TiltFrame.eye(self,new Vec3(Mth.lerp(tick,self.xo,self.getX()),Mth.lerp(tick,self.yo,self.getY()),Mth.lerp(tick,self.zo,self.getZ()))));
    }
    @Inject(method={"getOnPosLegacy","getOnPos()Lnet/minecraft/core/BlockPos;","getBlockPosBelowThatAffectsMyMovement"},at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$support(CallbackInfoReturnable<BlockPos> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) cir.setReturnValue(TiltFrame.support(self));
    }
    @Inject(method="collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$collide(Vec3 movement,CallbackInfoReturnable<Vec3> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) cir.setReturnValue(TiltFrame.collide(self,movement));
    }
    @Inject(method="move",at=@At("HEAD"))
    private void hunterwildcard$start(MoverType type,Vec3 movement,CallbackInfo ci) {
        Entity self=(Entity)(Object)this;
        hunterwildcard$moveStart=TiltFrame.active(self)?self.position():null;
    }
    @Inject(method="move",at=@At("TAIL"))
    private void hunterwildcard$ground(MoverType type,Vec3 movement,CallbackInfo ci) {
        if(hunterwildcard$moveStart==null)return;
        Entity self=(Entity)(Object)this;
        Vec3 requested=TiltFrame.local(TiltFrame.direction(self),movement);
        Vec3 actual=TiltFrame.local(TiltFrame.direction(self),self.position().subtract(hunterwildcard$moveStart));
        boolean landed=requested.y<0 && actual.y>requested.y+1.0E-7;
        self.setOnGround(landed);
        if(landed) {
            var d=TiltFrame.direction(self);
            var velocity=TiltFrame.local(d,self.getDeltaMovement());
            self.setDeltaMovement(new Vec3(velocity.x,requested.y,velocity.z));
            self.level().getBlockState(TiltFrame.support(self)).getBlock().updateEntityMovementAfterFallOn(self.level(),self);
            self.setDeltaMovement(TiltFrame.world(d,self.getDeltaMovement()));
        }
        self.verticalCollision=Math.abs(actual.y-requested.y)>1.0E-7;
        self.verticalCollisionBelow=landed;
        self.horizontalCollision=Math.abs(actual.x-requested.x)>1.0E-7||Math.abs(actual.z-requested.z)>1.0E-7;
        hunterwildcard$moveStart=null;
    }
}
