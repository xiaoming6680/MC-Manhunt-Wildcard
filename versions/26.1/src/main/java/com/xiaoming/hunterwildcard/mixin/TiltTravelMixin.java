package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class TiltTravelMixin {
    @Shadow protected abstract float getJumpPower();
    @Shadow protected abstract float getWaterSlowDown();
    @Inject(method="travelInAir",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$travel(Vec3 input,CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(!TiltFrame.active(self))return;
        ci.cancel();
        Direction d=TiltFrame.direction(self);
        float slipperiness=self.onGround()?self.level().getBlockState(TiltFrame.support(self)).getBlock().getFriction():1;
        float friction=slipperiness*.91F;
        float speed=self.onGround()?self.getSpeed()*.21600002F/(slipperiness*slipperiness*slipperiness):self.isSprinting()?.026F:.02F;
        Vec3 move=input.lengthSqr()>1?input.normalize():input;
        double yaw=Math.toRadians(TiltFrame.localAngles(self)[0]),sin=Math.sin(yaw),cos=Math.cos(yaw);
        Vec3 acceleration=new Vec3(move.x*cos-move.z*sin,move.y,move.z*cos+move.x*sin).scale(speed);
        self.setDeltaMovement(self.getDeltaMovement().add(TiltFrame.world(d,acceleration)));
        if(self.onClimbable()) {
            Vec3 climb=TiltFrame.local(d,self.getDeltaMovement());
            self.setDeltaMovement(TiltFrame.world(d,new Vec3(Math.clamp(climb.x,-.15,.15),self.isShiftKeyDown()?Math.max(0,climb.y):Math.max(-.15,climb.y),Math.clamp(climb.z,-.15,.15))));
            self.resetFallDistance();
        }
        self.move(MoverType.SELF,self.getDeltaMovement());
        Vec3 velocity=TiltFrame.local(d,self.getDeltaMovement());
        double vertical=self.onClimbable()&&self.horizontalCollision?.2:velocity.y;
        var levitation=self.getEffect(MobEffects.LEVITATION);
        if(levitation!=null)vertical+=(.05*(levitation.getAmplifier()+1)-vertical)*.2;
        else if(!self.isNoGravity()) {
            double gravity=self.getAttributeValue(Attributes.GRAVITY);
            if(self.hasEffect(MobEffects.SLOW_FALLING)&&vertical<=0){gravity=Math.min(gravity,.01);self.resetFallDistance();}
            vertical-=gravity;
        }
        self.setDeltaMovement(TiltFrame.world(d,new Vec3(velocity.x*friction,vertical*.98,velocity.z*friction)));
    }
    @Inject(method="jumpFromGround",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$jump(CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(!TiltFrame.active(self))return;
        Direction d=TiltFrame.direction(self);
        Vec3 velocity=TiltFrame.local(d,self.getDeltaMovement());
        Vec3 jump=new Vec3(velocity.x,Math.max(velocity.y,getJumpPower()),velocity.z);
        if(self.isSprinting()) {
            double yaw=Math.toRadians(TiltFrame.localAngles(self)[0]);
            jump=jump.add(-Math.sin(yaw)*.2,0,Math.cos(yaw)*.2);
        }
        self.setDeltaMovement(TiltFrame.world(d,jump));self.needsSync=true;ci.cancel();
    }
    @Inject(method="travelInFluid",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$fluid(Vec3 input,CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(!TiltFrame.active(self))return;
        ci.cancel();
        var d=TiltFrame.direction(self);
        Vec3 before=TiltFrame.local(d,self.getDeltaMovement());
        boolean falling=before.y<=0;
        double gravity=self.getGravity();
        if(falling&&self.hasEffect(MobEffects.SLOW_FALLING))gravity=Math.min(gravity,.01);
        Vec3 start=self.position();
        boolean water=self.isInWater();
        float friction=self.isSprinting()?.9F:getWaterSlowDown();
        float speed=.02F;
        if(water) {
            float efficiency=(float)self.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
            if(!self.onGround())efficiency*=.5F;
            if(efficiency>0) {
                friction+=(.54600006F-friction)*efficiency;
                speed+=(self.getSpeed()-speed)*efficiency;
            }
            if(self.hasEffect(MobEffects.DOLPHINS_GRACE))friction=.96F;
        }
        double yaw=Math.toRadians(TiltFrame.localAngles(self)[0]),sin=Math.sin(yaw),cos=Math.cos(yaw);
        Vec3 move=input.lengthSqr()>1?input.normalize():input;
        self.setDeltaMovement(self.getDeltaMovement().add(TiltFrame.world(d,new Vec3(move.x*cos-move.z*sin,move.y,move.z*cos+move.x*sin).scale(speed))));
        self.move(MoverType.SELF,self.getDeltaMovement());
        Vec3 velocity=TiltFrame.local(d,self.getDeltaMovement());
        if(water) {
            if(self.horizontalCollision&&self.onClimbable())velocity=new Vec3(velocity.x,.2,velocity.z);
            velocity=self.getFluidFallingAdjustedMovement(gravity,falling,velocity.multiply(friction,.800000011920929,friction));
        } else {
            if(self.getFluidHeight(net.minecraft.tags.FluidTags.LAVA)<=self.getFluidJumpThreshold())
                velocity=self.getFluidFallingAdjustedMovement(gravity,falling,velocity.multiply(.5,.800000011920929,.5));
            else velocity=velocity.scale(.5);
            velocity=velocity.add(0,-gravity/4,0);
        }
        // Vanilla's ledge escape probe, rotated into the same frame as movement and collision.
        double movedUp=TiltFrame.local(d,self.position().subtract(start)).y;
        Vec3 escape=TiltFrame.world(d,velocity.add(0,.6000000238418579-movedUp,0));
        if(self.horizontalCollision&&self.isFree(escape.x,escape.y,escape.z))
            velocity=new Vec3(velocity.x,.30000001192092896,velocity.z);
        self.setDeltaMovement(TiltFrame.world(d,velocity));self.resetFallDistance();
    }
    @Inject(method="goDownInWater",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$dive(CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(TiltFrame.active(self)) {self.setDeltaMovement(self.getDeltaMovement().add(TiltFrame.down(self).scale(.03999999910593033)));ci.cancel();}
    }
    @Inject(method="jumpInLiquid",at=@At("HEAD"),cancellable=true)
    private void hunterwildcard$swim(net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> fluid,CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(TiltFrame.active(self)) {self.setDeltaMovement(self.getDeltaMovement().add(TiltFrame.down(self).scale(-.03999999910593033)));ci.cancel();}
    }
}
