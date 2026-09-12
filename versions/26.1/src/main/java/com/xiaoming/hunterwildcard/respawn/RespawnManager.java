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
import com.xiaoming.hunterwildcard.mixin.TeleportPendingAccessor;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
        for (ServerPlayer hunter : context.getHunters()) {
            remainingLives.put(hunter.getUUID(), initialLives(PlayerRole.HUNTER, context.getConfig()));
        }
        for (ServerPlayer runner : context.getRunners()) {
            remainingLives.put(runner.getUUID(), initialLives(PlayerRole.RUNNER, context.getConfig()));
        }
        refreshHunterProgress(context);
    }

    /** Remembers the last hunter who damaged a runner so later deaths can still be credited. */
    public void recordHunterHit(ServerPlayer runner, ServerPlayer hunter) {
        lastHunterHits.put(runner.getUUID(), new HunterHit(hunter.getUUID(), tickCounter));
    }

    public DeathOutcome onPlayerDeath(GameContext context, ServerPlayer player, PlayerRole role, ServerPlayer directHunterKiller) {
        if (role == null) {
            return DeathOutcome.none();
        }
        pendingDeathRespawns.add(player.getUUID());

        ModConfig config = context.getConfig();
        // Previous deaths of this hunter drive the escalating respawn wait computed below.
        pendingHunterDeaths = role == PlayerRole.HUNTER ? hunterDeaths.merge(player.getUUID(), 1, Integer::sum) - 1 : 0;
        ServerPlayer creditedHunter = directHunterKiller;
        boolean assisted = false;
        HunterHit lastHit = lastHunterHits.remove(player.getUUID());
        if (creditedHunter == null && role == PlayerRole.RUNNER && lastHit != null
                && tickCounter - lastHit.tick() <= config.getHunterHitCreditTicks()) {
            ServerPlayer hunter = context.getServer().getPlayerList().getPlayer(lastHit.hunterId());
            if (hunter != null && context.getTeamManager().isHunter(hunter)) {
                creditedHunter = hunter;
                assisted = true;
            }
        }

        KillCredit credit = KillCredit.NONE;
        if (role == PlayerRole.RUNNER) {
            credit = registerRunnerDeath(config, creditedHunter);
        }
        Component deathPrefix = deathPrefix(player, role, creditedHunter, assisted, credit, config);
        String reasonSpec = creditedHunter != null
                ? HunterWildcardText.spec("hud.death_wait.killed_by", PlayerUtil.displayNameSpec(creditedHunter))
                : HunterWildcardText.spec("hud.death_wait.died");
        String endingReason = null;

        String killerSpec = creditedHunter != null ? PlayerUtil.displayNameSpec(creditedHunter) : HunterWildcardText.key("common.environment");
        if (creditedHunter != null) {
            lastKillers.put(player.getUUID(), creditedHunter.getUUID());
        } else {
            lastKillers.remove(player.getUUID());
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
            Component message = HunterWildcardText.translatable("msg.death.respawn_scheduled", deathPrefix, respawnSecondsFor(role, config));
            return endingReason != null ? DeathOutcome.messageAndEnd(message, endingReason) : DeathOutcome.message(message);
        }

        int livesAfterDeath = mode == RespawnMode.NO_RESPAWN ? 0 : decrementLife(player, role, config);
        if (livesAfterDeath > 0) {
            scheduleRespawn(context, player, role, respawnTicksFor(role, config), reasonSpec);
            refreshHunterProgress(context);
            sendEliminationFeedback(context, player, role, killerSpec, creditedHunter != null, Integer.toString(livesAfterDeath), false);
            Component message = HunterWildcardText.translatable("msg.death.limited_respawn_scheduled", deathPrefix, livesAfterDeath, respawnSecondsFor(role, config));
            return endingReason != null ? DeathOutcome.messageAndEnd(message, endingReason) : DeathOutcome.message(message);
        }

        markOut(player);
        refreshHunterProgress(context);
        sendEliminationFeedback(context, player, role, killerSpec, creditedHunter != null, "0", true);
        Component outMessage = HunterWildcardText.translatable("msg.death.out", deathPrefix);
        if (endingReason == null && role == PlayerRole.RUNNER) {
            endingReason = runnerLossReason(context, player);
        }
        return endingReason != null ? DeathOutcome.messageAndEnd(outMessage, endingReason) : DeathOutcome.message(outMessage);
    }

    /**
     * Top-right card for runner deaths outside kill-count mode (kill-count mode has its own card with
     * the target progress): who got whom, and lives left or "out".
     */
    private void sendEliminationFeedback(GameContext context, ServerPlayer player, PlayerRole role, String killerSpec,
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
    private KillCredit registerRunnerDeath(ModConfig config, ServerPlayer creditedHunter) {
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

    public void onAfterRespawn(GameContext context, ServerPlayer player, CompassTracker compassTracker) {
        pendingDeathRespawns.remove(player.getUUID());
        if (outPlayers.contains(player.getUUID()) || waiting.containsKey(player.getUUID())) {
            // Fabric fires before the network handler replaces its old player. Teleport only after that replacement.
            pendingRespawnSetup.add(player.getUUID());
            return;
        }
        finishVanillaRespawn(context, player, compassTracker);
    }

    private void finishVanillaRespawn(GameContext context, ServerPlayer player, CompassTracker compassTracker) {
        if (outPlayers.contains(player.getUUID())) {
            beginSpectating(context, player, player.level().dimension(), player.position());
            player.sendSystemMessage(HunterWildcardText.translatable("msg.respawn.out_spectating"), false);
            return;
        }

        WaitingPlayer wait = waiting.get(player.getUUID());
        if (wait != null) {
            if (returnsToOverworld(wait)) returnToOverworld(context, player);
            if (wait.remainingTicks <= 0) {
                waiting.remove(player.getUUID());
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
    private void highlightKiller(GameContext context, ServerPlayer player) {
        UUID killerId = lastKillers.remove(player.getUUID());
        if (killerId == null || !context.getTeamManager().isRunner(player)) {
            return;
        }
        ServerPlayer killer = context.getServer().getPlayerList().getPlayer(killerId);
        if (killer == null || !killer.isAlive() || !context.getTeamManager().isHunter(killer)) {
            return;
        }
        killer.addEffect(new MobEffectInstance(MobEffects.GLOWING, KILLER_HIGHLIGHT_TICKS, 0, false, false, false));
        player.sendSystemMessage(HunterWildcardText.translatable("msg.respawn.killer_highlighted", PlayerUtil.displayNameText(killer), KILLER_HIGHLIGHT_TICKS / 20).withStyle(ChatFormatting.RED), false);
    }

    private static boolean returnsToOverworld(WaitingPlayer wait) {
        return Level.NETHER.equals(wait.deathWorld) || Level.END.equals(wait.deathWorld);
    }

    /** Keep a valid vanilla Overworld respawn; dimension-specific anchors fall back to world spawn. */
    private void returnToOverworld(GameContext context, ServerPlayer player) {
        ServerLevel overworld = context.getServer().overworld();
        if (player.level() == overworld) return;
        var spawn = overworld.getRespawnData();
        BlockPos pos = player.adjustSpawnLocation(overworld, spawn.pos());
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(overworld, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                Set.<Relative>of(), spawn.yaw(), spawn.pitch(), true);
    }

    /** Save the respawn destination independently of the teammate camera. */
    private void beginHolding(GameContext context, ServerPlayer player, WaitingPlayer wait) {
        boolean relocate = context.getConfig().randomRespawnEnabled;
        ServerLevel deathWorld = context.getServer().getLevel(wait.deathWorld);
        if (relocate && !returnsToOverworld(wait) && deathWorld != null && !BackroomsDimension.isBackrooms(deathWorld)) {
            wait.holdWorld = deathWorld.dimension();
            wait.holdPos = wait.deathPos;
        } else {
            wait.holdWorld = player.level().dimension();
            wait.holdPos = player.position();
        }
        wait.holding = true;
        beginSpectating(context, player, wait.holdWorld, wait.holdPos);
        sendDeathWait(player, wait);
    }

    private void beginSpectating(GameContext context, ServerPlayer player, ResourceKey<Level> world, Vec3 pos) {
        DeathSpectator spectator = new DeathSpectator(context.getTeamManager().getRole(player), world, pos, player.getYRot(), player.getXRot());
        spectators.put(player.getUUID(), spectator);
        player.setGameMode(GameType.SPECTATOR);
        restoreAnchor(context, player, spectator);
        updateSpectating(context, player, spectator, 0);
    }

    /** Only this player's server-validated teammate list can be cycled; no client-supplied entity IDs. */
    public void cycleSpectating(GameContext context, ServerPlayer player, boolean previous) {
        DeathSpectator spectator = spectators.get(player.getUUID());
        if (spectator == null || !player.isSpectator() || player.isDeadOrDying()) return;
        updateSpectating(context, player, spectator, previous ? -1 : 1);
    }

    /** Runs before vanilla copies camera coordinates into the spectator's current world. */
    public void beforeSpectatorTick(ServerPlayer player) {
        if (!spectators.containsKey(player.getUUID())) return;
        Entity camera = player.getCamera();
        if (camera != player && (camera.isRemoved() || !camera.isAlive() || player.isShiftKeyDown()
                || camera.level() != player.level()
                || camera.position().distanceToSqr(player.position()) > CAMERA_RELOCATE_DISTANCE_SQUARED)) {
            setDeathCamera(player, player);
        }
    }

    /** Prevent vanilla teleportSpectatingPlayers from racing our pending camera transition. */
    public void beforeTargetTeleport(ServerPlayer target) {
        for (var entry : spectators.entrySet()) {
            var viewer = target.level().getServer().getPlayerList().getPlayer(entry.getKey());
            if (viewer != null && viewer.getCamera() == target) {
                setDeathCamera(viewer, viewer);
                entry.getValue().positionedTarget = null;
            }
        }
    }

    private static void setDeathCamera(ServerPlayer player, Entity target) {
        if (player.getCamera() == target) return;
        ((DeathCameraAccess) player).hunterwildcard$setDeathCamera(target);
        player.connection.send(new ClientboundSetCameraPacket(target));
        player.connection.resetPosition();
    }

    private static boolean teleportPending(ServerPlayer player) {
        return ((TeleportPendingAccessor) player.connection).hunterwildcard$pendingTeleport() != null;
    }

    private void updateSpectating(GameContext context, ServerPlayer player, DeathSpectator spectator, int direction) {
        if (outPlayers.contains(player.getUUID())) {
            updateFreeSpectating(context, player, spectator, direction);
            return;
        }
        List<ServerPlayer> targets = context.getParticipants().stream()
                .filter(target -> target != player && !target.isRemoved() && target.isAlive() && !target.isSpectator()
                        && spectator.role != null && context.getTeamManager().getRole(target) == spectator.role
                        && !waiting.containsKey(target.getUUID()) && !outPlayers.contains(target.getUUID()))
                .sorted(Comparator.comparing(ServerPlayer::getUUID)).toList();
        int current = -1;
        for (int i = 0; i < targets.size(); i++) if (targets.get(i).getUUID().equals(spectator.target)) current = i;
        ServerPlayer target = targets.isEmpty() ? null : targets.get(current < 0
                ? (direction < 0 ? targets.size() - 1 : 0) : Math.floorMod(current + direction, targets.size()));
        UUID targetId = target == null ? null : target.getUUID();
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
        if (target == null || player.isShiftKeyDown()) {
            setDeathCamera(player, player);
            return;
        }
        beforeSpectatorTick(player);
        // Never supersede an outstanding position confirmation with another camera teleport.
        // Further cycling updates the desired UUID above and is coalesced until the client catches up.
        if (teleportPending(player)) return;
        if (player.getCamera() != target) {
            if (!targetId.equals(spectator.positionedTarget) || player.level() != target.level()
                    || player.position().distanceToSqr(target.position()) > CAMERA_RELOCATE_DISTANCE_SQUARED) {
                if (tickCounter < spectator.nextRelocateTick) return;
                setDeathCamera(player, player);
                player.setDeltaMovement(Vec3.ZERO);
                if (player.teleportTo(target.level(), target.getX(), target.getY(), target.getZ(),
                        Set.<Relative>of(), target.getYRot(), target.getXRot(), true)) {
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
            player.connection.send(new ClientboundSetCameraPacket(target));
    }

    /** Eliminated players can jump to anyone, but neither the camera nor their position keeps following. */
    private void updateFreeSpectating(GameContext context, ServerPlayer player, DeathSpectator spectator, int direction) {
        List<ServerPlayer> targets = context.getServer().getPlayerList().getPlayers().stream()
                .filter(target -> target != player && !target.isRemoved() && target.isAlive())
                .sorted(Comparator.comparing(ServerPlayer::getUUID)).toList();
        int current = -1;
        for (int i = 0; i < targets.size(); i++) if (targets.get(i).getUUID().equals(spectator.target)) current = i;
        if (direction != 0 && !targets.isEmpty()) {
            current = current < 0 ? (direction < 0 ? targets.size() - 1 : 0) : Math.floorMod(current + direction, targets.size());
            spectator.target = targets.get(current).getUUID();
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
        ServerPlayer target = targets.get(current);
        setDeathCamera(player, player);
        player.setDeltaMovement(Vec3.ZERO);
        // Leave room behind the player so the free camera does not start inside their head.
        Vec3 position = target.position().subtract(Vec3.directionFromRotation(0, target.getYRot()).scale(1.5)).add(0, 0.5, 0);
        if (player.teleportTo(target.level(), position.x, position.y, position.z,
                Set.<Relative>of(), target.getYRot(), target.getXRot(), true)) {
            spectator.jumpPending = false;
            spectator.nextRelocateTick = tickCounter + 5;
        }
    }

    private void restoreAnchor(GameContext context, ServerPlayer player, DeathSpectator spectator) {
        ServerLevel world = context.getServer().getLevel(spectator.world);
        if (world != null) {
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(world, spectator.pos.x, spectator.pos.y, spectator.pos.z, Set.<Relative>of(), spectator.yaw, spectator.pitch, true);
        }
    }

    private void stopSpectating(GameContext context, ServerPlayer player) {
        DeathSpectator spectator = spectators.remove(player.getUUID());
        setDeathCamera(player, player);
        if (spectator != null) restoreAnchor(context, player, spectator);
        HunterWildcardPackets.sendDeathSpectate(player, false, "", 0);
    }

    public void tick(GameContext context, CompassTracker compassTracker) {
        tickCounter++;
        // Run the vanilla respawn entry point after death processing has finished, before iterating waiting entries.
        for (UUID uuid : new ArrayList<>(pendingDeathRespawns)) {
            pendingDeathRespawns.remove(uuid);
            ServerPlayer player = context.getServer().getPlayerList().getPlayer(uuid);
            if (player != null && player.isDeadOrDying()) {
                player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            }
        }
        for (UUID uuid : new ArrayList<>(pendingRespawnSetup)) {
            pendingRespawnSetup.remove(uuid);
            ServerPlayer player = context.getServer().getPlayerList().getPlayer(uuid);
            if (player != null && player.isAlive()) finishVanillaRespawn(context, player, compassTracker);
        }
        for (var entry : spectators.entrySet()) {
            ServerPlayer player = context.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.isAlive()) updateSpectating(context, player, entry.getValue(), 0);
        }
        Iterator<Map.Entry<UUID, WaitingPlayer>> iterator = waiting.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, WaitingPlayer> entry = iterator.next();
            WaitingPlayer wait = entry.getValue();
            ServerPlayer player = context.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || outPlayers.contains(entry.getKey())) {
                iterator.remove();
                continue;
            }

            wait.remainingTicks--;
            if (wait.remainingTicks > 0) {
                if (wait.holding && !player.isDeadOrDying()) {
                    if (wait.remainingTicks % DEATH_WAIT_SYNC_INTERVAL_TICKS == 0) {
                        sendDeathWait(player, wait);
                    }
                }
                continue;
            }

            if (player.isDeadOrDying()) {
                // If vanilla respawn is delayed, onAfterRespawn completes the expired wait.
                wait.remainingTicks = 0;
                continue;
            }

            iterator.remove();
            finishRespawn(context, player, wait, compassTracker);
        }
    }

    private void finishRespawn(GameContext context, ServerPlayer player, WaitingPlayer wait, CompassTracker compassTracker) {
        stopSpectating(context, player);
        ModConfig config = context.getConfig();
        highlightKiller(context, player);
        ServerLevel deathWorld = context.getServer().getLevel(wait.deathWorld);
        if (config.randomRespawnEnabled && deathWorld != null && !BackroomsDimension.isBackrooms(deathWorld)) {
            ServerLevel respawnWorld = returnsToOverworld(wait) ? context.getServer().overworld() : deathWorld;
            Vec3 origin = returnsToOverworld(wait) ? player.position() : wait.deathPos;
            int minDistance = wait.role == PlayerRole.HUNTER ? config.hunterRespawnDistance : config.runnerRespawnDistance;
            // Runners respawn away from hunters; hunters respawn away from runners so a death is not a free re-engage.
            List<ServerPlayer> avoid = wait.role == PlayerRole.RUNNER ? context.getHunters() : context.getRunners();
            double clearance = wait.role == PlayerRole.RUNNER ? config.runnerRespawnDistance : config.hunterRespawnRunnerClearance;
            BlockPos spot = RespawnSpotFinder.find(respawnWorld, origin, minDistance, minDistance * 2, avoid, clearance, context.getRandom());
            if (spot != null) {
                player.setDeltaMovement(Vec3.ZERO);
                player.teleportTo(respawnWorld, spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
            }
        }

        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.fallDistance = 0.0D;
        if (context.getTeamManager().isHunter(player)) {
            compassTracker.giveCompass(player);
        }
        HunterWildcardPackets.sendDeathWait(player, false, 0, "", "");
        HunterWildcardPackets.sendObjectiveTo(player);
        player.sendSystemMessage(HunterWildcardText.translatable("msg.respawn.rejoined"), false);
        HunterWildcardPackets.sendHudFeedback(
                context,
                HunterWildcardText.spec("hud.feedback.respawn.title"),
                HunterWildcardText.spec("hud.feedback.respawn.player", PlayerUtil.displayNameSpec(player)),
                HunterWildcardText.spec("hud.feedback.respawn.lives", remainingLivesArg(player, wait.role, config)),
                feedbackStyle(wait.role)
        );
    }

    private void sendDeathWait(ServerPlayer player, WaitingPlayer wait) {
        int seconds = Math.max(0, (wait.remainingTicks + 19) / 20);
        HunterWildcardPackets.sendDeathWait(player, true, seconds, wait.reasonSpec, wait.livesSpec);
    }

    public void remove(ServerPlayer player) {
        UUID uuid = player.getUUID();
        spectators.remove(uuid);
        pendingDeathRespawns.remove(uuid);
        pendingRespawnSetup.remove(uuid);
        waiting.remove(uuid);
        remainingLives.remove(uuid);
        outPlayers.remove(uuid);
        lastHunterHits.remove(uuid);
        lastKillers.remove(uuid);
    }

    public boolean isOut(ServerPlayer player) {
        return outPlayers.contains(player.getUUID());
    }

    public boolean isWaitingForRespawn(ServerPlayer player) {
        WaitingPlayer wait = waiting.get(player.getUUID());
        return wait != null && wait.remainingTicks > 0 && !outPlayers.contains(player.getUUID());
    }

    public int livesFor(ServerPlayer player) { int n = remainingLives.getOrDefault(player.getUUID(), -1); return n == Integer.MAX_VALUE ? -1 : n; }
    public int waitSeconds(ServerPlayer player) { WaitingPlayer w = waiting.get(player.getUUID()); return w == null ? 0 : Math.max(0, (w.remainingTicks + 19) / 20); }

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
            ServerPlayer player = context.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                stopSpectating(context, player);
                HunterWildcardPackets.sendDeathWait(player, false, 0, "", "");
                if (!player.isDeadOrDying()) {
                    player.setGameMode(GameType.SURVIVAL);
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
            List<ServerPlayer> runners = context.getRunners();
            int alive = 0;
            int livesLeft = 0;
            for (ServerPlayer runner : runners) {
                if (!outPlayers.contains(runner.getUUID())) {
                    alive++;
                    livesLeft += remainingLives.getOrDefault(runner.getUUID(), initialLives(PlayerRole.RUNNER, config));
                }
            }
            spec = config.getRunnerRespawnMode() == RespawnMode.LIMITED_LIVES
                    ? HunterWildcardText.spec("hud.objective.runners_alive_lives", alive, runners.size(), livesLeft)
                    : HunterWildcardText.spec("hud.objective.runners_alive", alive, runners.size());
        }
        HunterWildcardPackets.sendHunterProgress(context.getServer(), spec);
    }

    private void scheduleRespawn(GameContext context, ServerPlayer player, PlayerRole role, int respawnTicks, String reasonSpec) {
        ModConfig config = context.getConfig();
        WaitingPlayer wait = new WaitingPlayer(role, player.level().dimension(), player.position(), Math.max(1, respawnTicks));
        wait.reasonSpec = reasonSpec;
        wait.livesSpec = HunterWildcardText.spec("hud.death_wait.lives", remainingLivesArgAfterDeath(player, role, config));
        waiting.put(player.getUUID(), wait);
    }

    private void markOut(ServerPlayer player) {
        waiting.remove(player.getUUID());
        remainingLives.put(player.getUUID(), 0);
        outPlayers.add(player.getUUID());
    }

    private int decrementLife(ServerPlayer player, PlayerRole role, ModConfig config) {
        UUID uuid = player.getUUID();
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

    private String runnerLossReason(GameContext context, ServerPlayer outRunner) {
        RunnerTeamLossMode lossMode = context.getConfig().getRunnerTeamLossMode();
        if (lossMode == RunnerTeamLossMode.ANY_RUNNER_OUT) {
            return HunterWildcardText.spec("msg.win.hunter.runner_out", PlayerUtil.displayNameSpec(outRunner));
        }

        for (ServerPlayer runner : context.getRunners()) {
            if (!outPlayers.contains(runner.getUUID())) {
                return null;
            }
        }

        return HunterWildcardText.spec("msg.win.hunter.all_runners_out");
    }

    private Component deathPrefix(ServerPlayer player, PlayerRole role, ServerPlayer creditedHunter, boolean assisted, KillCredit credit, ModConfig config) {
        Component playerName = PlayerUtil.displayNameText(player);
        if (role != PlayerRole.RUNNER) {
            return HunterWildcardText.translatable("msg.death.player", role.getDisplayText(), playerName);
        }

        if (creditedHunter != null) {
            Component hunterName = PlayerUtil.displayNameText(creditedHunter);
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

        Component message = HunterWildcardText.prefixed(HunterWildcardText.translatable("msg.hunter.kill_target_remaining", remainingKills)).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer participant : context.getParticipants()) {
            participant.sendSystemMessage(message);
        }
    }

    private String remainingLivesArg(ServerPlayer player, PlayerRole role, ModConfig config) {
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

        return Integer.toString(remainingLives.getOrDefault(player.getUUID(), initialLives(role, config)));
    }

    private String remainingLivesArgAfterDeath(ServerPlayer player, PlayerRole role, ModConfig config) {
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
        private final ResourceKey<Level> deathWorld;
        private final Vec3 deathPos;
        private int remainingTicks;
        private boolean holding;
        private ResourceKey<Level> holdWorld;
        private Vec3 holdPos;
        private String reasonSpec = "";
        private String livesSpec = "";

        private WaitingPlayer(PlayerRole role, ResourceKey<Level> deathWorld, Vec3 deathPos, int remainingTicks) {
            this.role = role;
            this.deathWorld = deathWorld;
            this.deathPos = deathPos;
            this.remainingTicks = remainingTicks;
        }
    }

    private static final class DeathSpectator {
        final PlayerRole role;
        final ResourceKey<Level> world;
        final Vec3 pos;
        final float yaw, pitch;
        UUID target;
        int targetCount = -1;
        int cameraSyncTicks;
        UUID positionedTarget;
        int attachAfterTick, nextRelocateTick;
        boolean jumpPending;

        DeathSpectator(PlayerRole role, ResourceKey<Level> world, Vec3 pos, float yaw, float pitch) {
            this.role = role; this.world = world; this.pos = pos; this.yaw = yaw; this.pitch = pitch;
        }
    }

    public record DeathOutcome(Component message, String endingReason) {
        public static DeathOutcome none() {
            return new DeathOutcome(null, null);
        }

        public static DeathOutcome message(Component message) {
            return new DeathOutcome(message, null);
        }

        public static DeathOutcome end(String endingReason) {
            return new DeathOutcome(null, endingReason);
        }

        public static DeathOutcome messageAndEnd(Component message, String endingReason) {
            return new DeathOutcome(message, endingReason);
        }
    }
}
