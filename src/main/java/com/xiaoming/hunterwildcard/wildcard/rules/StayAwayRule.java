package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;

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
        if (stack.isEmpty() || !stack.isOf(Items.GOLDEN_SWORD)) {
            return false;
        }

        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getBoolean(DATA_KEY, false);
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
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            removeSwords(player);
        }
        for (ServerWorld world : server.getWorlds()) {
            List<Entity> toDiscard = new ArrayList<>();
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof ItemEntity item && isStayAwaySword(item.getStack())) {
                    toDiscard.add(entity);
                }
            }
            toDiscard.forEach(Entity::discard);
        }
    }

    private void giveMissingSwords(GameContext context) {
        for (ServerPlayerEntity runner : context.getRunners()) {
            if (hasSword(runner)) {
                continue;
            }

            ItemStack sword = createSword(context.getServer());
            if (!runner.getInventory().insertStack(sword)) {
                runner.dropItem(sword, false, true);
            }
            runner.sendMessage(HunterWildcardText.translatable("msg.wildcard.stay_away.received"), true);
        }
    }

    private static boolean hasSword(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (isStayAwaySword(inventory.getStack(slot))) {
                return true;
            }
        }
        return false;
    }

    private static void removeSwords(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (isStayAwaySword(inventory.getStack(slot))) {
                inventory.setStack(slot, ItemStack.EMPTY);
            }
        }
    }

    private static ItemStack createSword(MinecraftServer server) {
        ItemStack stack = new ItemStack(Items.GOLDEN_SWORD);
        Registry<Enchantment> enchantments = server.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
        stack.addEnchantment(sharpness, SHARPNESS_LEVEL);
        stack.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        stack.set(DataComponentTypes.ITEM_NAME, HunterWildcardText.translatable("item.stay_away_sword").formatted(Formatting.GOLD));
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(DATA_KEY, true));
        return stack;
    }
}
