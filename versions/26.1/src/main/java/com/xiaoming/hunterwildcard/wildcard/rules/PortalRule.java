package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Players are paired up at random and each gets a one-shot portal a few blocks away. Stepping into yours drops
 * you next to your partner's portal, whoever that turns out to be; the whole pair vanishes once used.
 * An odd player out joins a random pair, making a three-way portal where the exit is picked at random.
 * New portals appear every minute for as long as the wildcard lasts.
 */
public class PortalRule implements WildcardRule {
    private static final int RESPAWN_INTERVAL_TICKS = 20 * 60;
    private static final int MIN_OFFSET = 4;
    private static final int MAX_OFFSET = 8;
    private static final double ENTER_RADIUS_SQUARED = 0.7 * 0.7;
    private static final int AMBIENT_SOUND_TICKS = 50;

    private static final Set<GlobalPos> MANAGED_GATEWAYS = new HashSet<>();

    private final List<PortalGroup> groups = new ArrayList<>();
    private int ticks;

    /** True for gateway blocks this rule placed; the mixin on EndGatewayBlock skips vanilla travel for those. */
    public static boolean isManagedGateway(Level world, BlockPos pos) {
        return !MANAGED_GATEWAYS.isEmpty() && MANAGED_GATEWAYS.contains(GlobalPos.of(world.dimension(), pos));
    }

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        groups.clear();
        spawnPortals(context);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (ticks % RESPAWN_INTERVAL_TICKS == 0) {
            clearAllGateways(context);
            spawnPortals(context);
        }

        for (PortalGroup group : groups) {
            renderGroup(context, group);
        }

        for (ServerPlayer player : context.getParticipants()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            PortalGroup used = null;
            Portal entered = null;
            for (PortalGroup group : groups) {
                for (Portal portal : group.portals()) {
                    if (portal.worldKey() != player.level().dimension()) {
                        continue;
                    }
                    double dx = player.getX() - portal.center().x;
                    double dz = player.getZ() - portal.center().z;
                    double dy = player.getY() - portal.center().y;
                    if (dx * dx + dz * dz <= ENTER_RADIUS_SQUARED && dy > -1.0 && dy < 2.0) {
                        used = group;
                        entered = portal;
                        break;
                    }
                }
                if (used != null) {
                    break;
                }
            }
            if (used != null) {
                travel(context, player, used, entered);
                groups.remove(used);
                removeGatewayBlocks(context, used);
                break; // one traversal per tick keeps the iteration simple
            }
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (PortalGroup group : groups) {
            for (Portal portal : group.portals()) {
                ServerLevel world = context.getServer().getLevel(portal.worldKey());
                if (world != null) {
                    world.playSound(null, portal.center().x, portal.center().y, portal.center().z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.5F);
                }
            }
        }
        clearAllGateways(context);
    }

    private void clearAllGateways(GameContext context) {
        for (PortalGroup group : new ArrayList<>(groups)) {
            removeGatewayBlocks(context, group);
        }
        groups.clear();
        MANAGED_GATEWAYS.clear();
    }

    /** Two gateway blocks (feet and head) make the portal visible from afar; they are ours, so vanilla travel is off. */
    private static void placeGatewayBlocks(ServerLevel world, Portal portal) {
        for (BlockPos pos : gatewayBlocks(portal)) {
            if (world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                world.setBlock(pos, Blocks.END_GATEWAY.defaultBlockState(), Block.UPDATE_ALL);
                MANAGED_GATEWAYS.add(GlobalPos.of(world.dimension(), pos));
            }
        }
    }

    private static void removeGatewayBlocks(GameContext context, PortalGroup group) {
        for (Portal portal : group.portals()) {
            ServerLevel world = context.getServer().getLevel(portal.worldKey());
            for (BlockPos pos : gatewayBlocks(portal)) {
                GlobalPos key = GlobalPos.of(portal.worldKey(), pos);
                if (MANAGED_GATEWAYS.remove(key) && world != null && world.getBlockState(pos).is(Blocks.END_GATEWAY)) {
                    world.removeBlock(pos, false);
                }
            }
        }
    }

