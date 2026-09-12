package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;

public class WeaponOverheatRule implements WildcardRule {
    private static final int WINDOW_TICKS = 60;
    private static final int WEAKNESS_ATTACK_COUNT = 2;
    private static final int SLOWNESS_ATTACK_COUNT = 3;
    private static final int SEVERE_ATTACK_COUNT = 4;
    private static final int EFFECT_REFRESH_TICKS = 3;

    private final Map<UUID, Deque<Integer>> attackTicks = new HashMap<>();
    private final Map<UUID, Integer> lastSyncedHeat = new HashMap<>();
    private int ticks;

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        attackTicks.clear();
        lastSyncedHeat.clear();
        syncAllStatus(context);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        syncAllStatus(context);
    }

    @Override
    public void onPlayerAttack(GameContext context, ServerPlayer player, Entity target) {
        UUID playerId = player.getUUID();
        Deque<Integer> recent = attackTicks.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        recent.addLast(ticks);
        int attackCount = pruneAndCount(recent);
        updatePlayerStatus(player, attackCount);
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayer player : context.getParticipants()) {
            HunterWildcardPackets.clearWeaponOverheatStatus(player);
        }
        attackTicks.clear();
        lastSyncedHeat.clear();
    }

    private void syncAllStatus(GameContext context) {
        for (ServerPlayer player : context.getParticipants()) {
            UUID playerId = player.getUUID();
            Deque<Integer> recent = attackTicks.get(playerId);
            int attackCount = recent == null ? 0 : pruneAndCount(recent);
            updatePlayerStatus(player, attackCount);
        }
    }

    private int pruneAndCount(Deque<Integer> recent) {
        while (!recent.isEmpty() && ticks - recent.peekFirst() > WINDOW_TICKS) {
            recent.removeFirst();
        }
        return recent.size();
    }

    private void syncStatus(ServerPlayer player, int heat) {
        UUID playerId = player.getUUID();
        if (lastSyncedHeat.getOrDefault(playerId, -1) == heat) {
            return;
        }

        lastSyncedHeat.put(playerId, heat);
        HunterWildcardPackets.sendWeaponOverheatStatus(player, heat, SEVERE_ATTACK_COUNT);
    }

    private void updatePlayerStatus(ServerPlayer player, int attackCount) {
        int heat = Math.min(attackCount, SEVERE_ATTACK_COUNT);
        applyHeatEffects(player, heat);
        syncStatus(player, heat);
    }

    private void applyHeatEffects(ServerPlayer player, int heat) {
        if (heat >= SEVERE_ATTACK_COUNT) {
            applyOverheat(player, 1, 1);
        } else if (heat >= SLOWNESS_ATTACK_COUNT) {
            applyOverheat(player, 0, 0);
        } else if (heat >= WEAKNESS_ATTACK_COUNT) {
            applyOverheat(player, 0, -1);
            clearShortOverheatEffect(player, MobEffects.SLOWNESS);
        } else {
            clearShortOverheatEffect(player, MobEffects.WEAKNESS);
            clearShortOverheatEffect(player, MobEffects.SLOWNESS);
        }
    }

    private void applyOverheat(ServerPlayer player, int weaknessAmplifier, int slownessAmplifier) {
        applyOrReplaceShortOverheatEffect(player, MobEffects.WEAKNESS, weaknessAmplifier);
        if (slownessAmplifier >= 0) {
            applyOrReplaceShortOverheatEffect(player, MobEffects.SLOWNESS, slownessAmplifier);
        }
    }

    private void applyOrReplaceShortOverheatEffect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance currentEffect = player.getEffect(effect);
        if (currentEffect != null && currentEffect.getDuration() <= EFFECT_REFRESH_TICKS && currentEffect.getAmplifier() != amplifier) {
            player.removeEffect(effect);
        }
        player.addEffect(new MobEffectInstance(effect, EFFECT_REFRESH_TICKS, amplifier, false, false, true));
    }

    private void clearShortOverheatEffect(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance currentEffect = player.getEffect(effect);
        if (currentEffect != null && currentEffect.getDuration() <= EFFECT_REFRESH_TICKS) {
            player.removeEffect(effect);
        }
    }
}
