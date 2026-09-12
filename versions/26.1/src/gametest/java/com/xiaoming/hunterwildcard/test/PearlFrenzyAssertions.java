package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.team.TeamManager;
import com.xiaoming.hunterwildcard.wildcard.rules.PearlFrenzyRule;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** Source identity must survive stacking, consumption, transfer and a later wildcard session. */
final class PearlFrenzyAssertions {
    static void run(ClientGameTestContext c, TestSingleplayerContext world, UUID playerId, UUID otherId) {
        AtomicReference<GameContext> context = new AtomicReference<>();
        PearlFrenzyRule rule = new PearlFrenzyRule();
        AtomicReference<ItemStack> expired = new AtomicReference<>();
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayer(playerId);
            var other = server.getPlayerList().getPlayer(otherId);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(Items.ENDER_PEARL, 7));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.ENDER_PEARL, 3));
            ModConfig config = new ModConfig();
            config.pearlFrenzyMaxPearls = 64;
            context.set(new GameContext(server, config, new TeamManager(), new Random(0), List.of(player)));
            rule.onStart(context.get());
            check(count(player.getInventory(), false) == 10 && count(player.getInventory(), true) == 2, "Granted pearls stay separate from original pearls");
            ItemStack granted = ItemStack.EMPTY;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (PearlFrenzyRule.isFrenzyPearl(player.getInventory().getItem(i))) granted = player.getInventory().getItem(i);
            }
            granted.shrink(1); // One granted pearl was used; never subtract it from ordinary pearls later.
            expired.set(granted.copy());
            other.setItemInHand(InteractionHand.OFF_HAND, granted.split(1));
            player.containerMenu.setCarried(expired.get().copy());
            player.getEnderChestInventory().setItem(0, expired.get().copy());
            var dropped = new ItemEntity(player.level(), player.getX() + 20, player.getY(), player.getZ(), expired.get().copy());
            var ordinaryDrop = new ItemEntity(player.level(), player.getX() + 20, player.getY(), player.getZ(), new ItemStack(Items.ENDER_PEARL, 5));
            player.level().addFreshEntity(dropped);
            player.level().addFreshEntity(ordinaryDrop);
            rule.onStop(context.get());
            check(count(player.getInventory(), false) == 10 && count(player.getInventory(), true) == 0, "Original pearls survive cleanup after consumption");
            check(other.getOffhandItem().isEmpty(), "Transferred granted pearl is reclaimed");
            check(player.containerMenu.getCarried().isEmpty() && player.getEnderChestInventory().getItem(0).isEmpty(), "Cursor and ender chest are reclaimed");
            check(dropped.isRemoved() && !ordinaryDrop.isRemoved(), "Only granted ground drops are reclaimed");
            ordinaryDrop.discard();
            rule.onStart(context.get());
            check(PearlFrenzyRule.isExpiredPearl(expired.get()), "A new event does not reactivate old pearls");
            player.getInventory().setItem(5, expired.get().copy()); // Retrieved from previously closed storage.
            player.setItemInHand(InteractionHand.OFF_HAND, expired.get().copy());
            var result = UseItemCallback.EVENT.invoker().interact(player, player.level(), InteractionHand.OFF_HAND);
            check(result == InteractionResult.FAIL && player.getOffhandItem().isEmpty(), "Expired pearl cannot be thrown before cleanup tick");
        });
        c.waitTicks(25);
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayer(playerId);
            check(player.getInventory().getItem(5).isEmpty(), "Retrieved expired pearl is reclaimed on tick");
            check(count(player.getInventory(), true) == 2, "Current event pearls remain usable");
            rule.onStop(context.get());
            check(count(player.getInventory(), false) == 7, "Remaining ordinary pearls are never reclaimed");
        });
    }

    private static int count(net.minecraft.world.Container inventory, boolean granted) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.ENDER_PEARL) && PearlFrenzyRule.isFrenzyPearl(stack) == granted) count += stack.getCount();
        }
        return count;
    }

    private static void check(boolean passed, String message) { if (!passed) throw new AssertionError(message); }
}
