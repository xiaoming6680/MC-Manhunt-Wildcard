package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** At three hearts or less every hit you land is lethal. Staying that low is the gamble. */
public class BloodRageRule implements WildcardRule {
    private static final float RAGE_HEALTH = 6.0F;
    private static final float LETHAL_DAMAGE = 10_000.0F;
    private static final int CHECK_INTERVAL_TICKS = 5;

    private final Set<UUID> raging = new HashSet<>();

    @Override
    public void onStart(GameContext context) {
        raging.clear();
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayerEntity player : context.getParticipants()) {
            boolean low = player.isAlive() && player.getHealth() <= RAGE_HEALTH;
            UUID id = player.getUuid();
            if (low && raging.add(id)) {
                player.sendMessage(HunterWildcardText.translatable("msg.wildcard.blood_rage.armed").formatted(Formatting.DARK_RED), true);
                player.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.0F, 1.2F);
            } else if (!low && raging.remove(id)) {
                player.sendMessage(HunterWildcardText.translatable("msg.wildcard.blood_rage.calmed").formatted(Formatting.GRAY), true);
            }
            if (low && player.getWorld() instanceof ServerWorld world) {
                world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.3, 0.4, 0.3, 0.0);
            }
        }
    }

    @Override
    public float modifyDamage(GameContext context, LivingEntity victim, DamageSource source, float amount) {
        if (source.getAttacker() instanceof ServerPlayerEntity attacker && attacker != victim && attacker.isAlive() && attacker.getHealth() <= RAGE_HEALTH) {
            return Math.max(amount, LETHAL_DAMAGE);
        }
        return amount;
    }

    @Override
    public void onStop(GameContext context) {
        raging.clear();
    }
}
