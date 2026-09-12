package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.wildcard.rules.WindChargeBrawlRule;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WindCharge.class)
public class WindChargeEntityMixin {
    @ModifyArg(
            method = "explode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/util/random/WeightedList;Lnet/minecraft/core/Holder;)V"
            ),
            index = 6
    )
    private float hunterwildcard$boostWindChargeExplosion(float power) {
        GameManager gameManager = GameManager.getInstance();
        if (gameManager.getWildcardManager().getActiveRule() instanceof WindChargeBrawlRule windChargeBrawlRule) {
            return power * windChargeBrawlRule.getExplosionPowerMultiplier(gameManager.getConfig());
        }

        return power;
    }
}
