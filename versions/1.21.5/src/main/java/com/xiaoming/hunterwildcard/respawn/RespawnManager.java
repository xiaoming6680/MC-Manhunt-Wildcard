package com.xiaoming.hunterwildcard.respawn;

import com.xiaoming.hunterwildcard.backrooms.BackroomsDimension;
import com.xiaoming.hunterwildcard.compass.CompassTracker;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.game.HunterVictoryType;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.util.PlayerUtil;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.s2c.play.SetCameraEntityS2CPacket;
import com.xiaoming.hunterwildcard.mixin.TeleportPendingAccessor;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Deaths, kill credit, respawn waits and respawn placement.
 * <p>
 * Kill credit: a runner death is a hunter kill when a hunter dealt the final blow or hit the runner
 * within the credit window. Pure environment deaths are counted separately and every N of them
 * (configurable) convert into one hunter kill, so dying on purpose never resets a chase for free.
 * <p>
 * Waiting players follow living teammates; eliminated players freely visit any online player's position. The original respawn
 * anchor is saved separately, so watching someone never changes the respawn placement rules.
 */
public class RespawnManager {
    private static final int DEATH_WAIT_SYNC_INTERVAL_TICKS = 20;
    private static final double CAMERA_RELOCATE_DISTANCE_SQUARED = 64 * 64;
    private final Set<UUID> pendingDeathRespawns = new HashSet<>();
    private final Set<UUID> pendingRespawnSetup = new HashSet<>();
    private final Map<UUID, DeathSpectator> spectators = new HashMap<>();

    private final Map<UUID, WaitingPlayer> waiting = new HashMap<>();
    private final Map<UUID, Integer> remainingLives = new HashMap<>();
    private final Set<UUID> outPlayers = new HashSet<>();
    private final Map<UUID, HunterHit> lastHunterHits = new HashMap<>();
    private final Map<UUID, UUID> lastKillers = new HashMap<>();
    private final Map<UUID, Integer> hunterDeaths = new HashMap<>();
    private static final int MAX_HUNTER_PENALTY_SECONDS = 60;
    /** Deaths of the hunter currently being processed, before this one; read by respawnSecondsFor. */
    private int pendingHunterDeaths;
    private int runnerKillCount;
    private int environmentDeaths;
    private int tickCounter;

    public void start(GameContext context) {
        clear();
        for (ServerPlayerEntity hunter : context.getHunters()) {
            remainingLives.put(hunter.getUuid(), initialLives(PlayerRole.HUNTER, context.getConfig()));
        }
        for (ServerPlayerEntity runner : context.getRunners()) {
            remainingLives.put(runner.getUuid(), initialLives(PlayerRole.RUNNER, context.getConfig()));
        }
        refreshHunterProgress(context);
    }

    /** Remembers the last hunter who damaged a runner so later deaths can still be credited. */
    public void recordHunterHit(ServerPlayerEntity runner, ServerPlayerEntity hunter) {
        lastHunterHits.put(runner.getUuid(), new HunterHit(hunter.getUuid(), tickCounter));
    }

