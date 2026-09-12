package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Consumable.class)
public class ItemFinishUsingMixin {
    @Inject(method = "onConsume", at = @At("HEAD"))
    private void hunterwildcard$beforeFinishConsumption(Level world, LivingEntity user, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (!world.isClientSide() && user instanceof ServerPlayer player && stack.has(DataComponents.FOOD)) {
            GameManager.getInstance().handlePlayerAteFood(player, stack.copy());
        }
    }
}
