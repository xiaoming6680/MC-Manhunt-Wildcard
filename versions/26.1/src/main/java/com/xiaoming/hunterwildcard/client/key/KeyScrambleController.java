package com.xiaoming.hunterwildcard.client.key;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import com.mojang.blaze3d.platform.InputConstants;
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
    private static KeyMapping[] managedBindings;
    private static InputConstants.Key[] originalKeys;
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

    public static KeyMapping binding(int index) {
        return managedBindings[index];
    }

    public static boolean isRecentlyChanged(int index, long now) {
        return lastChangedMs != null && now - lastChangedMs[index] < HIGHLIGHT_MS;
    }

    public static void enable() {
        if (active) {
            return;
        }

        Options options = Minecraft.getInstance().options;
        if (options == null) {
            return;
        }

        managedBindings = new KeyMapping[] {
                options.keyUp,
                options.keyDown,
                options.keyLeft,
                options.keyRight,
                options.keyJump,
                options.keyShift,
                options.keySprint
        };
        originalKeys = new InputConstants.Key[managedBindings.length];
        lastChangedMs = new long[managedBindings.length];
        for (int i = 0; i < managedBindings.length; i++) {
            originalKeys[i] = KeyMappingHelper.getBoundKeyOf(managedBindings[i]);
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
        InputConstants.Key[] before = new InputConstants.Key[managedBindings.length];
        for (int i = 0; i < managedBindings.length; i++) {
            before[i] = KeyMappingHelper.getBoundKeyOf(managedBindings[i]);
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
            InputConstants.Key newKey = before[order.get(i)];
            if (!newKey.equals(before[i])) {
                managedBindings[i].setKey(newKey);
                lastChangedMs[i] = now;
            }
        }
        KeyMapping.resetMapping();
        KeyMapping.releaseAll();
        scrambled = true;

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.7F));
    }

    public static void restore() {
        if (!active) {
            return;
        }

        boolean needsWrite = scrambled;
        for (int i = 0; i < managedBindings.length; i++) {
            managedBindings[i].setKey(originalKeys[i]);
        }
        KeyMapping.resetMapping();
        KeyMapping.releaseAll();

        active = false;
        scrambled = false;
        managedBindings = null;
        originalKeys = null;
        lastChangedMs = null;

        // If the player saved the options screen while scrambled, options.txt holds the shuffled layout.
        // Writing once after restoring guarantees the file matches what they started with.
        if (needsWrite) {
            Options options = Minecraft.getInstance().options;
            if (options != null) {
                options.save();
            }
        }
    }
}
