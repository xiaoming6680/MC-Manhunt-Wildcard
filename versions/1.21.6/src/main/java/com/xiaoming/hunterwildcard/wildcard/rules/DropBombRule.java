package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Anything a participant throws out of their inventory explodes two seconds later. The blast hurts and knocks
 * back but never breaks blocks or destroys dropped items, so the same item can be picked up and thrown again.
 */
public class DropBombRule implements WildcardRule {
    private static final int FUSE_TICKS = 40;
    private static final float EXPLOSION_POWER = 1.6F;
    private static final int WARNING_COOLDOWN_TICKS = 20;
    /** Blocks are never destroyed (source type NONE) and dropped items are never hit, so loot on the floor survives. */
    private static final ExplosionBehavior ITEM_SAFE_BEHAVIOR = new ExplosionBehavior() {
        @Override
        public boolean shouldDamage(Explosion explosion, Entity entity) {
            return !(entity instanceof ItemEntity) && super.shouldDamage(explosion, entity);
        }
    };

    private static final Set<UUID> ARMED_ITEMS = new HashSet<>();

    private final List<ArmedItem> armed = new ArrayList<>();

    /** Checked by {@code ItemEntityMergeMixin} so fused items are never merged away before they go off. */
    public static boolean isArmed(ItemEntity item) {
        return !ARMED_ITEMS.isEmpty() && ARMED_ITEMS.contains(item.getUuid());
    }
    private final Map<UUID, Integer> lastWarningTick = new HashMap<>();
    private int ticks;

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        armed.clear();
        ARMED_ITEMS.clear();
        lastWarningTick.clear();
    }

    @Override
    public void onItemDropped(GameContext context, ServerPlayerEntity player, ItemEntity item) {
        if (!(item.getWorld() instanceof ServerWorld world)) {
            return;
        }
        armed.add(new ArmedItem(world.getRegistryKey(), item.getUuid(), player.getUuid(), ticks + FUSE_TICKS));
        ARMED_ITEMS.add(item.getUuid());
        world.playSound(null, item.getX(), item.getY(), item.getZ(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 0.7F, 1.4F);

        Integer last = lastWarningTick.get(player.getUuid());
        if (last == null || ticks - last >= WARNING_COOLDOWN_TICKS) {
            lastWarningTick.put(player.getUuid(), ticks);
            player.sendMessage(HunterWildcardText.translatable("msg.wildcard.drop_bomb.armed"), true);
        }
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (armed.isEmpty()) {
            return;
        }

        Iterator<ArmedItem> iterator = armed.iterator();
        while (iterator.hasNext()) {
            ArmedItem entry = iterator.next();
            ServerWorld world = context.getServer().getWorld(entry.worldKey());
            Entity entity = world == null ? null : world.getEntity(entry.itemId());
            if (!(entity instanceof ItemEntity item) || item.isRemoved()) {
                iterator.remove();
                ARMED_ITEMS.remove(entry.itemId());
                continue;
            }

            if (ticks < entry.explodeTick()) {
                if (ticks % 4 == 0) {
                    world.spawnParticles(ParticleTypes.SMOKE, item.getX(), item.getY() + 0.3, item.getZ(), 2, 0.05, 0.1, 0.05, 0.01);
                }
                continue;
            }

            iterator.remove();
            ARMED_ITEMS.remove(entry.itemId());
            Entity thrower = world.getEntity(entry.throwerId());
            world.createExplosion(
                    item,
                    world.getDamageSources().explosion(item, thrower),
                    ITEM_SAFE_BEHAVIOR,
                    item.getX(),
                    item.getY() + 0.25,
                    item.getZ(),
                    EXPLOSION_POWER,
                    false,
                    World.ExplosionSourceType.NONE
            );
        }
    }

    @Override
    public void onStop(GameContext context) {
        armed.clear();
        ARMED_ITEMS.clear();
        lastWarningTick.clear();
    }

    private record ArmedItem(RegistryKey<World> worldKey, UUID itemId, UUID throwerId, int explodeTick) {
    }
}
