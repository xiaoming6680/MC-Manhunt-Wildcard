package com.xiaoming.hunterwildcard.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.xiaoming.hunterwildcard.game.GameManager;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

/** Override rod success chance while retaining player-kill eligibility and other loot. */
@Mixin(LivingEntity.class)
public class BlazeRodLootMixin {
    @WrapMethod(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V")
    private void hunterwildcard$blazeRodChance(ServerLevel world, DamageSource source, boolean causedByPlayer,
                                             ResourceKey<LootTable> key, Consumer<ItemStack> consumer, Operation<Void> original) {
        var config = GameManager.getInstance().getConfig();
        if (!((Object) this instanceof Blaze blaze) || !causedByPlayer || !config.blazeRodChanceEnabled
                || !key.identifier().equals(Identifier.withDefaultNamespace("entities/blaze"))) {
            original.call(world, source, causedByPlayer, key, consumer);
            return;
        }
        var drops = new ArrayList<ItemStack>();
        original.call(world, source, causedByPlayer, key, (Consumer<ItemStack>) drops::add);
        boolean success = blaze.getRandom().nextInt(100) < Math.clamp(config.blazeRodChancePercent, 0, 100);
        if (!success) drops.removeIf(stack -> stack.is(Items.BLAZE_ROD));
        else if (drops.stream().noneMatch(stack -> stack.is(Items.BLAZE_ROD) && !stack.isEmpty())) drops.add(new ItemStack(Items.BLAZE_ROD));
        drops.forEach(consumer);
    }
}