    private static List<BlockPos> gatewayBlocks(Portal portal) {
        BlockPos feet = BlockPos.containing(portal.center());
        return List.of(feet, feet.above());
    }

    private void spawnPortals(GameContext context) {
        Random random = context.getRandom();
        Map<ResourceKey<Level>, List<ServerPlayer>> byWorld = new LinkedHashMap<>();
        for (ServerPlayer player : context.getParticipants()) {
            if (player.isAlive() && !player.isSpectator()) {
                byWorld.computeIfAbsent(player.level().dimension(), ignored -> new ArrayList<>()).add(player);
            }
        }

        for (List<ServerPlayer> players : byWorld.values()) {
            if (players.size() < 2) {
                continue;
            }
            Collections.shuffle(players, random);
            List<List<ServerPlayer>> buckets = new ArrayList<>();
            for (int i = 0; i + 1 < players.size(); i += 2) {
                buckets.add(new ArrayList<>(List.of(players.get(i), players.get(i + 1))));
            }
            if (players.size() % 2 == 1) {
                buckets.get(random.nextInt(buckets.size())).add(players.get(players.size() - 1));
            }

            for (List<ServerPlayer> bucket : buckets) {
                List<Portal> portals = new ArrayList<>();
                for (ServerPlayer player : bucket) {
                    Portal portal = placePortalNear(player, random);
                    if (portal != null) {
                        portals.add(portal);
                    }
                }
                if (portals.size() < 2) {
                    continue;
                }
                groups.add(new PortalGroup(portals));
                for (Portal portal : portals) {
                    ServerLevel portalWorld = context.getServer().getLevel(portal.worldKey());
                    if (portalWorld != null) {
                        placeGatewayBlocks(portalWorld, portal);
                    }
                }
                for (ServerPlayer player : bucket) {
                    player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.portal.spawned").withStyle(ChatFormatting.DARK_PURPLE), false);
                    player.playSound(SoundEvents.END_PORTAL_SPAWN, 0.5F, 1.5F);
                }
            }
        }
    }

