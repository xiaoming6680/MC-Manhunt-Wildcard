package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import java.util.ArrayList;
import java.util.List;

/**
 * "Stay away!": runners get a Sharpness 255 golden sword. It is unbreakable, cannot be dropped
 * (see PlayerDropItemMixin) and hunters cannot pick it up (see ItemEntityPickupMixin).
 * Every copy is removed from inventories and the ground when the wildcard ends.
 */
public class StayAwayRule implements WildcardRule {
    private static final String DATA_KEY = "hunterwildcard_stay_away_sword";
    private static final int SHARPNESS_LEVEL = 255;
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private static boolean active;

    public static boolean isActive() {
        return active;
    }

    public static boolean isStayAwaySword(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.GOLDEN_SWORD)) {
            return false;
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBooleanOr(DATA_KEY, false);
    }

    @Override
    public void onStart(GameContext context) {
        active = true;
        giveMissingSwords(context);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        // Runners that respawned (or somehow lost the sword) get a fresh one.
        giveMissingSwords(context);
    }

    @Override
    public void onStop(GameContext context) {
        active = false;
        MinecraftServer server = context.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            removeSwords(player);
        }
        for (ServerLevel world : server.getAllLevels()) {
            List<Entity> toDiscard = new ArrayList<>();
            for (Entity entity : world.getAllEntities()) {
                if (entity instanceof ItemEntity item && isStayAwaySword(item.getItem())) {
                    toDiscard.add(entity);
                }
            }
            toDiscard.forEach(Entity::discard);
        }
    }

    private void giveMissingSwords(GameContext context) {
        for (ServerPlayer runner : context.getRunners()) {
            if (hasSword(runner)) {
                continue;
            }

            ItemStack sword = createSword(context.getServer());
            if (!runner.getInventory().add(sword)) {
                runner.drop(sword, false, true);
            }
            runner.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.stay_away.received"), true);
        }
    }

    private static boolean hasSword(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isStayAwaySword(inventory.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private static void removeSwords(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isStayAwaySword(inventory.getItem(slot))) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private static ItemStack createSword(MinecraftServer server) {
        ItemStack stack = new ItemStack(Items.GOLDEN_SWORD);
        Registry<Enchantment> enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
        stack.enchant(sharpness, SHARPNESS_LEVEL);
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        stack.set(DataComponents.ITEM_NAME, HunterWildcardText.translatable("item.stay_away_sword").withStyle(ChatFormatting.GOLD));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(DATA_KEY, true));
        return stack;
    }
}
