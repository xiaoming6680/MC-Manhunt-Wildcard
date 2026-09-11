package com.xiaoming.hunterwildcard.backrooms;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.BackroomsPhase;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Moves players into and out of the Backrooms and keeps every promise around it: no fall damage on
 * either trip, a persisted return address, rescue on rejoin, and a clean sweep on server stop.
 * <p>
 * Timings: FALL (camera sink) 10 ticks, then blackout; the teleport happens 10 ticks into the
 * blackout so the destination assembles behind the cover.
 */
public final class BackroomsSession {
    public static final int ENTRY_FALL_TICKS = 10;
    public static final int ENTRY_BLACKOUT_TICKS = 30;
    public static final int ENTRY_TELEPORT_AT = ENTRY_FALL_TICKS + 8;
    public static final int EXIT_BLACKOUT_TICKS = 30;
    public static final int EXIT_TELEPORT_AT = 8;
    public static final int ENTRY_DROP_BLOCKS = 2;
    private static final int SKY_RETURN_HEIGHT = 150;
    private static final int SKY_RETURN_HEADROOM = 8;
    private static final int DESCENT_TIMEOUT_TICKS = 20 * 60;
    private static final int ENTRY_RANGE = 800_000;

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Integer> DESCENDING = new HashMap<>();
    /** Tick at which a descending player first touched the ground; the shield stays up a moment longer. */
    private static final Map<UUID, Integer> LANDED_AT = new HashMap<>();
    private static final int LANDING_GRACE_TICKS = 10;
    /** Players this wildcard has sent down, whether or not they are still there. */
    private static final Set<UUID> SENT = new HashSet<>();
    private static final Set<UUID> EXITED = new HashSet<>();
    private static final Random RANDOM = new Random();
    private static boolean initialized;
    private static boolean active;

