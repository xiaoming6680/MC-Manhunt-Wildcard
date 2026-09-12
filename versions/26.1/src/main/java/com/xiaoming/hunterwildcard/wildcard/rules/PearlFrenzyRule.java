package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
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
            if (server.getTickCount() % 20 == 0) removeExpiredFromPlayers(server);
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item && isExpiredPearl(item.getItem())) item.discard();
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide() && isExpiredPearl(player.getItemInHand(hand))) {
                player.setItemInHand(hand, ItemStack.EMPTY);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> activeSession = null);
    }

    public static boolean isFrenzyPearl(ItemStack stack) {
        if (!stack.is(Items.ENDER_PEARL)) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getString(DATA_KEY).isPresent();
    }

    public static boolean isExpiredPearl(ItemStack stack) {
        if (!isFrenzyPearl(stack)) return false;
        return !stack.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr(DATA_KEY, "").equals(activeSession);
    }

    private static void removeExpired(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isExpiredPearl(inventory.getItem(slot))) inventory.setItem(slot, ItemStack.EMPTY);
        }
    }

    private static void removeExpiredFromPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            removeExpired(player.getInventory());
            removeExpired(player.getEnderChestInventory());
            var screen = player.containerMenu;
            if (isExpiredPearl(screen.getCarried())) screen.setCarried(ItemStack.EMPTY);
            for (var slot : screen.slots) if (isExpiredPearl(slot.getItem())) slot.setByPlayer(ItemStack.EMPTY);
            screen.broadcastChanges();
        }
    }

    @Override
    public void onStart(GameContext context) {
        activeSession = UUID.randomUUID().toString();
        removeExpiredFromPlayers(context.getServer());
        ticks = 0;
        for (ServerPlayer player : context.getParticipants()) {
            giveUpTo(player, Items.ENDER_PEARL, 2, context.getConfig().pearlFrenzyMaxPearls);
        }
    }

    @Override
    public void onStop(GameContext context) {
        activeSession = null;
        removeExpiredFromPlayers(context.getServer());
        for (ServerLevel world : context.getServer().getAllLevels()) {
            var drops = new ArrayList<ItemEntity>();
            for (var entity : world.getAllEntities()) {
                if (entity instanceof ItemEntity item && isExpiredPearl(item.getItem())) drops.add(item);
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

        for (ServerPlayer player : context.getParticipants()) {
            giveUpTo(player, Items.ENDER_PEARL, 1, context.getConfig().pearlFrenzyMaxPearls);
        }
    }

    @Override
    public void onItemUse(GameContext context, ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (!stack.is(Items.ENDER_PEARL)) {
            return;
        }

        int roll = context.getRandom().nextInt(100);
        if (roll < 40) {
            return;
        }

        if (roll < 65) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SIDE_EFFECT_TICKS, 0, false, false, true));
        } else if (roll < 85) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SIDE_EFFECT_TICKS, 0, false, false, true));
        } else if (roll < 95) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, SIDE_EFFECT_TICKS, 0, false, false, true));
        } else if (player.level() instanceof ServerLevel world) {
            player.hurtServer(world, player.damageSources().magic(), 2.0F);
        }
        player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.pearl_frenzy.side_effect"), false);
    }

    private void giveUpTo(ServerPlayer player, Item item, int amount, int maxHeld) {
        int current = countItem(player, item);
        int toGive = Math.min(amount, Math.max(0, maxHeld - current));
        if (toGive > 0) {
            ItemStack stack = new ItemStack(item, toGive);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putString(DATA_KEY, activeSession));
            player.getInventory().placeItemBackInInventory(stack);
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
