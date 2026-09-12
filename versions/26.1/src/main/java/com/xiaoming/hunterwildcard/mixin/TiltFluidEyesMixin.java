package com.xiaoming.hunterwildcard.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 26.x moved eye-fluid detection out of Entity; use the rotated eye's column. */
@Mixin(EntityFluidInteraction.class)
public abstract class TiltFluidEyesMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBlockX()I"))
    private int hunterwildcard$eyeX(Entity entity, Operation<Integer> original) {
        return TiltFrame.active(entity) ? Mth.floor(entity.getEyePosition().x) : original.call(entity);
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBlockZ()I"))
    private int hunterwildcard$eyeZ(Entity entity, Operation<Integer> original) {
        return TiltFrame.active(entity) ? Mth.floor(entity.getEyePosition().z) : original.call(entity);
    }
}
