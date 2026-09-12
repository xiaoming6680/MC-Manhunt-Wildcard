package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class TiltTravelMixin {
    @Shadow protected abstract boolean shouldSwimInFluids();
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$legacyTravel(Vec3d input,CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if (!TiltFrame.active(self) || self.isFallFlying() || !self.isLogicalSideForUpdatingMovement()) return;
        if ((self.isTouchingWater() || self.isInLava()) && shouldSwimInFluids() && !self.canWalkOnFluid(self.getEntityWorld().getFluidState(self.getBlockPos())))
            hunterwildcard$fluid(input,ci);
        else hunterwildcard$travel(input,ci);
        self.updateLimbs(false);
    }
    @Shadow protected abstract float getJumpVelocity();
    @Shadow protected abstract float getBaseMovementSpeedMultiplier();
    @Unique
    private void hunterwildcard$travel(Vec3d input,CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(!TiltFrame.active(self))return;
        ci.cancel();
        Direction d=TiltFrame.direction(self);
        float slipperiness=self.isOnGround()?self.getEntityWorld().getBlockState(TiltFrame.support(self)).getBlock().getSlipperiness():1;
        float friction=slipperiness*.91F;
        float speed=self.isOnGround()?self.getMovementSpeed()*.21600002F/(slipperiness*slipperiness*slipperiness):self.isSprinting()?.026F:.02F;
        Vec3d move=input.lengthSquared()>1?input.normalize():input;
        double yaw=Math.toRadians(TiltFrame.localAngles(self)[0]),sin=Math.sin(yaw),cos=Math.cos(yaw);
        Vec3d acceleration=new Vec3d(move.x*cos-move.z*sin,move.y,move.z*cos+move.x*sin).multiply(speed);
        self.setVelocity(self.getVelocity().add(TiltFrame.world(d,acceleration)));
        if(self.isClimbing()) {
            Vec3d climb=TiltFrame.local(d,self.getVelocity());
            self.setVelocity(TiltFrame.world(d,new Vec3d(Math.clamp(climb.x,-.15,.15),self.isSneaking()?Math.max(0,climb.y):Math.max(-.15,climb.y),Math.clamp(climb.z,-.15,.15))));
            self.onLanding();
        }
        self.move(MovementType.SELF,self.getVelocity());
        Vec3d velocity=TiltFrame.local(d,self.getVelocity());
        double vertical=self.isClimbing()&&self.horizontalCollision?.2:velocity.y;
        var levitation=self.getStatusEffect(StatusEffects.LEVITATION);
        if(levitation!=null)vertical+=(.05*(levitation.getAmplifier()+1)-vertical)*.2;
        else if(!self.hasNoGravity()) {
            double gravity=self.getAttributeValue(EntityAttributes.GENERIC_GRAVITY);
            if(self.hasStatusEffect(StatusEffects.SLOW_FALLING)&&vertical<=0){gravity=Math.min(gravity,.01);self.onLanding();}
            vertical-=gravity;
        }
        self.setVelocity(TiltFrame.world(d,new Vec3d(velocity.x*friction,vertical*.98,velocity.z*friction)));
    }
    @Inject(method="jump",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$jump(CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(!TiltFrame.active(self))return;
        Direction d=TiltFrame.direction(self);
        Vec3d velocity=TiltFrame.local(d,self.getVelocity());
        Vec3d jump=new Vec3d(velocity.x,Math.max(velocity.y,getJumpVelocity()),velocity.z);
        if(self.isSprinting()) {
            double yaw=Math.toRadians(TiltFrame.localAngles(self)[0]);
            jump=jump.add(-Math.sin(yaw)*.2,0,Math.cos(yaw)*.2);
        }
        self.setVelocity(TiltFrame.world(d,jump));self.velocityDirty=true;ci.cancel();
    }
    @Unique
    private void hunterwildcard$fluid(Vec3d input,CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(!TiltFrame.active(self))return;
        ci.cancel();
        var d=TiltFrame.direction(self);
        Vec3d before=TiltFrame.local(d,self.getVelocity());
        boolean falling=before.y<=0;
        double gravity=self.getFinalGravity();
        if(falling&&self.hasStatusEffect(StatusEffects.SLOW_FALLING))gravity=Math.min(gravity,.01);
        Vec3d start=self.getPos();
        boolean water=self.isTouchingWater();
        float friction=self.isSprinting()?.9F:getBaseMovementSpeedMultiplier();
        float speed=.02F;
        if(water) {
            float efficiency=(float)self.getAttributeValue(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY);
            if(!self.isOnGround())efficiency*=.5F;
            if(efficiency>0) {
                friction+=(.54600006F-friction)*efficiency;
                speed+=(self.getMovementSpeed()-speed)*efficiency;
            }
            if(self.hasStatusEffect(StatusEffects.DOLPHINS_GRACE))friction=.96F;
        }
        double yaw=Math.toRadians(TiltFrame.localAngles(self)[0]),sin=Math.sin(yaw),cos=Math.cos(yaw);
        Vec3d move=input.lengthSquared()>1?input.normalize():input;
        self.setVelocity(self.getVelocity().add(TiltFrame.world(d,new Vec3d(move.x*cos-move.z*sin,move.y,move.z*cos+move.x*sin).multiply(speed))));
        self.move(MovementType.SELF,self.getVelocity());
        Vec3d velocity=TiltFrame.local(d,self.getVelocity());
        if(water) {
            if(self.horizontalCollision&&self.isClimbing())velocity=new Vec3d(velocity.x,.2,velocity.z);
            velocity=self.applyFluidMovingSpeed(gravity,falling,velocity.multiply(friction,.800000011920929,friction));
        } else {
            if(self.getFluidHeight(net.minecraft.registry.tag.FluidTags.LAVA)<=self.getSwimHeight())
                velocity=self.applyFluidMovingSpeed(gravity,falling,velocity.multiply(.5,.800000011920929,.5));
            else velocity=velocity.multiply(.5);
            velocity=velocity.add(0,-gravity/4,0);
        }
        // Vanilla's ledge escape probe, rotated into the same frame as movement and collision.
        double movedUp=TiltFrame.local(d,self.getPos().subtract(start)).y;
        Vec3d escape=TiltFrame.world(d,velocity.add(0,.6000000238418579-movedUp,0));
        if(self.horizontalCollision&&self.doesNotCollide(escape.x,escape.y,escape.z))
            velocity=new Vec3d(velocity.x,.30000001192092896,velocity.z);
        self.setVelocity(TiltFrame.world(d,velocity));self.onLanding();
    }
    @Inject(method="knockDownwards",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$dive(CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(TiltFrame.active(self)) {self.setVelocity(self.getVelocity().add(TiltFrame.down(self).multiply(.03999999910593033)));ci.cancel();}
    }
    @Inject(method="swimUpward",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$swim(net.minecraft.registry.tag.TagKey<net.minecraft.fluid.Fluid> fluid,CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(TiltFrame.active(self)) {self.setVelocity(self.getVelocity().add(TiltFrame.down(self).multiply(-.03999999910593033)));ci.cancel();}
    }
}
