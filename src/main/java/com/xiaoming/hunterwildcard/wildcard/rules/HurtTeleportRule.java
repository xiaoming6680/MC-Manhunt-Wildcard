package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Taking damage flings you 1 to 15 blocks in a random direction. The landing spot only has to be free of solid
 * blocks: lava and drops are part of the deal.
 */
public class HurtTeleportRule implements WildcardRule {
    private static final int TELEPORT_COOLDOWN_TICKS = 10;
    private static final double MIN_DISTANCE = 1.0;
    private static final double MAX_DISTANCE = 15.0;
    private static final int VERTICAL_SEARCH = 8;

    private final Map<UUID, Integer> lastTeleportTick = new HashMap<>();
    private int ticks;

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        lastTeleportTick.clear();
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
    }

    @Override
    public void onPlayerDamaged(GameContext context, ServerPlayerEntity player, DamageSource source, float damageTaken) {
        Integer last = lastTeleportTick.get(player.getUuid());
        if (last != null && ticks - last < TELEPORT_COOLDOWN_TICKS) {
            return;
        }
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        Random random = context.getRandom();
        if (!blinkRandomly(world, player, random, MIN_DISTANCE, MAX_DISTANCE)) {
            return;
        }
        lastTeleportTick.put(player.getUuid(), ticks);
        player.sendMessage(HunterWildcardText.translatable("msg.wildcard.hurt_teleport.teleported"), true);
    }

    @Override
    public void onStop(GameContext context) {
        lastTeleportTick.clear();
    }

    /** Teleports the player to a random spot between {@code min} and {@code max} blocks away; false if nothing fits. */
    static boolean blinkRandomly(ServerWorld world, ServerPlayerEntity player, Random random, double min, double max) {
        double distance = min + random.nextDouble() * (max - min);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double targetX = player.getX() + Math.cos(angle) * distance;
        double targetZ = player.getZ() + Math.sin(angle) * distance;
        BlockPos landing = findLanding(world, BlockPos.ofFloored(targetX, player.getY(), targetZ));
        if (landing == null) {
            return false;
        }

        Vec3d from = player.getEntityPos();
        player.teleport(world, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, Set.of(), player.getYaw(), player.getPitch(), false);
        player.fallDistance = 0.0F;
        world.playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8F, 1.6F);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1.0, from.z, 24, 0.3, 0.6, 0.3, 0.2);
        world.spawnParticles(ParticleTypes.PORTAL, landing.getX() + 0.5, landing.getY() + 1.0, landing.getZ() + 0.5, 24, 0.3, 0.6, 0.3, 0.2);
        return true;
    }

    /** Nearest y around the start where feet and head are free and there is something (block or liquid) underfoot. */
    static BlockPos findLanding(ServerWorld world, BlockPos start) {
        int minY = world.getBottomY() + 1;
        int maxY = world.getBottomY() + world.getDimension().height() - 2;
        for (int offset = 0; offset <= VERTICAL_SEARCH; offset++) {
            for (int sign = 0; sign < 2; sign++) {
                int y = offset == 0 ? start.getY() : start.getY() + (sign == 0 ? -offset : offset);
                if (offset == 0 && sign == 1) {
                    continue;
                }
                if (y < minY || y > maxY) {
                    continue;
                }
                BlockPos feet = new BlockPos(start.getX(), y, start.getZ());
                if (isFree(world, feet) && isFree(world, feet.up()) && hasFooting(world, feet.down())) {
                    return feet;
                }
            }
        }

        BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, start);
        return top.getY() > world.getBottomY() ? top : null;
    }

    private static boolean isFree(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }

    private static boolean hasFooting(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getCollisionShape(world, pos).isEmpty() || !state.getFluidState().isEmpty();
    }
}
