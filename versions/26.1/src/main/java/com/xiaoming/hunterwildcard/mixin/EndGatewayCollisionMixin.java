package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.PortalRule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gateway blocks placed by the Portals wildcard are decoration; the rule handles the teleport itself. */
@Mixin(EndGatewayBlock.class)
public class EndGatewayCollisionMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$skipManagedGateways(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean flag, CallbackInfo ci) {
        if (PortalRule.isManagedGateway(world, pos)) {
            ci.cancel();
        }
    }
}
