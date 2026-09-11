package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.wildcard.rules.StayAwayRule;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hunters walk straight over the "Stay away!" sword without picking it up. */
@Mixin(ItemEntity.class)
public class ItemEntityPickupMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$blockHunterPickup(PlayerEntity player, CallbackInfo ci) {
        if (!StayAwayRule.isActive() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        ItemEntity self = (ItemEntity) (Object) this;
        if (StayAwayRule.isStayAwaySword(self.getStack()) && GameManager.getInstance().getTeamManager().isHunter(serverPlayer)) {
            ci.cancel();
        }
    }
}
