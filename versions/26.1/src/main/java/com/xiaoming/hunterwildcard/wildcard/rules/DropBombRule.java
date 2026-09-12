package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;

/**
 * Anything a participant throws out of their inventory explodes two seconds later. The blast hurts and knocks
 * back but never breaks blocks or destroys dropped items, so the same item can be picked up and thrown again.
 */
public class DropBombRule implements WildcardRule {
    private static final int FUSE_TICKS = 40;
    private static final float EXPLOSION_POWER = 1.6F;
    private static final int WARNING_COOLDOWN_TICKS = 20;
    /** Blocks are never destroyed (source type NONE) and dropped items are never hit, so loot on the floor survives. */
    private static final ExplosionDamageCalculator ITEM_SAFE_BEHAVIOR = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return !(entity instanceof ItemEntity) && super.shouldDamageEntity(explosion, entity);
        }
    };

    private static final Set<UUID> ARMED_ITEMS = new HashSet<>();

    private final List<ArmedItem> armed = new ArrayList<>();

    /** Checked by {@code ItemEntityMergeMixin} so fused items are never merged away before they go off. */
    public static boolean isArmed(ItemEntity item) {
        return !ARMED_ITEMS.isEmpty() && ARMED_ITEMS.contains(item.getUUID());
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
    public void onItemDropped(GameContext context, ServerPlayer player, ItemEntity item) {
        if (!(item.level() instanceof ServerLevel world)) {
            return;
        }
        armed.add(new ArmedItem(world.dimension(), item.getUUID(), player.getUUID(), ticks + FUSE_TICKS));
        ARMED_ITEMS.add(item.getUUID());
        world.playSound(null, item.getX(), item.getY(), item.getZ(), SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.7F, 1.4F);

        Integer last = lastWarningTick.get(player.getUUID());
        if (last == null || ticks - last >= WARNING_COOLDOWN_TICKS) {
            lastWarningTick.put(player.getUUID(), ticks);
            player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.drop_bomb.armed"), true);
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
            ServerLevel world = context.getServer().getLevel(entry.worldKey());
            Entity entity = world == null ? null : world.getEntity(entry.itemId());
            if (!(entity instanceof ItemEntity item) || item.isRemoved()) {
                iterator.remove();
                ARMED_ITEMS.remove(entry.itemId());
                continue;
            }

            if (ticks < entry.explodeTick()) {
                if (ticks % 4 == 0) {
                    world.sendParticles(ParticleTypes.SMOKE, item.getX(), item.getY() + 0.3, item.getZ(), 2, 0.05, 0.1, 0.05, 0.01);
                }
                continue;
            }

            iterator.remove();
            ARMED_ITEMS.remove(entry.itemId());
            Entity thrower = world.getEntity(entry.throwerId());
            world.explode(
                    item,
                    world.damageSources().explosion(item, thrower),
                    ITEM_SAFE_BEHAVIOR,
                    item.getX(),
                    item.getY() + 0.25,
                    item.getZ(),
                    EXPLOSION_POWER,
                    false,
                    Level.ExplosionInteraction.NONE
            );
        }
    }

    @Override
    public void onStop(GameContext context) {
        armed.clear();
        ARMED_ITEMS.clear();
        lastWarningTick.clear();
    }

    private record ArmedItem(ResourceKey<Level> worldKey, UUID itemId, UUID throwerId, int explodeTick) {
    }
}
