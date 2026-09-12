package com.xiaoming.hunterwildcard.mixin;
import com.xiaoming.hunterwildcard.wildcard.rules.WorldTiltPhysics;
import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Preserve the superclass fall implementation; ServerPlayerEntity.fall itself is intentionally empty. */
@Mixin(ServerPlayerEntity.class)
public abstract class TiltFallMixin {
    @WrapOperation(method="handleFall",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerEntity;fall(DZLnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)V"))
    private void hunterwildcard$fallAlongGravity(ServerPlayerEntity receiver,double distance,boolean onGround,BlockState state,BlockPos pos,Operation<Void> original,double dx,double dy,double dz,boolean clientGround) {
        ServerPlayerEntity player=(ServerPlayerEntity)(Object)this;
        Vec3d gravity=WorldTiltPhysics.gravity(player);
        if(gravity==null){original.call(receiver,distance,onGround,state,pos);return;}
        if(player.isSpectator()||player.getAbilities().flying||player.hasVehicle()||player.isTouchingWater()||player.isClimbing()||player.hasStatusEffect(StatusEffects.SLOW_FALLING)){player.onLanding();return;}
        Vec3d allowed=Entity.adjustMovementForCollisions(player,gravity.multiply(.05),player.getBoundingBox(),player.getServerWorld(),java.util.List.of());
        boolean landed=allowed.dotProduct(gravity)<.049;
        BlockPos support=TiltFrame.support(player);
        original.call(receiver,-new Vec3d(dx,dy,dz).dotProduct(gravity),landed,player.getServerWorld().getBlockState(support),support);
    }
}