    private static Portal placePortalNear(ServerPlayer player, Random random) {
        if (!(player.level() instanceof ServerLevel world)) {
            return null;
        }
        for (int attempt = 0; attempt < 16; attempt++) {
            double distance = MIN_OFFSET + random.nextDouble() * (MAX_OFFSET - MIN_OFFSET);
            double angle = random.nextDouble() * Math.PI * 2.0;
            BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
            BlockPos feet = findStanding(world, column);
            if (feet != null) {
                return new Portal(world.dimension(), new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5));
            }
        }
        return null;
    }

    /** Two free blocks with solid footing, searched a few blocks up and down from the player's height. */
    private static BlockPos findStanding(ServerLevel world, BlockPos start) {
        for (int offset = 0; offset <= 6; offset++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                if (offset == 0 && sign == 1) {
                    continue;
                }
                BlockPos feet = start.above(sign * offset);
                if (isSafeStanding(world, feet)) {
                    return feet;
                }
            }
        }
        // A ceiling world's heightmap points above its bedrock roof, not to a walkable cave.
        if (world.dimensionType().hasCeiling()) return null;
        BlockPos top = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, start);
        return isSafeStanding(world, top) ? top : null;
    }

    private static boolean isSafeStanding(ServerLevel world, BlockPos feet) {
        if (!world.getWorldBorder().isWithinBounds(feet) || feet.getY() <= world.getMinY()
                || feet.getY() + 1 >= world.getMinY() + world.getHeight()) return false;
        if (world.dimensionType().hasCeiling()
                && feet.getY() > world.getMinY() + world.dimensionType().logicalHeight() - 8) return false;
        var ground = world.getBlockState(feet.below());
        if (!ground.isRedstoneConductor(world, feet.below()) || !world.getFluidState(feet.below()).isEmpty()
                || ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.CACTUS)) return false;
        for (BlockPos pos : List.of(feet, feet.above())) {
            var state = world.getBlockState(pos);
            if (!state.getCollisionShape(world, pos).isEmpty() || !world.getFluidState(pos).isEmpty()
                    || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.POWDER_SNOW)
                    || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.SWEET_BERRY_BUSH)
                    || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)
                    || (state.is(Blocks.END_GATEWAY) && !isManagedGateway(world, pos))) return false;
        }
        return true;
    }

    private void travel(GameContext context, ServerPlayer player, PortalGroup group, Portal entered) {
        List<Portal> exits = new ArrayList<>(group.portals());
        exits.remove(entered);
        if (exits.isEmpty()) {
            return;
        }
        Portal exit = exits.get(context.getRandom().nextInt(exits.size()));
        ServerLevel world = context.getServer().getLevel(exit.worldKey());
        if (world == null) {
            return;
        }

        // Terrain can change after the portals were created. Never send someone to a now-unsafe exit.
        if (!isSafeStanding(world, BlockPos.containing(exit.center()))) {
            player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.portal.unsafe").withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        Vec3 from = player.position();
        ServerLevel fromWorld = player.level() instanceof ServerLevel current ? current : world;
        fromWorld.playSound(null, from.x, from.y, from.z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4F, 1.4F);
        fromWorld.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1.0, from.z, 60, 0.4, 0.8, 0.4, 0.4);

        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(world, exit.center().x, exit.center().y, exit.center().z, Set.of(), player.getYRot(), player.getXRot(), false);
        player.fallDistance = 0.0F;
        world.sendParticles(ParticleTypes.PORTAL, exit.center().x, exit.center().y + 1.0, exit.center().z, 60, 0.4, 0.8, 0.4, 0.4);
        player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.portal.used").withStyle(ChatFormatting.DARK_PURPLE), true);

        for (Portal portal : group.portals()) {
            ServerLevel portalWorld = context.getServer().getLevel(portal.worldKey());
            if (portalWorld != null) {
                portalWorld.playSound(null, portal.center().x, portal.center().y, portal.center().z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7F, 1.6F);
                portalWorld.sendParticles(ParticleTypes.CLOUD, portal.center().x, portal.center().y + 1.0, portal.center().z, 20, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }

    private void renderGroup(GameContext context, PortalGroup group) {
        for (Portal portal : group.portals()) {
            ServerLevel world = context.getServer().getLevel(portal.worldKey());
            if (world == null) {
                continue;
            }
            Vec3 c = portal.center();
            if (ticks % 2 == 0) {
                world.sendParticles(ParticleTypes.PORTAL, c.x, c.y + 1.0, c.z, 12, 0.35, 0.9, 0.35, 0.15);
            }
            if (ticks % 5 == 0) {
                double angle = (ticks % 40) / 40.0 * Math.PI * 2.0;
                world.sendParticles(ParticleTypes.END_ROD, c.x + Math.cos(angle) * 0.6, c.y + 0.2 + (ticks % 40) / 40.0 * 1.8, c.z + Math.sin(angle) * 0.6, 1, 0.0, 0.0, 0.0, 0.0);
                world.sendParticles(ParticleTypes.END_ROD, c.x - Math.cos(angle) * 0.6, c.y + 2.0 - (ticks % 40) / 40.0 * 1.8, c.z - Math.sin(angle) * 0.6, 1, 0.0, 0.0, 0.0, 0.0);
            }
            if (ticks % AMBIENT_SOUND_TICKS == 0) {
                world.playSound(null, c.x, c.y, c.z, SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT, 0.5F, 1.2F);
            }
        }
    }

    private record Portal(ResourceKey<Level> worldKey, Vec3 center) {
    }

    private record PortalGroup(List<Portal> portals) {
    }
}
