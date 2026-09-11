package com.xiaoming.hunterwildcard.mixin.client;

import com.xiaoming.hunterwildcard.client.ClientGameStatus;
import com.xiaoming.hunterwildcard.wildcard.rules.WhoAreYouRule;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While "Who are you?" is active every player, including yourself, renders with the default Steve skin. */
@Mixin(PlayerListEntry.class)
public class PlayerListEntrySkinMixin {
    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$steveForEveryone(CallbackInfoReturnable<SkinTextures> cir) {
        if (ClientGameStatus.isWildcardActive(WhoAreYouRule.ID)) {
            cir.setReturnValue(DefaultSkinHelper.getSteve());
        }
    }
}
