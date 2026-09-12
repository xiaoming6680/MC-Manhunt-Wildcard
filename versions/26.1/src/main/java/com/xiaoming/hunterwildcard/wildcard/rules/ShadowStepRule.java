package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Land a hit on another player and they blink 1 to 4 blocks away; you hit an afterimage. */
public class ShadowStepRule implements WildcardRule {
    private static final int STEP_COOLDOWN_TICKS = 10;
    private static final double MIN_DISTANCE = 1.0;
    private static final double MAX_DISTANCE = 4.0;

    private final Map<UUID, Integer> lastStepTick = new HashMap<>();
    private int ticks;

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        lastStepTick.clear();
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
    }

    @Override
    public void onDamageDealt(GameContext context, ServerPlayer attacker, LivingEntity victim, float damageDealt) {
        if (!(victim instanceof ServerPlayer target) || target == attacker || !(target.level() instanceof ServerLevel world)) {
            return;
        }
        Integer last = lastStepTick.get(target.getUUID());
        if (last != null && ticks - last < STEP_COOLDOWN_TICKS) {
            return;
        }
        if (!HurtTeleportRule.blinkRandomly(world, target, context.getRandom(), MIN_DISTANCE, MAX_DISTANCE)) {
            return;
        }
        lastStepTick.put(target.getUUID(), ticks);
        target.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.shadow_step.stepped"), true);
        attacker.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.shadow_step.missed"), true);
    }

    @Override
    public void onStop(GameContext context) {
        lastStepTick.clear();
    }
}
