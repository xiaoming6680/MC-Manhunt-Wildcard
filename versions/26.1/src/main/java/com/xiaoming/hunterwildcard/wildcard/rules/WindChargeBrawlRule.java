package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class WindChargeBrawlRule implements WildcardRule {
    private static final int MAX_CHARGES = 16;
    private int ticks;

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        for (ServerPlayer player : context.getParticipants()) {
            giveUpTo(player, Items.WIND_CHARGE, 1, MAX_CHARGES);
        }
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (remainingTicks <= 0 || ticks % context.getConfig().getWindChargeBrawlIntervalTicks() != 0) {
            return;
        }

        for (ServerPlayer player : context.getParticipants()) {
            giveUpTo(player, Items.WIND_CHARGE, 1, MAX_CHARGES);
        }
    }

    public float getExplosionPowerMultiplier(ModConfig config) {
        return config.getWindChargeExplosionMultiplier();
    }

    private void giveUpTo(ServerPlayer player, Item item, int amount, int maxHeld) {
        int current = countItem(player, item);
        int toGive = Math.min(amount, Math.max(0, maxHeld - current));
        if (toGive > 0) {
            player.getInventory().placeItemBackInInventory(new ItemStack(item, toGive));
        }
    }

    private int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
