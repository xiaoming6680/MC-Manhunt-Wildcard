package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Sneaking freezes you in place: invisible and immune to damage, but you cannot move, jump or hurt anyone.
 * Standing back up ends it instantly.
 */
public class SneakFreezeRule implements WildcardRule {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(HunterWildcardMod.MOD_ID, "sneak_freeze");
    private static final int INVISIBILITY_REFRESH_TICKS = 5;
    private static final int INVISIBILITY_TICKS = 15;

    private final Set<UUID> frozen = new HashSet<>();
    private int ticks;

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        frozen.clear();
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        for (ServerPlayer player : context.getParticipants()) {
            boolean wantsFreeze = player.isShiftKeyDown() && player.isAlive();
            boolean isFrozen = frozen.contains(player.getUUID());
            if (wantsFreeze && !isFrozen) {
                freeze(player);
            } else if (!wantsFreeze && isFrozen) {
                thaw(player);
            } else if (wantsFreeze && ticks % INVISIBILITY_REFRESH_TICKS == 0) {
                applyInvisibility(player);
            }
        }
    }

    @Override
    public float modifyDamage(GameContext context, LivingEntity victim, DamageSource source, float amount) {
        if (victim instanceof ServerPlayer victimPlayer && frozen.contains(victimPlayer.getUUID())) {
            return 0.0F;
        }
        if (source.getEntity() instanceof ServerPlayer attacker && frozen.contains(attacker.getUUID())) {
            return 0.0F;
        }
        return amount;
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayer player : context.getParticipants()) {
            if (frozen.contains(player.getUUID())) {
                thaw(player);
            }
        }
        frozen.clear();
    }

    private void freeze(ServerPlayer player) {
        frozen.add(player.getUUID());
        addLock(player, Attributes.MOVEMENT_SPEED);
        addLock(player, Attributes.JUMP_STRENGTH);
        player.setDeltaMovement(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0);
        player.needsSync = true;
        applyInvisibility(player);
        player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.sneak_freeze.frozen"), true);
    }

    private void thaw(ServerPlayer player) {
        frozen.remove(player.getUUID());
        removeLock(player, Attributes.MOVEMENT_SPEED);
        removeLock(player, Attributes.JUMP_STRENGTH);
        player.removeEffect(MobEffects.INVISIBILITY);
    }

    private static void applyInvisibility(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_TICKS, 0, false, false, false));
    }

    private static void addLock(ServerPlayer player, Holder<Attribute> attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && !instance.hasModifier(MODIFIER_ID)) {
            instance.addTransientModifier(new AttributeModifier(MODIFIER_ID, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeLock(ServerPlayer player, Holder<Attribute> attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.hasModifier(MODIFIER_ID)) {
            instance.removeModifier(MODIFIER_ID);
        }
    }
}
