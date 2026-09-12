package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.WorldTiltPhysics;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class TiltFallMixin {
    @Shadow protected abstract void fall(double distance, boolean onGround, BlockState state, BlockPos pos);

    @Inject(method = "handleFall", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$fallAlongGravity(double dx, double dy, double dz, boolean onGround, CallbackInfo ci) {
        if (!((Object)this instanceof ServerPlayerEntity player)) return;
        Vec3d gravity = WorldTiltPhysics.gravity(player);
        if (gravity == null) return;
        ci.cancel();
        if (player.isSpectator() || player.getAbilities().flying || player.hasVehicle()
                || player.isTouchingWater() || player.isClimbing() || player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            player.onLanding();
            return;
        }
        // Probe the gravity-facing side of the actual collision box, never trust a client ground flag.
        Vec3d probe = gravity.multiply(.05);
        Vec3d allowed = Entity.adjustMovementForCollisions(player, probe, player.getBoundingBox(),
                player.getEntityWorld(), java.util.List.of());
        boolean landed = allowed.dotProduct(gravity) < .049;
        BlockPos pos = com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.support(player);
        // Keep native safe-fall distance, enchantments, effects, block landing hooks and damage source.
        fall(-new Vec3d(dx, dy, dz).dotProduct(gravity), landed,
                player.getEntityWorld().getBlockState(pos), pos);
    }
}
