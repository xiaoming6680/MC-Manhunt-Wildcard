package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.util.ActionResult;

import java.util.ArrayList;
import java.util.UUID;

public class PearlFrenzyRule implements WildcardRule {
    private static final int SIDE_EFFECT_TICKS = 600;
    private static final String DATA_KEY = "hunterwildcard_pearl_frenzy";
    private static String activeSession;
    private int ticks;

    public static void registerEvents() {
        // Clean up pearls retrieved from storage or restored after a death/reconnect, including old sessions.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) removeExpiredFromPlayers(server);
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item && isExpiredPearl(item.getStack())) item.discard();
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient() && isExpiredPearl(player.getStackInHand(hand))) {
                player.setStackInHand(hand, ItemStack.EMPTY);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> activeSession = null);
    }

    public static boolean isFrenzyPearl(ItemStack stack) {
        if (!stack.isOf(Items.ENDER_PEARL)) return false;
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getString(DATA_KEY).isPresent();
    }

    public static boolean isExpiredPearl(ItemStack stack) {
        if (!isFrenzyPearl(stack)) return false;
        return !stack.get(DataComponentTypes.CUSTOM_DATA).copyNbt().getString(DATA_KEY, "").equals(activeSession);
    }

    private static void removeExpired(Inventory inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (isExpiredPearl(inventory.getStack(slot))) inventory.setStack(slot, ItemStack.EMPTY);
        }
    }

    private static void removeExpiredFromPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            removeExpired(player.getInventory());
            removeExpired(player.getEnderChestInventory());
            var screen = player.currentScreenHandler;
            if (isExpiredPearl(screen.getCursorStack())) screen.setCursorStack(ItemStack.EMPTY);
            for (var slot : screen.slots) if (isExpiredPearl(slot.getStack())) slot.setStack(ItemStack.EMPTY);
            screen.sendContentUpdates();
        }
    }

    @Override
    public void onStart(GameContext context) {
        activeSession = UUID.randomUUID().toString();
        removeExpiredFromPlayers(context.getServer());
        ticks = 0;
        for (ServerPlayerEntity player : context.getParticipants()) {
            giveUpTo(player, Items.ENDER_PEARL, 2, context.getConfig().pearlFrenzyMaxPearls);
        }
    }

    @Override
    public void onStop(GameContext context) {
        activeSession = null;
        removeExpiredFromPlayers(context.getServer());
        for (ServerWorld world : context.getServer().getWorlds()) {
            var drops = new ArrayList<ItemEntity>();
            for (var entity : world.iterateEntities()) {
                if (entity instanceof ItemEntity item && isExpiredPearl(item.getStack())) drops.add(item);
            }
            drops.forEach(ItemEntity::discard);
        }
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (remainingTicks <= 0 || ticks % context.getConfig().getPearlFrenzyIntervalTicks() != 0) {
            return;
        }

        for (ServerPlayerEntity player : context.getParticipants()) {
            giveUpTo(player, Items.ENDER_PEARL, 1, context.getConfig().pearlFrenzyMaxPearls);
        }
    }

    @Override
    public void onItemUse(GameContext context, ServerPlayerEntity player, Hand hand, ItemStack stack) {
        if (!stack.isOf(Items.ENDER_PEARL)) {
            return;
        }

        int roll = context.getRandom().nextInt(100);
        if (roll < 40) {
            return;
        }

        if (roll < 65) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, SIDE_EFFECT_TICKS, 0, false, false, true));
        } else if (roll < 85) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, SIDE_EFFECT_TICKS, 0, false, false, true));
        } else if (roll < 95) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, SIDE_EFFECT_TICKS, 0, false, false, true));
        } else if (player.getEntityWorld() instanceof ServerWorld world) {
            player.damage(world, player.getDamageSources().magic(), 2.0F);
        }
        player.sendMessage(HunterWildcardText.translatable("msg.wildcard.pearl_frenzy.side_effect"), false);
    }

    private void giveUpTo(ServerPlayerEntity player, Item item, int amount, int maxHeld) {
        int current = countItem(player, item);
        int toGive = Math.min(amount, Math.max(0, maxHeld - current));
        if (toGive > 0) {
            ItemStack stack = new ItemStack(item, toGive);
            NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putString(DATA_KEY, activeSession));
            player.getInventory().offerOrDrop(stack);
        }
    }

    private int countItem(ServerPlayerEntity player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
