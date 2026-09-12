package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntity.class)
public class PlayerDamageMixin {
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float hunterwildcard$scaleHunterDamage(float amount, ServerWorld world, DamageSource source) {
        if ((Object) this instanceof ServerPlayerEntity victim) {
            return GameManager.getInstance().modifyIncomingDamage(victim, source, amount);
        }

        return amount;
    }
}
