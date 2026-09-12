package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sideways descent must reset the native floating timer just as vertical descent does. */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class TiltMovementValidationMixin {
    @Shadow public ServerPlayerEntity player;
    @Shadow private boolean floating;
    @Unique private Vec3d hunterwildcard$beforeMove;
    @Inject(method="onPlayerMove",at=@At("HEAD"))
    private void hunterwildcard$start(PlayerMoveC2SPacket packet,CallbackInfo ci) {
        hunterwildcard$beforeMove=TiltFrame.active(player)?player.getPos():null;
    }
    @Inject(method="onPlayerMove",at=@At("TAIL"))
    private void hunterwildcard$descent(PlayerMoveC2SPacket packet,CallbackInfo ci) {
        if(hunterwildcard$beforeMove!=null&&TiltFrame.active(player)
                &&player.getPos().subtract(hunterwildcard$beforeMove).dotProduct(TiltFrame.down(player))>.03125)
            floating=false;
    }
}
