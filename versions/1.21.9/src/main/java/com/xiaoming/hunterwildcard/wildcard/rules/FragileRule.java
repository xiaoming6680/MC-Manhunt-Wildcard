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
 * "Fragile": every participant's max health drops to three hearts (6 HP) for the duration.
 * Current health is clamped when the cap drops and scaled back up proportionally when it ends.
 */
public class FragileRule implements WildcardRule {
    private static final Identifier MODIFIER_ID = Identifier.of(HunterWildcardMod.MOD_ID, "fragile");
    private static final float FRAGILE_MAX_HEALTH = 6.0F;
    private static final float VANILLA_MAX_HEALTH = 20.0F;
    private static final int REFRESH_INTERVAL_TICKS = 20;

    @Override
    public void onStart(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            applyCap(player);
        }
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        // Respawned players come back with a fresh attribute container.
        for (ServerPlayerEntity player : context.getParticipants()) {
            applyCap(player);
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            removeCap(player);
        }
    }

    public static void applyCap(ServerPlayerEntity player) {
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (instance == null || instance.hasModifier(MODIFIER_ID)) {
            return;
        }

        double base = instance.getBaseValue();
        instance.addTemporaryModifier(new EntityAttributeModifier(MODIFIER_ID, FRAGILE_MAX_HEALTH - base, EntityAttributeModifier.Operation.ADD_VALUE));
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static void removeCap(ServerPlayerEntity player) {
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (instance == null || !instance.hasModifier(MODIFIER_ID)) {
            return;
        }

        float ratio = player.getMaxHealth() <= 0.0F ? 1.0F : player.getHealth() / player.getMaxHealth();
        instance.removeModifier(MODIFIER_ID);
        float restored = Math.max(1.0F, Math.min(player.getMaxHealth(), ratio * Math.max(VANILLA_MAX_HEALTH, player.getMaxHealth())));
        player.setHealth(restored);
    }
}
