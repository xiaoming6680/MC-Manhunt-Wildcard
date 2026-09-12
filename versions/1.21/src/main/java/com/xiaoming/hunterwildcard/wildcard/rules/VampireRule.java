package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/** Every point of damage you deal to any living thing comes back as health. */
public class VampireRule implements WildcardRule {
    @Override
    public void onDamageDealt(GameContext context, ServerPlayerEntity attacker, LivingEntity victim, float damageDealt) {
        if (damageDealt <= 0.0F || !attacker.isAlive()) {
            return;
        }
        attacker.heal(damageDealt);
        if (attacker.getServerWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.HEART, attacker.getX(), attacker.getY() + 1.6, attacker.getZ(), Math.max(1, Math.round(damageDealt / 2.0F)), 0.3, 0.2, 0.3, 0.0);
            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.6F, 1.6F);
        }
    }
}
