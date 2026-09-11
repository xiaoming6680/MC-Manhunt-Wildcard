package com.xiaoming.hunterwildcard.client.hud;

import com.xiaoming.hunterwildcard.client.ClientGameStatus;
import com.xiaoming.hunterwildcard.client.HunterWildcardClientText;
import com.xiaoming.hunterwildcard.client.key.HunterWildcardKeyBindings;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.game.HunterVictoryType;
import com.xiaoming.hunterwildcard.game.RunnerVictoryType;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.ConfigSnapshot;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.SyncConfigPayload;
import com.xiaoming.hunterwildcard.respawn.RespawnMode;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-left panel summarising the round. Shown automatically in the lobby while a game is being set up,
 * and toggled with the menu key by non-operators once the round has started.
 */
public final class GameStatusHud {
    private static final int PANEL_WIDTH = 150;
    private static final int MAX_PANEL_WIDTH = 300;
    private static final int ROW_HEIGHT = 11;
    private static final int MARGIN = 6;
    private static final int COLOR_LABEL = 0xFF9FAAB4;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_ACCENT_LOBBY = 0xFF7FC2FF;
    private static final int COLOR_ACCENT_GAME = 0xFFFFD966;
    private static final int COLOR_HINT = 0xFF7D8790;

    private GameStatusHud() {
    }

    public static boolean shouldRender() {
        SyncConfigPayload sync = ClientGameStatus.latest();
        if (sync == null) {
            return false;
        }

        if (sync.gameState() == GameState.WAITING) {
            // Lobby panel: only while a round is being set up, and never on top of a wildcard test.
            return (sync.playerInTeam() || sync.hunterCount() + sync.runnerCount() > 0) && !sync.activeWildcardRunning();
        }

        return ClientGameStatus.isStatusHudToggled();
    }

