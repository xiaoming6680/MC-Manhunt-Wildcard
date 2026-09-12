package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerEntityDropInventoryMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$dropInventory(ServerLevel world, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player
                && GameManager.getInstance().handleDeathInventoryDrop(player)) {
            ci.cancel();
        }
    }
}
