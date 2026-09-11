package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

/** Eating anything grants Speed VI for ten seconds. Food is now fuel. */
public class HungerChaseRule implements WildcardRule {
    private static final int SPEED_TICKS = 200;
    private static final int SPEED_AMPLIFIER = 5;

    @Override
    public void onPlayerAteFood(GameContext context, ServerPlayerEntity player, ItemStack eatenStack) {
        if (eatenStack.isEmpty() || !eatenStack.contains(DataComponentTypes.FOOD)) {
            return;
        }

        player.removeStatusEffect(StatusEffects.SPEED);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, SPEED_TICKS, SPEED_AMPLIFIER, false, false, true));
        player.playSound(SoundEvents.ENTITY_PLAYER_BURP, 0.8F, 1.4F);
        player.sendMessage(HunterWildcardText.translatable("msg.wildcard.hunger_chase.boost").formatted(Formatting.GOLD), true);
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            StatusEffectInstance speed = player.getStatusEffect(StatusEffects.SPEED);
            if (speed != null && speed.getAmplifier() == SPEED_AMPLIFIER) {
                player.removeStatusEffect(StatusEffects.SPEED);
            }
        }
    }
}
