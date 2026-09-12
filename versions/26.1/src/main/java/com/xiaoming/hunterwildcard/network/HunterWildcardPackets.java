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
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HunterWildcardPackets {
    // Bump this whenever a payload layout changes incompatibly, even within a mod release.
    public static final int PROTOCOL_VERSION = 3;

    private static Identifier protocolId(String name) {
        return Identifier.fromNamespaceAndPath(HunterWildcardMod.MOD_ID, "v" + PROTOCOL_VERSION + "/" + name);
    }

    public static boolean hasIncompatibleProtocol(Set<Identifier> channels, Identifier requiredChannel) {
        return !channels.contains(requiredChannel)
                && channels.stream().anyMatch(id -> id.getNamespace().equals(HunterWildcardMod.MOD_ID));
    }

    public static Component incompatibleProtocolMessage() {
        // The fallback also reaches older clients that do not have this translation key.
        return Component.translatableWithFallback("hunterwildcard.network.incompatible",
                "外卡追逃网络协议不兼容。请将客户端和服务端更新为同一份新版模组。\n"
                        + "Manhunt Wildcard protocol mismatch. Install the same new mod JAR on both client and server.");
    }

    public static final CustomPacketPayload.Type<RequestConfigPayload> C2S_REQUEST_CONFIG =
            new CustomPacketPayload.Type<>(protocolId("request_config"));
    public static final CustomPacketPayload.Type<SyncConfigPayload> S2C_SYNC_CONFIG =
            new CustomPacketPayload.Type<>(protocolId("sync_config"));
    public static final CustomPacketPayload.Type<OperationResultPayload> S2C_OPERATION_RESULT =
            new CustomPacketPayload.Type<>(protocolId("operation_result"));
    public static final CustomPacketPayload.Type<CloseConfigScreenPayload> S2C_CLOSE_CONFIG_SCREEN =
            new CustomPacketPayload.Type<>(protocolId("close_config_screen"));
    public static final CustomPacketPayload.Type<ClearChatPayload> S2C_CLEAR_CHAT =
            new CustomPacketPayload.Type<>(protocolId("clear_chat"));
    public static final CustomPacketPayload.Type<WildcardDrawPayload> S2C_WILDCARD_DRAW =
            new CustomPacketPayload.Type<>(protocolId("wildcard_draw"));
    public static final CustomPacketPayload.Type<WildcardIntroPayload> S2C_WILDCARD_INTRO =
            new CustomPacketPayload.Type<>(protocolId("wildcard_intro"));
    public static final CustomPacketPayload.Type<HunterKillFeedbackPayload> S2C_HUNTER_KILL_FEEDBACK =
            new CustomPacketPayload.Type<>(protocolId("hunter_kill_feedback"));
    public static final CustomPacketPayload.Type<HudFeedbackPayload> S2C_HUD_FEEDBACK =
            new CustomPacketPayload.Type<>(protocolId("hud_feedback"));
    public static final CustomPacketPayload.Type<ObjectiveStatusPayload> S2C_OBJECTIVE_STATUS =
            new CustomPacketPayload.Type<>(protocolId("objective_status"));
    public static final CustomPacketPayload.Type<ObjectiveNoticePayload> S2C_OBJECTIVE_NOTICE =
            new CustomPacketPayload.Type<>(protocolId("objective_notice"));
    public static final CustomPacketPayload.Type<WeaponOverheatStatusPayload> S2C_WEAPON_OVERHEAT_STATUS =
            new CustomPacketPayload.Type<>(protocolId("weapon_overheat_status"));
    public static final CustomPacketPayload.Type<WorldTiltPayload> S2C_WORLD_TILT =
            new CustomPacketPayload.Type<>(protocolId("world_tilt"));
    public static final CustomPacketPayload.Type<KeyScramblePayload> S2C_KEY_SCRAMBLE =
            new CustomPacketPayload.Type<>(protocolId("key_scramble"));
    public static final CustomPacketPayload.Type<BackroomsPhasePayload> S2C_BACKROOMS_PHASE =
            new CustomPacketPayload.Type<>(protocolId("backrooms_phase"));
    public static final CustomPacketPayload.Type<UpdateConfigPayload> C2S_UPDATE_CONFIG =
            new CustomPacketPayload.Type<>(protocolId("update_config"));
    public static final CustomPacketPayload.Type<ReloadConfigPayload> C2S_RELOAD_CONFIG =
            new CustomPacketPayload.Type<>(protocolId("reload_config"));
    public static final CustomPacketPayload.Type<DebugActionPayload> C2S_DEBUG_ACTION =
            new CustomPacketPayload.Type<>(protocolId("debug_action"));
    public static final CustomPacketPayload.Type<TestWildcardPayload> C2S_TEST_WILDCARD =
            new CustomPacketPayload.Type<>(protocolId("test_wildcard"));
    public static final CustomPacketPayload.Type<TeamActionPayload> C2S_TEAM_ACTION =
            new CustomPacketPayload.Type<>(protocolId("team_action"));
    public static final CustomPacketPayload.Type<GameActionPayload> C2S_GAME_ACTION =
            new CustomPacketPayload.Type<>(protocolId("game_action"));

    public static final CustomPacketPayload.Type<DeathWaitPayload> S2C_DEATH_WAIT =
            new CustomPacketPayload.Type<>(protocolId("death_wait"));
    public static final CustomPacketPayload.Type<DeathSpectatePayload> S2C_DEATH_SPECTATE =
            new CustomPacketPayload.Type<>(protocolId("death_spectate"));
    public static final CustomPacketPayload.Type<CycleDeathSpectatePayload> C2S_CYCLE_DEATH_SPECTATE =
            new CustomPacketPayload.Type<>(protocolId("cycle_death_spectate"));
    public static final CustomPacketPayload.Type<CompassMenuPayload> S2C_COMPASS_MENU =
            new CustomPacketPayload.Type<>(protocolId("compass_menu"));
    public static final CustomPacketPayload.Type<CompassSelectPayload> C2S_COMPASS_SELECT =
            new CustomPacketPayload.Type<>(protocolId("compass_select"));

    public static final CustomPacketPayload.Type<RoundDetailsPayload> S2C_ROUND_DETAILS =
            new CustomPacketPayload.Type<>(protocolId("round_details"));

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
        PayloadTypeRegistry.clientboundPlay().register(S2C_ROUND_DETAILS, RoundDetailsPayload.CODEC);

        PayloadTypeRegistry.serverboundPlay().register(C2S_REQUEST_CONFIG, RequestConfigPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_SYNC_CONFIG, SyncConfigPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_OPERATION_RESULT, OperationResultPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_CLOSE_CONFIG_SCREEN, CloseConfigScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_CLEAR_CHAT, ClearChatPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_WILDCARD_DRAW, WildcardDrawPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_WORLD_TILT, WorldTiltPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_WILDCARD_INTRO, WildcardIntroPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_HUNTER_KILL_FEEDBACK, HunterKillFeedbackPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_HUD_FEEDBACK, HudFeedbackPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_OBJECTIVE_STATUS, ObjectiveStatusPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_OBJECTIVE_NOTICE, ObjectiveNoticePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_WEAPON_OVERHEAT_STATUS, WeaponOverheatStatusPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_KEY_SCRAMBLE, KeyScramblePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_BACKROOMS_PHASE, BackroomsPhasePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2S_UPDATE_CONFIG, UpdateConfigPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2S_RELOAD_CONFIG, ReloadConfigPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2S_DEBUG_ACTION, DebugActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2S_TEST_WILDCARD, TestWildcardPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2S_TEAM_ACTION, TeamActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2S_GAME_ACTION, GameActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_DEATH_WAIT, DeathWaitPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_DEATH_SPECTATE, DeathSpectatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2S_CYCLE_DEATH_SPECTATE, CycleDeathSpectatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2C_COMPASS_MENU, CompassMenuPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2S_COMPASS_SELECT, CompassSelectPayload.CODEC);
    }

    public static void registerServerReceivers() {
        if (serverReceiversRegistered) {
            return;
        }
        serverReceiversRegistered = true;
        ServerPlayNetworking.registerGlobalReceiver(C2S_CYCLE_DEATH_SPECTATE, (payload, context) ->
                GameManager.getInstance().cycleDeathSpectate(context.player(), payload.previous()));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (hasIncompatibleProtocol(ServerPlayNetworking.getSendable(handler), S2C_SYNC_CONFIG.id())) {
                handler.disconnect(incompatibleProtocolMessage());
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_CONFIG, (payload, context) -> sendSync(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_UPDATE_CONFIG, (payload, context) -> handleUpdateConfig(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(C2S_RELOAD_CONFIG, (payload, context) -> handleReloadConfig(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_DEBUG_ACTION, (payload, context) -> handleDebugAction(context.player(), payload.action()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_TEST_WILDCARD, (payload, context) -> handleTestWildcard(context.player(), payload.wildcardName()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_TEAM_ACTION, (payload, context) -> handleTeamAction(context.player(), payload.action()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_GAME_ACTION, (payload, context) -> handleGameAction(context.player(), payload.action()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_COMPASS_SELECT, (payload, context) ->
                GameManager.getInstance().selectCompassTarget(context.player(), payload.nearest() ? null : payload.targetId()));
    }

    public static void sendSync(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, S2C_SYNC_CONFIG)) {
            ServerPlayNetworking.send(player, createSyncPayload(player));
            if (ServerPlayNetworking.canSend(player, S2C_ROUND_DETAILS)) ServerPlayNetworking.send(player, GameManager.getInstance().roundDetails(player, objectiveRunnerText, objectiveHunterText));
        }
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendSync(player);
        }
    }

    public static void closeConfigScreens(MinecraftServer server) {
        if (server == null) {
            return;
        }

        CloseConfigScreenPayload payload = new CloseConfigScreenPayload();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, S2C_CLEAR_CHAT)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void sendOperationResult(ServerPlayer player, boolean success, String message) {
        if (ServerPlayNetworking.canSend(player, S2C_OPERATION_RESULT)) {
            ServerPlayNetworking.send(player, new OperationResultPayload(success, message));
        }
    }

    private static void sendSyncAndResult(ServerPlayer player, boolean success, String message) {
        sendSync(player);
        sendOperationResult(player, success, message);
    }

    private static void syncAllAndResult(ServerPlayer player, boolean success, String message) {
        syncAll(player.level().getServer());
        sendOperationResult(player, success, message);
    }

    public static void sendWildcardDraw(GameContext context, String wildcardName) {
        for (ServerPlayer player : context.getParticipants()) {
            if (ServerPlayNetworking.canSend(player, S2C_WILDCARD_DRAW)) {
                ServerPlayNetworking.send(player, new WildcardDrawPayload(wildcardName));
            }
        }
    }

    public static void sendWildcardIntro(GameContext context, String wildcardId, String descriptionSpec) {
        WildcardIntroPayload payload = new WildcardIntroPayload(wildcardId, descriptionSpec, true);
        for (ServerPlayer player : context.getParticipants()) {
            if (ServerPlayNetworking.canSend(player, S2C_WILDCARD_INTRO)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void clearWildcardIntro(GameContext context) {
        WildcardIntroPayload payload = new WildcardIntroPayload("", "", false);
        for (ServerPlayer player : context.getParticipants()) {
            if (ServerPlayNetworking.canSend(player, S2C_WILDCARD_INTRO)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendHunterKillFeedback(GameContext context, String hunterName, String runnerName, int remainingKills, int currentKills, int targetKills) {
        HunterKillFeedbackPayload payload = new HunterKillFeedbackPayload(hunterName, runnerName, remainingKills, currentKills, targetKills);
        for (ServerPlayer player : context.getServer().getPlayerList().getPlayers()) {
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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
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
    public static void sendObjectiveTo(ServerPlayer player) {
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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, S2C_OBJECTIVE_STATUS)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    /** Full-screen death wait cover: remaining seconds plus two info lines; visible=false clears it. */
    public static void sendDeathSpectate(ServerPlayer player, boolean active, String targetName, int targetCount) {
        if (ServerPlayNetworking.canSend(player, S2C_DEATH_SPECTATE)) {
            ServerPlayNetworking.send(player, new DeathSpectatePayload(active, targetName, targetCount));
        }
    }

    public static void sendDeathWait(ServerPlayer player, boolean visible, int remainingSeconds, String line1, String line2) {
        if (ServerPlayNetworking.canSend(player, S2C_DEATH_WAIT)) {
            ServerPlayNetworking.send(player, new DeathWaitPayload(visible, remainingSeconds, line1, line2));
        }
    }

    public static void sendCompassMenu(ServerPlayer hunter, List<CompassTargetEntry> entries, boolean nearestSelected) {
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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, S2C_OBJECTIVE_NOTICE)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendWeaponOverheatStatus(ServerPlayer player, int heat, int maxHeat) {
        if (ServerPlayNetworking.canSend(player, S2C_WEAPON_OVERHEAT_STATUS)) {
            ServerPlayNetworking.send(player, new WeaponOverheatStatusPayload(Math.max(0, heat), Math.max(1, maxHeat), true));
        }
    }

    public static void clearWeaponOverheatStatus(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, S2C_WEAPON_OVERHEAT_STATUS)) {
            ServerPlayNetworking.send(player, new WeaponOverheatStatusPayload(0, 1, false));
        }
    }

    public static void sendBackroomsPhase(ServerPlayer player, BackroomsPhase phase, int holdTicks) {
        if (ServerPlayNetworking.canSend(player, S2C_BACKROOMS_PHASE)) {
            ServerPlayNetworking.send(player, new BackroomsPhasePayload(phase, holdTicks));
        }
    }

    public static void sendWorldTilt(ServerPlayer player, boolean active, int transitionTicks, double gravityX, double gravityZ) {
        if (ServerPlayNetworking.canSend(player, S2C_WORLD_TILT)) {
            ServerPlayNetworking.send(player, new WorldTiltPayload(active, transitionTicks, gravityX, gravityZ));
        }
    }

    public static void sendKeyScramble(ServerPlayer player, KeyScrambleAction action) {
        if (ServerPlayNetworking.canSend(player, S2C_KEY_SCRAMBLE)) {
            ServerPlayNetworking.send(player, new KeyScramblePayload(action));
        }
    }

    private static SyncConfigPayload createSyncPayload(ServerPlayer player) {
        GameManager manager = GameManager.getInstance();
        String activeWildcard = manager.getWildcardManager().getActiveRuleName();
        if (activeWildcard == null) {
            activeWildcard = "";
        }

        PlayerRole playerRole = manager.getTeamManager().getRole(player);
        boolean canManage = HunterWildcardCommand.canManageGame(player.createCommandSourceStack());

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

    private static void handleUpdateConfig(ServerPlayer player, UpdateConfigPayload request) {
        GameManager manager = GameManager.getInstance();
        if (!HunterWildcardCommand.canManageGame(player.createCommandSourceStack())) {
            saveResult(player, request.requestId(), false, "msg.permission.denied");
            return;
        }
        if (!ConfigSnapshot.from(manager.getConfig()).equals(request.base())) {
            saveResult(player, request.requestId(), false, "ui.save.conflict");
            return;
        }
        boolean live = manager.getState() != GameState.WAITING;
        if (live) manager.applyLiveConfig(request.config().toConfig());
        else manager.applyConfig(request.config().toConfig());
        boolean saved = manager.saveConfig();
        if (live) manager.getMessageManager().broadcast(player.level().getServer(),
                HunterWildcardText.translatable("msg.config.rules_updated_by", PlayerUtil.displayNameText(player)));
        syncAll(player.level().getServer());
        saveResult(player, request.requestId(), saved, saved ? "msg.config.saved" : "ui.save.disk_failed");
    }

    private static void saveResult(ServerPlayer player, long requestId, boolean success, String key) {
        sendSync(player);
        ServerPlayNetworking.send(player, new OperationResultPayload(success, HunterWildcardText.key(key), requestId));
    }

    private static void handleReloadConfig(ServerPlayer player) {
        if (!HunterWildcardCommand.canManageGame(player.createCommandSourceStack())) {
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

    private static void handleDebugAction(ServerPlayer player, DebugAction action) {
        if (!HunterWildcardCommand.canManageGame(player.createCommandSourceStack())) {
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
                manager.start(player.createCommandSourceStack());
                closeConfigScreens(player.level().getServer());
                syncAllAndResult(player, true, HunterWildcardText.spec("msg.game.preparing_started_short"));
            }
            case STOP_GAME -> {
                boolean hadGame = canStopGame(manager);
                manager.stop(player.createCommandSourceStack());
                syncAllAndResult(player, hadGame, hadGame ? HunterWildcardText.spec("msg.game.stopped") : HunterWildcardText.spec("msg.game.none_running"));
            }
            case ROLL_WILDCARD -> {
                if (manager.getState() != GameState.RUNNING) {
                    fail(player, HunterWildcardText.spec("msg.wildcard.roll_requires_running"));
                    return;
                }
                manager.rollWildcard(player.createCommandSourceStack());
                String activeRule = manager.getWildcardManager().getActiveRuleName();
                syncAllAndResult(player, activeRule != null, activeRule == null
                        ? HunterWildcardText.spec("msg.wildcard.none_available")
                        : HunterWildcardText.spec("msg.wildcard.random_triggered", HunterWildcardText.wildcardNameKey(activeRule)));
            }
            case STOP_WILDCARD -> {
                boolean hadWildcard = manager.getWildcardManager().hasRuleInProgress();
                manager.debugStopWildcard(player.createCommandSourceStack());
                syncAllAndResult(player, hadWildcard, hadWildcard ? HunterWildcardText.spec("msg.wildcard.stopped") : HunterWildcardText.spec("msg.wildcard.none_running"));
            }
        }
    }

    private static void handleTestWildcard(ServerPlayer player, String wildcardName) {
        if (!HunterWildcardCommand.canManageGame(player.createCommandSourceStack())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (!manager.isDebugMenuEnabled(player)) {
            fail(player, HunterWildcardText.spec("msg.debug_menu.required"));
            return;
        }

        manager.testWildcard(player.createCommandSourceStack(), wildcardName, player);
        String activeRule = manager.getWildcardManager().getActiveRuleName();
        boolean success = activeRule != null && activeRule.equals(wildcardName);
        syncAllAndResult(player, success, success
                ? HunterWildcardText.spec("msg.wildcard.test_triggered", HunterWildcardText.wildcardNameKey(wildcardName))
                : HunterWildcardText.spec("msg.wildcard.unavailable_or_disabled", wildcardName));
    }

    private static void handleTeamAction(ServerPlayer player, TeamAction action) {
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

    private static void handleGameAction(ServerPlayer player, GameAction action) {
        if (!HunterWildcardCommand.canManageGame(player.createCommandSourceStack())) {
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
                manager.start(player.createCommandSourceStack());
                closeConfigScreens(player.level().getServer());
                syncAllAndResult(player, true, HunterWildcardText.spec("msg.game.preparing_started_short"));
            }
            case STOP_GAME -> {
                boolean hadGame = canStopGame(manager);
                manager.stop(player.createCommandSourceStack());
                syncAllAndResult(player, hadGame, hadGame ? HunterWildcardText.spec("msg.game.stopped") : HunterWildcardText.spec("msg.game.none_running"));
            }
            case ROLL_WILDCARD -> {
                if (manager.getState() != GameState.RUNNING) {
                    fail(player, HunterWildcardText.spec("msg.wildcard.roll_requires_running"));
                    return;
                }
                manager.rollWildcard(player.createCommandSourceStack());
                String activeRule = manager.getWildcardManager().getActiveRuleName();
                syncAllAndResult(player, activeRule != null, activeRule == null
                        ? HunterWildcardText.spec("msg.wildcard.none_available")
                        : HunterWildcardText.spec("msg.wildcard.random_triggered", HunterWildcardText.wildcardNameKey(activeRule)));
            }
        }
    }

    private static void reject(ServerPlayer player) {
        fail(player, HunterWildcardText.spec("msg.permission.denied"));
    }

    private static void fail(ServerPlayer player, String messageSpec) {
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
            boolean environmentKillsEnabled,
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
            boolean blazeRodChanceEnabled,
            int blazeRodChancePercent,
            Map<String, Boolean> enabledWildcards
    ) {
        private static ConfigSnapshot fromBuf(RegistryFriendlyByteBuf buf) {
            return new ConfigSnapshot(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(),
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
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readInt(),
                    readWildcardToggles(buf)
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeInt(preparingSeconds);
            buf.writeInt(endingSeconds);
            buf.writeInt(compassUpdateSeconds);
            buf.writeInt(hunterRespawnSeconds);
            buf.writeInt(wildcardIntervalSeconds);
            buf.writeInt(wildcardDurationSeconds);
            buf.writeUtf(wildcardIntervalMode);
            buf.writeInt(wildcardIntervalMinSeconds);
            buf.writeInt(wildcardIntervalMaxSeconds);
            buf.writeUtf(wildcardDurationMode);
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
            buf.writeBoolean(environmentKillsEnabled);
            buf.writeBoolean(randomRespawnEnabled);
            buf.writeInt(runnerRespawnDistance);
            buf.writeInt(hunterRespawnDistance);
            buf.writeInt(hunterRespawnRunnerClearance);
            buf.writeInt(hunterRespawnPenaltySeconds);
            buf.writeBoolean(locatorBarTeamOnly);
            buf.writeBoolean(surviveBorderEnabled);
            buf.writeInt(surviveBorderRadius);
            buf.writeUtf(runnerVictoryType);
            buf.writeUtf(runnerWinMode);
            buf.writeBoolean(enableDragonWin);
            buf.writeBoolean(enableSurviveTimeWin);
            buf.writeInt(surviveTimeSeconds);
            buf.writeBoolean(enableReachLocationWin);
            buf.writeUtf(targetDimension);
            buf.writeInt(targetX);
            buf.writeInt(targetY);
            buf.writeInt(targetZ);
            buf.writeInt(targetRadius);
            buf.writeBoolean(enableCollectItemWin);
            buf.writeUtf(targetItemId);
            buf.writeInt(targetItemCount);
            buf.writeUtf(hunterRespawnMode);
            buf.writeInt(hunterLives);
            buf.writeUtf(runnerRespawnMode);
            buf.writeInt(runnerLives);
            buf.writeInt(runnerRespawnSeconds);
            buf.writeUtf(runnerTeamLossMode);
            buf.writeUtf(hunterVictoryType);
            buf.writeBoolean(hunterWinByRunnerKillsEnabled);
            buf.writeInt(hunterRunnerKillTarget);
            buf.writeBoolean(blazeRodChanceEnabled);
            buf.writeInt(blazeRodChancePercent);
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
                    config.environmentKillsEnabled,
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
                    config.blazeRodChanceEnabled,
                    config.blazeRodChancePercent,
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
            config.environmentKillsEnabled = environmentKillsEnabled;
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
            config.blazeRodChanceEnabled = blazeRodChanceEnabled;
            config.blazeRodChancePercent = blazeRodChancePercent;
            config.enabledWildcards = new LinkedHashMap<>(enabledWildcards);
            config.validate();
            return config;
        }
    }

    private static Map<String, Boolean> readWildcardToggles(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<String, Boolean> toggles = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf();
            toggles.put(id, buf.readBoolean());
        }
        return toggles;
    }

    private static void writeWildcardToggles(RegistryFriendlyByteBuf buf, Map<String, Boolean> toggles) {
        buf.writeVarInt(toggles.size());
        for (Map.Entry<String, Boolean> entry : toggles.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeBoolean(Boolean.TRUE.equals(entry.getValue()));
        }
    }

    public record RequestConfigPayload() implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestConfigPayload> CODEC =
                StreamCodec.ofMember(RequestConfigPayload::write, RequestConfigPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
        }

        private static RequestConfigPayload read(RegistryFriendlyByteBuf buf) {
            return new RequestConfigPayload();
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
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
    ) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigPayload> CODEC =
                StreamCodec.ofMember(SyncConfigPayload::write, SyncConfigPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeEnum(gameState);
            buf.writeInt(hunterCount);
            buf.writeInt(runnerCount);
            buf.writeUtf(activeWildcard);
            buf.writeUtf(playerRole);
            buf.writeBoolean(playerInTeam);
            buf.writeBoolean(activeWildcardRunning);
            buf.writeInt(phaseRemainingSeconds);
            buf.writeInt(activeWildcardRemainingSeconds);
            buf.writeInt(nextWildcardSeconds);
            buf.writeBoolean(canManage);
            buf.writeBoolean(debugPageEnabled);
            config.write(buf);
        }

        private static SyncConfigPayload read(RegistryFriendlyByteBuf buf) {
            return new SyncConfigPayload(
                    buf.readEnum(GameState.class),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(128),
                    buf.readUtf(64),
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
        public Type<? extends CustomPacketPayload> type() {
            return S2C_SYNC_CONFIG;
        }
    }

    public record OperationResultPayload(boolean success, String message, long requestId) implements CustomPacketPayload {
        public OperationResultPayload(boolean success, String message) { this(success, message, 0L); }
        public static final StreamCodec<RegistryFriendlyByteBuf, OperationResultPayload> CODEC =
                StreamCodec.ofMember(OperationResultPayload::write, OperationResultPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(success);
            buf.writeUtf(message);
            buf.writeLong(requestId);
        }

        private static OperationResultPayload read(RegistryFriendlyByteBuf buf) {
            return new OperationResultPayload(buf.readBoolean(), buf.readUtf(2048), buf.readLong());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_OPERATION_RESULT;
        }
    }

    public record CloseConfigScreenPayload() implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, CloseConfigScreenPayload> CODEC =
                StreamCodec.ofMember(CloseConfigScreenPayload::write, CloseConfigScreenPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
        }

        private static CloseConfigScreenPayload read(RegistryFriendlyByteBuf buf) {
            return new CloseConfigScreenPayload();
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_CLOSE_CONFIG_SCREEN;
        }
    }

    public record ClearChatPayload() implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClearChatPayload> CODEC =
                StreamCodec.ofMember(ClearChatPayload::write, ClearChatPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
        }

        private static ClearChatPayload read(RegistryFriendlyByteBuf buf) {
            return new ClearChatPayload();
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_CLEAR_CHAT;
        }
    }

    public record WildcardDrawPayload(String wildcardName) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, WildcardDrawPayload> CODEC =
                StreamCodec.ofMember(WildcardDrawPayload::write, WildcardDrawPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(wildcardName);
        }

        private static WildcardDrawPayload read(RegistryFriendlyByteBuf buf) {
            return new WildcardDrawPayload(buf.readUtf(64));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_WILDCARD_DRAW;
        }
    }

    public record WildcardIntroPayload(String wildcardName, String description, boolean visible) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, WildcardIntroPayload> CODEC =
                StreamCodec.ofMember(WildcardIntroPayload::write, WildcardIntroPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(wildcardName == null ? "" : wildcardName);
            buf.writeUtf(description == null ? "" : description);
            buf.writeBoolean(visible);
        }

        private static WildcardIntroPayload read(RegistryFriendlyByteBuf buf) {
            return new WildcardIntroPayload(
                    buf.readUtf(64),
                    buf.readUtf(160),
                    buf.readBoolean()
            );
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_WILDCARD_INTRO;
        }
    }

    public record HunterKillFeedbackPayload(String hunterName, String runnerName, int remainingKills, int currentKills, int targetKills) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, HunterKillFeedbackPayload> CODEC =
                StreamCodec.ofMember(HunterKillFeedbackPayload::write, HunterKillFeedbackPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(hunterName);
            buf.writeUtf(runnerName);
            buf.writeInt(remainingKills);
            buf.writeInt(currentKills);
            buf.writeInt(targetKills);
        }

        private static HunterKillFeedbackPayload read(RegistryFriendlyByteBuf buf) {
            return new HunterKillFeedbackPayload(
                    buf.readUtf(64),
                    buf.readUtf(64),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            );
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_HUNTER_KILL_FEEDBACK;
        }
    }

    public record HudFeedbackPayload(String title, String line1, String line2, String style) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, HudFeedbackPayload> CODEC =
                StreamCodec.ofMember(HudFeedbackPayload::write, HudFeedbackPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(title == null ? "" : title);
            buf.writeUtf(line1 == null ? "" : line1);
            buf.writeUtf(line2 == null ? "" : line2);
            buf.writeUtf(style == null ? "" : style);
        }

        private static HudFeedbackPayload read(RegistryFriendlyByteBuf buf) {
            return new HudFeedbackPayload(
                    buf.readUtf(64),
                    buf.readUtf(128),
                    buf.readUtf(128),
                    buf.readUtf(32)
            );
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_HUD_FEEDBACK;
        }
    }

    /** Left-hand objective panel: the runner objective line plus a hunter progress line (either may be empty). */
    public record ObjectiveStatusPayload(boolean visible, String text, String style, String hunterText) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, ObjectiveStatusPayload> CODEC =
                StreamCodec.ofMember(ObjectiveStatusPayload::write, ObjectiveStatusPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(visible);
            buf.writeUtf(text == null ? "" : text);
            buf.writeUtf(style == null ? "" : style);
            buf.writeUtf(hunterText == null ? "" : hunterText);
        }

        private static ObjectiveStatusPayload read(RegistryFriendlyByteBuf buf) {
            return new ObjectiveStatusPayload(
                    buf.readBoolean(),
                    buf.readUtf(192),
                    buf.readUtf(32),
                    buf.readUtf(256)
            );
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_OBJECTIVE_STATUS;
        }
    }

    public record ObjectiveNoticePayload(String message, String style) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, ObjectiveNoticePayload> CODEC =
                StreamCodec.ofMember(ObjectiveNoticePayload::write, ObjectiveNoticePayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(message == null ? "" : message);
            buf.writeUtf(style == null ? "" : style);
        }

        private static ObjectiveNoticePayload read(RegistryFriendlyByteBuf buf) {
            return new ObjectiveNoticePayload(
                    buf.readUtf(192),
                    buf.readUtf(32)
            );
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_OBJECTIVE_NOTICE;
        }
    }

    public record WeaponOverheatStatusPayload(int heat, int maxHeat, boolean visible) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, WeaponOverheatStatusPayload> CODEC =
                StreamCodec.ofMember(WeaponOverheatStatusPayload::write, WeaponOverheatStatusPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeInt(heat);
            buf.writeInt(maxHeat);
            buf.writeBoolean(visible);
        }

        private static WeaponOverheatStatusPayload read(RegistryFriendlyByteBuf buf) {
            return new WeaponOverheatStatusPayload(buf.readInt(), buf.readInt(), buf.readBoolean());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_WEAPON_OVERHEAT_STATUS;
        }
    }

    public record BackroomsPhasePayload(BackroomsPhase phase, int holdTicks) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, BackroomsPhasePayload> CODEC =
                StreamCodec.ofMember(BackroomsPhasePayload::write, BackroomsPhasePayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeEnum(phase);
            buf.writeInt(holdTicks);
        }

        private static BackroomsPhasePayload read(RegistryFriendlyByteBuf buf) {
            return new BackroomsPhasePayload(buf.readEnum(BackroomsPhase.class), buf.readInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_BACKROOMS_PHASE;
        }
    }

    public record KeyScramblePayload(KeyScrambleAction action) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, KeyScramblePayload> CODEC =
                StreamCodec.ofMember(KeyScramblePayload::write, KeyScramblePayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeEnum(action);
        }

        private static KeyScramblePayload read(RegistryFriendlyByteBuf buf) {
            return new KeyScramblePayload(buf.readEnum(KeyScrambleAction.class));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_KEY_SCRAMBLE;
        }
    }

    public record UpdateConfigPayload(ConfigSnapshot config, ConfigSnapshot base, long requestId) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateConfigPayload> CODEC =
                StreamCodec.ofMember(UpdateConfigPayload::write, UpdateConfigPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            config.write(buf);
            base.write(buf);
            buf.writeLong(requestId);
        }

        private static UpdateConfigPayload read(RegistryFriendlyByteBuf buf) {
            return new UpdateConfigPayload(ConfigSnapshot.fromBuf(buf), ConfigSnapshot.fromBuf(buf), buf.readLong());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return C2S_UPDATE_CONFIG;
        }
    }

    public record ReloadConfigPayload() implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, ReloadConfigPayload> CODEC =
                StreamCodec.ofMember(ReloadConfigPayload::write, ReloadConfigPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
        }

        private static ReloadConfigPayload read(RegistryFriendlyByteBuf buf) {
            return new ReloadConfigPayload();
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return C2S_RELOAD_CONFIG;
        }
    }

    public record DebugActionPayload(DebugAction action) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, DebugActionPayload> CODEC =
                StreamCodec.ofMember(DebugActionPayload::write, DebugActionPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeEnum(action);
        }

        private static DebugActionPayload read(RegistryFriendlyByteBuf buf) {
            return new DebugActionPayload(buf.readEnum(DebugAction.class));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return C2S_DEBUG_ACTION;
        }
    }

    public record TestWildcardPayload(String wildcardName) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, TestWildcardPayload> CODEC =
                StreamCodec.ofMember(TestWildcardPayload::write, TestWildcardPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(wildcardName);
        }

        private static TestWildcardPayload read(RegistryFriendlyByteBuf buf) {
            return new TestWildcardPayload(buf.readUtf(64));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return C2S_TEST_WILDCARD;
        }
    }

    public record WorldTiltPayload(boolean active, int transitionTicks, double gravityX, double gravityZ) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, WorldTiltPayload> CODEC =
                StreamCodec.ofMember(WorldTiltPayload::write, WorldTiltPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(active);
            buf.writeVarInt(transitionTicks);
            buf.writeDouble(gravityX);
            buf.writeDouble(gravityZ);
        }

        private static WorldTiltPayload read(RegistryFriendlyByteBuf buf) {
            return new WorldTiltPayload(buf.readBoolean(), buf.readVarInt(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_WORLD_TILT;
        }
    }

    public record TeamActionPayload(TeamAction action) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, TeamActionPayload> CODEC =
                StreamCodec.ofMember(TeamActionPayload::write, TeamActionPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeEnum(action);
        }

        private static TeamActionPayload read(RegistryFriendlyByteBuf buf) {
            return new TeamActionPayload(buf.readEnum(TeamAction.class));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return C2S_TEAM_ACTION;
        }
    }

    public record DeathSpectatePayload(boolean active, String targetName, int targetCount) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, DeathSpectatePayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> { buf.writeBoolean(value.active); buf.writeUtf(value.targetName, 256); buf.writeVarInt(value.targetCount); },
                buf -> new DeathSpectatePayload(buf.readBoolean(), buf.readUtf(256), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return S2C_DEATH_SPECTATE; }
    }

    public record CycleDeathSpectatePayload(boolean previous) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, CycleDeathSpectatePayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> buf.writeBoolean(value.previous), buf -> new CycleDeathSpectatePayload(buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return C2S_CYCLE_DEATH_SPECTATE; }
    }

    public record DeathWaitPayload(boolean visible, int remainingSeconds, String line1, String line2) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, DeathWaitPayload> CODEC =
                StreamCodec.ofMember(DeathWaitPayload::write, DeathWaitPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(visible);
            buf.writeInt(remainingSeconds);
            buf.writeUtf(line1 == null ? "" : line1);
            buf.writeUtf(line2 == null ? "" : line2);
        }

        private static DeathWaitPayload read(RegistryFriendlyByteBuf buf) {
            return new DeathWaitPayload(buf.readBoolean(), buf.readInt(), buf.readUtf(256), buf.readUtf(256));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_DEATH_WAIT;
        }
    }

    /** One runner in the hunter's compass menu. distance is -1 when in another dimension. */
    public record CompassTargetEntry(UUID playerId, String nameSpec, int distance, boolean sameDimension, boolean selected) {
        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUUID(playerId);
            buf.writeUtf(nameSpec);
            buf.writeInt(distance);
            buf.writeBoolean(sameDimension);
            buf.writeBoolean(selected);
        }

        private static CompassTargetEntry read(RegistryFriendlyByteBuf buf) {
            return new CompassTargetEntry(buf.readUUID(), buf.readUtf(128), buf.readInt(), buf.readBoolean(), buf.readBoolean());
        }
    }

    public record CompassMenuPayload(List<CompassTargetEntry> entries, boolean nearestSelected) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, CompassMenuPayload> CODEC =
                StreamCodec.ofMember(CompassMenuPayload::write, CompassMenuPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(entries.size());
            for (CompassTargetEntry entry : entries) {
                entry.write(buf);
            }
            buf.writeBoolean(nearestSelected);
        }

        private static CompassMenuPayload read(RegistryFriendlyByteBuf buf) {
            int count = Math.min(buf.readVarInt(), 256);
            List<CompassTargetEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                entries.add(CompassTargetEntry.read(buf));
            }
            return new CompassMenuPayload(entries, buf.readBoolean());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_COMPASS_MENU;
        }
    }

    /** nearest=true asks for automatic nearest-runner tracking; otherwise targetId names the runner. */
    public record CompassSelectPayload(boolean nearest, UUID targetId) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, CompassSelectPayload> CODEC =
                StreamCodec.ofMember(CompassSelectPayload::write, CompassSelectPayload::read);

        public static CompassSelectPayload trackNearest() {
            return new CompassSelectPayload(true, new UUID(0L, 0L));
        }

        public static CompassSelectPayload track(UUID targetId) {
            return new CompassSelectPayload(false, targetId);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(nearest);
            buf.writeUUID(targetId);
        }

        private static CompassSelectPayload read(RegistryFriendlyByteBuf buf) {
            return new CompassSelectPayload(buf.readBoolean(), buf.readUUID());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return C2S_COMPASS_SELECT;
        }
    }

    public record GameActionPayload(GameAction action) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, GameActionPayload> CODEC =
                StreamCodec.ofMember(GameActionPayload::write, GameActionPayload::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeEnum(action);
        }

        private static GameActionPayload read(RegistryFriendlyByteBuf buf) {
            return new GameActionPayload(buf.readEnum(GameAction.class));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return C2S_GAME_ACTION;
        }
    }
    public record MemberEntry(String name, String role, String state, int lives, int respawnSeconds) {}
    public record RoundDetailsPayload(List<MemberEntry> members, String winner, String reason, List<MemberEntry> resultMembers,
            String objective, String hunterObjective, String ownState, int ownLives) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, RoundDetailsPayload> CODEC = StreamCodec.ofMember(RoundDetailsPayload::write, RoundDetailsPayload::read);
        private static void members(RegistryFriendlyByteBuf b, List<MemberEntry> entries) {
            b.writeVarInt(entries.size());
            for (MemberEntry e : entries) { b.writeUtf(e.name()); b.writeUtf(e.role()); b.writeUtf(e.state()); b.writeInt(e.lives()); b.writeInt(e.respawnSeconds()); }
        }
        private static List<MemberEntry> members(RegistryFriendlyByteBuf b) {
            int n = b.readVarInt(); if (n < 0 || n > 1024) throw new IllegalArgumentException("Invalid roster size");
            List<MemberEntry> result = new ArrayList<>();
            for (int i=0; i<n; i++) result.add(new MemberEntry(b.readUtf(256), b.readUtf(64), b.readUtf(64), b.readInt(), b.readInt()));
            return List.copyOf(result);
        }
        private void write(RegistryFriendlyByteBuf b) {
            members(b, members); b.writeUtf(winner); b.writeUtf(reason); members(b, resultMembers);
            b.writeUtf(objective); b.writeUtf(hunterObjective); b.writeUtf(ownState); b.writeInt(ownLives);
        }
        private static RoundDetailsPayload read(RegistryFriendlyByteBuf b) {
            return new RoundDetailsPayload(members(b), b.readUtf(64), b.readUtf(4096), members(b), b.readUtf(4096), b.readUtf(4096), b.readUtf(64), b.readInt());
        }
        @Override public Type<? extends CustomPacketPayload> type() { return S2C_ROUND_DETAILS; }
    }

}
