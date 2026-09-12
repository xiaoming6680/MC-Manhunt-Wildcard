package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

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
    public void onPlayerDamaged(GameContext context, ServerPlayer player, DamageSource source, float damageTaken) {
        Integer last = lastTeleportTick.get(player.getUUID());
        if (last != null && ticks - last < TELEPORT_COOLDOWN_TICKS) {
            return;
        }
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }

        Random random = context.getRandom();
        if (!blinkRandomly(world, player, random, MIN_DISTANCE, MAX_DISTANCE)) {
            return;
        }
        lastTeleportTick.put(player.getUUID(), ticks);
        player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.hurt_teleport.teleported"), true);
    }

    @Override
    public void onStop(GameContext context) {
        lastTeleportTick.clear();
    }

    /** Teleports the player to a random spot between {@code min} and {@code max} blocks away; false if nothing fits. */
    static boolean blinkRandomly(ServerLevel world, ServerPlayer player, Random random, double min, double max) {
        double distance = min + random.nextDouble() * (max - min);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double targetX = player.getX() + Math.cos(angle) * distance;
        double targetZ = player.getZ() + Math.sin(angle) * distance;
        BlockPos landing = findLanding(world, BlockPos.containing(targetX, player.getY(), targetZ));
        if (landing == null) {
            return false;
        }

        Vec3 from = player.position();
        player.teleportTo(world, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), false);
        player.fallDistance = 0.0F;
        world.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.6F);
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1.0, from.z, 24, 0.3, 0.6, 0.3, 0.2);
        world.sendParticles(ParticleTypes.PORTAL, landing.getX() + 0.5, landing.getY() + 1.0, landing.getZ() + 0.5, 24, 0.3, 0.6, 0.3, 0.2);
        return true;
    }

    /** Nearest y around the start where feet and head are free and there is something (block or liquid) underfoot. */
    static BlockPos findLanding(ServerLevel world, BlockPos start) {
        int minY = world.getMinY() + 1;
        int maxY = world.getMinY() + world.dimensionType().height() - 2;
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
                if (isFree(world, feet) && isFree(world, feet.above()) && hasFooting(world, feet.below())) {
                    return feet;
                }
            }
        }

        BlockPos top = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, start);
        return top.getY() > world.getMinY() ? top : null;
    }

    private static boolean isFree(ServerLevel world, BlockPos pos) {
        return world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }

    private static boolean hasFooting(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getCollisionShape(world, pos).isEmpty() || !state.getFluidState().isEmpty();
    }
}
