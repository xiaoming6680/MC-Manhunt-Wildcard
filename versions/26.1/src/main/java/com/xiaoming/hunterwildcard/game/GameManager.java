package com.xiaoming.hunterwildcard.game;

import com.xiaoming.hunterwildcard.backrooms.BackroomsDimension;
import com.xiaoming.hunterwildcard.backrooms.BackroomsSession;
import com.xiaoming.hunterwildcard.compass.CompassTracker;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.prepare.HunterBoundaryManager;
import com.xiaoming.hunterwildcard.respawn.RespawnManager;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.team.TeamManager;
import com.xiaoming.hunterwildcard.ui.BossBarManager;
import com.xiaoming.hunterwildcard.ui.MessageManager;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.util.PlayerUtil;
import com.xiaoming.hunterwildcard.wildcard.WildcardManager;
import com.xiaoming.hunterwildcard.wildcard.rules.FragileRule;
import com.xiaoming.hunterwildcard.wildcard.rules.TinyPlayersRule;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class GameManager {
    private static final GameManager INSTANCE = new GameManager();
    private static final int LOBBY_GLOW_REFRESH_TICKS = 20;
    private int lobbyGlowTicks;
    private static final int STATUS_SYNC_INTERVAL_TICKS = 20;
    private static final int ROLE_ACTIONBAR_TICKS = 20 * 15;

    private final ModConfig config = ModConfig.load();
    private final TeamManager teamManager = new TeamManager();
    private final BossBarManager bossBarManager = new BossBarManager();
    private final MessageManager messageManager = new MessageManager();
    private final CompassTracker compassTracker = new CompassTracker();
    private final RespawnManager respawnManager = new RespawnManager();
    private final HunterBoundaryManager hunterBoundaryManager = new HunterBoundaryManager();
    private final WinConditionManager winConditionManager = new WinConditionManager();
    private final RoleModifierManager roleModifierManager = new RoleModifierManager();
    private final WildcardManager wildcardManager = new WildcardManager(bossBarManager, messageManager);
    private final Random random = new Random();
    private final Set<UUID> debugMenuPlayers = new HashSet<>();
    private final Map<UUID, List<ItemStack>> preservedDeathInventories = new HashMap<>();

    private GameState state = GameState.WAITING;
    private MinecraftServer server;
    private int preparingTicks;
    private int endingTicks;
    private int actionBarTicks;
    private int runningTicks;
    private int statusSyncTicks;
    private boolean eventsRegistered;
    private UUID wildcardTestPlayerUuid;
    private Boolean previousLocatorBarRule;
    private Boolean previousKeepInventoryRule;
    /** Players without a side who were put into spectator mode for the round, with the mode to restore. */
    private final Map<UUID, GameType> forcedSpectators = new HashMap<>();
    private int ruleEnforceTicks;

    private GameManager() {
    }

    private String lastWinner = "", lastReason = "";
    private List<HunterWildcardPackets.MemberEntry> lastMembers = List.of();
    private List<HunterWildcardPackets.MemberEntry> roster(ServerPlayer viewer) {
        List<HunterWildcardPackets.MemberEntry> list = new java.util.ArrayList<>();
        MinecraftServer rosterServer = viewer == null ? server : viewer.level().getServer();
        if (rosterServer == null) return list;
        for (ServerPlayer p : teamManager.getParticipants(rosterServer)) {
            PlayerRole role = teamManager.getRole(p);
            boolean reveal = viewer == null || p == viewer || teamManager.getRole(viewer) == role || state == GameState.WAITING || state == GameState.ENDING;
            String status = !reveal ? "ui.member.hidden" : state == GameState.WAITING ? "state.waiting" : respawnManager.isOut(p) ? "ui.member.out" : respawnManager.isWaitingForRespawn(p) ? "ui.member.respawning" : "ui.member.alive";
            list.add(new HunterWildcardPackets.MemberEntry(PlayerUtil.displayNameSpec(p), role.getTranslationKey(), HunterWildcardText.key(status), reveal ? respawnManager.livesFor(p) : -2, reveal ? respawnManager.waitSeconds(p) : 0));
        }
        return List.copyOf(list);
    }
    public HunterWildcardPackets.RoundDetailsPayload roundDetails(ServerPlayer viewer, String objective, String hunterObjective) {
        String own = teamManager.getRole(viewer)==null ? "ui.member.spectator" : respawnManager.isOut(viewer) ? "ui.member.out" : respawnManager.isWaitingForRespawn(viewer) ? "ui.member.respawning" : "ui.member.alive";
        boolean anonymous = com.xiaoming.hunterwildcard.wildcard.rules.WhoAreYouRule.isActive();
        return new HunterWildcardPackets.RoundDetailsPayload(roster(viewer), anonymous ? "" : lastWinner, anonymous ? "" : lastReason,
                anonymous ? List.of() : lastMembers, objective, hunterObjective, HunterWildcardText.key(own), teamManager.getRole(viewer)==null ? -2 : respawnManager.livesFor(viewer));
    }

    public static GameManager getInstance() {
        return INSTANCE;
    }

    public void registerEvents() {
        if (eventsRegistered) {
            return;
        }
        eventsRegistered = true;

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> handleDisconnect(handler.player));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, joinedServer) -> handleJoin(handler.player, joinedServer));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                handlePlayerDeath(player, damageSource);
            } else {
                handleEntityKilled(entity, damageSource);
            }

            if (entity instanceof EnderDragon) {
                handleDragonDeath();
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, damageSource, baseDamageTaken, damageTaken, blocked) -> {
            if (entity instanceof ServerPlayer player && damageTaken > 0.0F && !blocked) {
                handlePlayerDamaged(player, damageSource, damageTaken);
            }
            if (damageTaken > 0.0F && !blocked && damageSource.getEntity() instanceof ServerPlayer attacker && attacker != entity) {
                handleDamageDealt(attacker, entity, damageTaken);
            }
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                handlePlayerAttack(serverPlayer, entity);
            }
            return InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                handleItemUse(serverPlayer, hand, player.getItemInHand(hand));
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerLevel serverWorld && player instanceof ServerPlayer serverPlayer) {
                handleBlockBroken(serverPlayer, serverWorld, pos, state);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> handleAfterRespawn(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPING.register(this::handleServerStopping);
    }

    public void start(CommandSourceStack source) {
        if (state != GameState.WAITING) {
            source.sendFailure(HunterWildcardText.translatable("msg.game.already_started_or_ending"));
            return;
        }

        if (teamManager.count(PlayerRole.HUNTER) == 0 || teamManager.count(PlayerRole.RUNNER) == 0) {
            source.sendFailure(HunterWildcardText.translatable("msg.game.need_teams"));
            return;
        }

        server = source.getServer();
        if (wildcardManager.hasRuleInProgress()) {
            wildcardManager.clear(debugContext(server));
            wildcardTestPlayerUuid = null;
        }
        clearLobbyGlow(server);
        lastWinner = lastReason = ""; lastMembers = List.of();
        state = GameState.PREPARING;
        resetAdvancements(server);
        preparingTicks = config.getPreparingTicks();
        endingTicks = 0;
        actionBarTicks = 0;
        ruleEnforceTicks = 0;
        teamManager.syncScoreboardTeams(server);
        applyLocatorBarForRound(server);
        takeOverKeepInventory(server);
        forceSpectatorsForRound(server);
        wildcardManager.reset();
        respawnManager.clear();
        winConditionManager.clear();
        compassTracker.reset();
        hunterBoundaryManager.start(context());
        bossBarManager.updatePrepareBar(context(), preparingTicks, config.getPreparingTicks());

        HunterWildcardPackets.clearChat(server);
        messageManager.broadcast(server, HunterWildcardText.translatable("msg.game.preparing_started", config.preparingSeconds));
        source.sendSuccess(() -> HunterWildcardText.translatable("command.start.preparing"), true);
    }

    private void resetAdvancements(MinecraftServer targetServer) {
        // Include spectators and unfinished criteria, not just completed advancements or team members.
        for (ServerPlayer player : targetServer.getPlayerList().getPlayers()) {
            var tracker = player.getAdvancements();
            for (var advancement : targetServer.getAdvancements().getAllAdvancements()) {
                List<String> obtained = new ArrayList<>();
                tracker.getOrStartProgress(advancement).getCompletedCriteria().forEach(obtained::add);
                for (String criterion : obtained) {
                    tracker.revoke(advancement, criterion);
                }
            }
            tracker.flushDirty(player, false);
            tracker.save();
        }
    }

    public void stop(CommandSourceStack source) {
        MinecraftServer currentServer = source.getServer();
        if (state == GameState.WAITING && teamManager.count(PlayerRole.HUNTER) == 0 && teamManager.count(PlayerRole.RUNNER) == 0) {
            source.sendSuccess(() -> HunterWildcardText.translatable("msg.game.none_running"), false);
            return;
        }

        cleanupAndReset(currentServer);
        messageManager.broadcast(currentServer, HunterWildcardText.translatable("msg.game.stopped_by_admin"));
        source.sendSuccess(() -> HunterWildcardText.translatable("command.stop.stopped"), true);
    }

    public void join(ServerPlayer player, PlayerRole role) {
        if (state != GameState.WAITING) {
            messageManager.direct(player, HunterWildcardText.translatable("msg.team.cannot_switch_started"));
            return;
        }

        teamManager.join(player, role);
        messageManager.direct(player, HunterWildcardText.translatable("msg.team.joined", role.getDisplayText()));
    }

    public void leave(ServerPlayer player) {
        if (state != GameState.WAITING) {
            messageManager.direct(player, HunterWildcardText.translatable("msg.team.cannot_leave_started"));
            return;
        }

        PlayerRole oldRole = teamManager.leave(player);
        player.removeEffect(MobEffects.GLOWING);
        respawnManager.remove(player);
        hunterBoundaryManager.remove(player);
        compassTracker.removeCompass(player);
        if (oldRole == null) {
            messageManager.direct(player, HunterWildcardText.translatable("msg.team.not_in_team"));
            return;
        }

        messageManager.direct(player, HunterWildcardText.translatable("msg.team.left", oldRole.getDisplayText()));
        checkWinConditions();
    }

    public Component getStatusText() {
        String wildcardName = wildcardManager.getActiveRuleName();
        Component wildcardText = wildcardName == null
                ? HunterWildcardText.translatable("common.none")
                : HunterWildcardText.wildcardName(wildcardName);
        return HunterWildcardText.translatable(
                "command.status",
                HunterWildcardText.translatable("state." + state.name().toLowerCase()),
                teamManager.count(PlayerRole.HUNTER),
                teamManager.count(PlayerRole.RUNNER),
                wildcardText
        );
    }

    public GameState getState() {
        return state;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public WildcardManager getWildcardManager() {
        return wildcardManager;
    }

    public int getPhaseRemainingTicks() {
        return switch (state) {
            case PREPARING -> Math.max(0, preparingTicks);
            case ENDING -> Math.max(0, endingTicks);
            default -> -1;
        };
    }

    public int getActiveWildcardRemainingTicks() {
        return wildcardManager.getActiveRemainingTicks();
    }

    public int getTicksUntilNextWildcard() {
        return wildcardManager.getTicksUntilNextWildcard();
    }

    public ModConfig getConfig() {
        return config;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    /** True for team members and for the operator currently running a wildcard test in the lobby. */
    public boolean isWildcardParticipant(ServerPlayer player) {
        return teamManager.getRole(player) != null || player.getUUID().equals(wildcardTestPlayerUuid);
    }

    public void setDebugMenuEnabled(ServerPlayer player, boolean enabled) {
        if (enabled) {
            debugMenuPlayers.add(player.getUUID());
        } else {
            debugMenuPlayers.remove(player.getUUID());
        }
    }

    public boolean isDebugMenuEnabled(ServerPlayer player) {
        return debugMenuPlayers.contains(player.getUUID());
    }

    public void applyConfig(ModConfig newConfig) {
        config.copyFrom(newConfig);
        notifyConfigChanged();
    }

    public boolean saveConfig() {
        return config.save();
    }

    public boolean reloadConfig() {
        if (state != GameState.WAITING) {
            return false;
        }

        config.copyFrom(ModConfig.load());
        notifyConfigChanged();
        return true;
    }

    public void reloadConfig(CommandSourceStack source) {
        if (reloadConfig()) {
            source.sendSuccess(() -> HunterWildcardText.translatable("command.config.reload.success"), true);
        } else {
            source.sendFailure(HunterWildcardText.translatable("msg.config.cannot_reload_started"));
        }
    }

    public void saveConfig(CommandSourceStack source) {
        if (state != GameState.WAITING) {
            source.sendFailure(HunterWildcardText.translatable("msg.config.cannot_save_started"));
            return;
        }

        if (saveConfig()) {
            source.sendSuccess(() -> HunterWildcardText.translatable("command.config.save.success"), true);
        } else {
            source.sendFailure(HunterWildcardText.translatable("msg.config.save_failed"));
        }
    }

    public void rollWildcard(CommandSourceStack source) {
        if (state != GameState.RUNNING) {
            source.sendFailure(HunterWildcardText.translatable("msg.wildcard.roll_requires_running"));
            return;
        }

        boolean started = wildcardManager.rollNow(context());
        if (started) {
            source.sendSuccess(() -> HunterWildcardText.translatable("command.wildcard.roll.success"), true);
        } else {
            source.sendFailure(HunterWildcardText.translatable("msg.wildcard.none_available"));
        }
    }

    public void testWildcard(CommandSourceStack source, String wildcardName, ServerPlayer tester) {
        MinecraftServer targetServer = source.getServer();
        GameContext testContext = new GameContext(targetServer, config, teamManager, random, List.of(tester));
        boolean started = wildcardManager.startRuleByName(testContext, wildcardName);
        if (started) {
            server = targetServer;
            wildcardTestPlayerUuid = tester.getUUID();
            source.sendSuccess(() -> HunterWildcardText.translatable("command.wildcard.test.success", HunterWildcardText.wildcardName(wildcardName)), true);
        } else {
            source.sendFailure(HunterWildcardText.translatable("msg.wildcard.unavailable_or_disabled", Component.literal(wildcardName)));
        }
    }

    public void stopWildcard(CommandSourceStack source) {
        if (state != GameState.RUNNING) {
            source.sendFailure(HunterWildcardText.translatable("msg.wildcard.stop_requires_running"));
            return;
        }

        boolean stopped = wildcardManager.stopActiveRule(context());
        if (stopped) {
            source.sendSuccess(() -> HunterWildcardText.translatable("command.wildcard.stop.success"), true);
        } else {
            source.sendFailure(HunterWildcardText.translatable("msg.wildcard.none_running"));
        }
    }

    /** Lets a running rule end itself early (used by the Backrooms once a whole side is out). */
    public void requestWildcardStop() {
        // Applied by WildcardManager after the current rule tick; stopping synchronously from inside
        // a rule's onTick nulled the active rule mid-tick and crashed the server.
        wildcardManager.requestStop();
    }

    public void debugStopWildcard(CommandSourceStack source) {
        GameContext testContext = state == GameState.RUNNING && server != null ? context() : debugContext(source.getServer());
        boolean stopped = wildcardManager.stopActiveRule(testContext);
        if (stopped) {
            wildcardTestPlayerUuid = null;
            source.sendSuccess(() -> HunterWildcardText.translatable("command.wildcard.stop.success"), true);
        } else {
            source.sendFailure(HunterWildcardText.translatable("msg.wildcard.none_running"));
        }
    }

    public void beforeDeathSpectatorTick(ServerPlayer player) {
        respawnManager.beforeSpectatorTick(player);
    }

    public void beforeDeathTargetTeleport(ServerPlayer target) {
        respawnManager.beforeTargetTeleport(target);
    }

    private void tick(MinecraftServer tickServer) {
        if (state == GameState.WAITING && wildcardManager.hasRuleInProgress()) {
            server = tickServer;
            wildcardManager.tick(debugContext(tickServer));
            tickLobbyGlow(tickServer);
            statusSyncTicks++;
            if (statusSyncTicks >= STATUS_SYNC_INTERVAL_TICKS) {
                statusSyncTicks = 0;
                HunterWildcardPackets.syncAll(tickServer);
            }
            return;
        }

        if (state == GameState.WAITING) {
            tickLobbyGlow(tickServer);
            return;
        }

        server = tickServer;
        statusSyncTicks++;
        if (statusSyncTicks >= STATUS_SYNC_INTERVAL_TICKS) {
            statusSyncTicks = 0;
            HunterWildcardPackets.syncAll(tickServer);
        }
        ruleEnforceTicks++;
        if (ruleEnforceTicks >= STATUS_SYNC_INTERVAL_TICKS && state != GameState.ENDING) {
            ruleEnforceTicks = 0;
            enforceKeepInventory(tickServer);
        }

        if (state == GameState.PREPARING) {
            tickPreparing();
            return;
        }

        if (state == GameState.RUNNING) {
            tickRunning();
            return;
        }

        if (state == GameState.ENDING) {
            tickEnding();
        }
    }

    private void tickPreparing() {
        preparingTicks--;
        GameContext context = context();
        hunterBoundaryManager.tick(context);
        bossBarManager.updatePrepareBar(context, preparingTicks, config.getPreparingTicks());

        if (preparingTicks <= 0) {
            startRunning();
        }
    }

    private void startRunning() {
        state = GameState.RUNNING;
        actionBarTicks = 0;
        runningTicks = 0;
        GameContext context = context();
        hunterBoundaryManager.clear();
        bossBarManager.clearPrepareBar();
        respawnManager.start(context);
        winConditionManager.start(context);
        wildcardManager.reset();
        compassTracker.giveCompasses(context);
        roleModifierManager.apply(context);
        messageManager.broadcast(server, HunterWildcardText.translatable("msg.game.running_started"));
    }

    private void tickRunning() {
        GameContext context = context();
        compassTracker.tick(context, wildcardManager.getActiveRule());
        compassTracker.removeRunnerCompasses(context);
        respawnManager.tick(context, compassTracker);
        roleModifierManager.tick(context);
        wildcardManager.tick(context);
        checkWinConditions();
        if (state != GameState.RUNNING) {
            return;
        }

        String winReason = winConditionManager.tick(context);
        if (winReason != null) {
            enterEnding(winReason);
            return;
        }

        runningTicks++;
        actionBarTicks--;
        // The role reminder only runs for the first seconds of the chase; after that the HUD stays quiet.
        if (actionBarTicks <= 0 && runningTicks <= ROLE_ACTIONBAR_TICKS) {
            actionBarTicks = config.getActionBarIntervalTicks();
            for (ServerPlayer player : context.getParticipants()) {
                if (respawnManager.isWaitingForRespawn(player)) {
                    continue;
                }

                PlayerRole role = teamManager.getRole(player);
                Component roleName = role == null ? HunterWildcardText.translatable("role.spectator") : role.getDisplayText();
                messageManager.actionBar(player, HunterWildcardText.translatable("hud.actionbar.running_role", roleName));
            }
        }
    }

    private void tickEnding() {
        endingTicks--;
        if (endingTicks <= 0) {
            cleanupAndReset(server);
        }
    }

    private void handlePlayerDeath(ServerPlayer player, DamageSource damageSource) {
        if (state != GameState.RUNNING) {
            return;
        }

        GameContext context = context();
        wildcardManager.onPlayerDeath(context, player);

        PlayerRole role = teamManager.getRole(player);
        ServerPlayer hunterKiller = damageSource.getEntity() instanceof ServerPlayer attacker && teamManager.isHunter(attacker)
                ? attacker
                : null;
        ServerPlayer runnerKiller = damageSource.getEntity() instanceof ServerPlayer attacker && teamManager.isRunner(attacker)
                ? attacker
                : null;
        if (role == PlayerRole.HUNTER && runnerKiller != null) {
            HunterWildcardPackets.sendHudFeedback(
                    context,
                    HunterWildcardText.spec("hud.feedback.counter_kill.title"),
                    HunterWildcardText.spec("hud.feedback.versus", PlayerUtil.displayNameSpec(runnerKiller), PlayerUtil.displayNameSpec(player)),
                    HunterWildcardText.spec("hud.feedback.counter_kill.subtitle"),
                    "runner"
            );
        }
        RespawnManager.DeathOutcome outcome = respawnManager.onPlayerDeath(context, player, role, hunterKiller);
        if (outcome.message() != null) {
            messageManager.toParticipants(context, outcome.message());
        }
        if (outcome.endingReason() != null) {
            enterEnding(outcome.endingReason());
        }
    }

    private void handleEntityKilled(LivingEntity entity, DamageSource damageSource) {
        if (state != GameState.RUNNING || !(damageSource.getEntity() instanceof ServerPlayer killer)) {
            return;
        }

        wildcardManager.onEntityKilled(context(), killer, entity);
    }

    private void handleAfterRespawn(ServerPlayer player) {
        restorePreservedDeathInventory(player);
        if (state != GameState.RUNNING) {
            return;
        }

        respawnManager.onAfterRespawn(context(), player, compassTracker);
    }

    public void cycleDeathSpectate(ServerPlayer player, boolean previous) {
        if (state == GameState.RUNNING) respawnManager.cycleSpectating(context(), player, previous);
    }

    public void handlePlayerAttack(ServerPlayer player, Entity target) {
        GameContext eventContext = wildcardEventContext(player);
        if (eventContext != null) {
            wildcardManager.onPlayerAttack(eventContext, player, target);
        }
    }

    public void handlePlayerDamaged(ServerPlayer player, DamageSource source, float damageTaken) {
        if (state == GameState.RUNNING && teamManager.isRunner(player)
                && source.getEntity() instanceof ServerPlayer attacker && teamManager.isHunter(attacker)) {
            respawnManager.recordHunterHit(player, attacker);
        }

        GameContext eventContext = wildcardEventContext(player);
        if (eventContext != null) {
            wildcardManager.onPlayerDamaged(eventContext, player, source, damageTaken);
        }
    }

    /** Scales damage dealt by a hunter to a runner by the configured multiplier while a round is running, then lets the active wildcard rescale it. */
    public float modifyIncomingDamage(ServerPlayer victim, DamageSource source, float amount) {
        if (amount <= 0.0F) {
            return amount;
        }

        if (state == GameState.RUNNING && source.getEntity() instanceof ServerPlayer attacker
                && teamManager.isHunter(attacker) && teamManager.isRunner(victim)) {
            amount *= config.getHunterDamageMultiplier();
        }

        return modifyLivingDamage(victim, source, amount);
    }

    /** Wildcard damage hook for any living victim (players arrive here through {@link #modifyIncomingDamage}). */
    public float modifyLivingDamage(LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0.0F || !wildcardManager.hasRuleInProgress()) {
            return amount;
        }
        ServerPlayer anchor = victim instanceof ServerPlayer victimPlayer
                ? victimPlayer
                : source.getEntity() instanceof ServerPlayer attacker ? attacker : null;
        if (anchor == null) {
            return amount;
        }
        GameContext eventContext = wildcardEventContext(anchor);
        return eventContext == null ? amount : wildcardManager.modifyDamage(eventContext, victim, source, amount);
    }

    public void handleDamageDealt(ServerPlayer attacker, LivingEntity victim, float damageDealt) {
        GameContext eventContext = wildcardEventContext(attacker);
        if (eventContext != null) {
            wildcardManager.onDamageDealt(eventContext, attacker, victim, damageDealt);
        }
    }

    public void handleItemDropped(ServerPlayer player, ItemEntity item) {
        GameContext eventContext = wildcardEventContext(player);
        if (eventContext != null) {
            wildcardManager.onItemDropped(eventContext, player, item);
        }
    }

    public void handleItemUse(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (state == GameState.RUNNING && hand == InteractionHand.MAIN_HAND && teamManager.isHunter(player) && compassTracker.isHunterCompass(stack)) {
            if (player.isShiftKeyDown()) {
                compassTracker.cycleTarget(context(), player);
            } else {
                compassTracker.openMenu(context(), player);
            }
            return;
        }

        GameContext eventContext = wildcardEventContext(player);
        if (eventContext != null) {
            wildcardManager.onItemUse(eventContext, player, hand, stack);
        }
    }

    /** Compass menu choice from the client; target null means "nearest runner". */
    public void selectCompassTarget(ServerPlayer player, UUID target) {
        if (state != GameState.RUNNING || server == null || !teamManager.isHunter(player)) {
            return;
        }
        compassTracker.selectTarget(context(), player, target);
    }

    /** Applies the live-safe subset of a config mid-round (speeds, targets, wildcards, respawn tuning...). */
    public void applyLiveConfig(ModConfig newConfig) {
        config.copyLiveFrom(newConfig);
        notifyConfigChanged();
        if (server == null || state == GameState.WAITING) {
            return;
        }
        GameContext context = context();
        applyLocatorBarForRound(server);
        if (state == GameState.RUNNING) {
            roleModifierManager.apply(context);
            respawnManager.refreshHunterProgress(context);
        }
        HunterWildcardPackets.syncAll(server);
    }

    /** Locator bar filter: during a round you only see waypoints of your own side. */
    public boolean canSeeWaypoint(ServerPlayer receiver, LivingEntity source) {
        if (state == GameState.WAITING || !config.locatorBarTeamOnly || !(source instanceof ServerPlayer sourcePlayer)) {
            return true;
        }
        PlayerRole receiverRole = teamManager.getRole(receiver);
        if (receiverRole == null) {
            return true;
        }
        return receiverRole == teamManager.getRole(sourcePlayer);
    }

    public void handlePlayerAteFood(ServerPlayer player, ItemStack eatenStack) {
        GameContext eventContext = wildcardEventContext(player);
        if (eventContext != null) {
            wildcardManager.onPlayerAteFood(eventContext, player, eatenStack);
        }
    }

    public void handleBlockBroken(ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
        GameContext eventContext = wildcardEventContext(player);
        if (eventContext != null) {
            wildcardManager.onBlockBroken(eventContext, player, world, pos, state);
        }
    }

    public void handleBlockPlaced(ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
        GameContext eventContext = wildcardEventContext(player);
        if (eventContext != null) {
            wildcardManager.onBlockPlaced(eventContext, player, world, pos, state);
        }
    }

    public boolean handleDeathInventoryDrop(ServerPlayer player) {
        // Dying inside the Backrooms never scatters a kit somewhere nobody can go back for.
        if (BackroomsDimension.isInBackrooms(player)) {
            preserveDeathInventory(player);
            return true;
        }

        // keepInventory is already taken over when the preparation countdown starts.
        if (state != GameState.PREPARING && state != GameState.RUNNING) {
            return false;
        }

        PlayerRole role = teamManager.getRole(player);
        if (role == PlayerRole.HUNTER) {
            compassTracker.normalizeHunterCompasses(player);
            if (config.hunterDeathNoDrops) {
                preserveDeathInventory(player);
                return true;
            }

            compassTracker.removeCompass(player);
            return false;
        }

        if (role == PlayerRole.RUNNER && config.runnerDeathNoDrops) {
            preserveDeathInventory(player);
            return true;
        }

        return false;
    }

    private void preserveDeathInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<ItemStack> stacks = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            stacks.add(inventory.getItem(slot).copy());
        }
        preservedDeathInventories.put(player.getUUID(), stacks);
    }

    private void restorePreservedDeathInventory(ServerPlayer player) {
        List<ItemStack> stacks = preservedDeathInventories.remove(player.getUUID());
        if (stacks == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = slot < stacks.size() ? stacks.get(slot).copy() : ItemStack.EMPTY;
            inventory.setItem(slot, stack);
        }
        if (teamManager.isHunter(player)) {
            compassTracker.normalizeHunterCompasses(player);
        } else if (teamManager.isRunner(player)) {
            compassTracker.removeCompass(player);
        }
    }

    private void handleDragonDeath() {
        if (state != GameState.RUNNING || server == null) {
            return;
        }

        String reason = winConditionManager.onDragonKilled(context());
        if (reason != null) {
            enterEnding(reason);
        }
    }

    private void handleJoin(ServerPlayer player, MinecraftServer joinedServer) {
        teamManager.syncScoreboardTeams(joinedServer);
        if (state != GameState.WAITING && teamManager.getRole(player) == null) {
            forceSpectator(player);
        }
        HunterWildcardPackets.sendSync(player);
        HunterWildcardPackets.sendObjectiveTo(player);
    }

    private void handleDisconnect(ServerPlayer player) {
        debugMenuPlayers.remove(player.getUUID());
        forcedSpectators.remove(player.getUUID());
        if (player.getUUID().equals(wildcardTestPlayerUuid)) {
            wildcardTestPlayerUuid = null;
        }
        PlayerRole role = teamManager.leave(player);
        respawnManager.remove(player);
        hunterBoundaryManager.remove(player);
        preservedDeathInventories.remove(player.getUUID());
        if (role != null && state != GameState.WAITING) {
            checkWinConditions();
        }
    }

    private void handleServerStopping(MinecraftServer stoppingServer) {
        lastWinner = lastReason = ""; lastMembers = List.of();
        cleanupAndReset(stoppingServer);
        BackroomsSession.returnEveryone(stoppingServer);
    }

    private void checkWinConditions() {
        if (state != GameState.RUNNING || server == null) {
            return;
        }

        if (teamManager.getRunners(server).isEmpty()) {
            enterEnding(HunterWildcardText.spec("msg.win.no_online_runners"));
            return;
        }

        if (teamManager.getHunters(server).isEmpty()) {
            enterEnding(HunterWildcardText.spec("msg.win.no_online_hunters"));
        }
    }

    public void endGameWithReason(String reason) {
        enterEnding(reason);
    }

    private void enterEnding(String reason) {
        if (state == GameState.ENDING || state == GameState.WAITING) {
            return;
        }

        state = GameState.ENDING;
        endingTicks = config.getEndingTicks();
        GameContext endingContext = context();
        WinningSide winningSide = classifyWinner(reason);
        lastWinner = HunterWildcardText.key(winningSide == WinningSide.RUNNERS ? "team.runners" : winningSide == WinningSide.HUNTERS ? "team.hunters" : "common.none");
        lastReason = reason;
        lastMembers = roster(null);
        HunterWildcardPackets.clearChat(server);
        sendEndingFeedback(reason, winningSide);
        sendEndingSummary(endingContext, reason, winningSide);
        launchWinnerFireworks(endingContext, winningSide);
        clearRoundEffects(endingContext);
    }

    private void sendEndingFeedback(String reason, WinningSide winningSide) {
        if (winningSide == WinningSide.NONE) {
            return;
        }

        boolean runnerWin = winningSide == WinningSide.RUNNERS;
        String title = runnerWin
                ? HunterWildcardText.spec("hud.feedback.runner_victory.title")
                : HunterWildcardText.spec("hud.feedback.hunter_victory.title");
        String style = runnerWin ? "runner" : "hunter";
        HunterWildcardPackets.sendHudFeedback(server, title, compactEndingReason(reason), HunterWildcardText.spec("hud.feedback.ending.line2"), style);
    }

    private WinningSide classifyWinner(String reason) {
        String key = specKey(reason);
        if (key.equals(HunterWildcardText.key("msg.win.no_online_hunters"))
                || key.startsWith(HunterWildcardText.key("msg.win.runner."))) {
            return WinningSide.RUNNERS;
        }
        if (key.equals(HunterWildcardText.key("msg.win.no_online_runners"))
                || key.startsWith(HunterWildcardText.key("msg.win.hunter."))) {
            return WinningSide.HUNTERS;
        }
        return WinningSide.NONE;
    }

    private void sendEndingSummary(GameContext context, String reason, WinningSide winningSide) {
        Component winnerName = switch (winningSide) {
            case HUNTERS -> HunterWildcardText.translatable("team.hunters");
            case RUNNERS -> HunterWildcardText.translatable("team.runners");
            case NONE -> HunterWildcardText.translatable("common.undecided");
        };
        ChatFormatting winnerColor = switch (winningSide) {
            case HUNTERS -> ChatFormatting.RED;
            case RUNNERS -> ChatFormatting.AQUA;
            case NONE -> ChatFormatting.GOLD;
        };
        List<ServerPlayer> hunters = context.getHunters();
        List<ServerPlayer> runners = context.getRunners();
        String wildcardName = wildcardManager.getActiveRuleName();
        Component wildcardText = wildcardName == null || wildcardName.isBlank()
                ? HunterWildcardText.translatable("common.none")
                : HunterWildcardText.wildcardName(wildcardName);

        broadcastEndingLine(Component.empty());
        broadcastEndingLine(HunterWildcardText.translatable("msg.ending.summary.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        broadcastEndingLine(Component.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.winner.label").withStyle(ChatFormatting.GRAY))
                .append(winnerName.copy().withStyle(winnerColor, ChatFormatting.BOLD)));
        broadcastEndingLine(Component.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.reason.label").withStyle(ChatFormatting.GRAY))
                .append((reason == null || reason.isBlank() ? HunterWildcardText.translatable("common.no_reason") : HunterWildcardText.fromSpec(reason)).copy().withStyle(ChatFormatting.WHITE)));
        broadcastEndingLine(Component.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.wildcard.label").withStyle(ChatFormatting.GRAY))
                .append(wildcardText.copy().withStyle(ChatFormatting.YELLOW)));
        broadcastEndingLine(Component.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.online.label").withStyle(ChatFormatting.GRAY))
                .append(HunterWildcardText.translatable("msg.ending.summary.online.value", hunters.size(), runners.size()).withStyle(ChatFormatting.WHITE)));
        broadcastEndingLine(Component.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.hunters.label").withStyle(ChatFormatting.RED))
                .append(formatPlayerNames(hunters).copy().withStyle(ChatFormatting.WHITE)));
        broadcastEndingLine(Component.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.runners.label").withStyle(ChatFormatting.AQUA))
                .append(formatPlayerNames(runners).copy().withStyle(ChatFormatting.WHITE)));
        broadcastEndingLine(HunterWildcardText.translatable("msg.ending.summary.footer").withStyle(ChatFormatting.GOLD));
    }

    private void broadcastEndingLine(Component text) {
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(text, false);
        }
    }

    private Component formatPlayerNames(List<ServerPlayer> players) {
        if (players.isEmpty()) {
            return HunterWildcardText.translatable("msg.ending.summary.no_online_players");
        }

        int displayed = Math.min(players.size(), 6);
        var names = Component.empty();
        for (int i = 0; i < displayed; i++) {
            if (i > 0) {
                names.append(Component.literal(", "));
            }
            names.append(players.get(i).getName());
        }
        if (players.size() > displayed) {
            names.append(HunterWildcardText.translatable("msg.ending.summary.more_players", players.size()));
        }
        return names;
    }

    private void launchWinnerFireworks(GameContext context, WinningSide winningSide) {
        List<ServerPlayer> winners = switch (winningSide) {
            case HUNTERS -> context.getHunters();
            case RUNNERS -> context.getRunners();
            case NONE -> List.of();
        };
        for (ServerPlayer winner : winners) {
            ServerLevel world = winner.level();
            for (int i = 0; i < 2; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = 0.8D + random.nextDouble() * 0.7D;
                double x = winner.getX() + Math.cos(angle) * radius;
                double y = winner.getY() + 4.0D + i * 0.6D;
                double z = winner.getZ() + Math.sin(angle) * radius;
                FireworkRocketEntity firework = new FireworkRocketEntity(world, createWinnerFirework(winningSide), x, y, z, false);
                world.addFreshEntity(firework);
            }
        }
    }

    private ItemStack createWinnerFirework(WinningSide winningSide) {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        IntList colors = winningSide == WinningSide.RUNNERS
                ? IntList.of(0x55D6FF, 0xFFFFFF, 0x7FC2FF)
                : IntList.of(0xFF4D4D, 0xFFD966, 0xFFFFFF);
        IntList fadeColors = winningSide == WinningSide.RUNNERS
                ? IntList.of(0x2F80FF, 0xFFFFFF)
                : IntList.of(0xFF9F3F, 0xFFFFFF);
        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.STAR,
                colors,
                fadeColors,
                true,
                true
        );
        stack.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion)));
        return stack;
    }

    private String compactEndingReason(String reason) {
        String key = specKey(reason);
        if (key.equals(HunterWildcardText.key("msg.win.runner.dragon"))) {
            return HunterWildcardText.spec("hud.feedback.reason.dragon");
        }
        if (key.equals(HunterWildcardText.key("msg.win.runner.survive_time"))) {
            return HunterWildcardText.spec("hud.feedback.reason.survive_time");
        }
        if (key.equals(HunterWildcardText.key("msg.win.runner.reach_location"))) {
            return HunterWildcardText.spec("hud.feedback.reason.reach_location");
        }
        if (key.equals(HunterWildcardText.key("msg.win.runner.collect_item"))) {
            return HunterWildcardText.spec("hud.feedback.reason.collect_item");
        }
        if (key.equals(HunterWildcardText.key("msg.win.hunter.kill_target"))) {
            return HunterWildcardText.spec("hud.feedback.reason.kill_target_complete");
        }
        if (key.equals(HunterWildcardText.key("msg.win.hunter.all_runners_out"))) {
            return HunterWildcardText.spec("hud.feedback.reason.all_runners_out");
        }
        if (key.equals(HunterWildcardText.key("msg.win.no_online_runners"))) {
            return HunterWildcardText.spec("hud.feedback.reason.runners_offline");
        }
        if (key.equals(HunterWildcardText.key("msg.win.no_online_hunters"))) {
            return HunterWildcardText.spec("hud.feedback.reason.hunters_offline");
        }

        return reason == null ? "" : reason;
    }

    private String specKey(String spec) {
        if (spec == null || spec.isBlank()) {
            return "";
        }

        int separator = spec.indexOf(HunterWildcardText.SPEC_SEPARATOR);
        return separator < 0 ? spec : spec.substring(0, separator);
    }

    private void cleanupAndReset(MinecraftServer cleanupServer) {
        MinecraftServer targetServer = cleanupServer != null ? cleanupServer : server;
        if (targetServer != null) {
            clearRoundEffects(debugContext(targetServer));
            restoreLocatorBarRule(targetServer);
            restoreKeepInventory(targetServer);
            restoreForcedSpectators(targetServer);
            teamManager.clearScoreboardTeams(targetServer);
            HunterWildcardPackets.clearObjective(targetServer);
        }

        teamManager.clear();
        forcedSpectators.clear();
        previousKeepInventoryRule = null;
        state = GameState.WAITING;
        server = null;
        preparingTicks = 0;
        endingTicks = 0;
        actionBarTicks = 0;
        wildcardTestPlayerUuid = null;
        wildcardManager.reset();
        compassTracker.reset();
        respawnManager.clear();
        hunterBoundaryManager.clear();
        winConditionManager.clear();
        preservedDeathInventories.clear();
        previousLocatorBarRule = null;
    }

    private void clearRoundEffects(GameContext context) {
        wildcardManager.clear(context);
        bossBarManager.clear();
        hunterBoundaryManager.clear();
        winConditionManager.clear(context);
        respawnManager.clear(context);
        compassTracker.clear(context);
        roleModifierManager.clear(context);

        for (ServerPlayer player : context.getParticipants()) {
            player.removeEffect(MobEffects.SPEED);
            player.removeEffect(MobEffects.JUMP_BOOST);
            player.removeEffect(MobEffects.SLOW_FALLING);
            player.removeEffect(MobEffects.GLOWING);
            player.removeEffect(MobEffects.NIGHT_VISION);
            TinyPlayersRule.removeScale(player);
            FragileRule.removeCap(player);
        }
    }

    private GameContext context() {
        return new GameContext(server, config, teamManager, random);
    }

    /**
     * While picking sides in the lobby, everyone on a team glows in their team colour (red hunters, blue runners
     * via the scoreboard teams) so the split is visible at a glance. The glow is short and refreshed, so it fades
     * by itself once a player leaves their team or the round starts.
     */
    private void tickLobbyGlow(MinecraftServer tickServer) {
        lobbyGlowTicks++;
        if (lobbyGlowTicks < LOBBY_GLOW_REFRESH_TICKS) {
            return;
        }
        lobbyGlowTicks = 0;
        for (ServerPlayer player : teamManager.getParticipants(tickServer)) {
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, LOBBY_GLOW_REFRESH_TICKS + 30, 0, false, false, false));
        }
    }

    private void clearLobbyGlow(MinecraftServer targetServer) {
        for (ServerPlayer player : teamManager.getParticipants(targetServer)) {
            player.removeEffect(MobEffects.GLOWING);
        }
    }

    private GameContext wildcardEventContext(ServerPlayer player) {
        if (state == GameState.RUNNING && server != null) {
            return context();
        }

        if (!wildcardManager.hasRuleInProgress()) {
            return null;
        }

        MinecraftServer targetServer = player.level().getServer();
        if (targetServer == null) {
            return null;
        }

        server = targetServer;
        return debugContext(targetServer);
    }

    private GameContext debugContext(MinecraftServer targetServer) {
        if (wildcardTestPlayerUuid == null) {
            return new GameContext(targetServer, config, teamManager, random);
        }

        ServerPlayer tester = targetServer.getPlayerList().getPlayer(wildcardTestPlayerUuid);
        if (tester == null) {
            return new GameContext(targetServer, config, teamManager, random);
        }

        return new GameContext(targetServer, config, teamManager, random, List.of(tester));
    }

    private void notifyConfigChanged() {
        wildcardManager.onConfigChanged(config);
        compassTracker.onConfigChanged(config);
    }

    /** Team-only mode keeps the locator bar on (filtered by the waypoint mixin); otherwise it is off for the round. */
    private void applyLocatorBarForRound(MinecraftServer targetServer) {
        if (targetServer == null) {
            return;
        }

        if (previousLocatorBarRule == null) {
            previousLocatorBarRule = targetServer.overworld().getGameRules().get(GameRules.LOCATOR_BAR);
        }
        targetServer.overworld().getGameRules().set(GameRules.LOCATOR_BAR, config.locatorBarTeamOnly, targetServer);
    }

    /**
     * The mod decides death drops per side, so vanilla keepInventory is forced off for the round (and
     * re-forced every second in case the host toggles it) and restored afterwards.
     */
    private void takeOverKeepInventory(MinecraftServer targetServer) {
        if (targetServer == null) {
            return;
        }
        if (previousKeepInventoryRule == null) {
            previousKeepInventoryRule = targetServer.overworld().getGameRules().get(GameRules.KEEP_INVENTORY);
        }
        enforceKeepInventory(targetServer);
    }

    private void enforceKeepInventory(MinecraftServer targetServer) {
        if (targetServer == null || previousKeepInventoryRule == null) {
            return;
        }
        if (Boolean.TRUE.equals(targetServer.overworld().getGameRules().get(GameRules.KEEP_INVENTORY))) {
            targetServer.overworld().getGameRules().set(GameRules.KEEP_INVENTORY, false, targetServer);
        }
    }

    private void restoreKeepInventory(MinecraftServer targetServer) {
        if (targetServer == null || previousKeepInventoryRule == null) {
            return;
        }
        targetServer.overworld().getGameRules().set(GameRules.KEEP_INVENTORY, previousKeepInventoryRule, targetServer);
        previousKeepInventoryRule = null;
    }

    /** Everyone online without a side spectates for the round. */
    private void forceSpectatorsForRound(MinecraftServer targetServer) {
        if (targetServer == null) {
            return;
        }
        for (ServerPlayer player : targetServer.getPlayerList().getPlayers()) {
            if (teamManager.getRole(player) == null) {
                forceSpectator(player);
            }
        }
    }

    private void forceSpectator(ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        forcedSpectators.putIfAbsent(player.getUUID(), player.gameMode.getGameModeForPlayer());
        player.setGameMode(GameType.SPECTATOR);
        messageManager.direct(player, HunterWildcardText.translatable("msg.spectator.forced"));
    }

    private void restoreForcedSpectators(MinecraftServer targetServer) {
        for (Map.Entry<UUID, GameType> entry : forcedSpectators.entrySet()) {
            ServerPlayer player = targetServer.getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.isSpectator()) {
                player.setGameMode(entry.getValue() == GameType.SPECTATOR ? GameType.SURVIVAL : entry.getValue());
            }
        }
        forcedSpectators.clear();
    }

    private void restoreLocatorBarRule(MinecraftServer targetServer) {
        if (targetServer == null || previousLocatorBarRule == null) {
            return;
        }

        targetServer.overworld().getGameRules().set(GameRules.LOCATOR_BAR, previousLocatorBarRule, targetServer);
    }

    private enum WinningSide {
        HUNTERS,
        RUNNERS,
        NONE
    }
}
