package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** Stand still for one second and you glow (team-coloured); move and it stops. */
public class StillGlowRule implements WildcardRule {
    private static final int STILL_THRESHOLD_TICKS = 20;
    private static final double MOVE_THRESHOLD_SQUARED = 0.0004;
    private static final int GLOW_DURATION_TICKS = 20 * 60 * 60;

    private final Map<UUID, Vec3> lastPositions = new HashMap<>();
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
        for (ServerPlayer player : context.getParticipants()) {
            UUID id = player.getUUID();
            Vec3 pos = player.position();
            Vec3 last = lastPositions.put(id, pos);
            boolean moved = last == null || last.distanceToSqr(pos) > MOVE_THRESHOLD_SQUARED || !player.isAlive();
            int still = moved ? 0 : stillTicks.getOrDefault(id, 0) + 1;
            stillTicks.put(id, still);

            if (still >= STILL_THRESHOLD_TICKS && glowing.add(id)) {
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION_TICKS, 0, false, false, false));
                player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.still_glow.glowing"), true);
            } else if (moved && glowing.remove(id)) {
                player.removeEffect(MobEffects.GLOWING);
            } else if (still >= STILL_THRESHOLD_TICKS && !player.hasEffect(MobEffects.GLOWING)) {
                // Respawn or milk cleared it; keep the glow honest.
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION_TICKS, 0, false, false, false));
            }
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayer player : context.getParticipants()) {
            if (glowing.contains(player.getUUID())) {
                player.removeEffect(MobEffects.GLOWING);
            }
        }
        lastPositions.clear();
        stillTicks.clear();
        glowing.clear();
    }
}
