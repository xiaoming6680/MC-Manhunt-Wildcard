package com.xiaoming.hunterwildcard.compass;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.CompassTargetEntry;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.util.PlayerUtil;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker;

/**
 * The hunters' tracking compass. Each hunter either follows the nearest runner (default) or a runner
 * they picked: sneak + right-click cycles through nearest / each runner, plain right-click opens a
 * selection menu. The item name always shows the current target and distance.
 */
public class CompassTracker {
    private static final String LEGACY_TRACKING_COMPASS_NAME = "追猎指南针";
    private static final String LEGACY_COMPASS_NAME = "猎人指南针";
    private static final String COMPASS_DATA_KEY = "hunterwildcard_compass";

    /** hunter -> chosen runner; absent means "nearest runner". */
    private final Map<UUID, UUID> selectedTargets = new HashMap<>();
    private int updateTicks;

    public void reset() {
        updateTicks = 0;
        selectedTargets.clear();
    }

    public void tick(GameContext context, WildcardRule activeRule) {
        updateTicks--;
        if (updateTicks > 0) {
            return;
        }

        updateTicks = context.getConfig().getCompassUpdateTicks();
        updateHunterCompasses(context, activeRule);
    }

    public void onConfigChanged(ModConfig config) {
        if (updateTicks > config.getCompassUpdateTicks()) {
            updateTicks = config.getCompassUpdateTicks();
        }
    }

    public void giveCompasses(GameContext context) {
        for (ServerPlayer hunter : context.getHunters()) {
            giveCompass(hunter);
        }
    }

    public void giveCompass(ServerPlayer hunter) {
        if (ensureSingleHunterCompass(hunter)) {
            return;
        }

        ItemStack stack = createCompass();
        if (!hunter.getInventory().add(stack)) {
            hunter.drop(stack, false);
        }
    }

    public void clear(GameContext context) {
        for (ServerPlayer player : context.getParticipants()) {
            removeCompass(player);
        }
        selectedTargets.clear();
    }

    public void removeCompass(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isHunterCompass(stack)) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    public void removeRunnerCompasses(GameContext context) {
        for (ServerPlayer runner : context.getRunners()) {
            removeCompass(runner);
        }
    }

    public boolean normalizeHunterCompasses(ServerPlayer hunter) {
        return ensureSingleHunterCompass(hunter);
    }

    public boolean isHunterCompass(ItemStack stack) {
        if (!stack.is(Items.COMPASS)) {
            return false;
        }

        return isTaggedHunterCompass(stack) || isLegacyHunterCompass(stack);
    }

    /** Sneak + right-click: nearest -> runner 1 -> runner 2 -> ... -> nearest. */
    public void cycleTarget(GameContext context, ServerPlayer hunter) {
        List<ServerPlayer> runners = trackableRunners(context);
        UUID current = selectedTargets.get(hunter.getUUID());
        int index = -1;
        for (int i = 0; i < runners.size(); i++) {
            if (runners.get(i).getUUID().equals(current)) {
                index = i;
                break;
            }
        }
        int next = index + 1;
        if (next >= runners.size()) {
            selectedTargets.remove(hunter.getUUID());
        } else {
            selectedTargets.put(hunter.getUUID(), runners.get(next).getUUID());
        }
        announceTarget(context, hunter);
        updateTicks = Math.min(updateTicks, 1);
    }

    /** Right-click: opens the client-side selection menu. */
    public void openMenu(GameContext context, ServerPlayer hunter) {
        UUID selected = selectedTargets.get(hunter.getUUID());
        List<CompassTargetEntry> entries = new ArrayList<>();
        for (ServerPlayer runner : trackableRunners(context)) {
            boolean sameDimension = runner.level() == hunter.level();
            int distance = sameDimension ? PlayerUtil.roundDistance(Math.sqrt(hunter.distanceToSqr(runner))) : -1;
            entries.add(new CompassTargetEntry(runner.getUUID(), PlayerUtil.displayNameSpec(runner), distance, sameDimension, runner.getUUID().equals(selected)));
        }
        HunterWildcardPackets.sendCompassMenu(hunter, entries, selected == null);
    }

    /** Menu choice; null means nearest. */
    public void selectTarget(GameContext context, ServerPlayer hunter, UUID target) {
        if (target == null) {
            selectedTargets.remove(hunter.getUUID());
        } else {
            selectedTargets.put(hunter.getUUID(), target);
        }
        announceTarget(context, hunter);
        updateTicks = Math.min(updateTicks, 1);
    }

