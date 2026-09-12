package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/** Eating anything grants Speed VI for ten seconds. Food is now fuel. */
public class HungerChaseRule implements WildcardRule {
    private static final int SPEED_TICKS = 200;
    private static final int SPEED_AMPLIFIER = 5;

    @Override
    public void onPlayerAteFood(GameContext context, ServerPlayer player, ItemStack eatenStack) {
        if (eatenStack.isEmpty() || !eatenStack.has(DataComponents.FOOD)) {
            return;
        }

        player.removeEffect(MobEffects.SPEED);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, SPEED_TICKS, SPEED_AMPLIFIER, false, false, true));
        player.playSound(SoundEvents.PLAYER_BURP, 0.8F, 1.4F);
        player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.hunger_chase.boost").withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayer player : context.getParticipants()) {
            MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
            if (speed != null && speed.getAmplifier() == SPEED_AMPLIFIER) {
                player.removeEffect(MobEffects.SPEED);
            }
        }
    }
}
