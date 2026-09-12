package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.game.GameManager;
import java.util.ArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

final class BlazeRodAssertions {
    static void run(ServerPlayer player) {
        var config = GameManager.getInstance().getConfig();
        var world = player.level();
        var blaze = new Blaze(EntityType.BLAZE, world);
        blaze.hurtServer(world, player.damageSources().playerAttack(player), 1);
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
        sword.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        int max = 0;
        for (int i = 0; i < 100; i++) max = Math.max(max, rods(blaze, player, true));
        check(max > 1, "Successful drops retain vanilla Looting quantity bonuses");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        config.blazeRodChanceEnabled = false;
        int vanilla = 0;
        for (int i = 0; i < 400; i++) if (rods(blaze, player, true) > 0) vanilla++;
        check(vanilla > 140 && vanilla < 260, "Disabling override restores vanilla drop rolls");
    }

    private static int rods(Blaze blaze, ServerPlayer player, boolean playerKill) {
        var drops = new ArrayList<ItemStack>();
        blaze.dropFromLootTable(player.level(), player.damageSources().playerAttack(player), playerKill,
                blaze.getLootTable().orElseThrow(), drops::add);
        return drops.stream().filter(stack -> stack.is(Items.BLAZE_ROD)).mapToInt(ItemStack::getCount).sum();
    }

    private static void check(boolean passed, String message) { if (!passed) throw new AssertionError(message); }
}
