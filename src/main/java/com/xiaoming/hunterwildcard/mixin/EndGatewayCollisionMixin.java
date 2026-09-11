package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.wildcard.rules.PortalRule;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndGatewayBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gateway blocks placed by the Portals wildcard are decoration; the rule handles the teleport itself. */
@Mixin(EndGatewayBlock.class)
public class EndGatewayCollisionMixin {
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$skipManagedGateways(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler handler, boolean flag, CallbackInfo ci) {
        if (PortalRule.isManagedGateway(world, pos)) {
            ci.cancel();
        }
    }
}
