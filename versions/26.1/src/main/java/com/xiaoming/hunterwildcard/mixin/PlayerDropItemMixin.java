package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.wildcard.rules.StayAwayRule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Both the Q key and dragging an item out of the inventory screen end up in this method,
 * so cancelling here and handing the stack back keeps the "Stay away!" sword bound to its runner.
 */
@Mixin(ServerPlayer.class)
public class PlayerDropItemMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$blockSwordDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        if (!StayAwayRule.isActive() || !StayAwayRule.isStayAwaySword(stack)) {
            return;
        }

        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!player.getInventory().add(stack.copy())) {
            // Inventory full: keep the item in the player's hands instead of letting it hit the floor.
            player.setItemInHand(player.getUsedItemHand(), stack.copy());
        }
        cir.setReturnValue(null);
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("RETURN"))
    private void hunterwildcard$afterDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        ItemEntity item = cir.getReturnValue();
        ServerPlayer player = (ServerPlayer) (Object) this;
        // Death drops also pass through here; only living players count as "throwing" something.
        if (item != null && player.isAlive()) {
            GameManager.getInstance().handleItemDropped(player, item);
        }
    }
}
