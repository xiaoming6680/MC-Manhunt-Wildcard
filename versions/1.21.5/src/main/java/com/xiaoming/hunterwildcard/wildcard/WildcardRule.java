package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public interface WildcardRule {
    default String getName() {
        return HunterWildcardText.wildcardId(getClass().getSimpleName());
    }

    default String getNameKey() {
        return HunterWildcardText.wildcardNameKey(getName());
    }

    default Text getDisplayName() {
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

    default void onPlayerDeath(GameContext context, ServerPlayerEntity player) {
    }

    default void onEntityKilled(GameContext context, ServerPlayerEntity killer, LivingEntity killed) {
    }

    default void onPlayerAttack(GameContext context, ServerPlayerEntity player, Entity target) {
    }

    default void onPlayerDamaged(GameContext context, ServerPlayerEntity player, DamageSource source, float damageTaken) {
    }

    default void onPlayerAteFood(GameContext context, ServerPlayerEntity player, ItemStack eatenStack) {
    }

    default void onItemUse(GameContext context, ServerPlayerEntity player, Hand hand, ItemStack stack) {
    }

    default void onBlockPlaced(GameContext context, ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
    }

    /** Fired after a participant broke a block ({@code state} is what stood there). */
    default void onBlockBroken(GameContext context, ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
    }

    default void onStop(GameContext context) {
    }

    /** Fired after a participant dealt damage to any living entity ({@code damageDealt} is what actually landed). */
    default void onDamageDealt(GameContext context, ServerPlayerEntity attacker, LivingEntity victim, float damageDealt) {
    }

    /** Fired when a participant throws an item out of their inventory (Q key or dragging out of a screen). */
    default void onItemDropped(GameContext context, ServerPlayerEntity player, ItemEntity item) {
    }

    /**
     * Lets a rule rescale damage before it lands. Called for any living victim when either the victim or the
     * attacker is a participant. Returning 0 for a player victim cancels the hit entirely (no knockback either).
     */
    default float modifyDamage(GameContext context, LivingEntity victim, DamageSource source, float amount) {
        return amount;
    }
}
