package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.util.PlayerUtil;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

/**
 * Every runner glows (blue, through the scoreboard team) for the whole wildcard; in return each runner gets a
 * proximity warning whenever a hunter comes within the configured distance.
 */
public class HunterRadarRule implements WildcardRule {
    private static final int GLOW_REFRESH_TICKS = 100;
    private static final int GLOW_TICKS = GLOW_REFRESH_TICKS + 40;
    private static final int WARNING_INTERVAL_TICKS = 20;

    @Override
    public void onStart(GameContext context) {
        applyGlow(context);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks % GLOW_REFRESH_TICKS == 0) {
            applyGlow(context);
        }
        if (remainingTicks <= 0 || remainingTicks % WARNING_INTERVAL_TICKS != 0) {
            return;
        }

        int warningDistance = context.getConfig().hunterRadarWarningDistance;
        double warningDistanceSquared = (double) warningDistance * warningDistance;
        for (ServerPlayerEntity runner : context.getRunners()) {
            ServerPlayerEntity nearest = null;
            double nearestSquared = Double.MAX_VALUE;
            for (ServerPlayerEntity hunter : context.getHunters()) {
                if (hunter.getEntityWorld() != runner.getEntityWorld() || !hunter.isAlive()) {
                    continue;
                }
                double squared = PlayerUtil.distanceSquared(hunter, runner);
                if (squared < nearestSquared) {
                    nearestSquared = squared;
                    nearest = hunter;
                }
            }
            if (nearest == null || nearestSquared > warningDistanceSquared) {
                continue;
            }

            int distance = PlayerUtil.roundDistance(Math.sqrt(nearestSquared));
            runner.sendMessage(HunterWildcardText.translatable("msg.wildcard.hunter_radar.warning", distance).formatted(Formatting.RED), true);
            runner.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 0.8F, distance <= warningDistance / 2 ? 1.8F : 1.2F);
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayerEntity runner : context.getRunners()) {
            runner.removeStatusEffect(StatusEffects.GLOWING);
        }
    }

    private static void applyGlow(GameContext context) {
        for (ServerPlayerEntity runner : context.getRunners()) {
            runner.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, GLOW_TICKS, 0, false, false, false));
        }
    }
}
