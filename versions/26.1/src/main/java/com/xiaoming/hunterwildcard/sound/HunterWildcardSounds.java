package com.xiaoming.hunterwildcard.sound;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Sound events must be registered during main init, before registries freeze. */
public final class HunterWildcardSounds {
    public static final SoundEvent BACKROOMS_AMBIENCE = register("backrooms_ambience");

    private HunterWildcardSounds() {
    }

    public static void register() {
        HunterWildcardMod.LOGGER.info("Sound events registered.");
    }

    private static SoundEvent register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(HunterWildcardMod.MOD_ID, path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
