package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

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
        for (ServerPlayerEntity player : context.getParticipants()) {
            player.removeStatusEffect(StatusEffects.SPEED);
        }
    }

    private void apply(GameContext context, int duration) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            player.removeStatusEffect(StatusEffects.SPEED);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, SPEED_AMPLIFIER, false, false, true));
        }
    }
}
