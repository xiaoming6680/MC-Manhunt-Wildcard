package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface WildcardRule {
    default String getName() {
        return HunterWildcardText.wildcardId(getClass().getSimpleName());
    }

    default String getNameKey() {
        return HunterWildcardText.wildcardNameKey(getName());
    }

    default Component getDisplayName() {
        return HunterWildcardText.wildcardName(getName());
    }

    default String getDescriptionKey() {
        return HunterWildcardText.wildcardDescriptionKey(getName());
    }

    /** How long this rule stays active once drawn. Most rules use the shared (fixed or rolled) config value. */
    default int getDurationTicks(ModConfig config, Random random) {
        return config.rollWildcardDurationTicks(random);
    }

    default void onStart(GameContext context) {
    }

    default void onTick(GameContext context, int remainingTicks) {
    }

    default void onPlayerDeath(GameContext context, ServerPlayer player) {
    }

    default void onEntityKilled(GameContext context, ServerPlayer killer, LivingEntity killed) {
    }

    default void onPlayerAttack(GameContext context, ServerPlayer player, Entity target) {
    }

    default void onPlayerDamaged(GameContext context, ServerPlayer player, DamageSource source, float damageTaken) {
    }

    default void onPlayerAteFood(GameContext context, ServerPlayer player, ItemStack eatenStack) {
    }

    default void onItemUse(GameContext context, ServerPlayer player, InteractionHand hand, ItemStack stack) {
    }

    default void onBlockPlaced(GameContext context, ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
    }

    /** Fired after a participant broke a block ({@code state} is what stood there). */
    default void onBlockBroken(GameContext context, ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
    }

    default void onStop(GameContext context) {
    }

    /** Fired after a participant dealt damage to any living entity ({@code damageDealt} is what actually landed). */
    default void onDamageDealt(GameContext context, ServerPlayer attacker, LivingEntity victim, float damageDealt) {
    }

    /** Fired when a participant throws an item out of their inventory (Q key or dragging out of a screen). */
    default void onItemDropped(GameContext context, ServerPlayer player, ItemEntity item) {
    }

    /**
     * Lets a rule rescale damage before it lands. Called for any living victim when either the victim or the
     * attacker is a participant. Returning 0 for a player victim cancels the hit entirely (no knockback either).
     */
    default float modifyDamage(GameContext context, LivingEntity victim, DamageSource source, float amount) {
        return amount;
    }
}
