package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.ClientGameStatus;
import com.xiaoming.hunterwildcard.wildcard.rules.WhoAreYouRule;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While "Who are you?" is active every player, including yourself, renders with the default Steve skin. */
@Mixin(PlayerInfo.class)
public class PlayerListEntrySkinMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$steveForEveryone(CallbackInfoReturnable<PlayerSkin> cir) {
        if (ClientGameStatus.isWildcardActive(WhoAreYouRule.ID)) {
            cir.setReturnValue(DefaultPlayerSkin.getDefaultSkin());
        }
    }
}
