package com.xiaoming.hunterwildcard.client.key;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Client-side owner of the "Key Scramble" wildcard. Only the keys listed in the HUD are ever touched,
 * and the original bindings are always restored on stop or disconnect.
 */
public final class KeyScrambleController {
    private static final Random RANDOM = new Random();
    private static final long HIGHLIGHT_MS = 1500L;

    private static boolean active;
    private static boolean scrambled;
    private static KeyBinding[] managedBindings;
    private static InputUtil.Key[] originalKeys;
    private static long[] lastChangedMs;

    private KeyScrambleController() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> restore());
    }

    public static boolean isActive() {
        return active;
    }

    public static int size() {
        return managedBindings == null ? 0 : managedBindings.length;
    }

    public static KeyBinding binding(int index) {
        return managedBindings[index];
    }

    public static boolean isRecentlyChanged(int index, long now) {
        return lastChangedMs != null && now - lastChangedMs[index] < HIGHLIGHT_MS;
    }

    public static void enable() {
        if (active) {
            return;
        }

        GameOptions options = MinecraftClient.getInstance().options;
        if (options == null) {
            return;
        }

        managedBindings = new KeyBinding[] {
                options.forwardKey,
                options.backKey,
                options.leftKey,
                options.rightKey,
                options.jumpKey,
                options.sneakKey,
                options.sprintKey
        };
        originalKeys = new InputUtil.Key[managedBindings.length];
        lastChangedMs = new long[managedBindings.length];
        for (int i = 0; i < managedBindings.length; i++) {
            originalKeys[i] = KeyBindingHelper.getBoundKeyOf(managedBindings[i]);
            lastChangedMs[i] = Long.MIN_VALUE;
        }
        active = true;
        scrambled = false;
    }

    public static void shuffle() {
        if (!active) {
            enable();
        }
        if (!active) {
            return;
        }

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < managedBindings.length; i++) {
            order.add(i);
        }

        // Make sure at least one key actually moves; a no-op shuffle would feel like a bug.
        InputUtil.Key[] before = new InputUtil.Key[managedBindings.length];
        for (int i = 0; i < managedBindings.length; i++) {
            before[i] = KeyBindingHelper.getBoundKeyOf(managedBindings[i]);
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            Collections.shuffle(order, RANDOM);
            boolean changed = false;
            for (int i = 0; i < order.size(); i++) {
                if (!before[order.get(i)].equals(before[i])) {
                    changed = true;
                    break;
                }
            }
            if (changed) {
                break;
            }
        }

        long now = System.currentTimeMillis();
        for (int i = 0; i < managedBindings.length; i++) {
            InputUtil.Key newKey = before[order.get(i)];
            if (!newKey.equals(before[i])) {
                managedBindings[i].setBoundKey(newKey);
                lastChangedMs[i] = now;
            }
        }
        KeyBinding.updateKeysByCode();
        KeyBinding.unpressAll();
        scrambled = true;

        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 0.7F));
    }

    public static void restore() {
        if (!active) {
            return;
        }

        boolean needsWrite = scrambled;
        for (int i = 0; i < managedBindings.length; i++) {
            managedBindings[i].setBoundKey(originalKeys[i]);
        }
        KeyBinding.updateKeysByCode();
        KeyBinding.unpressAll();

        active = false;
        scrambled = false;
        managedBindings = null;
        originalKeys = null;
        lastChangedMs = null;

        // If the player saved the options screen while scrambled, options.txt holds the shuffled layout.
        // Writing once after restoring guarantees the file matches what they started with.
        if (needsWrite) {
            GameOptions options = MinecraftClient.getInstance().options;
            if (options != null) {
                options.write();
            }
        }
    }
}