    public DeathOutcome onPlayerDeath(GameContext context, ServerPlayerEntity player, PlayerRole role, ServerPlayerEntity directHunterKiller) {
        if (role == null) {
            return DeathOutcome.none();
        }
        pendingDeathRespawns.add(player.getUuid());

        ModConfig config = context.getConfig();
        // Previous deaths of this hunter drive the escalating respawn wait computed below.
        pendingHunterDeaths = role == PlayerRole.HUNTER ? hunterDeaths.merge(player.getUuid(), 1, Integer::sum) - 1 : 0;
        ServerPlayerEntity creditedHunter = directHunterKiller;
        boolean assisted = false;
        HunterHit lastHit = lastHunterHits.remove(player.getUuid());
        if (creditedHunter == null && role == PlayerRole.RUNNER && lastHit != null
                && tickCounter - lastHit.tick() <= config.getHunterHitCreditTicks()) {
            ServerPlayerEntity hunter = context.getServer().getPlayerManager().getPlayer(lastHit.hunterId());
            if (hunter != null && context.getTeamManager().isHunter(hunter)) {
                creditedHunter = hunter;
                assisted = true;
            }
        }

        KillCredit credit = KillCredit.NONE;
        if (role == PlayerRole.RUNNER) {
            credit = registerRunnerDeath(config, creditedHunter);
        }
        Text deathPrefix = deathPrefix(player, role, creditedHunter, assisted, credit, config);
        String reasonSpec = creditedHunter != null
                ? HunterWildcardText.spec("hud.death_wait.killed_by", PlayerUtil.displayNameSpec(creditedHunter))
                : HunterWildcardText.spec("hud.death_wait.died");
        String endingReason = null;

        String killerSpec = creditedHunter != null ? PlayerUtil.displayNameSpec(creditedHunter) : HunterWildcardText.key("common.environment");
        if (creditedHunter != null) {
            lastKillers.put(player.getUuid(), creditedHunter.getUuid());
        } else {
            lastKillers.remove(player.getUuid());
        }
        if (role == PlayerRole.RUNNER && config.getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT && credit.counted()) {
            int remainingKills = Math.max(0, config.hunterRunnerKillTarget - runnerKillCount);
            HunterWildcardPackets.sendHunterKillFeedback(
                    context,
                    killerSpec,
                    PlayerUtil.displayNameSpec(player),
                    remainingKills,
                    runnerKillCount,
                    config.hunterRunnerKillTarget
            );
            if (runnerKillCount >= config.hunterRunnerKillTarget) {
                endingReason = HunterWildcardText.spec("msg.win.hunter.kill_target", config.hunterRunnerKillTarget);
            } else {
                announceKillCountMilestone(context, remainingKills);
            }
        } else if (role == PlayerRole.RUNNER && config.getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT && credit.environment()) {
            // Environment death that has not filled the quota yet: still a top-right card, just calmer.
            HunterWildcardPackets.sendHudFeedback(
                    context,
                    HunterWildcardText.spec("hud.feedback.env_death.title"),
                    HunterWildcardText.spec("hud.feedback.versus", killerSpec, PlayerUtil.displayNameSpec(player)),
                    HunterWildcardText.spec("hud.feedback.env_progress", credit.environmentProgress(), config.environmentDeathsPerKill),
                    "neutral"
            );
        }
        refreshHunterProgress(context);

        RespawnMode mode = modeFor(role, config);
        if (mode == RespawnMode.INFINITE) {
            scheduleRespawn(context, player, role, respawnTicksFor(role, config), reasonSpec);
            sendEliminationFeedback(context, player, role, killerSpec, creditedHunter != null, HunterWildcardText.key("common.infinite"), false);
            Text message = HunterWildcardText.translatable("msg.death.respawn_scheduled", deathPrefix, respawnSecondsFor(role, config));
            return endingReason != null ? DeathOutcome.messageAndEnd(message, endingReason) : DeathOutcome.message(message);
        }

        int livesAfterDeath = mode == RespawnMode.NO_RESPAWN ? 0 : decrementLife(player, role, config);
        if (livesAfterDeath > 0) {
            scheduleRespawn(context, player, role, respawnTicksFor(role, config), reasonSpec);
            refreshHunterProgress(context);
            sendEliminationFeedback(context, player, role, killerSpec, creditedHunter != null, Integer.toString(livesAfterDeath), false);
            Text message = HunterWildcardText.translatable("msg.death.limited_respawn_scheduled", deathPrefix, livesAfterDeath, respawnSecondsFor(role, config));
            return endingReason != null ? DeathOutcome.messageAndEnd(message, endingReason) : DeathOutcome.message(message);
        }

        markOut(player);
        refreshHunterProgress(context);
        sendEliminationFeedback(context, player, role, killerSpec, creditedHunter != null, "0", true);
        Text outMessage = HunterWildcardText.translatable("msg.death.out", deathPrefix);
        if (endingReason == null && role == PlayerRole.RUNNER) {
            endingReason = runnerLossReason(context, player);
        }
        return endingReason != null ? DeathOutcome.messageAndEnd(outMessage, endingReason) : DeathOutcome.message(outMessage);
    }

    /**
     * Top-right card for runner deaths outside kill-count mode (kill-count mode has its own card with
     * the target progress): who got whom, and lives left or "out".
     */
    private void sendEliminationFeedback(GameContext context, ServerPlayerEntity player, PlayerRole role, String killerSpec,
                                         boolean hunterCredited, String livesArg, boolean out) {
        if (role != PlayerRole.RUNNER || context.getConfig().getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT) {
            return;
        }
        String title = hunterCredited ? HunterWildcardText.spec("hud.feedback.kill.title") : HunterWildcardText.spec("hud.feedback.env_kill.title");
        String line2 = out ? HunterWildcardText.spec("hud.feedback.runner_out") : HunterWildcardText.spec("hud.feedback.lives_left", livesArg);
        HunterWildcardPackets.sendHudFeedback(
                context,
                title,
                HunterWildcardText.spec("hud.feedback.versus", killerSpec, PlayerUtil.displayNameSpec(player)),
                line2,
                "kill"
        );
    }

    /** Updates the kill counters for a runner death and says whether it counted as a hunter kill. */
    private KillCredit registerRunnerDeath(ModConfig config, ServerPlayerEntity creditedHunter) {
        if (config.getHunterVictoryType() != HunterVictoryType.RUNNER_KILL_COUNT) {
            return KillCredit.NONE;
        }
        if (creditedHunter != null) {
            runnerKillCount++;
            return new KillCredit(true, false, 0);
        }
        if (!config.environmentKillsEnabled) {
            return KillCredit.NONE;
        }
        environmentDeaths++;
        if (environmentDeaths >= config.environmentDeathsPerKill) {
            environmentDeaths = 0;
            runnerKillCount++;
            return new KillCredit(true, true, 0);
        }
        return new KillCredit(false, true, environmentDeaths);
    }

