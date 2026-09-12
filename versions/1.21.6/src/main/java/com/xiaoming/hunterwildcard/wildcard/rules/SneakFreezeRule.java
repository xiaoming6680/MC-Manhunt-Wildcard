package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Sneaking freezes you in place: invisible and immune to damage, but you cannot move, jump or hurt anyone.
 * Standing back up ends it instantly.
 */
public class SneakFreezeRule implements WildcardRule {
    private static final Identifier MODIFIER_ID = Identifier.of(HunterWildcardMod.MOD_ID, "sneak_freeze");
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
        for (ServerPlayerEntity player : context.getParticipants()) {
            boolean wantsFreeze = player.isSneaking() && player.isAlive();
            boolean isFrozen = frozen.contains(player.getUuid());
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
        if (victim instanceof ServerPlayerEntity victimPlayer && frozen.contains(victimPlayer.getUuid())) {
            return 0.0F;
        }
        if (source.getAttacker() instanceof ServerPlayerEntity attacker && frozen.contains(attacker.getUuid())) {
            return 0.0F;
        }
        return amount;
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            if (frozen.contains(player.getUuid())) {
                thaw(player);
            }
        }
        frozen.clear();
    }

    private void freeze(ServerPlayerEntity player) {
        frozen.add(player.getUuid());
        addLock(player, EntityAttributes.MOVEMENT_SPEED);
        addLock(player, EntityAttributes.JUMP_STRENGTH);
        player.setVelocity(0.0, Math.min(0.0, player.getVelocity().y), 0.0);
        player.velocityDirty = true;
        applyInvisibility(player);
        player.sendMessage(HunterWildcardText.translatable("msg.wildcard.sneak_freeze.frozen"), true);
    }

    private void thaw(ServerPlayerEntity player) {
        frozen.remove(player.getUuid());
        removeLock(player, EntityAttributes.MOVEMENT_SPEED);
        removeLock(player, EntityAttributes.JUMP_STRENGTH);
        player.removeStatusEffect(StatusEffects.INVISIBILITY);
    }

    private static void applyInvisibility(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, INVISIBILITY_TICKS, 0, false, false, false));
    }

    private static void addLock(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null && !instance.hasModifier(MODIFIER_ID)) {
            instance.addTemporaryModifier(new EntityAttributeModifier(MODIFIER_ID, -1.0D, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeLock(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null && instance.hasModifier(MODIFIER_ID)) {
            instance.removeModifier(MODIFIER_ID);
        }
    }
}
