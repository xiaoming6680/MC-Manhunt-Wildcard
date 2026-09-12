package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKeys;

import java.util.ArrayList;

final class BlazeRodAssertions {
    static void run(ServerPlayerEntity player) {
        var config = GameManager.getInstance().getConfig();
        var world = player.getServerWorld();
        var blaze = new BlazeEntity(EntityType.BLAZE, world);
        blaze.damage(player.getDamageSources().playerAttack(player), 1);
        config.blazeRodChanceEnabled = true;
        for (int chance : new int[]{0, 25, 100}) {
            config.blazeRodChancePercent = chance;
            int successful = 0;
            blaze.getRandom().setSeed(812);
            for (int i = 0; i < 400; i++) if (rods(blaze, player, true) > 0) successful++;
            check(chance == 0 ? successful == 0 : chance == 100 ? successful == 400 : successful >= 65 && successful <= 135,
                    "Configured drop chance " + chance + ": " + successful + " / 400");
        }
        check(rods(blaze, player, false) == 0, "Non-player kills retain vanilla no-rod rule");
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).entryOf(Enchantments.LOOTING), 3);
        player.setStackInHand(Hand.MAIN_HAND, sword);
        int max = 0;
        for (int i = 0; i < 100; i++) max = Math.max(max, rods(blaze, player, true));
        check(max > 1, "Successful drops retain vanilla Looting quantity bonuses");
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        config.blazeRodChanceEnabled = false;
        int vanilla = 0;
        for (int i = 0; i < 400; i++) if (rods(blaze, player, true) > 0) vanilla++;
        check(vanilla > 140 && vanilla < 260, "Disabling override restores vanilla drop rolls");
    }

    private static int rods(BlazeEntity blaze, ServerPlayerEntity player, boolean playerKill) {
        try {
            var method = net.minecraft.entity.LivingEntity.class.getDeclaredMethod("dropLoot", net.minecraft.entity.damage.DamageSource.class, boolean.class);
            method.setAccessible(true);
            method.invoke(blaze, player.getDamageSources().playerAttack(player), playerKill);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        var drops = player.getServerWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class, blaze.getBoundingBox().expand(4), item -> item.getStack().isOf(Items.BLAZE_ROD));
        int count = drops.stream().mapToInt(item -> item.getStack().getCount()).sum();
        drops.forEach(net.minecraft.entity.ItemEntity::discard);
        return count;
    }

    private static void check(boolean passed, String message) { if (!passed) throw new AssertionError(message); }
}
