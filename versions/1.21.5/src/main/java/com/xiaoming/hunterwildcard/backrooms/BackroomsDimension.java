package com.xiaoming.hunterwildcard.backrooms;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public final class BackroomsDimension {
    public static final Identifier ID = Identifier.of(HunterWildcardMod.MOD_ID, "backrooms");
    public static final RegistryKey<World> WORLD_KEY = RegistryKey.of(RegistryKeys.WORLD, ID);

    private BackroomsDimension() {
    }

    /** Must run in mod init before any world loads, or the datapack silently drops the dimension. */
    public static void register() {
        Registry.register(Registries.CHUNK_GENERATOR, ID, BackroomsChunkGenerator.CODEC);
    }

    public static boolean isBackrooms(World world) {
        return world != null && world.getRegistryKey().equals(WORLD_KEY);
    }

    public static boolean isInBackrooms(Entity entity) {
        return entity != null && isBackrooms(entity.getWorld());
    }
}
