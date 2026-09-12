package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** Every point of damage you deal to any living thing comes back as health. */
public class VampireRule implements WildcardRule {
    @Override
    public void onDamageDealt(GameContext context, ServerPlayer attacker, LivingEntity victim, float damageDealt) {
        if (damageDealt <= 0.0F || !attacker.isAlive()) {
            return;
        }
        attacker.heal(damageDealt);
        if (attacker.level() instanceof ServerLevel world) {
            world.sendParticles(ParticleTypes.HEART, attacker.getX(), attacker.getY() + 1.6, attacker.getZ(), Math.max(1, Math.round(damageDealt / 2.0F)), 0.3, 0.2, 0.3, 0.0);
            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
        }
    }
}
