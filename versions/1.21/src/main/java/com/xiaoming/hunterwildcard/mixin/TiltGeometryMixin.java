package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.entity.*;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(Entity.class)
public abstract class TiltGeometryMixin {
    @Unique private Vec3d hunterwildcard$moveStart;
    @Inject(method="updateSupportingBlockPos",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$standingBlock(boolean grounded,Vec3d movement,CallbackInfo ci) {
        Entity self=(Entity)(Object)this;
        if(!TiltFrame.active(self))return;
        ci.cancel();
        self.supportingBlockPos=grounded?TiltFrame.supportingBlock(self):java.util.Optional.empty();
    }
    @WrapOperation(method="isInsideWall",at=@At(value="INVOKE",target="Lnet/minecraft/util/math/Box;of(Lnet/minecraft/util/math/Vec3d;DDD)Lnet/minecraft/util/math/Box;"))
    private Box hunterwildcard$headBox(Vec3d center,double x,double y,double z,Operation<Box> original) {
        Entity self=(Entity)(Object)this;
        if(!TiltFrame.active(self))return original.call(center,x,y,z);
        return TiltFrame.rotateBox(TiltFrame.direction(self),Box.of(Vec3d.ZERO,x,y,z),false).offset(center);
    }
    @ModifyArgs(method="updateSubmergedInWaterState",at=@At(value="INVOKE",target="Lnet/minecraft/util/math/BlockPos;ofFloored(DDD)Lnet/minecraft/util/math/BlockPos;"))
    private void hunterwildcard$fluidEyes(Args args) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)){args.set(0,self.getEyePos().x);args.set(2,self.getEyePos().z);}
    }
    @WrapOperation(method="move",at=@At(value="INVOKE",target="Lnet/minecraft/block/Block;onEntityLand(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;)V"))
    private void hunterwildcard$deferLanding(net.minecraft.block.Block block,net.minecraft.world.BlockView world,Entity entity,Operation<Void> original) {
        // Invoke once below, with gravity-local velocity; horizontal wall bumps aren't landings.
        if(!TiltFrame.active(entity))original.call(block,world,entity);
    }
    @Inject(method="calculateBoundingBox",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$box(CallbackInfoReturnable<Box> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) cir.setReturnValue(TiltFrame.box(self,self.getDimensions(self.getPose()),self.getPos()));
    }
    @Inject(method="onTrackedDataSet",at=@At("TAIL"))
    private void hunterwildcard$syncBox(TrackedData<?> data,CallbackInfo ci) {
        Entity self=(Entity)(Object)this;
        if(self instanceof com.xiaoming.hunterwildcard.wildcard.rules.TiltTrackedGravity)
            self.setBoundingBox(TiltFrame.box(self,self.getDimensions(self.getPose()),self.getPos()));
    }
    @Inject(method="calculateDimensions",at=@At("TAIL"))
    private void hunterwildcard$resizeBox(CallbackInfo ci) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) self.setBoundingBox(TiltFrame.box(self,self.getDimensions(self.getPose()),self.getPos()));
    }
    @Inject(method="getEyePos",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$eyes(CallbackInfoReturnable<Vec3d> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) cir.setReturnValue(TiltFrame.eye(self,self.getPos()));
    }
    @Inject(method="getEyeY",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$eyeY(CallbackInfoReturnable<Double> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self))cir.setReturnValue(TiltFrame.eye(self,self.getPos()).y);
    }
    @Inject(method="getCameraPosVec",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$camera(float tick,CallbackInfoReturnable<Vec3d> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) cir.setReturnValue(TiltFrame.eye(self,new Vec3d(MathHelper.lerp(tick,self.prevX,self.getX()),MathHelper.lerp(tick,self.prevY,self.getY()),MathHelper.lerp(tick,self.prevZ,self.getZ()))));
    }
    @Inject(method={"getLandingPos","getSteppingPos","getVelocityAffectingPos"},at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$support(CallbackInfoReturnable<BlockPos> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) cir.setReturnValue(TiltFrame.support(self));
    }
    @Inject(method="adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$collide(Vec3d movement,CallbackInfoReturnable<Vec3d> cir) {
        Entity self=(Entity)(Object)this;
        if(TiltFrame.active(self)) cir.setReturnValue(TiltFrame.collide(self,movement));
    }
    @Inject(method="move",at=@At("HEAD"))
    private void hunterwildcard$start(MovementType type,Vec3d movement,CallbackInfo ci) {
        Entity self=(Entity)(Object)this;
        hunterwildcard$moveStart=TiltFrame.active(self)?self.getPos():null;
    }
    @Inject(method="move",at=@At("TAIL"))
    private void hunterwildcard$ground(MovementType type,Vec3d movement,CallbackInfo ci) {
        if(hunterwildcard$moveStart==null)return;
        Entity self=(Entity)(Object)this;
        Vec3d requested=TiltFrame.local(TiltFrame.direction(self),movement);
        Vec3d actual=TiltFrame.local(TiltFrame.direction(self),self.getPos().subtract(hunterwildcard$moveStart));
        boolean landed=requested.y<0 && actual.y>requested.y+1.0E-7;
        self.setOnGround(landed);
        if(landed) {
            var d=TiltFrame.direction(self);
            var velocity=TiltFrame.local(d,self.getVelocity());
            self.setVelocity(new Vec3d(velocity.x,requested.y,velocity.z));
            self.getEntityWorld().getBlockState(TiltFrame.support(self)).getBlock().onEntityLand(self.getEntityWorld(),self);
            self.setVelocity(TiltFrame.world(d,self.getVelocity()));
        }
        self.verticalCollision=Math.abs(actual.y-requested.y)>1.0E-7;
        self.groundCollision=landed;
        self.horizontalCollision=Math.abs(actual.x-requested.x)>1.0E-7||Math.abs(actual.z-requested.z)>1.0E-7;
        hunterwildcard$moveStart=null;
    }
}