    /** @param topY y position to start at; callers pass the bottom of whatever panel sits above. */
    public static void render(DrawContext context, int topY) {
        MinecraftClient client = MinecraftClient.getInstance();
        SyncConfigPayload sync = ClientGameStatus.latest();
        if (sync == null || client.options.hudHidden) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        boolean lobby = sync.gameState() == GameState.WAITING;
        List<Row> rows = lobby ? lobbyRows(sync) : gameRows(sync);
        String title = tr(HunterWildcardText.key(lobby ? "hud.status.lobby_title" : "hud.status.game_title"));
        String keyName = HunterWildcardKeyBindings.openPanelKeyName();
        String hint = tr(HunterWildcardText.spec(lobby ? "hud.status.hint_open" : "hud.status.hint_close", keyName));
        int accent = lobby ? COLOR_ACCENT_LOBBY : COLOR_ACCENT_GAME;

        int screenWidth = client.getWindow().getScaledWidth();
        int labelWidth = 0;
        int valueWidth = 0;
        for (Row row : rows) {
            labelWidth = Math.max(labelWidth, textRenderer.getWidth(row.label));
            valueWidth = Math.max(valueWidth, textRenderer.getWidth(row.value));
        }
        int maxPanelWidth = Math.max(120, Math.min(MAX_PANEL_WIDTH, screenWidth * 45 / 100));
        labelWidth = Math.min(labelWidth, maxPanelWidth / 2 - 6);
        int neededWidth = 6 + labelWidth + 4 + Math.max(valueWidth, textRenderer.getWidth(hint)) + 6;
        int panelWidth = Math.max(PANEL_WIDTH, Math.min(maxPanelWidth, neededWidth));
        int panelHeight = 16 + rows.size() * ROW_HEIGHT + 12;
        int panelX = MARGIN;
        int panelY = Math.max(MARGIN, topY);

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xD8161B22);
        context.fill(panelX, panelY, panelX + 2, panelY + panelHeight, accent);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, accent);
        context.drawText(textRenderer, Text.literal(trim(textRenderer, title, panelWidth - 10)), panelX + 6, panelY + 4, accent, false);

        int valueX = panelX + 6 + labelWidth + 4;
        int valueAreaWidth = panelX + panelWidth - 5 - valueX;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int rowY = panelY + 16 + i * ROW_HEIGHT;
            context.drawText(textRenderer, Text.literal(trim(textRenderer, row.label, labelWidth)), panelX + 6, rowY, COLOR_LABEL, false);
            context.drawText(textRenderer, Text.literal(trim(textRenderer, row.value, valueAreaWidth)), valueX, rowY, row.color, false);
        }
        context.drawText(textRenderer, Text.literal(trim(textRenderer, hint, panelWidth - 10)), panelX + 6, panelY + panelHeight - 10, COLOR_HINT, false);
    }

    public static int panelHeightFor(SyncConfigPayload sync) {
        boolean lobby = sync.gameState() == GameState.WAITING;
        int rows = lobby ? lobbyRows(sync).size() : gameRows(sync).size();
        return 16 + rows * ROW_HEIGHT + 12;
    }

    private static List<Row> lobbyRows(SyncConfigPayload sync) {
        ConfigSnapshot config = sync.config();
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(label("hud.status.state"), tr(HunterWildcardText.key("state.waiting")), COLOR_ACCENT_LOBBY));
        rows.add(new Row(label("hud.status.teams"), tr(HunterWildcardText.spec("hud.status.teams_value", sync.hunterCount(), sync.runnerCount())), COLOR_VALUE));
        rows.add(new Row(label("hud.status.my_team"), tr(sync.playerRole()), sync.playerInTeam() ? COLOR_VALUE : COLOR_ACCENT_GAME));
        rows.add(new Row(label("hud.status.runner_win"), runnerWinText(config), COLOR_VALUE));
        rows.add(new Row(label("hud.status.hunter_win"), hunterWinText(config), COLOR_VALUE));
        rows.add(new Row(label("hud.status.hunter_respawn"), respawnText(config.hunterRespawnMode(), config.hunterLives(), RespawnMode.INFINITE), COLOR_VALUE));
        rows.add(new Row(label("hud.status.runner_respawn"), respawnText(config.runnerRespawnMode(), config.runnerLives(), RespawnMode.LIMITED_LIVES), COLOR_VALUE));
        rows.add(new Row(label("hud.status.wildcards"), tr(HunterWildcardText.spec("hud.status.wildcards_value",
                config.wildcardIntervalSeconds(), config.wildcardDurationSeconds(),
                ClientGameStatus.enabledWildcardCount(config), ClientGameStatus.totalWildcardCount())), COLOR_VALUE));
        rows.add(new Row(label("hud.status.prepare_time"), tr(HunterWildcardText.spec("hud.status.seconds", config.preparingSeconds())), COLOR_VALUE));
        return rows;
    }

    private static List<Row> gameRows(SyncConfigPayload sync) {
        ConfigSnapshot config = sync.config();
        List<Row> rows = new ArrayList<>();
        String stateName = tr(HunterWildcardText.key("state." + sync.gameState().name().toLowerCase()));
        String stateValue = sync.phaseRemainingSeconds() >= 0
                ? tr(HunterWildcardText.spec("hud.status.with_remaining", stateName, sync.phaseRemainingSeconds()))
                : stateName;
        rows.add(new Row(label("hud.status.state"), stateValue, COLOR_ACCENT_GAME));
        rows.add(new Row(label("hud.status.my_team"), tr(sync.playerRole()), COLOR_VALUE));
        rows.add(new Row(label("hud.status.teams"), tr(HunterWildcardText.spec("hud.status.teams_value", sync.hunterCount(), sync.runnerCount())), COLOR_VALUE));
        String wildcardValue;
        if (sync.activeWildcardRunning() && !sync.activeWildcard().isBlank()) {
            String name = tr(HunterWildcardText.wildcardNameKey(sync.activeWildcard()));
            wildcardValue = sync.activeWildcardRemainingSeconds() >= 0
                    ? tr(HunterWildcardText.spec("hud.status.with_remaining", name, sync.activeWildcardRemainingSeconds()))
                    : name;
        } else if (sync.nextWildcardSeconds() >= 0) {
            wildcardValue = tr(HunterWildcardText.spec("hud.status.wildcard_next", sync.nextWildcardSeconds()));
        } else {
            wildcardValue = tr(HunterWildcardText.key("common.none"));
        }
        rows.add(new Row(label("hud.status.wildcard"), wildcardValue, COLOR_ACCENT_LOBBY));
        rows.add(new Row(label("hud.status.runner_win"), runnerWinText(config), COLOR_VALUE));
        rows.add(new Row(label("hud.status.hunter_win"), hunterWinText(config), COLOR_VALUE));
        return rows;
    }

    private static String runnerWinText(ConfigSnapshot config) {
        RunnerVictoryType type = RunnerVictoryType.fromConfig(config.runnerVictoryType(), RunnerVictoryType.DRAGON);
        String base = tr(type.getTranslationKey());
        return switch (type) {
            case DRAGON -> base;
            case SURVIVE_TIME -> base + " " + tr(HunterWildcardText.spec("hud.status.paren_seconds", config.surviveTimeSeconds()));
            case REACH_LOCATION -> base + " (" + config.targetX() + ", " + config.targetY() + ", " + config.targetZ() + ")";
            case COLLECT_ITEM -> base + " (" + config.targetItemCount() + "x " + shortItemId(config.targetItemId()) + ")";
        };
    }

    private static String hunterWinText(ConfigSnapshot config) {
        HunterVictoryType type = HunterVictoryType.fromConfig(config.hunterVictoryType(), HunterVictoryType.RUNNERS_OUT);
        String base = tr(type.getTranslationKey());
        if (type == HunterVictoryType.RUNNER_KILL_COUNT) {
            return base + " " + tr(HunterWildcardText.spec("hud.status.paren_kills", config.hunterRunnerKillTarget()));
        }
        return base;
    }

    private static String respawnText(String modeValue, int lives, RespawnMode fallback) {
        RespawnMode mode = RespawnMode.fromConfig(modeValue, fallback);
        String base = tr(HunterWildcardText.key("config.respawn_mode." + mode.name().toLowerCase()));
        if (mode == RespawnMode.LIMITED_LIVES) {
            return base + " " + tr(HunterWildcardText.spec("hud.status.paren_lives", lives));
        }
        return base;
    }

    private static String shortItemId(String itemId) {
        if (itemId == null) {
            return "";
        }
        int colon = itemId.indexOf(':');
        return colon >= 0 ? itemId.substring(colon + 1) : itemId;
    }

    private static String label(String path) {
        return tr(HunterWildcardText.key(path));
    }

    private static String tr(String spec) {
        return HunterWildcardClientText.translate(spec);
    }

    private static String trim(TextRenderer textRenderer, String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        return textRenderer.trimToWidth(text, Math.max(0, maxWidth - textRenderer.getWidth("..."))) + "...";
    }

    private record Row(String label, String value, int color) {
    }
}
