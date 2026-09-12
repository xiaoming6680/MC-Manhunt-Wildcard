package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Wildcard damage hook for non-player victims (mobs hit by a participant). Players are handled in
 * {@link PlayerDamageMixin}, which runs first and then calls into this method through {@code super.damage}.
 */
@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float hunterwildcard$modifyWildcardDamage(float amount, ServerLevel world, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player) {
            return amount;
        }
        return GameManager.getInstance().modifyLivingDamage(self, source, amount);
    }
}
