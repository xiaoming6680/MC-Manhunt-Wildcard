package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.rules.WhoAreYouRule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Chat, death messages and similar all go through getDisplayName, so anonymising it here covers them at once. */
@Mixin(PlayerEntity.class)
public class PlayerDisplayNameMixin {
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$anonymousDisplayName(CallbackInfoReturnable<Text> cir) {
        if (WhoAreYouRule.isActive()
                && (Object) this instanceof ServerPlayerEntity player
                && GameManager.getInstance().isWildcardParticipant(player)) {
            cir.setReturnValue(HunterWildcardText.translatable("common.anonymous_player"));
        }
    }
}