    public void onAfterRespawn(GameContext context, ServerPlayerEntity player, CompassTracker compassTracker) {
        pendingDeathRespawns.remove(player.getUuid());
        if (outPlayers.contains(player.getUuid()) || waiting.containsKey(player.getUuid())) {
            // Fabric fires before the network handler replaces its old player. Teleport only after that replacement.
            pendingRespawnSetup.add(player.getUuid());
            return;
        }
        finishVanillaRespawn(context, player, compassTracker);
    }

    private void finishVanillaRespawn(GameContext context, ServerPlayerEntity player, CompassTracker compassTracker) {
        if (outPlayers.contains(player.getUuid())) {
            beginSpectating(context, player, player.getWorld().getRegistryKey(), player.getPos());
            player.sendMessage(HunterWildcardText.translatable("msg.respawn.out_spectating"), false);
            return;
        }

        WaitingPlayer wait = waiting.get(player.getUuid());
        if (wait != null) {
            if (returnsToOverworld(wait)) returnToOverworld(context, player);
            if (wait.remainingTicks <= 0) {
                waiting.remove(player.getUuid());
                finishRespawn(context, player, wait, compassTracker);
                return;
            }
            beginHolding(context, player, wait);
            return;
        }

        if (context.getTeamManager().isHunter(player)) {
            compassTracker.giveCompass(player);
        }
        highlightKiller(context, player);
        HunterWildcardPackets.sendObjectiveTo(player);
    }

