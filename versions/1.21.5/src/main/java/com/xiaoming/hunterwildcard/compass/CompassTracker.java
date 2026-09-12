package com.xiaoming.hunterwildcard.compass;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.CompassTargetEntry;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.util.PlayerUtil;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
        for (ServerPlayerEntity hunter : context.getHunters()) {
            giveCompass(hunter);
        }
    }

    public void giveCompass(ServerPlayerEntity hunter) {
        if (ensureSingleHunterCompass(hunter)) {
            return;
        }

        ItemStack stack = createCompass();
        if (!hunter.getInventory().insertStack(stack)) {
            hunter.dropItem(stack, false);
        }
    }

    public void clear(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            removeCompass(player);
        }
        selectedTargets.clear();
    }

    public void removeCompass(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (isHunterCompass(stack)) {
                player.getInventory().setStack(slot, ItemStack.EMPTY);
            }
        }
    }

    public void removeRunnerCompasses(GameContext context) {
        for (ServerPlayerEntity runner : context.getRunners()) {
            removeCompass(runner);
        }
    }

    public boolean normalizeHunterCompasses(ServerPlayerEntity hunter) {
        return ensureSingleHunterCompass(hunter);
    }

    public boolean isHunterCompass(ItemStack stack) {
        if (!stack.isOf(Items.COMPASS)) {
            return false;
        }

        return isTaggedHunterCompass(stack) || isLegacyHunterCompass(stack);
    }

    /** Sneak + right-click: nearest -> runner 1 -> runner 2 -> ... -> nearest. */
    public void cycleTarget(GameContext context, ServerPlayerEntity hunter) {
        List<ServerPlayerEntity> runners = trackableRunners(context);
        UUID current = selectedTargets.get(hunter.getUuid());
        int index = -1;
        for (int i = 0; i < runners.size(); i++) {
            if (runners.get(i).getUuid().equals(current)) {
                index = i;
                break;
            }
        }
        int next = index + 1;
        if (next >= runners.size()) {
            selectedTargets.remove(hunter.getUuid());
        } else {
            selectedTargets.put(hunter.getUuid(), runners.get(next).getUuid());
        }
        announceTarget(context, hunter);
        updateTicks = Math.min(updateTicks, 1);
    }

    /** Right-click: opens the client-side selection menu. */
    public void openMenu(GameContext context, ServerPlayerEntity hunter) {
        UUID selected = selectedTargets.get(hunter.getUuid());
        List<CompassTargetEntry> entries = new ArrayList<>();
        for (ServerPlayerEntity runner : trackableRunners(context)) {
            boolean sameDimension = runner.getWorld() == hunter.getWorld();
            int distance = sameDimension ? PlayerUtil.roundDistance(Math.sqrt(hunter.squaredDistanceTo(runner))) : -1;
            entries.add(new CompassTargetEntry(runner.getUuid(), PlayerUtil.displayNameSpec(runner), distance, sameDimension, runner.getUuid().equals(selected)));
        }
        HunterWildcardPackets.sendCompassMenu(hunter, entries, selected == null);
    }

    /** Menu choice; null means nearest. */
    public void selectTarget(GameContext context, ServerPlayerEntity hunter, UUID target) {
        if (target == null) {
            selectedTargets.remove(hunter.getUuid());
        } else {
            selectedTargets.put(hunter.getUuid(), target);
        }
        announceTarget(context, hunter);
        updateTicks = Math.min(updateTicks, 1);
    }

    private void announceTarget(GameContext context, ServerPlayerEntity hunter) {
        ServerPlayerEntity target = resolveTarget(context, hunter, trackableRunners(context));
        Text name = target == null
                ? HunterWildcardText.translatable("common.none")
                : PlayerUtil.displayNameText(target);
        Text message = selectedTargets.containsKey(hunter.getUuid())
                ? HunterWildcardText.translatable("msg.compass.target_selected", name)
                : HunterWildcardText.translatable("msg.compass.target_nearest", name);
        hunter.sendMessage(message.copy().formatted(Formatting.AQUA), true);
    }

    private List<ServerPlayerEntity> trackableRunners(GameContext context) {
        List<ServerPlayerEntity> runners = new ArrayList<>();
        for (ServerPlayerEntity runner : context.getRunners()) {
            if (!runner.isSpectator()) {
                runners.add(runner);
            }
        }
        return runners;
    }

    private ServerPlayerEntity resolveTarget(GameContext context, ServerPlayerEntity hunter, List<ServerPlayerEntity> runners) {
        UUID selected = selectedTargets.get(hunter.getUuid());
        if (selected != null) {
            for (ServerPlayerEntity runner : runners) {
                if (runner.getUuid().equals(selected)) {
                    return runner;
                }
            }
        }
        return PlayerUtil.findNearestRunner(hunter, runners);
    }

    private void updateHunterCompasses(GameContext context, WildcardRule activeRule) {
        List<ServerPlayerEntity> runners = trackableRunners(context);
        for (ServerPlayerEntity hunter : context.getHunters()) {
            if (hunter.isSpectator()) {
                continue;
            }
            giveCompass(hunter);
            ServerPlayerEntity runner = resolveTarget(context, hunter, runners);
            if (runner == null) {
                updateCompassStacks(hunter, null, HunterWildcardText.translatable("item.tracking_compass.no_target"));
                continue;
            }

            BlockPos target = runner.getBlockPos();

            GlobalPos globalPos = GlobalPos.create(runner.getWorld().getRegistryKey(), target);
            LodestoneTrackerComponent tracker = new LodestoneTrackerComponent(Optional.of(globalPos), false);
            boolean sameDimension = runner.getWorld() == hunter.getWorld();
            Text name = sameDimension
                    ? HunterWildcardText.translatable("item.tracking_compass.targeting", PlayerUtil.displayNameText(runner),
                            PlayerUtil.roundDistance(Math.sqrt(hunter.squaredDistanceTo(runner))))
                    : HunterWildcardText.translatable("item.tracking_compass.targeting_other_dimension", PlayerUtil.displayNameText(runner));
            updateCompassStacks(hunter, tracker, name);
        }
    }

    private void updateCompassStacks(ServerPlayerEntity hunter, LodestoneTrackerComponent tracker, Text targetLine) {
        boolean updated = false;
        for (int slot = 0; slot < hunter.getInventory().size(); slot++) {
            ItemStack stack = hunter.getInventory().getStack(slot);
            if (isHunterCompass(stack)) {
                if (updated) {
                    hunter.getInventory().setStack(slot, ItemStack.EMPTY);
                    continue;
                }

                markHunterCompass(stack);
                if (tracker != null) {
                    stack.set(DataComponentTypes.LODESTONE_TRACKER, tracker);
                }
                MutableText name = HunterWildcardText.translatable("item.tracking_compass").formatted(Formatting.AQUA)
                        .append(Text.literal(" ").formatted(Formatting.GRAY))
                        .append(targetLine.copy().formatted(Formatting.YELLOW));
                stack.set(DataComponentTypes.ITEM_NAME, name);
                updated = true;
            }
        }
    }

    private boolean ensureSingleHunterCompass(ServerPlayerEntity hunter) {
        boolean found = false;
        for (int slot = 0; slot < hunter.getInventory().size(); slot++) {
            ItemStack stack = hunter.getInventory().getStack(slot);
            if (isHunterCompass(stack)) {
                if (found) {
                    hunter.getInventory().setStack(slot, ItemStack.EMPTY);
                    continue;
                }

                markHunterCompass(stack);
                found = true;
            }
        }

        return found;
    }

    private boolean isTaggedHunterCompass(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) {
            return false;
        }

        return data.copyNbt().getBoolean(COMPASS_DATA_KEY, false);
    }

    private boolean isLegacyHunterCompass(ItemStack stack) {
        String name = stack.getName().getString();
        return (name.equals(LEGACY_TRACKING_COMPASS_NAME) || name.equals(LEGACY_COMPASS_NAME))
                && Boolean.TRUE.equals(stack.get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE));
    }

    private ItemStack createCompass() {
        ItemStack stack = new ItemStack(Items.COMPASS);
        markHunterCompass(stack);
        stack.set(DataComponentTypes.ITEM_NAME, HunterWildcardText.translatable("item.tracking_compass").formatted(Formatting.AQUA));
        return stack;
    }

    private void markHunterCompass(ItemStack stack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(COMPASS_DATA_KEY, true));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                HunterWildcardText.translatable("item.tracking_compass.lore_cycle").formatted(Formatting.GRAY),
                HunterWildcardText.translatable("item.tracking_compass.lore_menu").formatted(Formatting.GRAY)
        )));
    }
}
