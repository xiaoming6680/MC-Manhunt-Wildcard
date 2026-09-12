package com.xiaoming.hunterwildcard.game;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Per-role movement speed multipliers from config. Applied as a temporary attribute modifier
 * while a round is running and removed on cleanup; re-applied every second so respawns pick it up.
 */
public class RoleModifierManager {
    private static final Identifier SPEED_MODIFIER_ID = Identifier.of(HunterWildcardMod.MOD_ID, "role_speed");
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private int ticks;

    public void tick(GameContext context) {
        ticks++;
        if (ticks % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        apply(context);
    }

    public void apply(GameContext context) {
        ModConfig config = context.getConfig();
        for (ServerPlayerEntity player : context.getParticipants()) {
            PlayerRole role = context.getTeamManager().getRole(player);
            int percent = role == PlayerRole.HUNTER ? config.hunterSpeedPercent
                    : role == PlayerRole.RUNNER ? config.runnerSpeedPercent
                    : 100;
            applySpeed(player, percent);
        }
    }

    public void clear(GameContext context) {
        ticks = 0;
        for (ServerPlayerEntity player : context.getParticipants()) {
            removeSpeed(player);
        }
    }

    public static void applySpeed(ServerPlayerEntity player, int percent) {
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (instance == null) {
            return;
        }

        double multiplier = Math.max(1, percent) / 100.0D - 1.0D;
        if (Math.abs(multiplier) < 1.0E-6D) {
            instance.removeModifier(SPEED_MODIFIER_ID);
            return;
        }

        EntityAttributeModifier existing = instance.getModifier(SPEED_MODIFIER_ID);
        if (existing != null && Math.abs(existing.value() - multiplier) < 1.0E-6D) {
            return;
        }

        instance.removeModifier(SPEED_MODIFIER_ID);
        instance.addTemporaryModifier(new EntityAttributeModifier(SPEED_MODIFIER_ID, multiplier, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void removeSpeed(ServerPlayerEntity player) {
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (instance != null) {
            instance.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