    /** A respawned runner sees the hunter that killed them glow for ten seconds, so they know which way not to run. */
    private void highlightKiller(GameContext context, ServerPlayerEntity player) {
        UUID killerId = lastKillers.remove(player.getUuid());
        if (killerId == null || !context.getTeamManager().isRunner(player)) {
            return;
        }
        ServerPlayerEntity killer = context.getServer().getPlayerManager().getPlayer(killerId);
        if (killer == null || !killer.isAlive() || !context.getTeamManager().isHunter(killer)) {
            return;
        }
        killer.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, KILLER_HIGHLIGHT_TICKS, 0, false, false, false));
        player.sendMessage(HunterWildcardText.translatable("msg.respawn.killer_highlighted", PlayerUtil.displayNameText(killer), KILLER_HIGHLIGHT_TICKS / 20).formatted(Formatting.RED), false);
    }

    private static boolean returnsToOverworld(WaitingPlayer wait) {
        return World.NETHER.equals(wait.deathWorld) || World.END.equals(wait.deathWorld);
    }

    /** Keep a valid vanilla Overworld respawn; dimension-specific anchors fall back to world spawn. */
    private void returnToOverworld(GameContext context, ServerPlayerEntity player) {
        ServerWorld overworld = context.getServer().getOverworld();
        if (player.getWorld() == overworld) return;
        var spawn = overworld.getSpawnPos();
        BlockPos pos = spawn;
        player.setVelocity(Vec3d.ZERO);
        player.teleport(overworld, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                Set.<PositionFlag>of(), overworld.getSpawnAngle(), 0.0F, true);
    }

    /** Save the respawn destination independently of the teammate camera. */
    private void beginHolding(GameContext context, ServerPlayerEntity player, WaitingPlayer wait) {
        boolean relocate = context.getConfig().randomRespawnEnabled;
        ServerWorld deathWorld = context.getServer().getWorld(wait.deathWorld);
        if (relocate && !returnsToOverworld(wait) && deathWorld != null && !BackroomsDimension.isBackrooms(deathWorld)) {
            wait.holdWorld = deathWorld.getRegistryKey();
            wait.holdPos = wait.deathPos;
        } else {
            wait.holdWorld = player.getWorld().getRegistryKey();
            wait.holdPos = player.getPos();
        }
        wait.holding = true;
        beginSpectating(context, player, wait.holdWorld, wait.holdPos);
        sendDeathWait(player, wait);
    }

    private void beginSpectating(GameContext context, ServerPlayerEntity player, RegistryKey<World> world, Vec3d pos) {
        DeathSpectator spectator = new DeathSpectator(context.getTeamManager().getRole(player), world, pos, player.getYaw(), player.getPitch());
        spectators.put(player.getUuid(), spectator);
        player.changeGameMode(GameMode.SPECTATOR);
        restoreAnchor(context, player, spectator);
        updateSpectating(context, player, spectator, 0);
    }

    /** Only this player's server-validated teammate list can be cycled; no client-supplied entity IDs. */
    public void cycleSpectating(GameContext context, ServerPlayerEntity player, boolean previous) {
        DeathSpectator spectator = spectators.get(player.getUuid());
        if (spectator == null || !player.isSpectator() || player.isDead()) return;
        updateSpectating(context, player, spectator, previous ? -1 : 1);
    }

    /** Runs before vanilla copies camera coordinates into the spectator's current world. */
    public void beforeSpectatorTick(ServerPlayerEntity player) {
        if (!spectators.containsKey(player.getUuid())) return;
        Entity camera = player.getCameraEntity();
        if (camera != player && (camera.isRemoved() || !camera.isAlive() || player.isSneaking()
                || camera.getWorld() != player.getWorld()
                || camera.getPos().squaredDistanceTo(player.getPos()) > CAMERA_RELOCATE_DISTANCE_SQUARED)) {
            setDeathCamera(player, player);
        }
    }

    /** Prevent vanilla teleportSpectatingPlayers from racing our pending camera transition. */
    public void beforeTargetTeleport(ServerPlayerEntity target) {
        for (var entry : spectators.entrySet()) {
            var viewer = target.getServerWorld().getServer().getPlayerManager().getPlayer(entry.getKey());
            if (viewer != null && viewer.getCameraEntity() == target) {
                setDeathCamera(viewer, viewer);
                entry.getValue().positionedTarget = null;
            }
        }
    }

    private static void setDeathCamera(ServerPlayerEntity player, Entity target) {
        if (player.getCameraEntity() == target) return;
        ((DeathCameraAccess) player).hunterwildcard$setDeathCamera(target);
        player.networkHandler.sendPacket(new SetCameraEntityS2CPacket(target));
        player.networkHandler.syncWithPlayerPosition();
    }

    private static boolean teleportPending(ServerPlayerEntity player) {
        return ((TeleportPendingAccessor) player.networkHandler).hunterwildcard$pendingTeleport() != null;
    }

    private void updateSpectating(GameContext context, ServerPlayerEntity player, DeathSpectator spectator, int direction) {
        if (outPlayers.contains(player.getUuid())) {
            updateFreeSpectating(context, player, spectator, direction);
            return;
        }
        List<ServerPlayerEntity> targets = context.getParticipants().stream()
                .filter(target -> target != player && !target.isRemoved() && target.isAlive() && !target.isSpectator()
                        && spectator.role != null && context.getTeamManager().getRole(target) == spectator.role
                        && !waiting.containsKey(target.getUuid()) && !outPlayers.contains(target.getUuid()))
                .sorted(Comparator.comparing(ServerPlayerEntity::getUuid)).toList();
        int current = -1;
        for (int i = 0; i < targets.size(); i++) if (targets.get(i).getUuid().equals(spectator.target)) current = i;
        ServerPlayerEntity target = targets.isEmpty() ? null : targets.get(current < 0
                ? (direction < 0 ? targets.size() - 1 : 0) : Math.floorMod(current + direction, targets.size()));
        UUID targetId = target == null ? null : target.getUuid();
        boolean targetChanged = !java.util.Objects.equals(spectator.target, targetId);
        boolean changed = !java.util.Objects.equals(spectator.target, targetId) || spectator.targetCount != targets.size();
        spectator.target = targetId;
        spectator.targetCount = targets.size();
        if (changed || direction != 0 || tickCounter % DEATH_WAIT_SYNC_INTERVAL_TICKS == 0) {
            HunterWildcardPackets.sendDeathSpectate(player, true, target == null ? "" : PlayerUtil.displayNameSpec(target), targets.size());
        }
        if (targetChanged) {
            spectator.positionedTarget = null;
            setDeathCamera(player, player);
        }
        if (target == null || player.isSneaking()) {
            setDeathCamera(player, player);
            return;
        }
        beforeSpectatorTick(player);
        // Never supersede an outstanding position confirmation with another camera teleport.
        // Further cycling updates the desired UUID above and is coalesced until the client catches up.
        if (teleportPending(player)) return;
        if (player.getCameraEntity() != target) {
            if (!targetId.equals(spectator.positionedTarget) || player.getWorld() != target.getServerWorld()
                    || player.getPos().squaredDistanceTo(target.getPos()) > CAMERA_RELOCATE_DISTANCE_SQUARED) {
                if (tickCounter < spectator.nextRelocateTick) return;
                setDeathCamera(player, player);
                player.setVelocity(Vec3d.ZERO);
                if (player.teleport(target.getServerWorld(), target.getX(), target.getY(), target.getZ(),
                        Set.<PositionFlag>of(), target.getYaw(), target.getPitch(), true)) {
                    spectator.positionedTarget = targetId;
                    spectator.attachAfterTick = tickCounter + 2;
                    spectator.nextRelocateTick = tickCounter + 5;
                }
                return;
            }
            if (tickCounter < spectator.attachAfterTick || !PlayerLookup.tracking(target).contains(player)) return;
            setDeathCamera(player, target);
            spectator.cameraSyncTicks = 5;
        }
        // Only resend a camera that is in this world and already being tracked by this client.
        if (((spectator.cameraSyncTicks > 0 && --spectator.cameraSyncTicks == 0)
                || tickCounter % DEATH_WAIT_SYNC_INTERVAL_TICKS == 0) && PlayerLookup.tracking(target).contains(player))
            player.networkHandler.sendPacket(new SetCameraEntityS2CPacket(target));
    }

    /** Eliminated players can jump to anyone, but neither the camera nor their position keeps following. */
    private void updateFreeSpectating(GameContext context, ServerPlayerEntity player, DeathSpectator spectator, int direction) {
        List<ServerPlayerEntity> targets = context.getServer().getPlayerManager().getPlayerList().stream()
                .filter(target -> target != player && !target.isRemoved() && target.isAlive())
                .sorted(Comparator.comparing(ServerPlayerEntity::getUuid)).toList();
        int current = -1;
        for (int i = 0; i < targets.size(); i++) if (targets.get(i).getUuid().equals(spectator.target)) current = i;
        if (direction != 0 && !targets.isEmpty()) {
            current = current < 0 ? (direction < 0 ? targets.size() - 1 : 0) : Math.floorMod(current + direction, targets.size());
            spectator.target = targets.get(current).getUuid();
            spectator.jumpPending = true;
            setDeathCamera(player, player);
        } else if (current < 0) {
            spectator.target = null;
            spectator.jumpPending = false;
        }
        boolean countChanged = spectator.targetCount != targets.size();
        spectator.targetCount = targets.size();
        if (countChanged || direction != 0 || tickCounter % DEATH_WAIT_SYNC_INTERVAL_TICKS == 0)
            HunterWildcardPackets.sendDeathSpectate(player, true, "", targets.size());
        if (!spectator.jumpPending || current < 0 || teleportPending(player) || tickCounter < spectator.nextRelocateTick) return;
        ServerPlayerEntity target = targets.get(current);
        setDeathCamera(player, player);
        player.setVelocity(Vec3d.ZERO);
        // Leave room behind the player so the free camera does not start inside their head.
        Vec3d position = target.getPos().subtract(Vec3d.fromPolar(0, target.getYaw()).multiply(1.5)).add(0, 0.5, 0);
        if (player.teleport(target.getServerWorld(), position.x, position.y, position.z,
                Set.<PositionFlag>of(), target.getYaw(), target.getPitch(), true)) {
            spectator.jumpPending = false;
            spectator.nextRelocateTick = tickCounter + 5;
        }
    }

    private void restoreAnchor(GameContext context, ServerPlayerEntity player, DeathSpectator spectator) {
        ServerWorld world = context.getServer().getWorld(spectator.world);
        if (world != null) {
            player.setVelocity(Vec3d.ZERO);
            player.teleport(world, spectator.pos.x, spectator.pos.y, spectator.pos.z, Set.<PositionFlag>of(), spectator.yaw, spectator.pitch, true);
        }
    }

    private void stopSpectating(GameContext context, ServerPlayerEntity player) {
        DeathSpectator spectator = spectators.remove(player.getUuid());
        setDeathCamera(player, player);
        if (spectator != null) restoreAnchor(context, player, spectator);
        HunterWildcardPackets.sendDeathSpectate(player, false, "", 0);
    }

    public void tick(GameContext context, CompassTracker compassTracker) {
        tickCounter++;
        // Run the vanilla respawn entry point after death processing has finished, before iterating waiting entries.
        for (UUID uuid : new ArrayList<>(pendingDeathRespawns)) {
            pendingDeathRespawns.remove(uuid);
            ServerPlayerEntity player = context.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && player.isDead()) {
                player.networkHandler.onClientStatus(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
            }
        }
        for (UUID uuid : new ArrayList<>(pendingRespawnSetup)) {
            pendingRespawnSetup.remove(uuid);
            ServerPlayerEntity player = context.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && player.isAlive()) finishVanillaRespawn(context, player, compassTracker);
        }
        for (var entry : spectators.entrySet()) {
            ServerPlayerEntity player = context.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null && player.isAlive()) updateSpectating(context, player, entry.getValue(), 0);
        }
        Iterator<Map.Entry<UUID, WaitingPlayer>> iterator = waiting.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, WaitingPlayer> entry = iterator.next();
            WaitingPlayer wait = entry.getValue();
            ServerPlayerEntity player = context.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player == null || outPlayers.contains(entry.getKey())) {
                iterator.remove();
                continue;
            }

            wait.remainingTicks--;
            if (wait.remainingTicks > 0) {
                if (wait.holding && !player.isDead()) {
                    if (wait.remainingTicks % DEATH_WAIT_SYNC_INTERVAL_TICKS == 0) {
                        sendDeathWait(player, wait);
                    }
                }
                continue;
            }

            if (player.isDead()) {
                // If vanilla respawn is delayed, onAfterRespawn completes the expired wait.
                wait.remainingTicks = 0;
                continue;
            }

            iterator.remove();
            finishRespawn(context, player, wait, compassTracker);
        }
    }

    private void finishRespawn(GameContext context, ServerPlayerEntity player, WaitingPlayer wait, CompassTracker compassTracker) {
        stopSpectating(context, player);
        ModConfig config = context.getConfig();
        highlightKiller(context, player);
        ServerWorld deathWorld = context.getServer().getWorld(wait.deathWorld);
        if (config.randomRespawnEnabled && deathWorld != null && !BackroomsDimension.isBackrooms(deathWorld)) {
            ServerWorld respawnWorld = returnsToOverworld(wait) ? context.getServer().getOverworld() : deathWorld;
            Vec3d origin = returnsToOverworld(wait) ? player.getPos() : wait.deathPos;
            int minDistance = wait.role == PlayerRole.HUNTER ? config.hunterRespawnDistance : config.runnerRespawnDistance;
            // Runners respawn away from hunters; hunters respawn away from runners so a death is not a free re-engage.
            List<ServerPlayerEntity> avoid = wait.role == PlayerRole.RUNNER ? context.getHunters() : context.getRunners();
            double clearance = wait.role == PlayerRole.RUNNER ? config.runnerRespawnDistance : config.hunterRespawnRunnerClearance;
            BlockPos spot = RespawnSpotFinder.find(respawnWorld, origin, minDistance, minDistance * 2, avoid, clearance, context.getRandom());
            if (spot != null) {
                player.setVelocity(Vec3d.ZERO);
                player.teleport(respawnWorld, spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, Set.<PositionFlag>of(), player.getYaw(), player.getPitch(), true);
            }
        }

        player.changeGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.fallDistance = 0.0D;
        if (context.getTeamManager().isHunter(player)) {
            compassTracker.giveCompass(player);
        }
        HunterWildcardPackets.sendDeathWait(player, false, 0, "", "");
        HunterWildcardPackets.sendObjectiveTo(player);
        player.sendMessage(HunterWildcardText.translatable("msg.respawn.rejoined"), false);
        HunterWildcardPackets.sendHudFeedback(
                context,
                HunterWildcardText.spec("hud.feedback.respawn.title"),
                HunterWildcardText.spec("hud.feedback.respawn.player", PlayerUtil.displayNameSpec(player)),
                HunterWildcardText.spec("hud.feedback.respawn.lives", remainingLivesArg(player, wait.role, config)),
                feedbackStyle(wait.role)
        );
    }

    private void sendDeathWait(ServerPlayerEntity player, WaitingPlayer wait) {
        int seconds = Math.max(0, (wait.remainingTicks + 19) / 20);
        HunterWildcardPackets.sendDeathWait(player, true, seconds, wait.reasonSpec, wait.livesSpec);
    }

    public void remove(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        spectators.remove(uuid);
        pendingDeathRespawns.remove(uuid);
        pendingRespawnSetup.remove(uuid);
        waiting.remove(uuid);
        remainingLives.remove(uuid);
        outPlayers.remove(uuid);
        lastHunterHits.remove(uuid);
        lastKillers.remove(uuid);
    }

    public boolean isOut(ServerPlayerEntity player) {
        return outPlayers.contains(player.getUuid());
    }

    public boolean isWaitingForRespawn(ServerPlayerEntity player) {
        WaitingPlayer wait = waiting.get(player.getUuid());
        return wait != null && wait.remainingTicks > 0 && !outPlayers.contains(player.getUuid());
    }

    public int livesFor(ServerPlayerEntity player) { int n = remainingLives.getOrDefault(player.getUuid(), -1); return n == Integer.MAX_VALUE ? -1 : n; }
    public int waitSeconds(ServerPlayerEntity player) { WaitingPlayer w = waiting.get(player.getUuid()); return w == null ? 0 : Math.max(0, (w.remainingTicks + 19) / 20); }

    public int getRunnerKillCount() {
        return runnerKillCount;
    }

    public void clear() {
        spectators.clear();
        pendingDeathRespawns.clear();
        pendingRespawnSetup.clear();
        waiting.clear();
        remainingLives.clear();
        outPlayers.clear();
        lastHunterHits.clear();
        lastKillers.clear();
        hunterDeaths.clear();
        pendingHunterDeaths = 0;
        runnerKillCount = 0;
        environmentDeaths = 0;
    }

    public void clear(GameContext context) {
        Set<UUID> affectedPlayers = new HashSet<>();
        affectedPlayers.addAll(waiting.keySet());
        affectedPlayers.addAll(outPlayers);

        for (UUID uuid : affectedPlayers) {
            ServerPlayerEntity player = context.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                stopSpectating(context, player);
                HunterWildcardPackets.sendDeathWait(player, false, 0, "", "");
                if (!player.isDead()) {
                    player.changeGameMode(GameMode.SURVIVAL);
                }
            }
        }

        clear();
    }

    /** Pushes the hunter progress line of the objective panel (kills, or runners still standing). */
    public void refreshHunterProgress(GameContext context) {
        ModConfig config = context.getConfig();
        String spec;
        if (config.getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT) {
            spec = config.environmentKillsEnabled && config.environmentDeathsPerKill > 1
                    ? HunterWildcardText.spec("hud.objective.hunter_kills_env", runnerKillCount, config.hunterRunnerKillTarget, environmentDeaths, config.environmentDeathsPerKill)
                    : HunterWildcardText.spec("hud.objective.hunter_kills", runnerKillCount, config.hunterRunnerKillTarget);
        } else {
            List<ServerPlayerEntity> runners = context.getRunners();
            int alive = 0;
            int livesLeft = 0;
            for (ServerPlayerEntity runner : runners) {
                if (!outPlayers.contains(runner.getUuid())) {
                    alive++;
                    livesLeft += remainingLives.getOrDefault(runner.getUuid(), initialLives(PlayerRole.RUNNER, config));
                }
            }
            spec = config.getRunnerRespawnMode() == RespawnMode.LIMITED_LIVES
                    ? HunterWildcardText.spec("hud.objective.runners_alive_lives", alive, runners.size(), livesLeft)
                    : HunterWildcardText.spec("hud.objective.runners_alive", alive, runners.size());
        }
        HunterWildcardPackets.sendHunterProgress(context.getServer(), spec);
    }

    private void scheduleRespawn(GameContext context, ServerPlayerEntity player, PlayerRole role, int respawnTicks, String reasonSpec) {
        ModConfig config = context.getConfig();
        WaitingPlayer wait = new WaitingPlayer(role, player.getWorld().getRegistryKey(), player.getPos(), Math.max(1, respawnTicks));
        wait.reasonSpec = reasonSpec;
        wait.livesSpec = HunterWildcardText.spec("hud.death_wait.lives", remainingLivesArgAfterDeath(player, role, config));
        waiting.put(player.getUuid(), wait);
    }

    private void markOut(ServerPlayerEntity player) {
        waiting.remove(player.getUuid());
        remainingLives.put(player.getUuid(), 0);
        outPlayers.add(player.getUuid());
    }

    private int decrementLife(ServerPlayerEntity player, PlayerRole role, ModConfig config) {
        UUID uuid = player.getUuid();
        int current = remainingLives.computeIfAbsent(uuid, ignored -> initialLives(role, config));
        int remaining = Math.max(0, current - 1);
        remainingLives.put(uuid, remaining);
        return remaining;
    }

    private int initialLives(PlayerRole role, ModConfig config) {
        RespawnMode mode = modeFor(role, config);
        if (mode == RespawnMode.INFINITE) {
            return Integer.MAX_VALUE;
        }
        if (mode == RespawnMode.NO_RESPAWN) {
            return 1;
        }

        int configured = role == PlayerRole.HUNTER ? config.hunterLives : config.runnerLives;
        return Math.max(1, configured);
    }

    private RespawnMode modeFor(PlayerRole role, ModConfig config) {
        return role == PlayerRole.HUNTER ? config.getHunterRespawnMode() : config.getRunnerRespawnMode();
    }

    private int respawnTicksFor(PlayerRole role, ModConfig config) {
        return respawnSecondsFor(role, config) * 20;
    }

    /** Hunters wait longer with every death this round (penalty per death, at most 60 extra seconds). */
    private int respawnSecondsFor(PlayerRole role, ModConfig config) {
        if (role != PlayerRole.HUNTER) {
            return config.runnerRespawnSeconds;
        }
        int penalty = Math.min(MAX_HUNTER_PENALTY_SECONDS, config.hunterRespawnPenaltySeconds * pendingHunterDeaths);
        return config.hunterRespawnSeconds + penalty;
    }

    private String runnerLossReason(GameContext context, ServerPlayerEntity outRunner) {
        RunnerTeamLossMode lossMode = context.getConfig().getRunnerTeamLossMode();
        if (lossMode == RunnerTeamLossMode.ANY_RUNNER_OUT) {
            return HunterWildcardText.spec("msg.win.hunter.runner_out", PlayerUtil.displayNameSpec(outRunner));
        }

        for (ServerPlayerEntity runner : context.getRunners()) {
            if (!outPlayers.contains(runner.getUuid())) {
                return null;
            }
        }

        return HunterWildcardText.spec("msg.win.hunter.all_runners_out");
    }

    private Text deathPrefix(ServerPlayerEntity player, PlayerRole role, ServerPlayerEntity creditedHunter, boolean assisted, KillCredit credit, ModConfig config) {
        Text playerName = PlayerUtil.displayNameText(player);
        if (role != PlayerRole.RUNNER) {
            return HunterWildcardText.translatable("msg.death.player", role.getDisplayText(), playerName);
        }

        if (creditedHunter != null) {
            Text hunterName = PlayerUtil.displayNameText(creditedHunter);
            return assisted
                    ? HunterWildcardText.translatable("msg.death.runner_killed_assist", playerName, hunterName)
                    : HunterWildcardText.translatable("msg.death.runner_killed", playerName, hunterName);
        }

        if (config.getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT && credit.environment()) {
            if (credit.counted()) {
                return HunterWildcardText.translatable("msg.death.runner_env_counted", playerName);
            }
            return HunterWildcardText.translatable("msg.death.runner_env_progress", playerName, credit.environmentProgress(), config.environmentDeathsPerKill);
        }

        return HunterWildcardText.translatable("msg.death.runner_died", playerName);
    }

    private String feedbackStyle(PlayerRole role) {
        if (role == PlayerRole.HUNTER) {
            return "hunter";
        }
        if (role == PlayerRole.RUNNER) {
            return "respawn";
        }
        return "neutral";
    }

    private void announceKillCountMilestone(GameContext context, int remainingKills) {
        if (remainingKills != 5 && remainingKills != 3 && remainingKills != 2 && remainingKills != 1) {
            return;
        }

        Text message = HunterWildcardText.prefixed(HunterWildcardText.translatable("msg.hunter.kill_target_remaining", remainingKills)).formatted(Formatting.GOLD);
        for (ServerPlayerEntity participant : context.getParticipants()) {
            participant.sendMessage(message);
        }
    }

    private String remainingLivesArg(ServerPlayerEntity player, PlayerRole role, ModConfig config) {
        if (role == null) {
            return HunterWildcardText.key("common.unknown");
        }

        RespawnMode mode = modeFor(role, config);
        if (mode == RespawnMode.INFINITE) {
            return HunterWildcardText.key("common.infinite");
        }

        if (mode == RespawnMode.NO_RESPAWN) {
            return "0";
        }

        return Integer.toString(remainingLives.getOrDefault(player.getUuid(), initialLives(role, config)));
    }

    private String remainingLivesArgAfterDeath(ServerPlayerEntity player, PlayerRole role, ModConfig config) {
        return remainingLivesArg(player, role, config);
    }

    private static final int KILLER_HIGHLIGHT_TICKS = 200;

    private record HunterHit(UUID hunterId, int tick) {
    }

    /** counted: this death added a hunter kill; environment: no hunter involved; environmentProgress: env deaths so far towards the next kill. */
    private record KillCredit(boolean counted, boolean environment, int environmentProgress) {
        private static final KillCredit NONE = new KillCredit(false, false, 0);
    }

    private static final class WaitingPlayer {
        private final PlayerRole role;
        private final RegistryKey<World> deathWorld;
        private final Vec3d deathPos;
        private int remainingTicks;
        private boolean holding;
        private RegistryKey<World> holdWorld;
        private Vec3d holdPos;
        private String reasonSpec = "";
        private String livesSpec = "";

        private WaitingPlayer(PlayerRole role, RegistryKey<World> deathWorld, Vec3d deathPos, int remainingTicks) {
            this.role = role;
            this.deathWorld = deathWorld;
            this.deathPos = deathPos;
            this.remainingTicks = remainingTicks;
        }
    }

    private static final class DeathSpectator {
        final PlayerRole role;
        final RegistryKey<World> world;
        final Vec3d pos;
        final float yaw, pitch;
        UUID target;
        int targetCount = -1;
        int cameraSyncTicks;
        UUID positionedTarget;
        int attachAfterTick, nextRelocateTick;
        boolean jumpPending;

        DeathSpectator(PlayerRole role, RegistryKey<World> world, Vec3d pos, float yaw, float pitch) {
            this.role = role; this.world = world; this.pos = pos; this.yaw = yaw; this.pitch = pitch;
        }
    }

    public record DeathOutcome(Text message, String endingReason) {
        public static DeathOutcome none() {
            return new DeathOutcome(null, null);
        }

        public static DeathOutcome message(Text message) {
            return new DeathOutcome(message, null);
        }

        public static DeathOutcome end(String endingReason) {
            return new DeathOutcome(null, endingReason);
        }

        public static DeathOutcome messageAndEnd(Text message, String endingReason) {
            return new DeathOutcome(message, endingReason);
        }
    }
}