    private void announceTarget(GameContext context, ServerPlayer hunter) {
        ServerPlayer target = resolveTarget(context, hunter, trackableRunners(context));
        Component name = target == null
                ? HunterWildcardText.translatable("common.none")
                : PlayerUtil.displayNameText(target);
        Component message = selectedTargets.containsKey(hunter.getUUID())
                ? HunterWildcardText.translatable("msg.compass.target_selected", name)
                : HunterWildcardText.translatable("msg.compass.target_nearest", name);
        hunter.sendSystemMessage(message.copy().withStyle(ChatFormatting.AQUA), true);
    }

    private List<ServerPlayer> trackableRunners(GameContext context) {
        List<ServerPlayer> runners = new ArrayList<>();
        for (ServerPlayer runner : context.getRunners()) {
            if (!runner.isSpectator()) {
                runners.add(runner);
            }
        }
        return runners;
    }

    private ServerPlayer resolveTarget(GameContext context, ServerPlayer hunter, List<ServerPlayer> runners) {
        UUID selected = selectedTargets.get(hunter.getUUID());
        if (selected != null) {
            for (ServerPlayer runner : runners) {
                if (runner.getUUID().equals(selected)) {
                    return runner;
                }
            }
        }
        return PlayerUtil.findNearestRunner(hunter, runners);
    }

    private void updateHunterCompasses(GameContext context, WildcardRule activeRule) {
        List<ServerPlayer> runners = trackableRunners(context);
        for (ServerPlayer hunter : context.getHunters()) {
            if (hunter.isSpectator()) {
                continue;
            }
            giveCompass(hunter);
            ServerPlayer runner = resolveTarget(context, hunter, runners);
            if (runner == null) {
                updateCompassStacks(hunter, null, HunterWildcardText.translatable("item.tracking_compass.no_target"));
                continue;
            }

            BlockPos target = runner.blockPosition();

            GlobalPos globalPos = GlobalPos.of(runner.level().dimension(), target);
            LodestoneTracker tracker = new LodestoneTracker(Optional.of(globalPos), false);
            boolean sameDimension = runner.level() == hunter.level();
            Component name = sameDimension
                    ? HunterWildcardText.translatable("item.tracking_compass.targeting", PlayerUtil.displayNameText(runner),
                            PlayerUtil.roundDistance(Math.sqrt(hunter.distanceToSqr(runner))))
                    : HunterWildcardText.translatable("item.tracking_compass.targeting_other_dimension", PlayerUtil.displayNameText(runner));
            updateCompassStacks(hunter, tracker, name);
        }
    }

    private void updateCompassStacks(ServerPlayer hunter, LodestoneTracker tracker, Component targetLine) {
        boolean updated = false;
        for (int slot = 0; slot < hunter.getInventory().getContainerSize(); slot++) {
            ItemStack stack = hunter.getInventory().getItem(slot);
            if (isHunterCompass(stack)) {
                if (updated) {
                    hunter.getInventory().setItem(slot, ItemStack.EMPTY);
                    continue;
                }

                markHunterCompass(stack);
                if (tracker != null) {
                    stack.set(DataComponents.LODESTONE_TRACKER, tracker);
                }
                MutableComponent name = HunterWildcardText.translatable("item.tracking_compass").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                        .append(targetLine.copy().withStyle(ChatFormatting.YELLOW));
                stack.set(DataComponents.ITEM_NAME, name);
                updated = true;
            }
        }
    }

    private boolean ensureSingleHunterCompass(ServerPlayer hunter) {
        boolean found = false;
        for (int slot = 0; slot < hunter.getInventory().getContainerSize(); slot++) {
            ItemStack stack = hunter.getInventory().getItem(slot);
            if (isHunterCompass(stack)) {
                if (found) {
                    hunter.getInventory().setItem(slot, ItemStack.EMPTY);
                    continue;
                }

                markHunterCompass(stack);
                found = true;
            }
        }

        return found;
    }

    private boolean isTaggedHunterCompass(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }

        return data.copyTag().getBooleanOr(COMPASS_DATA_KEY, false);
    }

    private boolean isLegacyHunterCompass(ItemStack stack) {
        String name = stack.getHoverName().getString();
        return (name.equals(LEGACY_TRACKING_COMPASS_NAME) || name.equals(LEGACY_COMPASS_NAME))
                && Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    private ItemStack createCompass() {
        ItemStack stack = new ItemStack(Items.COMPASS);
        markHunterCompass(stack);
        stack.set(DataComponents.ITEM_NAME, HunterWildcardText.translatable("item.tracking_compass").withStyle(ChatFormatting.AQUA));
        return stack;
    }

    private void markHunterCompass(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(COMPASS_DATA_KEY, true));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                HunterWildcardText.translatable("item.tracking_compass.lore_cycle").withStyle(ChatFormatting.GRAY),
                HunterWildcardText.translatable("item.tracking_compass.lore_menu").withStyle(ChatFormatting.GRAY)
        )));
    }
}
