package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

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
        for (ServerPlayer player : context.getParticipants()) {
            boolean low = player.isAlive() && player.getHealth() <= RAGE_HEALTH;
            UUID id = player.getUUID();
            if (low && raging.add(id)) {
                player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.blood_rage.armed").withStyle(ChatFormatting.DARK_RED), true);
                player.playSound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.2F);
            } else if (!low && raging.remove(id)) {
                player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.blood_rage.calmed").withStyle(ChatFormatting.GRAY), true);
            }
            if (low && player.level() instanceof ServerLevel world) {
                world.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.3, 0.4, 0.3, 0.0);
            }
        }
    }

    @Override
    public float modifyDamage(GameContext context, LivingEntity victim, DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim && attacker.isAlive() && attacker.getHealth() <= RAGE_HEALTH) {
            return Math.max(amount, LETHAL_DAMAGE);
        }
        return amount;
    }

    @Override
    public void onStop(GameContext context) {
        raging.clear();
    }
}
