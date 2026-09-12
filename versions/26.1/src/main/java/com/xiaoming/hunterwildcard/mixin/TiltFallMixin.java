package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.WorldTiltPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class TiltFallMixin {
    @Shadow protected abstract void checkFallDamage(double distance, boolean onGround, BlockState state, BlockPos pos);

    @Inject(method = "doCheckFallDamage", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$fallAlongGravity(double dx, double dy, double dz, boolean onGround, CallbackInfo ci) {
        if (!((Object)this instanceof ServerPlayer player)) return;
        Vec3 gravity = WorldTiltPhysics.gravity(player);
        if (gravity == null) return;
        ci.cancel();
        if (player.isSpectator() || player.getAbilities().flying || player.isPassenger()
                || player.isInWater() || player.onClimbable() || player.hasEffect(MobEffects.SLOW_FALLING)) {
            player.resetFallDistance();
            return;
        }
        // Probe the gravity-facing side of the actual collision box, never trust a client ground flag.
        Vec3 probe = gravity.scale(.05);
        Vec3 allowed = Entity.collideBoundingBox(player, probe, player.getBoundingBox(),
                player.level(), java.util.List.of());
        boolean landed = allowed.dot(gravity) < .049;
        BlockPos pos = com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.support(player);
        // Keep native safe-fall distance, enchantments, effects, block landing hooks and damage source.
        checkFallDamage(-new Vec3(dx, dy, dz).dot(gravity), landed,
                player.level().getBlockState(pos), pos);
    }
}
