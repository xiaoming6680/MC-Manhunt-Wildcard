package com.xiaoming.hunterwildcard.network;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.command.HunterWildcardCommand;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.util.PlayerUtil;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HunterWildcardPackets {
    public static final CustomPayload.Id<RequestConfigPayload> C2S_REQUEST_CONFIG =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "request_config"));
    public static final CustomPayload.Id<SyncConfigPayload> S2C_SYNC_CONFIG =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "sync_config"));
    public static final CustomPayload.Id<OperationResultPayload> S2C_OPERATION_RESULT =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "operation_result"));
    public static final CustomPayload.Id<CloseConfigScreenPayload> S2C_CLOSE_CONFIG_SCREEN =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "close_config_screen"));
    public static final CustomPayload.Id<ClearChatPayload> S2C_CLEAR_CHAT =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "clear_chat"));
    public static final CustomPayload.Id<WildcardDrawPayload> S2C_WILDCARD_DRAW =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "wildcard_draw"));
    public static final CustomPayload.Id<WildcardIntroPayload> S2C_WILDCARD_INTRO =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "wildcard_intro"));
    public static final CustomPayload.Id<HunterKillFeedbackPayload> S2C_HUNTER_KILL_FEEDBACK =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "hunter_kill_feedback"));
    public static final CustomPayload.Id<HudFeedbackPayload> S2C_HUD_FEEDBACK =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "hud_feedback"));
    public static final CustomPayload.Id<ObjectiveStatusPayload> S2C_OBJECTIVE_STATUS =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "objective_status"));
    public static final CustomPayload.Id<ObjectiveNoticePayload> S2C_OBJECTIVE_NOTICE =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "objective_notice"));
    public static final CustomPayload.Id<WeaponOverheatStatusPayload> S2C_WEAPON_OVERHEAT_STATUS =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "weapon_overheat_status"));
    public static final CustomPayload.Id<WorldTiltPayload> S2C_WORLD_TILT =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "world_tilt"));
    public static final CustomPayload.Id<KeyScramblePayload> S2C_KEY_SCRAMBLE =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "key_scramble"));
    public static final CustomPayload.Id<BackroomsPhasePayload> S2C_BACKROOMS_PHASE =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "backrooms_phase"));
    public static final CustomPayload.Id<UpdateConfigPayload> C2S_UPDATE_CONFIG =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "update_config"));
    public static final CustomPayload.Id<ReloadConfigPayload> C2S_RELOAD_CONFIG =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "reload_config"));
    public static final CustomPayload.Id<DebugActionPayload> C2S_DEBUG_ACTION =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "debug_action"));
    public static final CustomPayload.Id<TestWildcardPayload> C2S_TEST_WILDCARD =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "test_wildcard"));
    public static final CustomPayload.Id<TeamActionPayload> C2S_TEAM_ACTION =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "team_action"));
    public static final CustomPayload.Id<GameActionPayload> C2S_GAME_ACTION =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "game_action"));

    public static final CustomPayload.Id<DeathWaitPayload> S2C_DEATH_WAIT =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "death_wait"));
    public static final CustomPayload.Id<CompassMenuPayload> S2C_COMPASS_MENU =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "compass_menu"));
    public static final CustomPayload.Id<CompassSelectPayload> C2S_COMPASS_SELECT =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "compass_select"));

    private static boolean payloadTypesRegistered;
    private static boolean serverReceiversRegistered;
    private static boolean objectiveRunnerVisible;
    private static String objectiveRunnerText = "";
    private static String objectiveRunnerStyle = "";
    private static String objectiveHunterText = "";

    private HunterWildcardPackets() {
    }

    public static void registerPayloadTypes() {
        if (payloadTypesRegistered) {
            return;
        }
        payloadTypesRegistered = true;

        PayloadTypeRegistry.playC2S().register(C2S_REQUEST_CONFIG, RequestConfigPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_SYNC_CONFIG, SyncConfigPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_OPERATION_RESULT, OperationResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_CLOSE_CONFIG_SCREEN, CloseConfigScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_CLEAR_CHAT, ClearChatPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_WILDCARD_DRAW, WildcardDrawPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_WORLD_TILT, WorldTiltPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_WILDCARD_INTRO, WildcardIntroPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_HUNTER_KILL_FEEDBACK, HunterKillFeedbackPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_HUD_FEEDBACK, HudFeedbackPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_OBJECTIVE_STATUS, ObjectiveStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_OBJECTIVE_NOTICE, ObjectiveNoticePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_WEAPON_OVERHEAT_STATUS, WeaponOverheatStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_KEY_SCRAMBLE, KeyScramblePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_BACKROOMS_PHASE, BackroomsPhasePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_UPDATE_CONFIG, UpdateConfigPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_RELOAD_CONFIG, ReloadConfigPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_DEBUG_ACTION, DebugActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_TEST_WILDCARD, TestWildcardPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_TEAM_ACTION, TeamActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_GAME_ACTION, GameActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_DEATH_WAIT, DeathWaitPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_COMPASS_MENU, CompassMenuPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_COMPASS_SELECT, CompassSelectPayload.CODEC);
    }

    public static void registerServerReceivers() {
        if (serverReceiversRegistered) {
            return;
        }
        serverReceiversRegistered = true;

        ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_CONFIG, (payload, context) -> sendSync(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_UPDATE_CONFIG, (payload, context) -> handleUpdateConfig(context.player(), payload.config()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_RELOAD_CONFIG, (payload, context) -> handleReloadConfig(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_DEBUG_ACTION, (payload, context) -> handleDebugAction(context.player(), payload.action()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_TEST_WILDCARD, (payload, context) -> handleTestWildcard(context.player(), payload.wildcardName()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_TEAM_ACTION, (payload, context) -> handleTeamAction(context.player(), payload.action()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_GAME_ACTION, (payload, context) -> handleGameAction(context.player(), payload.action()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_COMPASS_SELECT, (payload, context) ->
                GameManager.getInstance().selectCompassTarget(context.player(), payload.nearest() ? null : payload.targetId()));
    }

    public static void sendSync(ServerPlayerEntity player) {
        if (ServerPlayNetworking.canSend(player, S2C_SYNC_CONFIG)) {
            ServerPlayNetworking.send(player, createSyncPayload(player));
        }
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendSync(player);
        }
    }

    public static void closeConfigScreens(MinecraftServer server) {
        if (server == null) {
            return;
        }

        CloseConfigScreenPayload payload = new CloseConfigScreenPayload();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_CLOSE_CONFIG_SCREEN)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void clearChat(MinecraftServer server) {
        if (server == null) {
            return;
        }

        ClearChatPayload payload = new ClearChatPayload();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_CLEAR_CHAT)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void sendOperationResult(ServerPlayerEntity player, boolean success, String message) {
        if (ServerPlayNetworking.canSend(player, S2C_OPERATION_RESULT)) {
            ServerPlayNetworking.send(player, new OperationResultPayload(success, message));
        }
    }

    private static void sendSyncAndResult(ServerPlayerEntity player, boolean success, String message) {
        sendSync(player);
        sendOperationResult(player, success, message);
    }

    private static void syncAllAndResult(ServerPlayerEntity player, boolean success, String message) {
        syncAll(player.getEntityWorld().getServer());
        sendOperationResult(player, success, message);
    }

    public static void sendWildcardDraw(GameContext context, String wildcardName) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            if (ServerPlayNetworking.canSend(player, S2C_WILDCARD_DRAW)) {
                ServerPlayNetworking.send(player, new WildcardDrawPayload(wildcardName));
            }
        }
    }

    public static void sendWildcardIntro(GameContext context, String wildcardId, String descriptionSpec) {
        WildcardIntroPayload payload = new WildcardIntroPayload(wildcardId, descriptionSpec, true);
        for (ServerPlayerEntity player : context.getParticipants()) {
            if (ServerPlayNetworking.canSend(player, S2C_WILDCARD_INTRO)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void clearWildcardIntro(GameContext context) {
        WildcardIntroPayload payload = new WildcardIntroPayload("", "", false);
        for (ServerPlayerEntity player : context.getParticipants()) {
            if (ServerPlayNetworking.canSend(player, S2C_WILDCARD_INTRO)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendHunterKillFeedback(GameContext context, String hunterName, String runnerName, int remainingKills, int currentKills, int targetKills) {
        HunterKillFeedbackPayload payload = new HunterKillFeedbackPayload(hunterName, runnerName, remainingKills, currentKills, targetKills);
        for (ServerPlayerEntity player : context.getServer().getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_HUNTER_KILL_FEEDBACK)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendHudFeedback(GameContext context, String title, String line1, String line2, String style) {
        sendHudFeedback(context.getServer(), title, line1, line2, style);
    }

    public static void sendHudFeedback(MinecraftServer server, String title, String line1, String line2, String style) {
        if (server == null) {
            return;
        }

        HudFeedbackPayload payload = new HudFeedbackPayload(title, line1, line2, style);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_HUD_FEEDBACK)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendObjectiveStatus(GameContext context, boolean visible, String text, String style) {
        sendObjectiveStatus(context.getServer(), visible, text, style);
    }

    /** Updates the runner objective half of the panel; the hunter progress line is kept as last sent. */
    public static void sendObjectiveStatus(MinecraftServer server, boolean visible, String text, String style) {
        objectiveRunnerVisible = visible;
        objectiveRunnerText = text == null ? "" : text;
        objectiveRunnerStyle = style == null ? "" : style;
        broadcastObjective(server);
    }

    /** Updates the hunter progress half of the panel; an empty spec hides that line. */
    public static void sendHunterProgress(MinecraftServer server, String hunterText) {
        objectiveHunterText = hunterText == null ? "" : hunterText;
        broadcastObjective(server);
    }

    public static void clearObjective(MinecraftServer server) {
        objectiveRunnerVisible = false;
        objectiveRunnerText = "";
        objectiveRunnerStyle = "";
        objectiveHunterText = "";
        broadcastObjective(server);
    }

    /** Re-sends the current panel to one player (used after respawn / join). */
    public static void sendObjectiveTo(ServerPlayerEntity player) {
        if (ServerPlayNetworking.canSend(player, S2C_OBJECTIVE_STATUS)) {
            ServerPlayNetworking.send(player, currentObjectivePayload());
        }
    }

    private static ObjectiveStatusPayload currentObjectivePayload() {
        boolean visible = objectiveRunnerVisible || !objectiveHunterText.isEmpty();
        return new ObjectiveStatusPayload(visible, objectiveRunnerVisible ? objectiveRunnerText : "", objectiveRunnerStyle, objectiveHunterText);
    }

    private static void broadcastObjective(MinecraftServer server) {
        if (server == null) {
            return;
        }

        ObjectiveStatusPayload payload = currentObjectivePayload();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_OBJECTIVE_STATUS)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    /** Full-screen death wait cover: remaining seconds plus two info lines; visible=false clears it. */
    public static void sendDeathWait(ServerPlayerEntity player, boolean visible, int remainingSeconds, String line1, String line2) {
        if (ServerPlayNetworking.canSend(player, S2C_DEATH_WAIT)) {
            ServerPlayNetworking.send(player, new DeathWaitPayload(visible, remainingSeconds, line1, line2));
        }
    }

    public static void sendCompassMenu(ServerPlayerEntity hunter, List<CompassTargetEntry> entries, boolean nearestSelected) {
        if (ServerPlayNetworking.canSend(hunter, S2C_COMPASS_MENU)) {
            ServerPlayNetworking.send(hunter, new CompassMenuPayload(entries, nearestSelected));
        }
    }

    public static void sendObjectiveNotice(GameContext context, String message, String style) {
        sendObjectiveNotice(context.getServer(), message, style);
    }

    public static void sendObjectiveNotice(MinecraftServer server, String message, String style) {
        if (server == null) {
            return;
        }

        ObjectiveNoticePayload payload = new ObjectiveNoticePayload(message, style);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_OBJECTIVE_NOTICE)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendWeaponOverheatStatus(ServerPlayerEntity player, int heat, int maxHeat) {
        if (ServerPlayNetworking.canSend(player, S2C_WEAPON_OVERHEAT_STATUS)) {
            ServerPlayNetworking.send(player, new WeaponOverheatStatusPayload(Math.max(0, heat), Math.max(1, maxHeat), true));
        }
    }

    public static void clearWeaponOverheatStatus(ServerPlayerEntity player) {
        if (ServerPlayNetworking.canSend(player, S2C_WEAPON_OVERHEAT_STATUS)) {
            ServerPlayNetworking.send(player, new WeaponOverheatStatusPayload(0, 1, false));
        }
    }

    public static void sendBackroomsPhase(ServerPlayerEntity player, BackroomsPhase phase, int holdTicks) {
        if (ServerPlayNetworking.canSend(player, S2C_BACKROOMS_PHASE)) {
            ServerPlayNetworking.send(player, new BackroomsPhasePayload(phase, holdTicks));
        }
    }

    public static void sendWorldTilt(ServerPlayerEntity player, boolean active, int transitionTicks, double gravityX, double gravityZ) {
        if (ServerPlayNetworking.canSend(player, S2C_WORLD_TILT)) {
            ServerPlayNetworking.send(player, new WorldTiltPayload(active, transitionTicks, gravityX, gravityZ));
        }
    }

    public static void sendKeyScramble(ServerPlayerEntity player, KeyScrambleAction action) {
        if (ServerPlayNetworking.canSend(player, S2C_KEY_SCRAMBLE)) {
            ServerPlayNetworking.send(player, new KeyScramblePayload(action));
        }
    }

    private static SyncConfigPayload createSyncPayload(ServerPlayerEntity player) {
        GameManager manager = GameManager.getInstance();
        String activeWildcard = manager.getWildcardManager().getActiveRuleName();
        if (activeWildcard == null) {
            activeWildcard = "";
        }

        PlayerRole playerRole = manager.getTeamManager().getRole(player);
        boolean canManage = HunterWildcardCommand.canManageGame(player.getCommandSource());

        return new SyncConfigPayload(
                manager.getState(),
                manager.getTeamManager().count(PlayerRole.HUNTER),
                manager.getTeamManager().count(PlayerRole.RUNNER),
                activeWildcard,
                playerRole == null ? HunterWildcardText.key("role.not_joined") : playerRole.getDisplayName(),
                playerRole != null,
                manager.getWildcardManager().hasRuleInProgress(),
                ticksToSeconds(manager.getPhaseRemainingTicks()),
                ticksToSeconds(manager.getActiveWildcardRemainingTicks()),
                ticksToSeconds(manager.getTicksUntilNextWildcard()),
                canManage,
                canManage && manager.isDebugMenuEnabled(player),
                ConfigSnapshot.from(manager.getConfig())
        );
    }

    private static void handleUpdateConfig(ServerPlayerEntity player, ConfigSnapshot snapshot) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (manager.getState() != GameState.WAITING) {
            // Mid-round only the live-safe subset is applied; everyone is told the rules changed.
            manager.applyLiveConfig(snapshot.toConfig());
            manager.saveConfig();
            String message = HunterWildcardText.spec("msg.config.rules_updated");
            manager.getMessageManager().broadcast(player.getEntityWorld().getServer(),
                    HunterWildcardText.translatable("msg.config.rules_updated_by", PlayerUtil.displayNameText(player)));
            syncAllAndResult(player, true, message);
            return;
        }

        manager.applyConfig(snapshot.toConfig());
        if (manager.saveConfig()) {
            String message = HunterWildcardText.spec("msg.config.saved");
            manager.getMessageManager().directSpec(player, message);
            syncAllAndResult(player, true, message);
        } else {
            String message = HunterWildcardText.spec("msg.config.save_failed");
            player.sendMessage(HunterWildcardText.fromSpec(message), false);
            syncAllAndResult(player, false, message);
        }
    }

    private static void handleReloadConfig(ServerPlayerEntity player) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (!manager.reloadConfig()) {
            fail(player, HunterWildcardText.spec("msg.config.cannot_reload_started"));
            return;
        }

        String message = HunterWildcardText.spec("msg.config.reloaded");
        manager.getMessageManager().directSpec(player, message);
        syncAllAndResult(player, true, message);
    }

    private static void handleDebugAction(ServerPlayerEntity player, DebugAction action) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (!manager.isDebugMenuEnabled(player)) {
            fail(player, HunterWildcardText.spec("msg.debug_menu.required"));
            return;
        }

        switch (action) {
            case START_GAME -> {
                String error = validateStartGame(manager);
                if (error != null) {
                    fail(player, error);
                    return;
                }
                manager.start(player.getCommandSource());
                closeConfigScreens(player.getEntityWorld().getServer());
                syncAllAndResult(player, true, HunterWildcardText.spec("msg.game.preparing_started_short"));
            }
            case STOP_GAME -> {
                boolean hadGame = canStopGame(manager);
                manager.stop(player.getCommandSource());
                syncAllAndResult(player, hadGame, hadGame ? HunterWildcardText.spec("msg.game.stopped") : HunterWildcardText.spec("msg.game.none_running"));
            }
            case ROLL_WILDCARD -> {
                if (manager.getState() != GameState.RUNNING) {
                    fail(player, HunterWildcardText.spec("msg.wildcard.roll_requires_running"));
                    return;
                }
                manager.rollWildcard(player.getCommandSource());
                String activeRule = manager.getWildcardManager().getActiveRuleName();
                syncAllAndResult(player, activeRule != null, activeRule == null
                        ? HunterWildcardText.spec("msg.wildcard.none_available")
                        : HunterWildcardText.spec("msg.wildcard.random_triggered", HunterWildcardText.wildcardNameKey(activeRule)));
            }
            case STOP_WILDCARD -> {
                boolean hadWildcard = manager.getWildcardManager().hasRuleInProgress();
                manager.debugStopWildcard(player.getCommandSource());
                syncAllAndResult(player, hadWildcard, hadWildcard ? HunterWildcardText.spec("msg.wildcard.stopped") : HunterWildcardText.spec("msg.wildcard.none_running"));
            }
        }
    }

    private static void handleTestWildcard(ServerPlayerEntity player, String wildcardName) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (!manager.isDebugMenuEnabled(player)) {
            fail(player, HunterWildcardText.spec("msg.debug_menu.required"));
            return;
        }

        manager.testWildcard(player.getCommandSource(), wildcardName, player);
        String activeRule = manager.getWildcardManager().getActiveRuleName();
        boolean success = activeRule != null && activeRule.equals(wildcardName);
        syncAllAndResult(player, success, success
                ? HunterWildcardText.spec("msg.wildcard.test_triggered", HunterWildcardText.wildcardNameKey(wildcardName))
                : HunterWildcardText.spec("msg.wildcard.unavailable_or_disabled", wildcardName));
    }

    private static void handleTeamAction(ServerPlayerEntity player, TeamAction action) {
        GameManager manager = GameManager.getInstance();
        PlayerRole previousRole = manager.getTeamManager().getRole(player);
        boolean canChangeTeam = manager.getState() == GameState.WAITING;
        if (!canChangeTeam) {
            String message = action == TeamAction.LEAVE
                    ? HunterWildcardText.spec("msg.team.cannot_leave_started")
                    : HunterWildcardText.spec("msg.team.cannot_switch_started");
            fail(player, message);
            return;
        }

        switch (action) {
            case JOIN_HUNTER -> manager.join(player, PlayerRole.HUNTER);
            case JOIN_RUNNER -> manager.join(player, PlayerRole.RUNNER);
            case LEAVE -> manager.leave(player);
        }
        switch (action) {
            case JOIN_HUNTER -> syncAllAndResult(player, true, HunterWildcardText.spec("msg.team.joined", PlayerRole.HUNTER.getTranslationKey()));
            case JOIN_RUNNER -> syncAllAndResult(player, true, HunterWildcardText.spec("msg.team.joined", PlayerRole.RUNNER.getTranslationKey()));
            case LEAVE -> syncAllAndResult(player, previousRole != null, previousRole == null
                    ? HunterWildcardText.spec("msg.team.not_in_team")
                    : HunterWildcardText.spec("msg.team.left", previousRole.getTranslationKey()));
        }
    }

    private static void handleGameAction(ServerPlayerEntity player, GameAction action) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        switch (action) {
            case START_GAME -> {
                String error = validateStartGame(manager);
                if (error != null) {
                    fail(player, error);
                    return;
                }
                manager.start(player.getCommandSource());
                closeConfigScreens(player.getEntityWorld().getServer());
                syncAllAndResult(player, true, HunterWildcardText.spec("msg.game.preparing_started_short"));
            }
            case STOP_GAME -> {
                boolean hadGame = canStopGame(manager);
                manager.stop(player.getCommandSource());
                syncAllAndResult(player, hadGame, hadGame ? HunterWildcardText.spec("msg.game.stopped") : HunterWildcardText.spec("msg.game.none_running"));
            }
            case ROLL_WILDCARD -> {
                if (manager.getState() != GameState.RUNNING) {
                    fail(player, HunterWildcardText.spec("msg.wildcard.roll_requires_running"));
                    return;
                }
                manager.rollWildcard(player.getCommandSource());
                String activeRule = manager.getWildcardManager().getActiveRuleName();
                syncAllAndResult(player, activeRule != null, activeRule == null
                        ? HunterWildcardText.spec("msg.wildcard.none_available")
                        : HunterWildcardText.spec("msg.wildcard.random_triggered", HunterWildcardText.wildcardNameKey(activeRule)));
            }
        }
    }

    private static void reject(ServerPlayerEntity player) {
        fail(player, HunterWildcardText.spec("msg.permission.denied"));
    }

    private static void fail(ServerPlayerEntity player, String messageSpec) {
        GameManager.getInstance().getMessageManager().directSpec(player, messageSpec);
        sendSyncAndResult(player, false, messageSpec);
    }

    private static String validateStartGame(GameManager manager) {
        if (manager.getState() != GameState.WAITING) {
            return HunterWildcardText.spec("msg.game.already_started_or_ending");
        }

        if (manager.getTeamManager().count(PlayerRole.HUNTER) == 0 || manager.getTeamManager().count(PlayerRole.RUNNER) == 0) {
            return HunterWildcardText.spec("msg.game.need_teams");
        }

        return null;
    }

    private static boolean canStopGame(GameManager manager) {
        return manager.getState() != GameState.WAITING
                || manager.getTeamManager().count(PlayerRole.HUNTER) > 0
                || manager.getTeamManager().count(PlayerRole.RUNNER) > 0;
    }

    private static int ticksToSeconds(int ticks) {
        return ticks < 0 ? -1 : Math.max(0, (ticks + 19) / 20);
    }

    public enum DebugAction {
        START_GAME,
        STOP_GAME,
        ROLL_WILDCARD,
        STOP_WILDCARD
    }

    public enum TeamAction {
        JOIN_HUNTER,
        JOIN_RUNNER,
        LEAVE
    }

    public enum GameAction {
        START_GAME,
        STOP_GAME,
        ROLL_WILDCARD
    }

    public enum KeyScrambleAction {
        RESTORE,
        ENABLE,
        SHUFFLE
    }

    public enum BackroomsPhase {
        FALL,
        ENTER,
        EXIT,
        CLEAR
    }

    public record ConfigSnapshot(
            int preparingSeconds,
            int endingSeconds,
            int compassUpdateSeconds,
            int hunterRespawnSeconds,
            int wildcardIntervalSeconds,
            int wildcardDurationSeconds,
            String wildcardIntervalMode,
            int wildcardIntervalMinSeconds,
            int wildcardIntervalMaxSeconds,
            String wildcardDurationMode,
            int wildcardDurationMinSeconds,
            int wildcardDurationMaxSeconds,
            int actionBarIntervalSeconds,
            int hunterRadarWarningDistance,
            int supplyDropIntervalSeconds,
            int spaceShiftIntervalSeconds,
            int blockDecaySeconds,
            int pearlFrenzyMaxPearls,
            int pearlFrenzyIntervalSeconds,
            int windChargeBrawlIntervalSeconds,
            int windChargeExplosionMultiplierPercent,
            int backroomsDurationSeconds,
            boolean hunterPrepareBoundaryEnabled,
            int hunterPrepareBoundaryRadius,
            int hunterPrepareBoundaryWarnDistance,
            boolean runnerDeathNoDrops,
            boolean hunterDeathNoDrops,
            boolean piglinPearlBoostEnabled,
            int piglinPearlChancePercent,
            int hunterDamageMultiplierPercent,
            int hunterSpeedPercent,
            int runnerSpeedPercent,
            int hunterHitCreditSeconds,
            int environmentDeathsPerKill,
            boolean randomRespawnEnabled,
            int runnerRespawnDistance,
            int hunterRespawnDistance,
            int hunterRespawnRunnerClearance,
            int hunterRespawnPenaltySeconds,
            boolean locatorBarTeamOnly,
            boolean surviveBorderEnabled,
            int surviveBorderRadius,
            String runnerVictoryType,
            String runnerWinMode,
            boolean enableDragonWin,
            boolean enableSurviveTimeWin,
            int surviveTimeSeconds,
            boolean enableReachLocationWin,
            String targetDimension,
            int targetX,
            int targetY,
            int targetZ,
            int targetRadius,
            boolean enableCollectItemWin,
            String targetItemId,
            int targetItemCount,
            String hunterRespawnMode,
            int hunterLives,
            String runnerRespawnMode,
            int runnerLives,
            int runnerRespawnSeconds,
            String runnerTeamLossMode,
            String hunterVictoryType,
            boolean hunterWinByRunnerKillsEnabled,
            int hunterRunnerKillTarget,
            Map<String, Boolean> enabledWildcards
    ) {
        private static ConfigSnapshot fromBuf(RegistryByteBuf buf) {
            return new ConfigSnapshot(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readString(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readString(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readString(32),
                    buf.readString(32),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readString(128),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readString(128),
                    buf.readInt(),
                    buf.readString(32),
                    buf.readInt(),
                    buf.readString(32),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readString(32),
                    buf.readString(32),
                    buf.readBoolean(),
                    buf.readInt(),
                    readWildcardToggles(buf)
            );
        }

        private void write(RegistryByteBuf buf) {
            buf.writeInt(preparingSeconds);
            buf.writeInt(endingSeconds);
            buf.writeInt(compassUpdateSeconds);
            buf.writeInt(hunterRespawnSeconds);
            buf.writeInt(wildcardIntervalSeconds);
            buf.writeInt(wildcardDurationSeconds);
            buf.writeString(wildcardIntervalMode);
            buf.writeInt(wildcardIntervalMinSeconds);
            buf.writeInt(wildcardIntervalMaxSeconds);
            buf.writeString(wildcardDurationMode);
            buf.writeInt(wildcardDurationMinSeconds);
            buf.writeInt(wildcardDurationMaxSeconds);
            buf.writeInt(actionBarIntervalSeconds);
            buf.writeInt(hunterRadarWarningDistance);
            buf.writeInt(supplyDropIntervalSeconds);
            buf.writeInt(spaceShiftIntervalSeconds);
            buf.writeInt(blockDecaySeconds);
            buf.writeInt(pearlFrenzyMaxPearls);
            buf.writeInt(pearlFrenzyIntervalSeconds);
            buf.writeInt(windChargeBrawlIntervalSeconds);
            buf.writeInt(windChargeExplosionMultiplierPercent);
            buf.writeInt(backroomsDurationSeconds);
            buf.writeBoolean(hunterPrepareBoundaryEnabled);
            buf.writeInt(hunterPrepareBoundaryRadius);
            buf.writeInt(hunterPrepareBoundaryWarnDistance);
            buf.writeBoolean(runnerDeathNoDrops);
            buf.writeBoolean(hunterDeathNoDrops);
            buf.writeBoolean(piglinPearlBoostEnabled);
            buf.writeInt(piglinPearlChancePercent);
            buf.writeInt(hunterDamageMultiplierPercent);
            buf.writeInt(hunterSpeedPercent);
            buf.writeInt(runnerSpeedPercent);
            buf.writeInt(hunterHitCreditSeconds);
            buf.writeInt(environmentDeathsPerKill);
            buf.writeBoolean(randomRespawnEnabled);
            buf.writeInt(runnerRespawnDistance);
            buf.writeInt(hunterRespawnDistance);
            buf.writeInt(hunterRespawnRunnerClearance);
            buf.writeInt(hunterRespawnPenaltySeconds);
            buf.writeBoolean(locatorBarTeamOnly);
            buf.writeBoolean(surviveBorderEnabled);
            buf.writeInt(surviveBorderRadius);
            buf.writeString(runnerVictoryType);
            buf.writeString(runnerWinMode);
            buf.writeBoolean(enableDragonWin);
            buf.writeBoolean(enableSurviveTimeWin);
            buf.writeInt(surviveTimeSeconds);
            buf.writeBoolean(enableReachLocationWin);
            buf.writeString(targetDimension);
            buf.writeInt(targetX);
            buf.writeInt(targetY);
            buf.writeInt(targetZ);
            buf.writeInt(targetRadius);
            buf.writeBoolean(enableCollectItemWin);
            buf.writeString(targetItemId);
            buf.writeInt(targetItemCount);
            buf.writeString(hunterRespawnMode);
            buf.writeInt(hunterLives);
            buf.writeString(runnerRespawnMode);
            buf.writeInt(runnerLives);
            buf.writeInt(runnerRespawnSeconds);
            buf.writeString(runnerTeamLossMode);
            buf.writeString(hunterVictoryType);
            buf.writeBoolean(hunterWinByRunnerKillsEnabled);
            buf.writeInt(hunterRunnerKillTarget);
            writeWildcardToggles(buf, enabledWildcards);
        }

        public static ConfigSnapshot from(ModConfig config) {
            return new ConfigSnapshot(
                    config.preparingSeconds,
                    config.endingSeconds,
                    config.compassUpdateSeconds,
                    config.hunterRespawnSeconds,
                    config.wildcardIntervalSeconds,
                    config.wildcardDurationSeconds,
                    config.wildcardIntervalMode,
                    config.wildcardIntervalMinSeconds,
                    config.wildcardIntervalMaxSeconds,
                    config.wildcardDurationMode,
                    config.wildcardDurationMinSeconds,
                    config.wildcardDurationMaxSeconds,
                    config.actionBarIntervalSeconds,
                    config.hunterRadarWarningDistance,
                    config.supplyDropIntervalSeconds,
                    config.spaceShiftIntervalSeconds,
                    config.blockDecaySeconds,
                    config.pearlFrenzyMaxPearls,
                    config.pearlFrenzyIntervalSeconds,
                    config.windChargeBrawlIntervalSeconds,
                    config.windChargeExplosionMultiplierPercent,
                    config.backroomsDurationSeconds,
                    config.hunterPrepareBoundaryEnabled,
                    config.hunterPrepareBoundaryRadius,
                    config.hunterPrepareBoundaryWarnDistance,
                    config.runnerDeathNoDrops,
                    config.hunterDeathNoDrops,
                    config.piglinPearlBoostEnabled,
                    config.piglinPearlChancePercent,
                    config.hunterDamageMultiplierPercent,
                    config.hunterSpeedPercent,
                    config.runnerSpeedPercent,
                    config.hunterHitCreditSeconds,
                    config.environmentDeathsPerKill,
                    config.randomRespawnEnabled,
                    config.runnerRespawnDistance,
                    config.hunterRespawnDistance,
                    config.hunterRespawnRunnerClearance,
                    config.hunterRespawnPenaltySeconds,
                    config.locatorBarTeamOnly,
                    config.surviveBorderEnabled,
                    config.surviveBorderRadius,
                    config.runnerVictoryType,
                    config.runnerWinMode,
                    config.enableDragonWin,
                    config.enableSurviveTimeWin,
                    config.surviveTimeSeconds,
                    config.enableReachLocationWin,
                    config.targetDimension,
                    config.targetX,
                    config.targetY,
                    config.targetZ,
                    config.targetRadius,
                    config.enableCollectItemWin,
                    config.targetItemId,
                    config.targetItemCount,
                    config.hunterRespawnMode,
                    config.hunterLives,
                    config.runnerRespawnMode,
                    config.runnerLives,
                    config.runnerRespawnSeconds,
                    config.runnerTeamLossMode,
                    config.hunterVictoryType,
                    config.hunterWinByRunnerKillsEnabled,
                    config.hunterRunnerKillTarget,
                    new LinkedHashMap<>(config.enabledWildcards)
            );
        }

        public ModConfig toConfig() {
            ModConfig config = new ModConfig();
            config.preparingSeconds = preparingSeconds;
            config.endingSeconds = endingSeconds;
            config.compassUpdateSeconds = compassUpdateSeconds;
            config.hunterRespawnSeconds = hunterRespawnSeconds;
            config.wildcardIntervalSeconds = wildcardIntervalSeconds;
            config.wildcardDurationSeconds = wildcardDurationSeconds;
            config.wildcardIntervalMode = wildcardIntervalMode;
            config.wildcardIntervalMinSeconds = wildcardIntervalMinSeconds;
            config.wildcardIntervalMaxSeconds = wildcardIntervalMaxSeconds;
            config.wildcardDurationMode = wildcardDurationMode;
            config.wildcardDurationMinSeconds = wildcardDurationMinSeconds;
            config.wildcardDurationMaxSeconds = wildcardDurationMaxSeconds;
            config.actionBarIntervalSeconds = actionBarIntervalSeconds;
            config.hunterRadarWarningDistance = hunterRadarWarningDistance;
            config.supplyDropIntervalSeconds = supplyDropIntervalSeconds;
            config.spaceShiftIntervalSeconds = spaceShiftIntervalSeconds;
            config.blockDecaySeconds = blockDecaySeconds;
            config.pearlFrenzyMaxPearls = pearlFrenzyMaxPearls;
            config.pearlFrenzyIntervalSeconds = pearlFrenzyIntervalSeconds;
            config.windChargeBrawlIntervalSeconds = windChargeBrawlIntervalSeconds;
            config.windChargeExplosionMultiplierPercent = windChargeExplosionMultiplierPercent;
            config.backroomsDurationSeconds = backroomsDurationSeconds;
            config.hunterPrepareBoundaryEnabled = hunterPrepareBoundaryEnabled;
            config.hunterPrepareBoundaryRadius = hunterPrepareBoundaryRadius;
            config.hunterPrepareBoundaryWarnDistance = hunterPrepareBoundaryWarnDistance;
            config.runnerDeathNoDrops = runnerDeathNoDrops;
            config.hunterDeathNoDrops = hunterDeathNoDrops;
            config.piglinPearlBoostEnabled = piglinPearlBoostEnabled;
            config.piglinPearlChancePercent = piglinPearlChancePercent;
            config.hunterDamageMultiplierPercent = hunterDamageMultiplierPercent;
            config.hunterSpeedPercent = hunterSpeedPercent;
            config.runnerSpeedPercent = runnerSpeedPercent;
            config.hunterHitCreditSeconds = hunterHitCreditSeconds;
            config.environmentDeathsPerKill = environmentDeathsPerKill;
            config.randomRespawnEnabled = randomRespawnEnabled;
            config.runnerRespawnDistance = runnerRespawnDistance;
            config.hunterRespawnDistance = hunterRespawnDistance;
            config.hunterRespawnRunnerClearance = hunterRespawnRunnerClearance;
            config.hunterRespawnPenaltySeconds = hunterRespawnPenaltySeconds;
            config.locatorBarTeamOnly = locatorBarTeamOnly;
            config.surviveBorderEnabled = surviveBorderEnabled;
            config.surviveBorderRadius = surviveBorderRadius;
            config.runnerVictoryType = runnerVictoryType;
            config.runnerWinMode = runnerWinMode;
            config.enableDragonWin = enableDragonWin;
            config.enableSurviveTimeWin = enableSurviveTimeWin;
            config.surviveTimeSeconds = surviveTimeSeconds;
            config.enableReachLocationWin = enableReachLocationWin;
            config.targetDimension = targetDimension;
            config.targetX = targetX;
            config.targetY = targetY;
            config.targetZ = targetZ;
            config.targetRadius = targetRadius;
            config.enableCollectItemWin = enableCollectItemWin;
            config.targetItemId = targetItemId;
            config.targetItemCount = targetItemCount;
            config.hunterRespawnMode = hunterRespawnMode;
            config.hunterLives = hunterLives;
            config.runnerRespawnMode = runnerRespawnMode;
            config.runnerLives = runnerLives;
            config.runnerRespawnSeconds = runnerRespawnSeconds;
            config.runnerTeamLossMode = runnerTeamLossMode;
            config.hunterVictoryType = hunterVictoryType;
            config.hunterWinByRunnerKillsEnabled = hunterWinByRunnerKillsEnabled;
            config.hunterRunnerKillTarget = hunterRunnerKillTarget;
            config.enabledWildcards = new LinkedHashMap<>(enabledWildcards);
            config.validate();
            return config;
        }
    }

    private static Map<String, Boolean> readWildcardToggles(RegistryByteBuf buf) {
        int count = buf.readVarInt();
        Map<String, Boolean> toggles = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String id = buf.readString();
            toggles.put(id, buf.readBoolean());
        }
        return toggles;
    }

    private static void writeWildcardToggles(RegistryByteBuf buf, Map<String, Boolean> toggles) {
        buf.writeVarInt(toggles.size());
        for (Map.Entry<String, Boolean> entry : toggles.entrySet()) {
            buf.writeString(entry.getKey());
            buf.writeBoolean(Boolean.TRUE.equals(entry.getValue()));
        }
    }

    public record RequestConfigPayload() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, RequestConfigPayload> CODEC =
                PacketCodec.of(RequestConfigPayload::write, RequestConfigPayload::read);

        private void write(RegistryByteBuf buf) {
        }

        private static RequestConfigPayload read(RegistryByteBuf buf) {
            return new RequestConfigPayload();
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_REQUEST_CONFIG;
        }
    }

    public record SyncConfigPayload(
            GameState gameState,
            int hunterCount,
            int runnerCount,
            String activeWildcard,
            String playerRole,
            boolean playerInTeam,
            boolean activeWildcardRunning,
            int phaseRemainingSeconds,
            int activeWildcardRemainingSeconds,
            int nextWildcardSeconds,
            boolean canManage,
            boolean debugPageEnabled,
            ConfigSnapshot config
    ) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, SyncConfigPayload> CODEC =
                PacketCodec.of(SyncConfigPayload::write, SyncConfigPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeEnumConstant(gameState);
            buf.writeInt(hunterCount);
            buf.writeInt(runnerCount);
            buf.writeString(activeWildcard);
            buf.writeString(playerRole);
            buf.writeBoolean(playerInTeam);
            buf.writeBoolean(activeWildcardRunning);
            buf.writeInt(phaseRemainingSeconds);
            buf.writeInt(activeWildcardRemainingSeconds);
            buf.writeInt(nextWildcardSeconds);
            buf.writeBoolean(canManage);
            buf.writeBoolean(debugPageEnabled);
            config.write(buf);
        }

        private static SyncConfigPayload read(RegistryByteBuf buf) {
            return new SyncConfigPayload(
                    buf.readEnumConstant(GameState.class),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readString(128),
                    buf.readString(64),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    ConfigSnapshot.fromBuf(buf)
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_SYNC_CONFIG;
        }
    }

    public record OperationResultPayload(boolean success, String message) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, OperationResultPayload> CODEC =
                PacketCodec.of(OperationResultPayload::write, OperationResultPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(success);
            buf.writeString(message);
        }

        private static OperationResultPayload read(RegistryByteBuf buf) {
            return new OperationResultPayload(buf.readBoolean(), buf.readString(256));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_OPERATION_RESULT;
        }
    }

    public record CloseConfigScreenPayload() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, CloseConfigScreenPayload> CODEC =
                PacketCodec.of(CloseConfigScreenPayload::write, CloseConfigScreenPayload::read);

        private void write(RegistryByteBuf buf) {
        }

        private static CloseConfigScreenPayload read(RegistryByteBuf buf) {
            return new CloseConfigScreenPayload();
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_CLOSE_CONFIG_SCREEN;
        }
    }

    public record ClearChatPayload() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, ClearChatPayload> CODEC =
                PacketCodec.of(ClearChatPayload::write, ClearChatPayload::read);

        private void write(RegistryByteBuf buf) {
        }

        private static ClearChatPayload read(RegistryByteBuf buf) {
            return new ClearChatPayload();
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_CLEAR_CHAT;
        }
    }

    public record WildcardDrawPayload(String wildcardName) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, WildcardDrawPayload> CODEC =
                PacketCodec.of(WildcardDrawPayload::write, WildcardDrawPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(wildcardName);
        }

        private static WildcardDrawPayload read(RegistryByteBuf buf) {
            return new WildcardDrawPayload(buf.readString(64));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_WILDCARD_DRAW;
        }
    }

    public record WildcardIntroPayload(String wildcardName, String description, boolean visible) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, WildcardIntroPayload> CODEC =
                PacketCodec.of(WildcardIntroPayload::write, WildcardIntroPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(wildcardName == null ? "" : wildcardName);
            buf.writeString(description == null ? "" : description);
            buf.writeBoolean(visible);
        }

        private static WildcardIntroPayload read(RegistryByteBuf buf) {
            return new WildcardIntroPayload(
                    buf.readString(64),
                    buf.readString(160),
                    buf.readBoolean()
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_WILDCARD_INTRO;
        }
    }

    public record HunterKillFeedbackPayload(String hunterName, String runnerName, int remainingKills, int currentKills, int targetKills) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, HunterKillFeedbackPayload> CODEC =
                PacketCodec.of(HunterKillFeedbackPayload::write, HunterKillFeedbackPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(hunterName);
            buf.writeString(runnerName);
            buf.writeInt(remainingKills);
            buf.writeInt(currentKills);
            buf.writeInt(targetKills);
        }

        private static HunterKillFeedbackPayload read(RegistryByteBuf buf) {
            return new HunterKillFeedbackPayload(
                    buf.readString(64),
                    buf.readString(64),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_HUNTER_KILL_FEEDBACK;
        }
    }

    public record HudFeedbackPayload(String title, String line1, String line2, String style) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, HudFeedbackPayload> CODEC =
                PacketCodec.of(HudFeedbackPayload::write, HudFeedbackPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(title == null ? "" : title);
            buf.writeString(line1 == null ? "" : line1);
            buf.writeString(line2 == null ? "" : line2);
            buf.writeString(style == null ? "" : style);
        }

        private static HudFeedbackPayload read(RegistryByteBuf buf) {
            return new HudFeedbackPayload(
                    buf.readString(64),
                    buf.readString(128),
                    buf.readString(128),
                    buf.readString(32)
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_HUD_FEEDBACK;
        }
    }

    /** Left-hand objective panel: the runner objective line plus a hunter progress line (either may be empty). */
    public record ObjectiveStatusPayload(boolean visible, String text, String style, String hunterText) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, ObjectiveStatusPayload> CODEC =
                PacketCodec.of(ObjectiveStatusPayload::write, ObjectiveStatusPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(visible);
            buf.writeString(text == null ? "" : text);
            buf.writeString(style == null ? "" : style);
            buf.writeString(hunterText == null ? "" : hunterText);
        }

        private static ObjectiveStatusPayload read(RegistryByteBuf buf) {
            return new ObjectiveStatusPayload(
                    buf.readBoolean(),
                    buf.readString(192),
                    buf.readString(32),
                    buf.readString(256)
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_OBJECTIVE_STATUS;
        }
    }

    public record ObjectiveNoticePayload(String message, String style) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, ObjectiveNoticePayload> CODEC =
                PacketCodec.of(ObjectiveNoticePayload::write, ObjectiveNoticePayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(message == null ? "" : message);
            buf.writeString(style == null ? "" : style);
        }

        private static ObjectiveNoticePayload read(RegistryByteBuf buf) {
            return new ObjectiveNoticePayload(
                    buf.readString(192),
                    buf.readString(32)
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_OBJECTIVE_NOTICE;
        }
    }

    public record WeaponOverheatStatusPayload(int heat, int maxHeat, boolean visible) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, WeaponOverheatStatusPayload> CODEC =
                PacketCodec.of(WeaponOverheatStatusPayload::write, WeaponOverheatStatusPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeInt(heat);
            buf.writeInt(maxHeat);
            buf.writeBoolean(visible);
        }

        private static WeaponOverheatStatusPayload read(RegistryByteBuf buf) {
            return new WeaponOverheatStatusPayload(buf.readInt(), buf.readInt(), buf.readBoolean());
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_WEAPON_OVERHEAT_STATUS;
        }
    }

    public record BackroomsPhasePayload(BackroomsPhase phase, int holdTicks) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, BackroomsPhasePayload> CODEC =
                PacketCodec.of(BackroomsPhasePayload::write, BackroomsPhasePayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeEnumConstant(phase);
            buf.writeInt(holdTicks);
        }

        private static BackroomsPhasePayload read(RegistryByteBuf buf) {
            return new BackroomsPhasePayload(buf.readEnumConstant(BackroomsPhase.class), buf.readInt());
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_BACKROOMS_PHASE;
        }
    }

    public record KeyScramblePayload(KeyScrambleAction action) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, KeyScramblePayload> CODEC =
                PacketCodec.of(KeyScramblePayload::write, KeyScramblePayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeEnumConstant(action);
        }

        private static KeyScramblePayload read(RegistryByteBuf buf) {
            return new KeyScramblePayload(buf.readEnumConstant(KeyScrambleAction.class));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_KEY_SCRAMBLE;
        }
    }

    public record UpdateConfigPayload(ConfigSnapshot config) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, UpdateConfigPayload> CODEC =
                PacketCodec.of(UpdateConfigPayload::write, UpdateConfigPayload::read);

        private void write(RegistryByteBuf buf) {
            config.write(buf);
        }

        private static UpdateConfigPayload read(RegistryByteBuf buf) {
            return new UpdateConfigPayload(ConfigSnapshot.fromBuf(buf));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_UPDATE_CONFIG;
        }
    }

    public record ReloadConfigPayload() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, ReloadConfigPayload> CODEC =
                PacketCodec.of(ReloadConfigPayload::write, ReloadConfigPayload::read);

        private void write(RegistryByteBuf buf) {
        }

        private static ReloadConfigPayload read(RegistryByteBuf buf) {
            return new ReloadConfigPayload();
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_RELOAD_CONFIG;
        }
    }

    public record DebugActionPayload(DebugAction action) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, DebugActionPayload> CODEC =
                PacketCodec.of(DebugActionPayload::write, DebugActionPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeEnumConstant(action);
        }

        private static DebugActionPayload read(RegistryByteBuf buf) {
            return new DebugActionPayload(buf.readEnumConstant(DebugAction.class));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_DEBUG_ACTION;
        }
    }

    public record TestWildcardPayload(String wildcardName) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, TestWildcardPayload> CODEC =
                PacketCodec.of(TestWildcardPayload::write, TestWildcardPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(wildcardName);
        }

        private static TestWildcardPayload read(RegistryByteBuf buf) {
            return new TestWildcardPayload(buf.readString(64));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_TEST_WILDCARD;
        }
    }

    public record WorldTiltPayload(boolean active, int transitionTicks, double gravityX, double gravityZ) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, WorldTiltPayload> CODEC =
                PacketCodec.of(WorldTiltPayload::write, WorldTiltPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(active);
            buf.writeVarInt(transitionTicks);
            buf.writeDouble(gravityX);
            buf.writeDouble(gravityZ);
        }

        private static WorldTiltPayload read(RegistryByteBuf buf) {
            return new WorldTiltPayload(buf.readBoolean(), buf.readVarInt(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_WORLD_TILT;
        }
    }

    public record TeamActionPayload(TeamAction action) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, TeamActionPayload> CODEC =
                PacketCodec.of(TeamActionPayload::write, TeamActionPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeEnumConstant(action);
        }

        private static TeamActionPayload read(RegistryByteBuf buf) {
            return new TeamActionPayload(buf.readEnumConstant(TeamAction.class));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_TEAM_ACTION;
        }
    }

    public record DeathWaitPayload(boolean visible, int remainingSeconds, String line1, String line2) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, DeathWaitPayload> CODEC =
                PacketCodec.of(DeathWaitPayload::write, DeathWaitPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(visible);
            buf.writeInt(remainingSeconds);
            buf.writeString(line1 == null ? "" : line1);
            buf.writeString(line2 == null ? "" : line2);
        }

        private static DeathWaitPayload read(RegistryByteBuf buf) {
            return new DeathWaitPayload(buf.readBoolean(), buf.readInt(), buf.readString(256), buf.readString(256));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_DEATH_WAIT;
        }
    }

    /** One runner in the hunter's compass menu. distance is -1 when in another dimension. */
    public record CompassTargetEntry(UUID playerId, String nameSpec, int distance, boolean sameDimension, boolean selected) {
        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerId);
            buf.writeString(nameSpec);
            buf.writeInt(distance);
            buf.writeBoolean(sameDimension);
            buf.writeBoolean(selected);
        }

        private static CompassTargetEntry read(RegistryByteBuf buf) {
            return new CompassTargetEntry(buf.readUuid(), buf.readString(128), buf.readInt(), buf.readBoolean(), buf.readBoolean());
        }
    }

    public record CompassMenuPayload(List<CompassTargetEntry> entries, boolean nearestSelected) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, CompassMenuPayload> CODEC =
                PacketCodec.of(CompassMenuPayload::write, CompassMenuPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeVarInt(entries.size());
            for (CompassTargetEntry entry : entries) {
                entry.write(buf);
            }
            buf.writeBoolean(nearestSelected);
        }

        private static CompassMenuPayload read(RegistryByteBuf buf) {
            int count = Math.min(buf.readVarInt(), 256);
            List<CompassTargetEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                entries.add(CompassTargetEntry.read(buf));
            }
            return new CompassMenuPayload(entries, buf.readBoolean());
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_COMPASS_MENU;
        }
    }

    /** nearest=true asks for automatic nearest-runner tracking; otherwise targetId names the runner. */
    public record CompassSelectPayload(boolean nearest, UUID targetId) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, CompassSelectPayload> CODEC =
                PacketCodec.of(CompassSelectPayload::write, CompassSelectPayload::read);

        public static CompassSelectPayload trackNearest() {
            return new CompassSelectPayload(true, new UUID(0L, 0L));
        }

        public static CompassSelectPayload track(UUID targetId) {
            return new CompassSelectPayload(false, targetId);
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(nearest);
            buf.writeUuid(targetId);
        }

        private static CompassSelectPayload read(RegistryByteBuf buf) {
            return new CompassSelectPayload(buf.readBoolean(), buf.readUuid());
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_COMPASS_SELECT;
        }
    }

    public record GameActionPayload(GameAction action) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, GameActionPayload> CODEC =
                PacketCodec.of(GameActionPayload::write, GameActionPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeEnumConstant(action);
        }

        private static GameActionPayload read(RegistryByteBuf buf) {
            return new GameActionPayload(buf.readEnumConstant(GameAction.class));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_GAME_ACTION;
        }
    }
}
