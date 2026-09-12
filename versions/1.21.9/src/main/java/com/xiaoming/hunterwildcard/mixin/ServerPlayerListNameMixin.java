package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.rules.WhoAreYouRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tab list entries use getPlayerListName; the rule resends the list whenever it starts or stops. */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerListNameMixin {
    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$anonymousListName(CallbackInfoReturnable<Text> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (WhoAreYouRule.isActive() && GameManager.getInstance().isWildcardParticipant(player)) {
            cir.setReturnValue(HunterWildcardText.translatable("common.anonymous_player"));
        }
    }
}
