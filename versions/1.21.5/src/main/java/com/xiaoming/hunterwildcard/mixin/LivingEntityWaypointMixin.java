package com.xiaoming.hunterwildcard.mixin;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
/** Minecraft versions before 1.21.6 have no vanilla locator bar to filter. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityWaypointMixin {}