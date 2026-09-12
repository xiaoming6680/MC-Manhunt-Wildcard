package com.xiaoming.hunterwildcard.sound;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Sound events must be registered during main init, before registries freeze. */
public final class HunterWildcardSounds {
    public static final SoundEvent BACKROOMS_AMBIENCE = register("backrooms_ambience");

    private HunterWildcardSounds() {
    }

    public static void register() {
        HunterWildcardMod.LOGGER.info("Sound events registered.");
    }

    private static SoundEvent register(String path) {
        Identifier id = Identifier.of(HunterWildcardMod.MOD_ID, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
