package com.xiaoming.hunterwildcard.backrooms;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xiaoming.hunterwildcard.HunterWildcardMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

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
            Identifier.fromNamespaceAndPath(HunterWildcardMod.MOD_ID, "backrooms_return"), CODEC);

    public static BackroomsReturnData capture(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        return new BackroomsReturnData(player.level().dimension().identifier().toString(),
                pos.getX(), pos.getY(), pos.getZ(), player.getYRot(), player.getXRot());
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }
}
