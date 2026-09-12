package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.util.PlayerUtil;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

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
        for (ServerPlayer runner : context.getRunners()) {
            ServerPlayer nearest = null;
            double nearestSquared = Double.MAX_VALUE;
            for (ServerPlayer hunter : context.getHunters()) {
                if (hunter.level() != runner.level() || !hunter.isAlive()) {
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
            runner.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.hunter_radar.warning", distance).withStyle(ChatFormatting.RED), true);
            runner.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.8F, distance <= warningDistance / 2 ? 1.8F : 1.2F);
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayer runner : context.getRunners()) {
            runner.removeEffect(MobEffects.GLOWING);
        }
    }

    private static void applyGlow(GameContext context) {
        for (ServerPlayer runner : context.getRunners()) {
            runner.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false, false));
        }
    }
}
