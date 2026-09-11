package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Stand still for one second and you glow (team-coloured); move and it stops. */
public class StillGlowRule implements WildcardRule {
    private static final int STILL_THRESHOLD_TICKS = 20;
    private static final double MOVE_THRESHOLD_SQUARED = 0.0004;
    private static final int GLOW_DURATION_TICKS = 20 * 60 * 60;

    private final Map<UUID, Vec3d> lastPositions = new HashMap<>();
    private final Map<UUID, Integer> stillTicks = new HashMap<>();
    private final Set<UUID> glowing = new HashSet<>();

    @Override
    public void onStart(GameContext context) {
        lastPositions.clear();
        stillTicks.clear();
        glowing.clear();
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            UUID id = player.getUuid();
            Vec3d pos = player.getEntityPos();
            Vec3d last = lastPositions.put(id, pos);
            boolean moved = last == null || last.squaredDistanceTo(pos) > MOVE_THRESHOLD_SQUARED || !player.isAlive();
            int still = moved ? 0 : stillTicks.getOrDefault(id, 0) + 1;
            stillTicks.put(id, still);

            if (still >= STILL_THRESHOLD_TICKS && glowing.add(id)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, GLOW_DURATION_TICKS, 0, false, false, false));
                player.sendMessage(HunterWildcardText.translatable("msg.wildcard.still_glow.glowing"), true);
            } else if (moved && glowing.remove(id)) {
                player.removeStatusEffect(StatusEffects.GLOWING);
            } else if (still >= STILL_THRESHOLD_TICKS && !player.hasStatusEffect(StatusEffects.GLOWING)) {
                // Respawn or milk cleared it; keep the glow honest.
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, GLOW_DURATION_TICKS, 0, false, false, false));
            }
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            if (glowing.contains(player.getUuid())) {
                player.removeStatusEffect(StatusEffects.GLOWING);
            }
        }
        lastPositions.clear();
        stillTicks.clear();
        glowing.clear();
    }
}
