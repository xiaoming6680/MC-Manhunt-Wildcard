package com.xiaoming.hunterwildcard.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.function.Consumer;

/** Override rod success chance while retaining player-kill eligibility and other loot. */
@Mixin(LivingEntity.class)
public class BlazeRodLootMixin {
    @WrapMethod(method = "generateLoot")
    private void hunterwildcard$blazeRodChance(ServerWorld world, DamageSource source, boolean causedByPlayer,
                                             RegistryKey<LootTable> key, Consumer<ItemStack> consumer, Operation<Void> original) {
        var config = GameManager.getInstance().getConfig();
        if (!((Object) this instanceof BlazeEntity blaze) || !causedByPlayer || !config.blazeRodChanceEnabled
                || !key.getValue().equals(Identifier.ofVanilla("entities/blaze"))) {
            original.call(world, source, causedByPlayer, key, consumer);
            return;
        }
        var drops = new ArrayList<ItemStack>();
        original.call(world, source, causedByPlayer, key, (Consumer<ItemStack>) drops::add);
        boolean success = blaze.getRandom().nextInt(100) < Math.clamp(config.blazeRodChancePercent, 0, 100);
        if (!success) drops.removeIf(stack -> stack.isOf(Items.BLAZE_ROD));
        else if (drops.stream().noneMatch(stack -> stack.isOf(Items.BLAZE_ROD) && !stack.isEmpty())) drops.add(new ItemStack(Items.BLAZE_ROD));
        drops.forEach(consumer);
    }
}
