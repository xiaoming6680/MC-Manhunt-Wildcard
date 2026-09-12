package com.xiaoming.hunterwildcard.backrooms;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class BackroomsDimension {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(HunterWildcardMod.MOD_ID, "backrooms");
    public static final ResourceKey<Level> WORLD_KEY = ResourceKey.create(Registries.DIMENSION, ID);

    private BackroomsDimension() {
    }

    /** Must run in mod init before any world loads, or the datapack silently drops the dimension. */
    public static void register() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, ID, BackroomsChunkGenerator.CODEC);
    }

    public static boolean isBackrooms(Level world) {
        return world != null && world.dimension().equals(WORLD_KEY);
    }

    public static boolean isInBackrooms(Entity entity) {
        return entity != null && isBackrooms(entity.level());
    }
}
