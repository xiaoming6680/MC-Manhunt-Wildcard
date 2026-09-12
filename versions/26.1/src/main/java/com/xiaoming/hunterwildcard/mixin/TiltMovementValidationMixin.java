package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sideways descent must reset the native floating timer just as vertical descent does. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class TiltMovementValidationMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;
    @Unique private Vec3 hunterwildcard$beforeMove;
    @Inject(method="handleMovePlayer",at=@At("HEAD"))
    private void hunterwildcard$start(ServerboundMovePlayerPacket packet,CallbackInfo ci) {
        hunterwildcard$beforeMove=TiltFrame.active(player)?player.position():null;
    }
    @Inject(method="handleMovePlayer",at=@At("TAIL"))
    private void hunterwildcard$descent(ServerboundMovePlayerPacket packet,CallbackInfo ci) {
        if(hunterwildcard$beforeMove!=null&&TiltFrame.active(player)
                &&player.position().subtract(hunterwildcard$beforeMove).dot(TiltFrame.down(player))>.03125)
            clientIsFloating=false;
    }
}
