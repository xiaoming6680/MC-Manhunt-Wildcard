package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Shrinks every participant to half size (about one block tall) using the vanilla scale attribute.
 * Hitbox, eye height and camera all follow the attribute automatically.
 */
public class TinyPlayersRule implements WildcardRule {
    private static final Identifier MODIFIER_ID = Identifier.of(HunterWildcardMod.MOD_ID, "tiny_players");
    private static final double SCALE_MULTIPLIER = -0.5D;
    private static final int REFRESH_INTERVAL_TICKS = 20;

    @Override
    public void onStart(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            applyScale(player);
        }
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        // Respawned players get a fresh attribute container, so re-apply for anyone missing it.
        for (ServerPlayerEntity player : context.getParticipants()) {
            applyScale(player);
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            removeScale(player);
        }
    }

    public static void applyScale(ServerPlayerEntity player) {
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.SCALE);
        if (instance == null || instance.hasModifier(MODIFIER_ID)) {
            return;
        }

        instance.addTemporaryModifier(new EntityAttributeModifier(MODIFIER_ID, SCALE_MULTIPLIER, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        player.calculateDimensions();
    }

    public static void removeScale(ServerPlayerEntity player) {
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.SCALE);
        if (instance == null || !instance.hasModifier(MODIFIER_ID)) {
            return;
        }

        instance.removeModifier(MODIFIER_ID);
        player.calculateDimensions();
    }
}
