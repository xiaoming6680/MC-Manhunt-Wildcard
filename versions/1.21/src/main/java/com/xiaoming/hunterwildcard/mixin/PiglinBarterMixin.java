package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PiglinBrain.class)
public class PiglinBarterMixin {
    /** Vanilla piglin_bartering loot table: ender pearl weight 10 out of 459 total. */
    private static final float VANILLA_PEARL_CHANCE = 10.0F / 459.0F;

    @Inject(method = "getBarteredItem", at = @At("RETURN"), cancellable = true)
    private static void hunterwildcard$boostEnderPearls(PiglinEntity piglin, CallbackInfoReturnable<List<ItemStack>> cir) {
        float targetChance = GameManager.getInstance().getConfig().getPiglinPearlChance();
        if (targetChance <= VANILLA_PEARL_CHANCE) {
            return;
        }

        List<ItemStack> vanillaResult = cir.getReturnValue();
        if (vanillaResult != null && vanillaResult.stream().anyMatch(stack -> stack.isOf(Items.ENDER_PEARL))) {
            return;
        }

        // Replace a non-pearl roll with probability q so that the overall pearl chance lands exactly on targetChance:
        // P(pearl) = v + (1 - v) * q = target  =>  q = (target - v) / (1 - v)
        float replaceChance = (targetChance - VANILLA_PEARL_CHANCE) / (1.0F - VANILLA_PEARL_CHANCE);
        if (piglin.getRandom().nextFloat() < replaceChance) {
            int count = 2 + piglin.getRandom().nextInt(3);
            cir.setReturnValue(List.of(new ItemStack(Items.ENDER_PEARL, count)));
        }
    }
}
