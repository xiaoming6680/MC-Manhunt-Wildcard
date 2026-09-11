package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.StayAwayRule;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Both the Q key and dragging an item out of the inventory screen end up in this method,
 * so cancelling here and handing the stack back keeps the "Stay away!" sword bound to its runner.
 */
@Mixin(ServerPlayerEntity.class)
public class PlayerDropItemMixin {
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$blockSwordDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        if (!StayAwayRule.isActive() || !StayAwayRule.isStayAwaySword(stack)) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (!player.getInventory().insertStack(stack.copy())) {
            // Inventory full: keep the item in the player's hands instead of letting it hit the floor.
            player.setStackInHand(player.getActiveHand(), stack.copy());
        }
        cir.setReturnValue(null);
    }
}