    private BackroomsSession() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(BackroomsSession::tick);
        // A single tick of free fall already exceeds the three-block threshold, so resetting the
        // distance is not enough on its own: the sky return simply may not cost fall damage.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayerEntity player && source.isOf(DamageTypes.FALL) && DESCENDING.containsKey(player.getUuid())));
        ServerPlayerEvents.JOIN.register(BackroomsSession::recoverOnJoin);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // Vanilla already placed them at their bed / world spawn; only the bookkeeping is ours.
            newPlayer.removeAttached(BackroomsReturnData.ATTACHMENT);
            PENDING.remove(newPlayer.getUuid());
            DESCENDING.remove(newPlayer.getUuid());
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> !BackroomsDimension.isBackrooms(world));
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!BackroomsDimension.isBackrooms(world)) {
                return ActionResult.PASS;
            }
            ItemStack held = player.getStackInHand(hand);
            boolean worldChanging = held.getItem() instanceof BucketItem || held.isOf(Items.FLINT_AND_STEEL) || held.isOf(Items.FIRE_CHARGE);
            return worldChanging ? ActionResult.FAIL : ActionResult.PASS;
        });
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean hasExited(UUID playerId) {
        return EXITED.contains(playerId);
    }

    public static boolean wasSent(UUID playerId) {
        return SENT.contains(playerId);
    }

    /** Drops every participant into a fresh patch of the maze, hunters kept apart from runners. */
    public static void begin(GameContext context) {
        MinecraftServer server = context.getServer();
        ServerWorld layer = server.getWorld(BackroomsDimension.WORLD_KEY);
        if (layer == null) {
            HunterWildcardMod.LOGGER.warn("Backrooms dimension is missing; the wildcard does nothing.");
            return;
        }

        active = true;
        SENT.clear();
        EXITED.clear();
        long seed = seedOf(layer);
        int baseCellX = BackroomsLayout.cell(RANDOM.nextInt(-ENTRY_RANGE, ENTRY_RANGE));
        int baseCellZ = BackroomsLayout.cell(RANDOM.nextInt(-ENTRY_RANGE, ENTRY_RANGE));

        List<ServerPlayerEntity> runners = new ArrayList<>(context.getRunners());
        List<ServerPlayerEntity> hunters = new ArrayList<>(context.getHunters());
        List<ServerPlayerEntity> others = new ArrayList<>();
        for (ServerPlayerEntity participant : context.getParticipants()) {
            if (!runners.contains(participant) && !hunters.contains(participant)) {
                others.add(participant);
            }
        }
        // Runners scatter within ~60 blocks of the centre; hunters land on a ring ~100-140 blocks out.
        int[][] runnerOffsets = {{0, 0}, {8, 0}, {-8, 0}, {0, 8}, {0, -8}, {8, 8}, {-8, -8}, {8, -8}, {-8, 8}, {16, 0}, {-16, 0}, {0, 16}};
        int[][] hunterOffsets = {{24, 0}, {-24, 0}, {0, 24}, {0, -24}, {24, 24}, {-24, -24}, {24, -24}, {-24, 24}, {28, 8}, {-28, -8}, {8, 28}, {-8, -28}};
        int tick = server.getTicks();
        int index = 0;
        for (ServerPlayerEntity runner : runners) {
            int[] offset = runnerOffsets[index++ % runnerOffsets.length];
            schedule(runner, layer, seed, baseCellX + offset[0], baseCellZ + offset[1], tick);
        }
        index = RANDOM.nextInt(hunterOffsets.length);
        for (ServerPlayerEntity hunter : hunters) {
            int[] offset = hunterOffsets[index++ % hunterOffsets.length];
            schedule(hunter, layer, seed, baseCellX + offset[0], baseCellZ + offset[1], tick);
        }
        for (ServerPlayerEntity other : others) {
            schedule(other, layer, seed, baseCellX + 4, baseCellZ + 4, tick);
        }
    }

    private static void schedule(ServerPlayerEntity player, ServerWorld layer, long seed, int cellX, int cellZ, int now) {
        if (BackroomsDimension.isInBackrooms(player)) {
            return;
        }
        int[] cell = BackroomsMazePolicy.standableCell(seed, cellX, cellZ);
        int[] centre = BackroomsMazePolicy.cellCentre(cell[0], cell[1]);
        BlockPos target = new BlockPos(centre[0], BackroomsLayout.FLOOR_Y + 1 + ENTRY_DROP_BLOCKS, centre[1]);
        player.setAttached(BackroomsReturnData.ATTACHMENT, BackroomsReturnData.capture(player));
        SENT.add(player.getUuid());
        PENDING.put(player.getUuid(), new Pending(player.getUuid(), target, now + ENTRY_FALL_TICKS, now + ENTRY_TELEPORT_AT, true));
        HunterWildcardPackets.sendBackroomsPhase(player, BackroomsPhase.FALL, ENTRY_FALL_TICKS);
        // Ask for the chunk now so it is generated by the time the cover lifts.
        layer.getChunk(target.getX() >> 4, target.getZ() >> 4);
    }

    /** The wildcard ended: everyone still inside sinks into the floor and comes home. */
    public static void end(GameContext context) {
        MinecraftServer server = context.getServer();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (BackroomsDimension.isInBackrooms(player) || PENDING.containsKey(player.getUuid())) {
                PENDING.remove(player.getUuid());
                beginReturn(server, player, true);
            }
        }
        active = false;
        SENT.clear();
        EXITED.clear();
    }

    /** Whether a whole side has left the maze (found an exit, died or disconnected). */
    public static boolean sideFullyOut(GameContext context, PlayerRole role) {
        List<ServerPlayerEntity> members = role == PlayerRole.HUNTER ? context.getHunters() : context.getRunners();
        boolean anySent = false;
        for (ServerPlayerEntity member : members) {
            if (!SENT.contains(member.getUuid())) {
                continue;
            }
            anySent = true;
            if (BackroomsDimension.isInBackrooms(member) || PENDING.containsKey(member.getUuid())) {
                return false;
            }
        }
        return anySent;
    }

    private static void beginReturn(MinecraftServer server, ServerPlayerEntity player, boolean sink) {
        int now = server.getTicks();
        int fall = sink ? ENTRY_FALL_TICKS : 0;
        if (sink) {
            HunterWildcardPackets.sendBackroomsPhase(player, BackroomsPhase.FALL, ENTRY_FALL_TICKS);
        }
        PENDING.put(player.getUuid(), new Pending(player.getUuid(), null, now + fall, now + fall + EXIT_TELEPORT_AT, false));
    }

    private static void tick(MinecraftServer server) {
        int now = server.getTicks();
        for (Pending pending : List.copyOf(PENDING.values())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(pending.playerId);
            if (player == null) {
                PENDING.remove(pending.playerId);
                continue;
            }
            if (now >= pending.coverTick && !pending.coverSent) {
                pending.coverSent = true;
                HunterWildcardPackets.sendBackroomsPhase(player, pending.entering ? BackroomsPhase.ENTER : BackroomsPhase.EXIT,
                        pending.entering ? ENTRY_BLACKOUT_TICKS : EXIT_BLACKOUT_TICKS);
            }
            if (now < pending.dueTick) {
                continue;
            }
            PENDING.remove(pending.playerId);
            if (pending.entering) {
                arrive(server, player, pending.target);
            } else {
                returnHome(server, player);
            }
        }

        if (active) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (BackroomsDimension.isInBackrooms(player) && player.getY() < BackroomsLayout.voidThreshold()
                        && !PENDING.containsKey(player.getUuid())) {
                    EXITED.add(player.getUuid());
                    beginReturn(server, player, false);
                }
            }
        }

        if (!DESCENDING.isEmpty()) {
            for (Map.Entry<UUID, Integer> entry : Map.copyOf(DESCENDING).entrySet()) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player == null || player.isRemoved() || !player.isAlive() || now - entry.getValue() > DESCENT_TIMEOUT_TICKS) {
                    DESCENDING.remove(entry.getKey());
                    continue;
                }
                player.fallDistance = 0.0D;
                if (player.isOnGround()) {
                    int landedAt = LANDED_AT.computeIfAbsent(entry.getKey(), ignored -> now);
                    if (now - landedAt >= LANDING_GRACE_TICKS) {
                        DESCENDING.remove(entry.getKey());
                        LANDED_AT.remove(entry.getKey());
                    }
                } else {
                    LANDED_AT.remove(entry.getKey());
                }
            }
        }
    }

    private static void arrive(MinecraftServer server, ServerPlayerEntity player, BlockPos target) {
        ServerWorld layer = server.getWorld(BackroomsDimension.WORLD_KEY);
        if (layer == null) {
            player.removeAttached(BackroomsReturnData.ATTACHMENT);
            return;
        }
        layer.getChunk(target.getX() >> 4, target.getZ() >> 4);
        place(player, layer, target, player.getYaw(), player.getPitch());
    }

    /** Sky return over the recorded position when the source has open sky; straight back otherwise. */
    private static void returnHome(MinecraftServer server, ServerPlayerEntity player) {
        BackroomsReturnData data = player.getAttached(BackroomsReturnData.ATTACHMENT);
        ServerWorld source = null;
        BlockPos entry;
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        if (data != null) {
            Identifier id = Identifier.tryParse(data.dimension());
            if (id != null) {
                source = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
            }
            entry = data.pos();
            yaw = data.yaw();
            pitch = data.pitch();
        } else {
            entry = null;
        }
        if (source == null || BackroomsDimension.isBackrooms(source)) {
            source = server.getOverworld();
        }
        if (entry == null) {
            entry = source.getSpawnPoint().getPos();
        }

        BlockPos target;
        if (!source.getDimension().hasCeiling()) {
            int ceiling = source.getBottomY() + source.getHeight() - SKY_RETURN_HEADROOM;
            int y = Math.min(ceiling, entry.getY() + SKY_RETURN_HEIGHT);
            target = new BlockPos(entry.getX(), y, entry.getZ());
            DESCENDING.put(player.getUuid(), server.getTicks());
        } else {
            target = safeSpot(source, entry);
        }
        source.getChunk(target.getX() >> 4, target.getZ() >> 4);
        place(player, source, target, yaw, pitch);
        player.removeAttached(BackroomsReturnData.ATTACHMENT);
        HunterWildcardPackets.sendBackroomsPhase(player, BackroomsPhase.CLEAR, 0);
    }

    private static BlockPos safeSpot(ServerWorld world, BlockPos entry) {
        BlockPos.Mutable cursor = entry.mutableCopy();
        for (int up = 0; up < 6; up++) {
            if (world.getBlockState(cursor).getCollisionShape(world, cursor).isEmpty()
                    && world.getBlockState(cursor.up()).getCollisionShape(world, cursor.up()).isEmpty()) {
                return cursor.toImmutable();
            }
            cursor.move(0, 1, 0);
        }
        return entry;
    }

    private static void place(ServerPlayerEntity player, ServerWorld world, BlockPos target, float yaw, float pitch) {
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0D;
        player.teleport(world, target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, Set.<PositionFlag>of(), yaw, pitch, true);
        player.fallDistance = 0.0D;
    }

    /** A player who logged out inside the maze while no wildcard is running gets sent home on login. */
    private static void recoverOnJoin(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        if (BackroomsDimension.isInBackrooms(player)) {
            if (active && SENT.contains(player.getUuid())) {
                return;
            }
            returnHome(server, player);
            return;
        }
        player.removeAttached(BackroomsReturnData.ATTACHMENT);
    }

    /** Fired from GameManager on server stop; everyone comes home without ceremony. */
    public static void returnEveryone(MinecraftServer server) {
        for (ServerPlayerEntity player : List.copyOf(server.getPlayerManager().getPlayerList())) {
            if (BackroomsDimension.isInBackrooms(player)) {
                returnHome(server, player);
            }
        }
        PENDING.clear();
        DESCENDING.clear();
        LANDED_AT.clear();
        SENT.clear();
        EXITED.clear();
        active = false;
    }

    public static int surfaceY(ServerWorld world, int x, int z) {
        return world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
    }

    private static long seedOf(ServerWorld layer) {
        if (layer.getChunkManager().getChunkGenerator() instanceof BackroomsChunkGenerator generator) {
            return generator.getSeed();
        }
        return BackroomsLayout.DEFAULT_SEED;
    }

    private static final class Pending {
        private final UUID playerId;
        private final BlockPos target;
        private final int coverTick;
        private final int dueTick;
        private final boolean entering;
        private boolean coverSent;

        private Pending(UUID playerId, BlockPos target, int coverTick, int dueTick, boolean entering) {
            this.playerId = playerId;
            this.target = target;
            this.coverTick = coverTick;
            this.dueTick = dueTick;
            this.entering = entering;
        }
    }
}
