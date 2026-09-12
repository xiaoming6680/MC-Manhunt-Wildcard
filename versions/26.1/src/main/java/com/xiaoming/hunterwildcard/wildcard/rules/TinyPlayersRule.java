package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Shrinks every participant to half size (about one block tall) using the vanilla scale attribute.
 * Hitbox, eye height and camera all follow the attribute automatically.
 */
public class TinyPlayersRule implements WildcardRule {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(HunterWildcardMod.MOD_ID, "tiny_players");
    private static final double SCALE_MULTIPLIER = -0.5D;
    private static final int REFRESH_INTERVAL_TICKS = 20;

    @Override
    public void onStart(GameContext context) {
        for (ServerPlayer player : context.getParticipants()) {
            applyScale(player);
        }
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        // Respawned players get a fresh attribute container, so re-apply for anyone missing it.
        for (ServerPlayer player : context.getParticipants()) {
            applyScale(player);
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayer player : context.getParticipants()) {
            removeScale(player);
        }
    }

    public static void applyScale(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(Attributes.SCALE);
        if (instance == null || instance.hasModifier(MODIFIER_ID)) {
            return;
        }

        instance.addTransientModifier(new AttributeModifier(MODIFIER_ID, SCALE_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        player.refreshDimensions();
    }

    public static void removeScale(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(Attributes.SCALE);
        if (instance == null || !instance.hasModifier(MODIFIER_ID)) {
            return;
        }

        instance.removeModifier(MODIFIER_ID);
        player.refreshDimensions();
    }
}
