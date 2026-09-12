package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
    public static boolean isManagedGateway(World world, BlockPos pos) {
        return !MANAGED_GATEWAYS.isEmpty() && MANAGED_GATEWAYS.contains(GlobalPos.create(world.getRegistryKey(), pos));
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

        for (ServerPlayerEntity player : context.getParticipants()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            PortalGroup used = null;
            Portal entered = null;
            for (PortalGroup group : groups) {
                for (Portal portal : group.portals()) {
                    if (portal.worldKey() != player.getWorld().getRegistryKey()) {
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
                ServerWorld world = context.getServer().getWorld(portal.worldKey());
                if (world != null) {
                    world.playSound(null, portal.center().x, portal.center().y, portal.center().z, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.6F, 1.5F);
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
    private static void placeGatewayBlocks(ServerWorld world, Portal portal) {
        for (BlockPos pos : gatewayBlocks(portal)) {
            if (world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                world.setBlockState(pos, Blocks.END_GATEWAY.getDefaultState(), Block.NOTIFY_ALL);
                MANAGED_GATEWAYS.add(GlobalPos.create(world.getRegistryKey(), pos));
            }
        }
    }

    private static void removeGatewayBlocks(GameContext context, PortalGroup group) {
        for (Portal portal : group.portals()) {
            ServerWorld world = context.getServer().getWorld(portal.worldKey());
            for (BlockPos pos : gatewayBlocks(portal)) {
                GlobalPos key = GlobalPos.create(portal.worldKey(), pos);
                if (MANAGED_GATEWAYS.remove(key) && world != null && world.getBlockState(pos).isOf(Blocks.END_GATEWAY)) {
                    world.removeBlock(pos, false);
                }
            }
        }
    }

    private static List<BlockPos> gatewayBlocks(Portal portal) {
        BlockPos feet = BlockPos.ofFloored(portal.center());
        return List.of(feet, feet.up());
    }

    private void spawnPortals(GameContext context) {
        Random random = context.getRandom();
        Map<RegistryKey<World>, List<ServerPlayerEntity>> byWorld = new LinkedHashMap<>();
        for (ServerPlayerEntity player : context.getParticipants()) {
            if (player.isAlive() && !player.isSpectator()) {
                byWorld.computeIfAbsent(player.getWorld().getRegistryKey(), ignored -> new ArrayList<>()).add(player);
            }
        }

        for (List<ServerPlayerEntity> players : byWorld.values()) {
            if (players.size() < 2) {
                continue;
            }
            Collections.shuffle(players, random);
            List<List<ServerPlayerEntity>> buckets = new ArrayList<>();
            for (int i = 0; i + 1 < players.size(); i += 2) {
                buckets.add(new ArrayList<>(List.of(players.get(i), players.get(i + 1))));
            }
            if (players.size() % 2 == 1) {
                buckets.get(random.nextInt(buckets.size())).add(players.get(players.size() - 1));
            }

            for (List<ServerPlayerEntity> bucket : buckets) {
                List<Portal> portals = new ArrayList<>();
                for (ServerPlayerEntity player : bucket) {
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
                    ServerWorld portalWorld = context.getServer().getWorld(portal.worldKey());
                    if (portalWorld != null) {
                        placeGatewayBlocks(portalWorld, portal);
                    }
                }
                for (ServerPlayerEntity player : bucket) {
                    player.sendMessage(HunterWildcardText.translatable("msg.wildcard.portal.spawned").formatted(Formatting.DARK_PURPLE), false);
                    player.playSound(SoundEvents.BLOCK_END_PORTAL_SPAWN, 0.5F, 1.5F);
                }
            }
        }
    }

    private static Portal placePortalNear(ServerPlayerEntity player, Random random) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return null;
        }
        for (int attempt = 0; attempt < 16; attempt++) {
            double distance = MIN_OFFSET + random.nextDouble() * (MAX_OFFSET - MIN_OFFSET);
            double angle = random.nextDouble() * Math.PI * 2.0;
            BlockPos column = BlockPos.ofFloored(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
            BlockPos feet = findStanding(world, column);
            if (feet != null) {
                return new Portal(world.getRegistryKey(), new Vec3d(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5));
            }
        }
        return null;
    }

    /** Two free blocks with solid footing, searched a few blocks up and down from the player's height. */
    private static BlockPos findStanding(ServerWorld world, BlockPos start) {
        for (int offset = 0; offset <= 6; offset++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                if (offset == 0 && sign == 1) {
                    continue;
                }
                BlockPos feet = start.up(sign * offset);
                if (isSafeStanding(world, feet)) {
                    return feet;
                }
            }
        }
        // A ceiling world's heightmap points above its bedrock roof, not to a walkable cave.
        if (world.getDimension().hasCeiling()) return null;
        BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, start);
        return isSafeStanding(world, top) ? top : null;
    }

    private static boolean isSafeStanding(ServerWorld world, BlockPos feet) {
        if (!world.getWorldBorder().contains(feet) || feet.getY() <= world.getBottomY()
                || feet.getY() + 1 >= world.getBottomY() + world.getHeight()) return false;
        if (world.getDimension().hasCeiling()
                && feet.getY() > world.getBottomY() + world.getDimension().logicalHeight() - 8) return false;
        var ground = world.getBlockState(feet.down());
        if (!ground.isSolidBlock(world, feet.down()) || !world.getFluidState(feet.down()).isEmpty()
                || ground.isOf(Blocks.MAGMA_BLOCK) || ground.isOf(Blocks.CACTUS)) return false;
        for (BlockPos pos : List.of(feet, feet.up())) {
            var state = world.getBlockState(pos);
            if (!state.getCollisionShape(world, pos).isEmpty() || !world.getFluidState(pos).isEmpty()
                    || state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE) || state.isOf(Blocks.POWDER_SNOW)
                    || state.isOf(Blocks.WITHER_ROSE) || state.isOf(Blocks.SWEET_BERRY_BUSH)
                    || state.isOf(Blocks.NETHER_PORTAL) || state.isOf(Blocks.END_PORTAL)
                    || (state.isOf(Blocks.END_GATEWAY) && !isManagedGateway(world, pos))) return false;
        }
        return true;
    }

    private void travel(GameContext context, ServerPlayerEntity player, PortalGroup group, Portal entered) {
        List<Portal> exits = new ArrayList<>(group.portals());
        exits.remove(entered);
        if (exits.isEmpty()) {
            return;
        }
        Portal exit = exits.get(context.getRandom().nextInt(exits.size()));
        ServerWorld world = context.getServer().getWorld(exit.worldKey());
        if (world == null) {
            return;
        }

        // Terrain can change after the portals were created. Never send someone to a now-unsafe exit.
        if (!isSafeStanding(world, BlockPos.ofFloored(exit.center()))) {
            player.sendMessage(HunterWildcardText.translatable("msg.wildcard.portal.unsafe").formatted(Formatting.YELLOW), true);
            return;
        }

        Vec3d from = player.getPos();
        ServerWorld fromWorld = player.getWorld() instanceof ServerWorld current ? current : world;
        fromWorld.playSound(null, from.x, from.y, from.z, SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.4F, 1.4F);
        fromWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1.0, from.z, 60, 0.4, 0.8, 0.4, 0.4);

        player.setVelocity(Vec3d.ZERO);
        player.teleport(world, exit.center().x, exit.center().y, exit.center().z, Set.of(), player.getYaw(), player.getPitch(), false);
        player.fallDistance = 0.0F;
        world.spawnParticles(ParticleTypes.PORTAL, exit.center().x, exit.center().y + 1.0, exit.center().z, 60, 0.4, 0.8, 0.4, 0.4);
        player.sendMessage(HunterWildcardText.translatable("msg.wildcard.portal.used").formatted(Formatting.DARK_PURPLE), true);

        for (Portal portal : group.portals()) {
            ServerWorld portalWorld = context.getServer().getWorld(portal.worldKey());
            if (portalWorld != null) {
                portalWorld.playSound(null, portal.center().x, portal.center().y, portal.center().z, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.7F, 1.6F);
                portalWorld.spawnParticles(ParticleTypes.CLOUD, portal.center().x, portal.center().y + 1.0, portal.center().z, 20, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }

    private void renderGroup(GameContext context, PortalGroup group) {
        for (Portal portal : group.portals()) {
            ServerWorld world = context.getServer().getWorld(portal.worldKey());
            if (world == null) {
                continue;
            }
            Vec3d c = portal.center();
            if (ticks % 2 == 0) {
                world.spawnParticles(ParticleTypes.PORTAL, c.x, c.y + 1.0, c.z, 12, 0.35, 0.9, 0.35, 0.15);
            }
            if (ticks % 5 == 0) {
                double angle = (ticks % 40) / 40.0 * Math.PI * 2.0;
                world.spawnParticles(ParticleTypes.END_ROD, c.x + Math.cos(angle) * 0.6, c.y + 0.2 + (ticks % 40) / 40.0 * 1.8, c.z + Math.sin(angle) * 0.6, 1, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticles(ParticleTypes.END_ROD, c.x - Math.cos(angle) * 0.6, c.y + 2.0 - (ticks % 40) / 40.0 * 1.8, c.z - Math.sin(angle) * 0.6, 1, 0.0, 0.0, 0.0, 0.0);
            }
            if (ticks % AMBIENT_SOUND_TICKS == 0) {
                world.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT, 0.5F, 1.2F);
            }
        }
    }

    private record Portal(RegistryKey<World> worldKey, Vec3d center) {
    }

    private record PortalGroup(List<Portal> portals) {
    }
}
