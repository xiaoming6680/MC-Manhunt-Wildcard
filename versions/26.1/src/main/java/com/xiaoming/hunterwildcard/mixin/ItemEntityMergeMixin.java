package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.DropBombRule;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Items with a Drop Bomb fuse never merge with neighbouring stacks: a merge discards one of the two entities,
 * which would silently defuse it.
 */
@Mixin(ItemEntity.class)
public class ItemEntityMergeMixin {
    @Inject(method = "isMergable()Z", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$keepArmedItemsApart(CallbackInfoReturnable<Boolean> cir) {
        if (DropBombRule.isArmed((ItemEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
