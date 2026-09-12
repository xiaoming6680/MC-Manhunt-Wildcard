package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Wildcard damage hook for non-player victims (mobs hit by a participant). Players are handled in
 * {@link PlayerDamageMixin}, which runs first and then calls into this method through {@code super.damage}.
 */
@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float hunterwildcard$modifyWildcardDamage(float amount, ServerWorld world, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity) {
            return amount;
        }
        return GameManager.getInstance().modifyLivingDamage(self, source, amount);
    }
}
