package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Player-versus-player hits only count from behind, where they deal double damage. Hits from the front are
 * cancelled outright (no damage, no knockback).
 */
public class BackstabRule implements WildcardRule {
    private static final float BACKSTAB_MULTIPLIER = 2.0F;
    private static final int MESSAGE_COOLDOWN_TICKS = 10;

    private final Map<UUID, Integer> lastMessageTick = new HashMap<>();
    private int ticks;

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        lastMessageTick.clear();
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
    }

    @Override
    public float modifyDamage(GameContext context, LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayer target) || !(source.getEntity() instanceof ServerPlayer attacker) || attacker == target) {
            return amount;
        }

        Vec3 facing = target.getViewVector(1.0F);
        Vec3 toAttacker = attacker.position().subtract(target.position());
        facing = new Vec3(facing.x, 0.0, facing.z);
        toAttacker = new Vec3(toAttacker.x, 0.0, toAttacker.z);
        if (facing.lengthSqr() < 1.0E-6 || toAttacker.lengthSqr() < 1.0E-6) {
            return amount;
        }
        boolean behind = facing.normalize().dot(toAttacker.normalize()) < 0.0;

        if (behind) {
            if (target.level() instanceof ServerLevel world) {
                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.8F);
                world.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
            }
            notify(attacker, "msg.wildcard.backstab.hit");
            return amount * BACKSTAB_MULTIPLIER;
        }

        notify(attacker, "msg.wildcard.backstab.blocked");
        return 0.0F;
    }

    @Override
    public void onStop(GameContext context) {
        lastMessageTick.clear();
    }

    private void notify(ServerPlayer attacker, String key) {
        Integer last = lastMessageTick.get(attacker.getUUID());
        if (last != null && ticks - last < MESSAGE_COOLDOWN_TICKS) {
            return;
        }
        lastMessageTick.put(attacker.getUUID(), ticks);
        attacker.sendSystemMessage(HunterWildcardText.translatable(key), true);
    }
}
