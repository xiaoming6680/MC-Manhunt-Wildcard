package com.xiaoming.hunterwildcard.backrooms;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xiaoming.hunterwildcard.HunterWildcardMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Where a player came from before being dropped into the Backrooms. Persisted on the player so a
 * disconnect mid-wildcard can still be undone when they come back.
 */
public record BackroomsReturnData(String dimension, int x, int y, int z, float yaw, float pitch) {
    public static final Codec<BackroomsReturnData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(BackroomsReturnData::dimension),
            Codec.INT.fieldOf("x").forGetter(BackroomsReturnData::x),
            Codec.INT.fieldOf("y").forGetter(BackroomsReturnData::y),
            Codec.INT.fieldOf("z").forGetter(BackroomsReturnData::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(BackroomsReturnData::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(BackroomsReturnData::pitch)
    ).apply(instance, BackroomsReturnData::new));

    /** Not copied on death: dying inside the Backrooms respawns the player normally, so nothing is owed. */
    public static final AttachmentType<BackroomsReturnData> ATTACHMENT = AttachmentRegistry.createPersistent(
            Identifier.of(HunterWildcardMod.MOD_ID, "backrooms_return"), CODEC);

    public static BackroomsReturnData capture(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        return new BackroomsReturnData(player.getWorld().getRegistryKey().getValue().toString(),
                pos.getX(), pos.getY(), pos.getZ(), player.getYaw(), player.getPitch());
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }
}
