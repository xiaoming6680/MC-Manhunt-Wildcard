package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        if (!(victim instanceof ServerPlayerEntity target) || !(source.getAttacker() instanceof ServerPlayerEntity attacker) || attacker == target) {
            return amount;
        }

        Vec3d facing = target.getRotationVec(1.0F);
        Vec3d toAttacker = attacker.getPos().subtract(target.getPos());
        facing = new Vec3d(facing.x, 0.0, facing.z);
        toAttacker = new Vec3d(toAttacker.x, 0.0, toAttacker.z);
        if (facing.lengthSquared() < 1.0E-6 || toAttacker.lengthSquared() < 1.0E-6) {
            return amount;
        }
        boolean behind = facing.normalize().dotProduct(toAttacker.normalize()) < 0.0;

        if (behind) {
            if (target.getWorld() instanceof ServerWorld world) {
                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 0.8F);
                world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
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

    private void notify(ServerPlayerEntity attacker, String key) {
        Integer last = lastMessageTick.get(attacker.getUuid());
        if (last != null && ticks - last < MESSAGE_COOLDOWN_TICKS) {
            return;
        }
        lastMessageTick.put(attacker.getUuid(), ticks);
        attacker.sendMessage(HunterWildcardText.translatable(key), true);
    }
}
