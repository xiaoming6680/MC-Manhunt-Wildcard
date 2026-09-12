package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Everyone gets Speed X. Good luck steering. */
public class FlashRule implements WildcardRule {
    private static final int SPEED_AMPLIFIER = 9;

    @Override
    public void onStart(GameContext context) {
        apply(context, 140);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks % 100 == 0) {
            apply(context, Math.max(120, remainingTicks + 40));
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayer player : context.getParticipants()) {
            player.removeEffect(MobEffects.SPEED);
        }
    }

    private void apply(GameContext context, int duration) {
        for (ServerPlayer player : context.getParticipants()) {
            player.removeEffect(MobEffects.SPEED);
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, SPEED_AMPLIFIER, false, false, true));
        }
    }
}
