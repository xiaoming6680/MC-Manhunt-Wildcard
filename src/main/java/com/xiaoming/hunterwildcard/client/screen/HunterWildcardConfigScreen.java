package com.xiaoming.hunterwildcard.client.screen;

import com.xiaoming.hunterwildcard.client.ClientGameStatus;
import com.xiaoming.hunterwildcard.client.hud.WildcardIcons;
import com.xiaoming.hunterwildcard.client.HunterWildcardClientText;
import com.xiaoming.hunterwildcard.client.screen.widget.DropdownWidget;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.game.HunterVictoryType;
import com.xiaoming.hunterwildcard.game.RunnerVictoryType;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.ConfigSnapshot;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.DebugAction;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.GameAction;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.OperationResultPayload;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.SyncConfigPayload;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.TeamAction;
import com.xiaoming.hunterwildcard.respawn.RespawnMode;
import com.xiaoming.hunterwildcard.respawn.RunnerTeamLossMode;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardIds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HunterWildcardConfigScreen extends Screen {
    private static final int CARD_PADDING_X = 10;
    private static final int CARD_PADDING_TOP = 10;
    private static final int CARD_PADDING_BOTTOM = 8;
    private static final int CARD_TITLE_HEIGHT = 13;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 3;
    private static final int CARD_GAP = 8;
    private static final int COMPACT_CARD_GAP = 12;
    private static final int SMALL_CARD_MAX_WIDTH = 300;
    private static final int MEDIUM_CARD_MAX_WIDTH = 320;
    private static final int LARGE_CARD_MAX_WIDTH = 420;
    private static final int STATUS_BLOCK_MAX_WIDTH = 160;
    private static final int WILDCARD_TOGGLE_TARGET_WIDTH = 220;
    private static final int WILDCARD_TOGGLE_MAX_WIDTH = 260;
    private static final int WILDCARD_TOGGLE_HEIGHT = 32;
    private static final int TWO_COLUMN_GAP = COMPACT_CARD_GAP;
    private static final int TWO_COLUMN_MIN_WIDTH = 460;
    private static final int LABEL_WIDTH = 112;
    private static final int CONTROL_WIDTH = 104;
    private static final int DROPDOWN_WIDTH = 180;
    private static final int UNIT_WIDTH = 24;
    private static final int CONTROL_HEIGHT = 18;
    private static final int STACKED_LABEL_HEIGHT = 13;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_GAP = 6;
    private static final int HINT_MAX_LINES = 3;
    private static final int SCROLL_STEP = 32;
    private static final int SCROLL_BAR_RESERVE = 12;
    private static final int REALTIME_REFRESH_TICKS = 5;
    private static final int CONFIG_IDLE_REFRESH_TICKS = 10;
    private static final long TOAST_FADE_IN_MS = 140L;
    private static final long TOAST_HOLD_MS = 1800L;
    private static final long TOAST_FADE_OUT_MS = 220L;
    private static ConfigSnapshot cachedEditableConfig;

    private final List<Label> labels = new ArrayList<>();
    private final List<WrappedLabel> wrappedLabels = new ArrayList<>();
    private final List<Box> boxes = new ArrayList<>();
    private final List<Icon> icons = new ArrayList<>();
    /** Every widget that lives inside the scrolling content area; moved on scroll instead of rebuilt. */
    private final List<ClickableWidget> contentWidgets = new ArrayList<>();
    private int builtScroll;
    private boolean syncRebuildPending;
    private final Map<NumberField, TextFieldWidget> numberFields = new EnumMap<>(NumberField.class);
    private final Map<StringField, TextFieldWidget> stringFields = new EnumMap<>(StringField.class);
    private final Map<DropdownField, DropdownWidget> dropdownFields = new EnumMap<>(DropdownField.class);
    private final Map<String, Float> pageScrollOffsets = new HashMap<>();
    private final Map<String, Float> pageTargetScrollOffsets = new HashMap<>();
    /** Raw text the user typed into number fields; survives rebuilds until committed to {@link #editableConfig}. */
    private final Map<NumberField, String> pendingNumberText = new EnumMap<>(NumberField.class);
    private final Map<StringField, String> pendingStringText = new EnumMap<>(StringField.class);

    private Page currentPage = Page.GAME;
    /** Open RULES sub-page, or null while the hub is shown. Per-instance: resets whenever the screen is reopened. */
    private RulesSubPage rulesSubPage;
    /** Text field that had keyboard focus before the last rebuild; restored after {@link #init()}. */
    private Object focusedInputKey;
    private int focusedCursor = -1;
    private int focusedSelectionStart = -1;
    private SyncConfigPayload serverSync;
    private ConfigSnapshot editableConfig;
    private boolean canManage;
    private boolean requested;
    private int refreshTicks;
    private float scrollOffset;
    private float targetScrollOffset;
    private float maxScroll;
    private float navScroll;
    private float maxNavScroll;
    private int renderedScroll;
    private int pageContentHeight;
    private String statusMessage = key("screen.status.requesting_server");
    private StatusKind statusKind = StatusKind.INFO;
    private String toastMessage = "";
    private StatusKind toastKind = StatusKind.INFO;
    private long toastStartTimeMs;
    private long toastDurationMs;
    private String hoverTooltip = "";
    private int hoverTooltipX;
    private int hoverTooltipY;
    private boolean hasSyncedOnce;
    private boolean manualReloadRequested;
    private boolean manualSaveRequested;
    private StyledButtonWidget saveButton;
    private ToggleField selectedWildcardSettings;

    public HunterWildcardConfigScreen() {
        super(Text.translatable(HunterWildcardText.key("screen.title")));
    }

    private static String key(String path) {
        return HunterWildcardText.key(path);
    }

    private static String spec(String path, Object... args) {
        return HunterWildcardText.spec(path, args);
    }

    private static String tr(String spec) {
        return HunterWildcardClientText.translate(spec);
    }

    private static Text text(String spec) {
        return HunterWildcardClientText.text(spec);
    }

    public static void receiveSync(SyncConfigPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HunterWildcardConfigScreen screen) {
            screen.applySync(payload);
        }
    }

    public static void receiveOperationResult(OperationResultPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HunterWildcardConfigScreen screen) {
            if (payload.success()) {
                cachedEditableConfig = null;
                screen.showToast(payload.message(), StatusKind.SUCCESS);
            } else {
                screen.showToast(payload.message(), StatusKind.ERROR);
            }
        }
    }

    public static void closeFromServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HunterWildcardConfigScreen) {
            client.setScreen(null);
        }
    }

    @Override
    protected void init() {
        captureInputState();
        labels.clear();
        wrappedLabels.clear();
        boxes.clear();
        icons.clear();
        numberFields.clear();
        stringFields.clear();
        dropdownFields.clear();
        saveButton = null;

        Layout layout = layout();
        contentWidgets.clear();
        renderedScroll = Math.round(scrollOffset);
        int buildScroll = renderedScroll;
        pageContentHeight = layout.viewportHeight();
        ensureVisiblePage();
        buildNavigation(layout);
        buildHeader(layout);

        switch (currentPage) {
            case GAME -> buildGamePage(layout);
            case RULES -> buildRulesPage(layout);
            case WILDCARD -> buildWildcardPage(layout);
            case DEBUG -> buildDebugPage(layout);
        }

        updateMaxScroll(layout);
        if (renderedScroll != buildScroll) {
            clearAndInit();
            return;
        }

        buildFooter(layout);
        builtScroll = renderedScroll;
        updateContentWidgetVisibility(layout);
        restoreInputState();

        if (!requested) {
            requested = true;
            requestConfig();
        }
    }

    /**
     * Remembers which text field has keyboard focus (plus cursor/selection) before the widgets are thrown away.
     * A field that was not rebuilt (scrolled out of view) keeps its remembered focus so it regains it when it returns.
     */
    private void captureInputState() {
        boolean sawFocused = false;
        for (Map.Entry<NumberField, TextFieldWidget> entry : numberFields.entrySet()) {
            if (entry.getValue().isFocused()) {
                rememberFocus(entry.getKey(), entry.getValue());
                sawFocused = true;
            } else if (entry.getKey() == focusedInputKey) {
                focusedInputKey = null;
            }
        }
        for (Map.Entry<StringField, TextFieldWidget> entry : stringFields.entrySet()) {
            if (entry.getValue().isFocused()) {
                rememberFocus(entry.getKey(), entry.getValue());
                sawFocused = true;
            } else if (entry.getKey() == focusedInputKey) {
                focusedInputKey = null;
            }
        }
        if (!sawFocused && getFocused() != null && !(getFocused() instanceof TextFieldWidget)) {
            // Focus moved to a button/dropdown: the remembered field must not steal it back.
            focusedInputKey = null;
        }
    }

    private void rememberFocus(Object key, TextFieldWidget widget) {
        focusedInputKey = key;
        focusedCursor = widget.getCursor();
        focusedSelectionStart = selectionAnchor(widget);
    }

    private int selectionAnchor(TextFieldWidget widget) {
        // TextFieldWidget exposes no selection getter; keep the cursor and let the restore collapse the selection.
        return widget.getCursor();
    }

    private void restoreInputState() {
        if (focusedInputKey == null) {
            return;
        }

        TextFieldWidget widget = focusedInputKey instanceof NumberField numberField
                ? numberFields.get(numberField)
                : focusedInputKey instanceof StringField stringField ? stringFields.get(stringField) : null;
        if (widget == null || !widget.active) {
            return;
        }

        setFocused(widget);
        widget.setFocused(true);
        int length = widget.getText().length();
        int cursor = focusedCursor < 0 ? length : Math.min(focusedCursor, length);
        widget.setCursor(cursor, false);
        if (focusedSelectionStart >= 0 && focusedSelectionStart != cursor) {
            widget.setSelectionStart(Math.min(focusedSelectionStart, length));
            widget.setSelectionEnd(cursor);
        }
    }

    private void buildHeader(Layout layout) {
        if (!hasOpenSubPage()) {
            return;
        }

        addButton(layout.contentX(), layout.panelY() + 11, 18, 18, key("screen.button.back"), key("screen.tooltip.back"), widget -> closeSubPage(), ButtonVariant.NORMAL, true);
    }

    private boolean hasOpenSubPage() {
        return (currentPage == Page.RULES && rulesSubPage != null)
                || (currentPage == Page.WILDCARD && selectedWildcardSettings != null);
    }

    private String headerTitle() {
        if (currentPage == Page.RULES && rulesSubPage != null) {
            return spec("screen.breadcrumb", currentPage.label, rulesSubPage.label);
        }
        if (currentPage == Page.WILDCARD && selectedWildcardSettings != null) {
            return spec("screen.breadcrumb_wildcard_settings", currentPage.label, selectedWildcardSettings.label);
        }
        return currentPage.label;
    }

    private String headerDescription() {
        if (currentPage == Page.RULES && rulesSubPage != null) {
            return rulesSubPage.description;
        }
        if (currentPage == Page.WILDCARD && selectedWildcardSettings != null) {
            return key("screen.page.wildcard_settings.description");
        }
        return currentPage.description;
    }

    private void closeSubPage() {
        commitVisibleInputs();
        closeDropdowns();
        rememberCurrentScroll();
        rulesSubPage = null;
        selectedWildcardSettings = null;
        restorePageScroll(scrollKey());
        clearAndInit();
    }

    private void openRulesSubPage(RulesSubPage subPage) {
        commitVisibleInputs();
        closeDropdowns();
        rememberCurrentScroll();
        rulesSubPage = subPage;
        restorePageScroll(scrollKey());
        clearAndInit();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Layout layout = layout();
        updateSmoothScroll(layout, delta);
        int scrollDelta = renderedScroll - builtScroll;
        if (scrollDelta != 0) {
            shiftContent(-scrollDelta);
            builtScroll = renderedScroll;
            updateContentWidgetVisibility(layout);
        }

        context.fill(0, 0, width, height, 0x88000000);
        context.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.panelWidth(), layout.panelY() + layout.panelHeight(), 0xD0161B22);
        context.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.navWidth(), layout.panelY() + layout.panelHeight(), 0xE01E252D);
        context.fill(layout.panelX() + layout.navWidth(), layout.panelY(), layout.panelX() + layout.navWidth() + 1, layout.panelY() + layout.panelHeight(), 0xFF35404B);

        context.drawText(textRenderer, text(key("screen.title")), layout.panelX() + 12, layout.panelY() + 13, 0xFFFFFFFF, true);
        int headerX = hasOpenSubPage() ? layout.contentX() + 24 : layout.contentX();
        int headerWidth = Math.max(20, layout.usableContentWidth() - (headerX - layout.contentX()));
        context.drawText(textRenderer, Text.literal(trim(tr(headerTitle()), headerWidth)), headerX, layout.panelY() + 14, 0xFFFFFFFF, true);
        context.drawText(textRenderer, Text.literal(trim(tr(headerDescription()), headerWidth)), headerX, layout.panelY() + 29, 0xFF9FAAB4, false);

        context.enableScissor(layout.contentX(), layout.viewportTop(), layout.contentX() + layout.usableContentWidth(), layout.viewportBottom());
        for (Box box : boxes) {
            context.fill(box.x, box.y, box.x + box.width, box.y + box.height, box.color);
            context.fill(box.x, box.y, box.x + box.width, box.y + 1, box.borderColor);
            context.fill(box.x, box.y + box.height - 1, box.x + box.width, box.y + box.height, box.borderColor);
            context.fill(box.x, box.y, box.x + 1, box.y + box.height, box.borderColor);
            context.fill(box.x + box.width - 1, box.y, box.x + box.width, box.y + box.height, box.borderColor);
        }

        for (Label label : labels) {
            context.drawText(textRenderer, Text.literal(trim(tr(label.text), label.maxWidth(layout))), label.x, label.y, label.color, label.shadow);
        }
        for (WrappedLabel label : wrappedLabels) {
            renderWrappedLabel(context, layout, label);
        }
        for (Icon icon : icons) {
            context.drawItem(icon.stack, icon.x, icon.y);
        }
        context.disableScissor();

        renderFooterStatus(context, layout);
        renderScrollBar(context, layout);
        renderNavigationScrollBar(context, layout);

        hoverTooltip = "";
        if (saveButton != null) {
            saveButton.active = canSaveConfig();
        }
        for (ClickableWidget widget : contentWidgets) {
            widget.visible = false;
        }
        super.render(context, mouseX, mouseY, delta);
        context.enableScissor(layout.contentX(), layout.viewportTop(), layout.contentX() + layout.usableContentWidth(), layout.viewportBottom());
        for (ClickableWidget widget : contentWidgets) {
            if (intersectsViewport(layout, widget)) {
                widget.visible = true;
                widget.render(context, mouseX, mouseY, delta);
            }
        }
        context.disableScissor();
        renderToast(context, layout, delta);
        renderDropdownOverlays(context, mouseX, mouseY, delta);
        renderHoverTooltip(context);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();

        for (DropdownWidget dropdown : dropdownFields.values()) {
            if (dropdown.isExpanded() && dropdown.containsPoint(mouseX, mouseY)) {
                if (dropdown.mouseClicked(click, doubled)) {
                    return true;
                }
            }
        }

        for (DropdownWidget dropdown : dropdownFields.values()) {
            if (dropdown.containsPoint(mouseX, mouseY)) {
                if (dropdown.mouseClicked(click, doubled)) {
                    return true;
                }
            }
        }

        closeDropdowns();
        boolean inside = isInsideContent(layout(), mouseX, mouseY);
        List<ClickableWidget> hidden = new ArrayList<>();
        if (!inside) {
            for (ClickableWidget widget : contentWidgets) {
                if (widget.visible) {
                    widget.visible = false;
                    hidden.add(widget);
                }
            }
        }
        boolean handled = super.mouseClicked(click, doubled);
        for (ClickableWidget widget : hidden) {
            widget.visible = true;
        }
        if (handled && inside) {
            ensureFocusedInputVisible(layout());
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout layout = layout();
        if (isInsideNavigation(layout, mouseX, mouseY)) {
            updateNavigationScroll(layout);
            if (maxNavScroll > 0.5F) {
                float delta = (float) (verticalAmount * SCROLL_STEP);
                if (Math.abs(delta) < 0.5F) {
                    delta = verticalAmount > 0 ? SCROLL_STEP : -SCROLL_STEP;
                }
                navScroll = clamp(navScroll - delta, 0.0F, maxNavScroll);
                clearAndInit();
                return true;
            }
        }

        if (!isInsideContent(layout, mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        updateMaxScroll(layout);
        if (maxScroll <= 0.5F) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        closeDropdowns();
        float delta = (float) (verticalAmount * SCROLL_STEP);
        if (Math.abs(delta) < 0.5F) {
            delta = verticalAmount > 0 ? SCROLL_STEP : -SCROLL_STEP;
        }
        targetScrollOffset = clamp(targetScrollOffset - delta, 0.0F, maxScroll);
        rememberCurrentScroll();
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        // Closing is never blocked: invalid text is reverted by commitVisibleInputs, then we leave.
        if (isConfigEditPage() && editableConfig != null) {
            commitVisibleInputs();
            cachedEditableConfig = editableConfig;
        }

        closeDropdowns();
        super.close();
    }

    @Override
    public void tick() {
        int refreshInterval = refreshIntervalTicks();
        if (refreshInterval <= 0) {
            refreshTicks = 0;
            return;
        }

        refreshTicks++;
        if (refreshTicks >= refreshInterval) {
            refreshTicks = 0;
            requestConfig(false);
        }
    }

    private int refreshIntervalTicks() {
        if (isRealtimeStatusPage()) {
            return REALTIME_REFRESH_TICKS;
        }

        if (isConfigEditPage()) {
            return hasUnsavedChanges() || hasVisibleInputChanges() || hasFocusedTextField() || hasExpandedDropdown() ? -1 : CONFIG_IDLE_REFRESH_TICKS;
        }

        return CONFIG_IDLE_REFRESH_TICKS;
    }

    private boolean isRealtimeStatusPage() {
        return currentPage == Page.GAME || currentPage == Page.DEBUG;
    }

    private void buildNavigation(Layout layout) {
        int x = layout.panelX() + 10;
        int navTop = navigationTop(layout);
        int navBottom = navigationBottom(layout);
        int buttonHeight = 24;
        int buttonGap = 6;
        updateNavigationScroll(layout);
        int y = navTop - Math.round(navScroll);
        for (Page page : visiblePages()) {
            if (y + buttonHeight < navTop || y > navBottom) {
                y += buttonHeight + buttonGap;
                continue;
            }

            StyledButtonWidget button = new StyledButtonWidget(
                    x,
                    y,
                    layout.navWidth() - 20,
                    buttonHeight,
                    page.label,
                    "",
                    widget -> switchPage(page),
                    page == currentPage ? ButtonVariant.SELECTED : ButtonVariant.NORMAL
            );
            button.active = true;
            addDrawableChild(button);
            y += buttonHeight + buttonGap;
        }
    }

    private void buildGamePage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.usableContentWidth();

        if (serverSync == null) {
            CardBuilder card = addCard(layout, x, layout.contentY(), w, key("screen.card.game_status"));
            card.hint(key("screen.hint.waiting_server_sync"));
            markContentBottom(layout, card.finish());
            return;
        }

        boolean waiting = serverSync.gameState() == GameState.WAITING;
        String startTooltip = startGameTooltip(waiting);
        int currentY = addTeamManagementCard(layout, x, y, w);
        currentY = addGameActionRow(layout, x, currentY, w, waiting, startTooltip);
        currentY = addStatusPills(layout, x, currentY, w, List.of(
                new StatusBlock(key("screen.status.identity"), serverSync.playerRole(), serverSync.playerInTeam() ? 0xFFFFFFFF : 0xFFFFD966),
                new StatusBlock(key("screen.status.permission"), serverSync.canManage() ? key("screen.permission.op") : key("screen.permission.normal"), serverSync.canManage() ? 0xFF77E287 : 0xFFC9D4DE),
                new StatusBlock(key("role.hunter"), spec("screen.count.players", serverSync.hunterCount()), serverSync.hunterCount() > 0 ? 0xFF77E287 : 0xFFFFD966),
                new StatusBlock(key("role.runner"), spec("screen.count.players", serverSync.runnerCount()), serverSync.runnerCount() > 0 ? 0xFF77E287 : 0xFFFFD966),
                new StatusBlock(key("screen.status.wildcard"), compactWildcardDisplayName(), serverSync.activeWildcardRunning() ? 0xFF7FC2FF : 0xFFC9D4DE)
        ));

        ConfigSnapshot summaryConfig = serverSync.config();
        currentY = addTwoColumnCards(
                layout,
                x,
                currentY,
                w,
                MEDIUM_CARD_MAX_WIDTH,
                key("screen.card.current_game"),
                card -> {
                    card.info(key("screen.field.state"), waiting ? key("state.waiting") : stateName(serverSync.gameState()), stateColor(serverSync.gameState()));
                    card.info(key("screen.field.my_team"), serverSync.playerRole(), serverSync.playerInTeam() ? 0xFFFFFFFF : 0xFFFFD966);
                    if (waiting) {
                        card.info(key("screen.field.start_condition"), startConditionDisplay(startTooltip), startTooltip.isBlank() ? 0xFF77E287 : 0xFFFFD966);
                    } else {
                        card.info(key("screen.field.current_wildcard"), wildcardDisplayName(), serverSync.activeWildcardRunning() ? 0xFF7FC2FF : 0xFFC9D4DE);
                        card.info(key("screen.field.next_wildcard"), formatSeconds(serverSync.nextWildcardSeconds()), 0xFF7FC2FF);
                    }
                },
                key("screen.card.rules_summary"),
                card -> {
                    card.info(key("hud.status.runner_win"), runnerWinSummary(summaryConfig), 0xFFFFFFFF);
                    card.info(key("hud.status.hunter_win"), hunterWinSummary(summaryConfig), 0xFFFFFFFF);
                    card.info(key("hud.status.hunter_respawn"), respawnSummary(summaryConfig.hunterRespawnMode(), summaryConfig.hunterLives(), RespawnMode.INFINITE), 0xFFFFFFFF);
                    card.info(key("hud.status.runner_respawn"), respawnSummary(summaryConfig.runnerRespawnMode(), summaryConfig.runnerLives(), RespawnMode.LIMITED_LIVES), 0xFFFFFFFF);
                }
        );

        markContentBottom(layout, currentY);
    }

    private int addGameActionRow(Layout layout, int x, int currentY, int w, boolean waiting, String startTooltip) {
        List<ButtonSpec> actions = new ArrayList<>();
        if (waiting) {
            if (canManage) {
                actions.add(new ButtonSpec(key("screen.button.start_game"), widget -> sendGameAction(GameAction.START_GAME), ButtonVariant.PRIMARY, startTooltip.isBlank(), startTooltip));
            }
        } else {
            boolean hudShown = ClientGameStatus.isStatusHudToggled();
            actions.add(new ButtonSpec(
                    hudShown ? key("screen.button.hide_status_hud") : key("screen.button.show_status_hud"),
                    widget -> toggleStatusHud(),
                    hudShown ? ButtonVariant.TOGGLE_ON : ButtonVariant.NORMAL,
                    true,
                    key("screen.tooltip.status_hud")
            ));
        }
        if (!actions.isEmpty()) {
            currentY = addButtonRow(layout, actions, x, currentY, w, actions.size(), 240) - ROW_GAP + CARD_GAP;
        }
        return currentY;
    }

    private void toggleStatusHud() {
        ClientGameStatus.toggleStatusHud();
        clearAndInit();
    }

    private void buildRulesPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.usableContentWidth();

        if (editableConfig == null) {
            CardBuilder card = addCard(layout, x, layout.contentY(), w, key("screen.page.rules"));
            card.hint(key("screen.hint.waiting_config_sync"));
            markContentBottom(layout, card.finish());
            return;
        }

        if (rulesSubPage == null) {
            buildRulesHub(layout, x, y, w);
            return;
        }

        switch (rulesSubPage) {
            case TIME_BOUNDARY -> buildTimeBoundarySubPage(layout, x, y, w);
            case VICTORY -> buildVictorySubPage(layout, x, y, w);
            case RESPAWN -> buildRespawnSubPage(layout, x, y, w);
            case KILL_CREDIT -> buildKillCreditSubPage(layout, x, y, w);
            case BALANCE -> buildBalanceSubPage(layout, x, y, w);
        }
    }

    private void buildRulesHub(Layout layout, int x, int y, int w) {
        int currentY = y;
        if (isRoundRunning()) {
            CardBuilder note = addCard(layout, x, currentY, w, key("screen.card.live_rules"));
            note.hint(key("screen.hint.live_rules"));
            currentY = note.finish() + CARD_GAP;
        }

        RulesSubPage[] subPages = RulesSubPage.values();
        for (int i = 0; i < subPages.length; i += 2) {
            RulesSubPage left = subPages[i];
            if (i + 1 < subPages.length) {
                RulesSubPage right = subPages[i + 1];
                currentY = addTwoColumnCards(layout, x, currentY, w, LARGE_CARD_MAX_WIDTH, left.label, card -> buildRulesHubCard(card, left), right.label, card -> buildRulesHubCard(card, right));
            } else {
                int cardWidth = Math.min(w, LARGE_CARD_MAX_WIDTH);
                CardBuilder card = addCard(layout, x + Math.max(0, (w - cardWidth) / 2), currentY, cardWidth, left.label);
                buildRulesHubCard(card, left);
                currentY = card.finish() + CARD_GAP;
            }
        }
        markContentBottom(layout, currentY);
    }

    private void buildRulesHubCard(CardBuilder card, RulesSubPage subPage) {
        card.titleButtons(List.of(new ButtonSpec(key("screen.button.edit"), widget -> openRulesSubPage(subPage), ButtonVariant.PRIMARY, true, subPage.description)), 60);
        card.hint(rulesSummary(subPage));
    }

    private String rulesSummary(RulesSubPage subPage) {
        ConfigSnapshot config = editableConfig;
        return switch (subPage) {
            case TIME_BOUNDARY -> tr(spec("screen.summary.time_boundary", config.preparingSeconds(),
                    config.hunterPrepareBoundaryEnabled() ? tr(spec("screen.summary.boundary_on", config.hunterPrepareBoundaryRadius())) : tr(key("screen.summary.boundary_off"))));
            case VICTORY -> tr(spec("screen.summary.victory", runnerWinSummary(config), hunterWinSummary(config)));
            case RESPAWN -> tr(spec("screen.summary.respawn",
                    respawnSummary(config.hunterRespawnMode(), config.hunterLives(), RespawnMode.INFINITE),
                    respawnSummary(isHunterKillCountMode() ? RespawnMode.INFINITE.name() : config.runnerRespawnMode(), config.runnerLives(), RespawnMode.LIMITED_LIVES),
                    tr(config.randomRespawnEnabled() ? key("screen.toggle.enabled") : key("screen.toggle.disabled"))));
            case KILL_CREDIT -> tr(config.environmentKillsEnabled()
                    ? spec("screen.summary.kill_credit", config.hunterHitCreditSeconds(), config.environmentDeathsPerKill())
                    : spec("screen.summary.kill_credit_no_env", config.hunterHitCreditSeconds()));
            case BALANCE -> tr(spec("screen.summary.balance", config.hunterDamageMultiplierPercent(), config.hunterSpeedPercent(), config.runnerSpeedPercent()));
        };
    }

    private String runnerWinSummary(ConfigSnapshot config) {
        RunnerVictoryType type = RunnerVictoryType.fromConfig(config.runnerVictoryType(), RunnerVictoryType.DRAGON);
        String base = tr(type.getTranslationKey());
        return switch (type) {
            case DRAGON -> base;
            case SURVIVE_TIME -> base + " " + tr(spec("hud.status.paren_seconds", config.surviveTimeSeconds()));
            case REACH_LOCATION -> base + " (" + config.targetX() + ", " + config.targetY() + ", " + config.targetZ() + ")";
            case COLLECT_ITEM -> base + " (" + config.targetItemCount() + "x " + shortItemId(config.targetItemId()) + ")";
        };
    }

    private String hunterWinSummary(ConfigSnapshot config) {
        HunterVictoryType type = HunterVictoryType.fromConfig(config.hunterVictoryType(), HunterVictoryType.RUNNERS_OUT);
        String base = tr(type.getTranslationKey());
        return type == HunterVictoryType.RUNNER_KILL_COUNT
                ? base + " " + tr(spec("hud.status.paren_kills", config.hunterRunnerKillTarget()))
                : base;
    }

    private String respawnSummary(String modeValue, int lives, RespawnMode fallback) {
        RespawnMode mode = RespawnMode.fromConfig(modeValue, fallback);
        String base = tr(key("config.respawn_mode." + mode.name().toLowerCase()));
        return mode == RespawnMode.LIMITED_LIVES ? base + " " + tr(spec("hud.status.paren_lives", lives)) : base;
    }

    private static String shortItemId(String itemId) {
        if (itemId == null) {
            return "";
        }
        int colon = itemId.indexOf(':');
        return colon >= 0 ? itemId.substring(colon + 1) : itemId;
    }

    private void buildTimeBoundarySubPage(Layout layout, int x, int y, int w) {
        int bottom = addTwoColumnCards(
                layout,
                x,
                y,
                w,
                key("screen.card.game_flow"),
                card -> {
                    card.number(NumberField.PREPARING_SECONDS);
                    card.hint(key("screen.hint.preparing_seconds"));
                },
                key("screen.card.world_boundary"),
                card -> {
                    boolean boundaryEnabled = editableConfig.hunterPrepareBoundaryEnabled();
                    card.booleanField(BooleanField.HUNTER_PREPARE_BOUNDARY_ENABLED);
                    card.number(NumberField.HUNTER_PREPARE_BOUNDARY_RADIUS, boundaryEnabled);
                    card.hint(boundaryEnabled ? key("screen.hint.boundary_enabled") : key("screen.hint.boundary_disabled"));
                }
        );
        markContentBottom(layout, bottom);
    }

    private void buildVictorySubPage(Layout layout, int x, int y, int w) {
        RunnerVictoryType victoryType = RunnerVictoryType.fromConfig(editableConfig.runnerVictoryType(), RunnerVictoryType.DRAGON);
        HunterVictoryType hunterVictoryType = HunterVictoryType.fromConfig(editableConfig.hunterVictoryType(), HunterVictoryType.RUNNERS_OUT);
        int bottom = addTwoColumnCards(
                layout,
                x,
                y,
                w,
                360,
                260,
                key("screen.card.runner_victory"),
                card -> buildRunnerVictoryCard(card, victoryType),
                key("screen.card.hunter_victory"),
                card -> buildHunterVictoryCard(card, hunterVictoryType)
        );
        markContentBottom(layout, bottom);
    }

    private void buildRespawnSubPage(Layout layout, int x, int y, int w) {
        RespawnMode hunterMode = RespawnMode.fromConfig(editableConfig.hunterRespawnMode(), RespawnMode.INFINITE);
        boolean killCountMode = isHunterKillCountMode();
        RespawnMode runnerMode = killCountMode ? RespawnMode.INFINITE : RespawnMode.fromConfig(editableConfig.runnerRespawnMode(), RespawnMode.LIMITED_LIVES);
        int bottom = addTwoColumnCards(
                layout,
                x,
                y,
                w,
                LARGE_CARD_MAX_WIDTH,
                key("screen.card.hunter_respawn"),
                card -> {
                    card.dropdown(DropdownField.HUNTER_RESPAWN_MODE);
                    card.number(NumberField.HUNTER_LIVES, hunterMode == RespawnMode.LIMITED_LIVES);
                    card.number(NumberField.HUNTER_RESPAWN_SECONDS, hunterMode != RespawnMode.NO_RESPAWN);
                    String hunterHint = respawnHint(hunterMode, key("role.hunter"));
                    if (!hunterHint.isBlank()) {
                        card.hint(hunterHint);
                    }
                },
                key("screen.card.runner_respawn"),
                card -> {
                    if (killCountMode) {
                        card.info(key("config.dropdown.runner_respawn_mode"), key("screen.respawn.infinite_locked"), 0xFF7FC2FF);
                        card.number(NumberField.RUNNER_LIVES, false);
                        card.number(NumberField.RUNNER_RESPAWN_SECONDS, true);
                        card.hint(key("screen.hint.kill_count_runner_infinite"));
                    } else {
                        card.dropdown(DropdownField.RUNNER_RESPAWN_MODE);
                        card.number(NumberField.RUNNER_LIVES, runnerMode == RespawnMode.LIMITED_LIVES);
                        card.number(NumberField.RUNNER_RESPAWN_SECONDS, runnerMode != RespawnMode.NO_RESPAWN);
                        String runnerHint = respawnHint(runnerMode, key("role.runner"));
                        if (!runnerHint.isBlank()) {
                            card.hint(runnerHint);
                        }
                    }
                }
        );
        boolean randomRespawn = editableConfig.randomRespawnEnabled();
        bottom = addTwoColumnCards(
                layout,
                x,
                bottom,
                w,
                LARGE_CARD_MAX_WIDTH,
                key("screen.card.random_respawn"),
                card -> {
                    card.booleanField(BooleanField.RANDOM_RESPAWN_ENABLED);
                    card.number(NumberField.RUNNER_RESPAWN_DISTANCE, randomRespawn);
                    card.number(NumberField.HUNTER_RESPAWN_DISTANCE, randomRespawn);
                    card.number(NumberField.HUNTER_RESPAWN_RUNNER_CLEARANCE, randomRespawn);
                    card.number(NumberField.HUNTER_RESPAWN_PENALTY_SECONDS);
                    card.hint(randomRespawn ? key("screen.hint.random_respawn") : key("screen.hint.random_respawn_disabled"));
                    card.hint(key("screen.hint.death_blackout"));
                },
                key("screen.card.death_drops"),
                card -> {
                    card.booleanField(BooleanField.RUNNER_DEATH_NO_DROPS);
                    card.booleanField(BooleanField.HUNTER_DEATH_NO_DROPS);
                    card.hint(key("screen.hint.death_drops"));
                }
        );
        markContentBottom(layout, bottom);
    }

    private void buildKillCreditSubPage(Layout layout, int x, int y, int w) {
        int cardWidth = Math.min(w, LARGE_CARD_MAX_WIDTH);
        int cardX = x + Math.max(0, (w - cardWidth) / 2);
        CardBuilder card = addCard(layout, cardX, y, cardWidth, key("screen.card.kill_credit"));
        card.number(NumberField.HUNTER_HIT_CREDIT_SECONDS);
        card.hint(key("screen.hint.hit_credit"));
        card.gap(4);
        card.booleanField(BooleanField.ENVIRONMENT_KILLS_ENABLED);
        if (editableConfig.environmentKillsEnabled()) {
            card.number(NumberField.ENVIRONMENT_DEATHS_PER_KILL);
            card.hint(key("screen.hint.environment_deaths"));
        } else {
            card.hint(key("screen.hint.environment_kills_disabled"));
        }
        if (!isHunterKillCountMode()) {
            card.hint(key("screen.hint.environment_deaths_unused"));
        }
        markContentBottom(layout, card.finish() + CARD_GAP);
    }

    private void buildBalanceSubPage(Layout layout, int x, int y, int w) {
        boolean piglinBoostEnabled = editableConfig.piglinPearlBoostEnabled();
        int bottom = addTwoColumnCards(
                layout,
                x,
                y,
                w,
                key("screen.card.role_balance"),
                card -> {
                    card.number(NumberField.HUNTER_DAMAGE_MULTIPLIER_PERCENT);
                    card.number(NumberField.HUNTER_SPEED_PERCENT);
                    card.number(NumberField.RUNNER_SPEED_PERCENT);
                    card.hint(key("screen.hint.role_balance"));
                },
                key("screen.card.piglin_bartering"),
                card -> {
                    card.booleanField(BooleanField.PIGLIN_PEARL_BOOST_ENABLED);
                    card.number(NumberField.PIGLIN_PEARL_CHANCE_PERCENT, piglinBoostEnabled);
                    card.hint(piglinBoostEnabled ? key("screen.hint.piglin_bartering") : key("screen.hint.piglin_bartering_disabled"));
                }
        );
        int cardWidth = Math.min(w, MEDIUM_CARD_MAX_WIDTH * 2 + TWO_COLUMN_GAP);
        int cardX = x + Math.max(0, (w - cardWidth) / 2);
        CardBuilder locatorCard = addCard(layout, cardX, bottom, cardWidth, key("screen.card.locator_bar"));
        locatorCard.booleanField(BooleanField.LOCATOR_BAR_TEAM_ONLY);
        locatorCard.hint(key("screen.hint.locator_bar"));
        markContentBottom(layout, locatorCard.finish() + CARD_GAP);
    }

    private void buildWildcardPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.usableContentWidth();

        if (editableConfig == null) {
            CardBuilder card = addCard(layout, x, layout.contentY(), w, key("screen.card.wildcard_rules"));
            card.hint(key("screen.hint.waiting_config_sync"));
            markContentBottom(layout, card.finish());
            return;
        }

        if (selectedWildcardSettings != null && hasWildcardSettings(selectedWildcardSettings)) {
            // Sub-page: only the per-wildcard settings card; the header breadcrumb/back button leads back.
            markContentBottom(layout, addWildcardSettingsCard(layout, x, y, w, selectedWildcardSettings));
            return;
        }

        selectedWildcardSettings = null;
        int bottom = addTwoColumnCards(
                layout,
                x,
                y,
                w,
                MEDIUM_CARD_MAX_WIDTH,
                key("screen.card.wildcard_general"),
                card -> {
                    card.titleButtons(List.of(
                            new ButtonSpec(key("screen.button.enable_all_wildcards"), widget -> setAllWildcards(true), ButtonVariant.NORMAL, canEditConfig(), key("screen.tooltip.enable_all_wildcards")),
                            new ButtonSpec(key("screen.button.disable_all_wildcards"), widget -> setAllWildcards(false), ButtonVariant.DANGER, canEditConfig(), key("screen.tooltip.disable_all_wildcards"))
                    ), 70);
                    card.dropdown(DropdownField.WILDCARD_INTERVAL_MODE);
                    if ("RANDOM".equals(editableConfig.wildcardIntervalMode())) {
                        card.numberPair(NumberField.WILDCARD_INTERVAL_MIN_SECONDS, NumberField.WILDCARD_INTERVAL_MAX_SECONDS);
                    } else {
                        card.number(NumberField.WILDCARD_INTERVAL_SECONDS);
                    }
                    card.dropdown(DropdownField.WILDCARD_DURATION_MODE);
                    if ("RANDOM".equals(editableConfig.wildcardDurationMode())) {
                        card.numberPair(NumberField.WILDCARD_DURATION_MIN_SECONDS, NumberField.WILDCARD_DURATION_MAX_SECONDS);
                    } else {
                        card.number(NumberField.WILDCARD_DURATION_SECONDS);
                    }
                    card.info(key("screen.field.enabled_wildcards"), spec("screen.count.enabled_wildcards", enabledWildcardCount(editableConfig), ToggleField.values().length), enabledWildcardCount(editableConfig) > 0 ? 0xFF77E287 : 0xFFC9D4DE);
                },
                key("screen.card.wildcard_trigger_status"),
                card -> {
                    if (serverSync == null) {
                        card.hint(key("screen.hint.waiting_server_sync"));
                    } else {
                        card.info(key("screen.field.current_wildcard"), wildcardDisplayName(), serverSync.activeWildcardRunning() ? 0xFF7FC2FF : 0xFFC9D4DE);
                        card.info(key("screen.field.next_wildcard"), formatSeconds(serverSync.nextWildcardSeconds()), 0xFFC9D4DE);
                        card.info(key("screen.field.remaining"), formatSeconds(serverSync.activeWildcardRemainingSeconds()), serverSync.activeWildcardRunning() ? 0xFF7FC2FF : 0xFFC9D4DE);
                    }
                }
        );
        for (WildcardCategory category : WildcardCategory.values()) {
            bottom = addWildcardToggleMatrixCard(layout, x, bottom, w, category);
        }
        markContentBottom(layout, bottom);
    }

    private void buildRunnerVictoryCard(CardBuilder card, RunnerVictoryType victoryType) {
        card.dropdown(DropdownField.RUNNER_VICTORY_TYPE);
        switch (victoryType) {
            case DRAGON -> card.hint(key("screen.hint.dragon_goal"));
            case SURVIVE_TIME -> {
                card.number(NumberField.SURVIVE_TIME_SECONDS);
                boolean borderEnabled = editableConfig.surviveBorderEnabled();
                card.booleanField(BooleanField.SURVIVE_BORDER_ENABLED);
                card.number(NumberField.SURVIVE_BORDER_RADIUS, borderEnabled);
                card.hint(key("screen.hint.survive_border"));
            }
            case REACH_LOCATION -> {
                card.dropdown(DropdownField.TARGET_DIMENSION);
                card.coordinates(NumberField.TARGET_X, NumberField.TARGET_Y, NumberField.TARGET_Z);
                card.number(NumberField.TARGET_RADIUS);
                card.buttonGrid(List.of(new ButtonSpec(
                        key("screen.button.set_current_location"),
                        widget -> setTargetToCurrentLocation(),
                        ButtonVariant.NORMAL,
                        canEditConfig() && editableConfig != null && client != null && client.player != null && client.world != null,
                        key("screen.tooltip.set_current_location")
                )), 1, 132);
            }
            case COLLECT_ITEM -> {
                card.string(StringField.TARGET_ITEM_ID);
                card.number(NumberField.TARGET_ITEM_COUNT);
            }
        }
    }

    private void buildHunterVictoryCard(CardBuilder card, HunterVictoryType hunterVictoryType) {
        card.dropdown(DropdownField.HUNTER_VICTORY_TYPE);
        if (hunterVictoryType == HunterVictoryType.RUNNER_KILL_COUNT) {
            card.number(NumberField.HUNTER_RUNNER_KILL_TARGET);
            card.hint(key("screen.hint.runner_locked_infinite"));
        } else {
            card.dropdown(DropdownField.RUNNER_TEAM_LOSS_MODE);
        }
    }

    private String respawnHint(RespawnMode mode, String roleName) {
        return switch (mode) {
            case INFINITE -> spec("screen.hint.respawn_infinite", roleName);
            case NO_RESPAWN -> spec("screen.hint.no_respawn", roleName);
            case LIMITED_LIVES -> "";
        };
    }

    private void buildDebugPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.usableContentWidth();

        CardBuilder actionCard = addCard(layout, x, y, w, key("screen.card.game_controls"));
        actionCard.hint(canManage ? key("screen.hint.debug_actions") : key("screen.error.debug_op_only"));
        actionCard.buttonGrid(List.of(
                new ButtonSpec(key("screen.button.stop_game"), widget -> sendDebugAction(DebugAction.STOP_GAME), ButtonVariant.DANGER, canManage && isGameActive(), key("screen.tooltip.stop_game")),
                new ButtonSpec(key("screen.button.roll_wildcard"), widget -> sendDebugAction(DebugAction.ROLL_WILDCARD), ButtonVariant.NORMAL, canManage && serverSync != null && serverSync.gameState() == GameState.RUNNING, key("screen.tooltip.roll_wildcard")),
                new ButtonSpec(key("screen.button.stop_wildcard"), widget -> sendDebugAction(DebugAction.STOP_WILDCARD), ButtonVariant.DANGER, canManage && serverSync != null && serverSync.activeWildcardRunning(), key("screen.tooltip.stop_wildcard"))
        ), w >= 900 ? 3 : 2, 150);
        int currentY = actionCard.finish() + CARD_GAP;

        CardBuilder testCard = addCard(layout, x, currentY, w, key("screen.card.test_wildcard"));
        List<ButtonSpec> testButtons = new ArrayList<>();
        for (ToggleField field : ToggleField.values()) {
            boolean enabled = editableConfig != null && getToggle(editableConfig, field);
            testButtons.add(new ButtonSpec(
                    enabled ? spec("screen.button.test_wildcard", field.label) : spec("screen.button.wildcard_disabled", field.label),
                    widget -> sendTestWildcard(field),
                    ButtonVariant.NORMAL,
                    canManage && enabled,
                    enabled ? key("screen.tooltip.test_wildcard") : key("screen.tooltip.test_wildcard_disabled")
            ));
        }
        testCard.buttonGrid(testButtons, w >= 900 ? 3 : 2, 150);
        markContentBottom(layout, testCard.finish());
    }

    private void buildFooter(Layout layout) {
        int y = layout.panelY() + layout.panelHeight() - 32;
        if (isConfigEditPage()) {
            int buttonWidth = Math.max(76, Math.min(108, (layout.usableContentWidth() - 16) / 3));
            int x = layout.contentX() + Math.max(0, layout.usableContentWidth() - (buttonWidth * 3 + 16));
            String saveLabel = isRoundRunning() ? key("screen.button.apply_rules") : key("screen.button.save_config");
            String saveTooltip = isRoundRunning() ? key("screen.tooltip.apply_rules") : key("screen.tooltip.save_config");
            saveButton = addButton(x, y, buttonWidth, 24, saveLabel, saveTooltip, widget -> saveConfig(), ButtonVariant.PRIMARY, canSaveConfig());
            addButton(x + buttonWidth + 8, y, buttonWidth, 24, key("screen.button.restore_default"), key("screen.tooltip.restore_default"), widget -> restoreDefaultConfig(), ButtonVariant.NORMAL, canEditConfig() && editableConfig != null);
            addButton(x + (buttonWidth + 8) * 2, y, buttonWidth, 24, key("screen.button.close"), "", widget -> close(), ButtonVariant.NORMAL, true);
            return;
        }

        addButton(layout.contentX() + layout.usableContentWidth() - 86, y, 86, 24, key("screen.button.close"), "", widget -> close(), ButtonVariant.NORMAL, true);
    }

    private CardBuilder addCard(Layout layout, int x, int y, int width, String title) {
        return new CardBuilder(layout, x, y, width, title);
    }

    private int addTwoColumnCards(
            Layout layout,
            int x,
            int y,
            int width,
            String leftTitle,
            CardBody leftBody,
            String rightTitle,
            CardBody rightBody
    ) {
        return addTwoColumnCards(layout, x, y, width, MEDIUM_CARD_MAX_WIDTH, leftTitle, leftBody, rightTitle, rightBody);
    }

    private int addTwoColumnCards(
            Layout layout,
            int x,
            int y,
            int width,
            int maxCardWidth,
            String leftTitle,
            CardBody leftBody,
            String rightTitle,
            CardBody rightBody
    ) {
        return addTwoColumnCards(layout, x, y, width, maxCardWidth, maxCardWidth, leftTitle, leftBody, rightTitle, rightBody);
    }

    private int addTwoColumnCards(
            Layout layout,
            int x,
            int y,
            int width,
            int leftMaxWidth,
            int rightMaxWidth,
            String leftTitle,
            CardBody leftBody,
            String rightTitle,
            CardBody rightBody
    ) {
        if (useTwoColumns(width)) {
            int availableWidth = width - TWO_COLUMN_GAP;
            int minColumnWidth = Math.min(150, Math.max(100, availableWidth / 2));
            int totalMaxWidth = Math.max(1, leftMaxWidth + rightMaxWidth);
            int leftWidth = Math.min(leftMaxWidth, Math.max(minColumnWidth, availableWidth * leftMaxWidth / totalMaxWidth));
            int rightWidth = Math.min(rightMaxWidth, availableWidth - leftWidth);
            if (rightWidth < minColumnWidth) {
                rightWidth = minColumnWidth;
                leftWidth = availableWidth - rightWidth;
            }
            if (leftWidth < minColumnWidth) {
                leftWidth = minColumnWidth;
                rightWidth = availableWidth - leftWidth;
            }

            int extraWidth = Math.max(0, availableWidth - leftWidth - rightWidth);
            int addLeft = Math.min(extraWidth, Math.max(0, leftMaxWidth - leftWidth));
            leftWidth += addLeft;
            extraWidth -= addLeft;
            rightWidth += Math.min(extraWidth, Math.max(0, rightMaxWidth - rightWidth));

            int rowWidth = leftWidth + rightWidth + TWO_COLUMN_GAP;
            int rowX = x + Math.max(0, (width - rowWidth) / 2);
            CardBuilder leftCard = addCard(layout, rowX, y, leftWidth, leftTitle);
            leftBody.build(leftCard);
            CardBuilder rightCard = addCard(layout, rowX + leftWidth + TWO_COLUMN_GAP, y, rightWidth, rightTitle);
            rightBody.build(rightCard);
            int leftBottom = leftCard.finish();
            int rightBottom = rightCard.finish();
            return Math.max(leftBottom, rightBottom) + CARD_GAP;
        }

        int cardWidth = Math.min(width, Math.max(leftMaxWidth, rightMaxWidth));
        int cardX = x + Math.max(0, (width - cardWidth) / 2);
        CardBuilder leftCard = addCard(layout, cardX, y, cardWidth, leftTitle);
        leftBody.build(leftCard);
        int nextY = leftCard.finish() + CARD_GAP;
        CardBuilder rightCard = addCard(layout, cardX, nextY, cardWidth, rightTitle);
        rightBody.build(rightCard);
        return rightCard.finish() + CARD_GAP;
    }

    private int addWildcardToggleMatrixCard(Layout layout, int x, int y, int width, WildcardCategory category) {
        int columns = wildcardToggleColumns(width);
        int cardWidth = width;
        int cardX = x;
        ToggleField[] fields = ToggleField.inCategory(category);
        int enabledCount = 0;
        for (ToggleField field : fields) {
            if (getToggle(editableConfig, field)) {
                enabledCount++;
            }
        }
        CardBuilder matrixCard = addCard(layout, cardX, y, cardWidth, spec("screen.card.wildcard_category", category.label, enabledCount, fields.length));
        matrixCard.titleButtons(List.of(
                new ButtonSpec(key("screen.button.enable_category"), widget -> setCategoryWildcards(category, true), ButtonVariant.NORMAL, canEditConfig(), key("screen.tooltip.enable_category")),
                new ButtonSpec(key("screen.button.disable_category"), widget -> setCategoryWildcards(category, false), ButtonVariant.DANGER, canEditConfig(), key("screen.tooltip.disable_category"))
        ), 70);
        matrixCard.toggleGrid(fields, columns);
        return matrixCard.finish() + CARD_GAP;
    }

    private int addWildcardSettingsCard(Layout layout, int x, int y, int width, ToggleField field) {
        int cardWidth = Math.min(width, MEDIUM_CARD_MAX_WIDTH);
        int cardX = x + Math.max(0, (width - cardWidth) / 2);
        CardBuilder card = addCard(layout, cardX, y, cardWidth, spec("screen.card.wildcard_settings", field.label));
        boolean enabled = getToggle(editableConfig, field);
        card.info(key("screen.field.wildcard_state"), enabled ? key("screen.toggle.enabled") : key("screen.toggle.disabled"), enabled ? 0xFF77E287 : 0xFF9FAAB4);
        switch (field) {
            case HUNTER_RADAR -> {
                card.number(NumberField.HUNTER_RADAR_WARNING_DISTANCE);
                card.hint(key("screen.hint.hunter_radar_settings"));
            }
            case SPACE_SHIFT -> {
                card.number(NumberField.SPACE_SHIFT_INTERVAL_SECONDS);
                card.hint(key("screen.hint.space_shift_settings"));
            }
            case SUPPLY_DROP -> {
                card.number(NumberField.SUPPLY_DROP_INTERVAL_SECONDS);
                card.hint(key("screen.hint.supply_drop_settings"));
            }
            case BLOCK_DECAY -> {
                card.number(NumberField.BLOCK_DECAY_SECONDS);
                card.hint(key("screen.hint.block_decay_settings"));
            }
            case PEARL_FRENZY -> {
                card.number(NumberField.PEARL_FRENZY_MAX_PEARLS);
                card.number(NumberField.PEARL_FRENZY_INTERVAL_SECONDS);
                card.hint(key("screen.hint.pearl_frenzy_settings"));
            }
            case WIND_CHARGE_BRAWL -> {
                card.number(NumberField.WIND_CHARGE_BRAWL_INTERVAL_SECONDS);
                card.number(NumberField.WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT);
                card.hint(key("screen.hint.wind_charge_brawl_settings"));
            }
            case BACKROOMS -> {
                card.number(NumberField.BACKROOMS_DURATION_SECONDS);
                card.hint(key("screen.hint.backrooms_settings"));
            }
            default -> card.hint(key("screen.hint.no_wildcard_settings"));
        }
        card.hint(wildcardInfoTooltip(field));
        return card.finish() + CARD_GAP;
    }

    private int addStatusBlocks(Layout layout, int x, int y, int width, List<StatusBlock> blocks) {
        int gap = 12;
        int columns;
        if (width >= 430) {
            columns = 3;
        } else if (width >= 300) {
            columns = 2;
        } else {
            columns = 1;
        }
        columns = Math.max(1, Math.min(columns, blocks.size()));

        int blockHeight = 48;
        int blockWidth = Math.min(STATUS_BLOCK_MAX_WIDTH, Math.max(80, (width - (columns - 1) * gap) / columns));
        int gridWidth = blockWidth * columns + (columns - 1) * gap;
        int gridX = x + Math.max(0, (width - gridWidth) / 2);
        for (int i = 0; i < blocks.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            addStatusBlock(layout, gridX + column * (blockWidth + gap), y + row * (blockHeight + gap), blockWidth, blockHeight, blocks.get(i));
        }

        int rows = (blocks.size() + columns - 1) / columns;
        return y + rows * blockHeight + (rows - 1) * gap + CARD_GAP;
    }

    private void addStatusBlock(Layout layout, int x, int y, int width, int height, StatusBlock block) {
        if (isVisibleInContentPartial(layout, y, height)) {
            boxes.add(new Box(x, y, width, height, 0x77303A46, block.color()));
        }
        if (isVisibleInContentPartial(layout, y, height)) {
            labels.add(new Label(block.label(), x + 8, y + 7, 0xFF9FAAB4, false, Math.max(20, width - 16)));
            labels.add(new Label(block.value(), x + 8, y + 27, block.color(), true, Math.max(20, width - 16)));
        }
    }

    private int addStatusPills(Layout layout, int x, int y, int width, List<StatusBlock> blocks) {
        int gap = 8;
        int rowGap = 7;
        int pillHeight = 28;
        List<Integer> pillWidths = new ArrayList<>();
        for (StatusBlock block : blocks) {
            int textWidth = textRenderer.getWidth(tr(spec("screen.status_pill", tr(block.label()), tr(block.value()))));
            pillWidths.add(Math.min(190, Math.max(88, textWidth + 24)));
        }

        int index = 0;
        int rowY = y;
        while (index < blocks.size()) {
            int rowStart = index;
            int rowWidth = 0;
            while (index < blocks.size()) {
                int nextWidth = pillWidths.get(index);
                int candidateWidth = rowWidth == 0 ? nextWidth : rowWidth + gap + nextWidth;
                if (candidateWidth > width && index > rowStart) {
                    break;
                }

                rowWidth = candidateWidth;
                index++;
            }

            int pillX = x + Math.max(0, (width - rowWidth) / 2);
            for (int i = rowStart; i < index; i++) {
                StatusBlock block = blocks.get(i);
                int pillWidth = pillWidths.get(i);
                addStatusPill(layout, pillX, rowY, pillWidth, pillHeight, block);
                pillX += pillWidth + gap;
            }

            rowY += pillHeight + rowGap;
        }

        return rowY - rowGap + CARD_GAP;
    }

    private void addStatusPill(Layout layout, int x, int y, int width, int height, StatusBlock block) {
        if (isVisibleInContentPartial(layout, y, height)) {
            boxes.add(new Box(x, y, width, height, 0x66303A46, block.color()));
        }

        String label = spec("screen.label_colon", tr(block.label()));
        int labelWidth = Math.min(textRenderer.getWidth(tr(label)), Math.max(20, width / 2));
        int textY = y + Math.max(4, (height - textRenderer.fontHeight) / 2);
        if (isVisibleInContentPartial(layout, y, height)) {
            labels.add(new Label(label, x + 8, textY, 0xFF9FAAB4, false, labelWidth));
            labels.add(new Label(block.value(), x + 8 + labelWidth + 4, textY, block.color(), true, Math.max(20, width - labelWidth - 18)));
        }
    }

    private int addTeamManagementCard(Layout layout, int x, int y, int width) {
        // Two side-by-side team blocks whenever there is room for two 150px sections, so the home page fits without scrolling.
        boolean twoColumns = width >= 2 * 132 + CARD_PADDING_X * 2 + TWO_COLUMN_GAP;
        int sectionGap = TWO_COLUMN_GAP;
        int availableSectionWidth = twoColumns ? (width - CARD_PADDING_X * 2 - sectionGap) / 2 : width - CARD_PADDING_X * 2;
        int sectionWidth = Math.min(twoColumns ? SMALL_CARD_MAX_WIDTH : MEDIUM_CARD_MAX_WIDTH, Math.max(80, availableSectionWidth));
        int sectionsWidth = twoColumns ? sectionWidth * 2 + sectionGap : sectionWidth;
        int cardWidth = Math.min(width, sectionsWidth + CARD_PADDING_X * 2);
        int cardX = x + Math.max(0, (width - cardWidth) / 2);
        int contentX = cardX + CARD_PADDING_X;
        int contentWidth = Math.max(80, cardWidth - CARD_PADDING_X * 2);
        int sectionsX = contentX + Math.max(0, (contentWidth - sectionsWidth) / 2);
        int sectionHeight = 46;
        int sectionY = y + CARD_PADDING_TOP + CARD_TITLE_HEIGHT + 4;
        int secondSectionY = twoColumns ? sectionY : sectionY + sectionHeight + sectionGap;
        int sectionsBottom = twoColumns ? sectionY + sectionHeight : secondSectionY + sectionHeight;
        int bottomRowY = sectionsBottom + 6;
        int cardHeight = bottomRowY + BUTTON_HEIGHT + CARD_PADDING_BOTTOM - y;

        drawCardBackground(layout, cardX, y, cardWidth, cardHeight);
        addCardTitle(layout, key("screen.card.team_management"), contentX, y + CARD_PADDING_TOP);

        boolean isHunter = PlayerRole.HUNTER.getTranslationKey().equals(serverSync.playerRole());
        boolean isRunner = PlayerRole.RUNNER.getTranslationKey().equals(serverSync.playerRole());
        boolean canChangeTeam = canChangeTeam();
        String lockedTooltip = canChangeTeam ? "" : key("screen.team.locked_tooltip");
        addTeamSection(
                layout,
                sectionsX,
                sectionY,
                sectionWidth,
                sectionHeight,
                key("team.hunters"),
                serverSync.hunterCount(),
                isHunter,
                0xFFD76474,
                isHunter ? key("screen.team.already_hunter") : key("screen.team.join_hunter"),
                canChangeTeam ? (isHunter ? key("screen.team.already_hunter_tooltip") : key("screen.team.join_hunter_tooltip")) : lockedTooltip,
                widget -> sendTeamAction(TeamAction.JOIN_HUNTER),
                canChangeTeam && !isHunter
        );
        addTeamSection(
                layout,
                twoColumns ? sectionsX + sectionWidth + sectionGap : sectionsX,
                secondSectionY,
                sectionWidth,
                sectionHeight,
                key("team.runners"),
                serverSync.runnerCount(),
                isRunner,
                0xFF7FC2FF,
                isRunner ? key("screen.team.already_runner") : key("screen.team.join_runner"),
                canChangeTeam ? (isRunner ? key("screen.team.already_runner_tooltip") : key("screen.team.join_runner_tooltip")) : lockedTooltip,
                widget -> sendTeamAction(TeamAction.JOIN_RUNNER),
                canChangeTeam && !isRunner
        );

        int leaveWidth = Math.min(260, Math.max(104, contentWidth / 4));
        int teamLabelWidth = Math.max(40, contentWidth - leaveWidth - BUTTON_GAP);
        if (isVisibleInContent(layout, bottomRowY, BUTTON_HEIGHT)) {
            labels.add(new Label(spec("screen.team.current_team", serverSync.playerRole()), contentX, bottomRowY + 6, 0xFFC9D4DE, false, teamLabelWidth));
        }
        addContentButton(
                layout,
                contentX + contentWidth - leaveWidth,
                bottomRowY,
                leaveWidth,
                BUTTON_HEIGHT,
                key("screen.team.leave"),
                canChangeTeam ? (serverSync.playerInTeam() ? key("screen.team.leave_tooltip") : key("screen.team.not_joined_tooltip")) : lockedTooltip,
                widget -> sendTeamAction(TeamAction.LEAVE),
                ButtonVariant.DANGER,
                canChangeTeam && serverSync.playerInTeam()
        );
        return y + cardHeight + CARD_GAP;
    }

    private void addTeamSection(
            Layout layout,
            int x,
            int y,
            int width,
            int height,
            String title,
            int count,
            boolean current,
            int accent,
            String buttonTitle,
            String tooltip,
            ButtonWidget.PressAction action,
            boolean enabled
    ) {
        if (isVisibleInContentPartial(layout, y, height)) {
            boxes.add(new Box(x, y, width, height, 0x55303A46, 0xFF4C5A66));
            boxes.add(new Box(x, y, 3, height, accent, accent));
        }
        int buttonWidth = Math.min(96, Math.max(78, width / 3));
        int textWidth = Math.max(40, width - buttonWidth - 24);
        addContentLabel(layout, title, x + 10, y + 9, accent, true);
        if (isVisibleInContentPartial(layout, y, height)) {
            labels.add(new Label(current ? key("screen.team.current_side") : key("screen.team.can_join"), x + Math.max(66, textRenderer.getWidth(tr(title)) + 18), y + 9, current ? 0xFF77E287 : 0xFF9FAAB4, false, Math.max(40, textWidth - 62)));
        }
        addContentLabel(layout, spec("screen.team.current_count", count), x + 10, y + 29, 0xFFC9D4DE, false);
        addContentButton(layout, x + width - buttonWidth - 8, y + height - BUTTON_HEIGHT - 7, buttonWidth, BUTTON_HEIGHT, buttonTitle, tooltip, action, ButtonVariant.NORMAL, enabled);
    }

    private void addWildcardToggleTile(Layout layout, ToggleField field, int x, int y, int width, int height) {
        boolean enabled = getToggle(editableConfig, field);
        boolean configurable = hasWildcardSettings(field);
        int borderColor = enabled ? 0xFF55B978 : 0xFF59636C;
        if (isVisibleInContentPartial(layout, y, height)) {
            boxes.add(new Box(x, y, width, height, enabled ? 0x55324B3F : 0x55303A46, borderColor));
        }

        int toggleWidth = Math.min(34, Math.max(30, width / 5));
        int settingsWidth = configurable ? 42 : 0;
        int settingsGap = configurable ? 4 : 0;
        int buttonsWidth = toggleWidth + settingsWidth + settingsGap;
        int infoButtonSize = 12;
        int infoGap = 3;
        int iconSize = 16;
        int iconX = x + 7;
        int iconY = y + Math.max(4, (height - iconSize) / 2);
        int labelX = iconX + iconSize + 6;
        int rightButtonsX = x + width - buttonsWidth - 5;
        int maxInfoX = rightButtonsX - infoButtonSize - 4;
        int desiredTextWidth = Math.max(8, width - buttonsWidth - (labelX - x) - infoButtonSize - infoGap - 12);
        int firstLineWidth = textRenderer.getWidth(wrapLabelText(tr(field.label), desiredTextWidth, 2).get(0));
        int infoX = Math.min(labelX + Math.min(firstLineWidth, desiredTextWidth) + infoGap, Math.max(labelX, maxInfoX));
        int textWidth = Math.max(8, infoX - labelX - infoGap);
        if (isVisibleInContentPartial(layout, y, height)) {
            icons.add(new Icon(WildcardIcons.iconFor(field.id), iconX, iconY));
            wrappedLabels.add(new WrappedLabel(field.label, labelX, y, height, enabled ? 0xFFFFFFFF : 0xFFC9D4DE, false, textWidth, 2));
        }
        if (!isVisibleInContent(layout, y, height)) {
            // Widgets are not clipped by the scissor; only show the tile's buttons once the whole tile is in view.
            return;
        }
        addContentButton(
                layout,
                infoX,
                y + Math.max(0, (height - infoButtonSize) / 2),
                infoButtonSize,
                infoButtonSize,
                key("screen.button.info"),
                wildcardInfoTooltip(field),
                widget -> {
                },
                ButtonVariant.NORMAL,
                true
        );
        if (configurable) {
            addContentButton(
                    layout,
                    x + width - buttonsWidth - 5,
                    y + Math.max(4, (height - 18) / 2),
                    settingsWidth,
                    18,
                    key("screen.button.settings"),
                    "",
                    widget -> openWildcardSettings(field),
                    ButtonVariant.NORMAL,
                    editableConfig != null
            );
        }
        addContentButton(
                layout,
                x + width - toggleWidth - 5,
                y + Math.max(4, (height - 18) / 2),
                toggleWidth,
                18,
                enabled ? key("screen.toggle.on_short") : key("screen.toggle.off_short"),
                wildcardToggleTooltip(field, enabled),
                widget -> toggleField(field),
                enabled ? ButtonVariant.TOGGLE_ON : ButtonVariant.TOGGLE_OFF,
                canEditConfig()
        );
    }

    private String wildcardToggleTooltip(ToggleField field, boolean enabled) {
        return spec("screen.tooltip.wildcard_toggle", field.label, enabled ? key("screen.toggle.enabled") : key("screen.toggle.disabled"));
    }

    private String wildcardInfoTooltip(ToggleField field) {
        ConfigSnapshot config = editableConfig;
        return switch (field) {
            case SUPPLY_DROP -> spec("screen.wildcard_info.supply_drop", configuredCount(config == null ? 60 : config.supplyDropIntervalSeconds()));
            case HUNTER_RADAR -> spec("screen.wildcard_info.hunter_radar", configuredCount(config == null ? 40 : config.hunterRadarWarningDistance()));
            case SPACE_SHIFT -> spec("screen.wildcard_info.space_shift", configuredCount(config == null ? 60 : config.spaceShiftIntervalSeconds()));
            case BLOCK_DECAY -> spec("screen.wildcard_info.block_decay", configuredCount(config == null ? 10 : config.blockDecaySeconds()));
            case PEARL_FRENZY -> spec("screen.wildcard_info.pearl_frenzy", configuredCount(config == null ? 4 : config.pearlFrenzyMaxPearls()), configuredCount(config == null ? 45 : config.pearlFrenzyIntervalSeconds()));
            case WIND_CHARGE_BRAWL -> spec("screen.wildcard_info.wind_charge_brawl", configuredCount(config == null ? 5 : config.windChargeBrawlIntervalSeconds()), configuredPercent(config == null ? 180 : config.windChargeExplosionMultiplierPercent()));
            case BACKROOMS -> spec("screen.wildcard_info.backrooms", configuredCount(config == null ? 240 : config.backroomsDurationSeconds()));
            default -> key("screen.wildcard_info." + field.id);
        };
    }

    private String configuredCount(int count) {
        return Integer.toString(Math.max(1, count));
    }

    private String configuredPercent(int percent) {
        return Math.max(1, percent) + "%";
    }

    private boolean useTwoColumns(int usableContentWidth) {
        return usableContentWidth >= TWO_COLUMN_MIN_WIDTH;
    }

    private void drawCardBackground(Layout layout, int x, int y, int width, int height) {
        if (isVisibleInContentPartial(layout, y, height)) {
            boxes.add(new Box(x, y, width, height, 0x88303A46, 0xFF4C5A66));
        }
    }

    private void drawCardBorder(Layout layout, int x, int y, int width, int height) {
        drawCardBackground(layout, x, y, width, height);
    }

    private void addCardTitle(Layout layout, String title, int x, int y) {
        addContentLabel(layout, title, x, y, 0xFFFFFFFF, true);
    }

    private void addHintText(Layout layout, String text, int x, int y, int width) {
        int height = hintHeight(text, width);
        if (isVisibleInContentPartial(layout, y, height)) {
            wrappedLabels.add(new WrappedLabel(text, x, y, height, 0xFF9FAAB4, false, width, HINT_MAX_LINES));
        }
    }

    private int hintHeight(String text, int width) {
        int lines = wrapLabelText(tr(text), width, HINT_MAX_LINES).size();
        return lines * textRenderer.fontHeight + Math.max(0, lines - 1) * 2;
    }

    private int addInfoRow(Layout layout, String label, String value, int x, int y, int width, int valueColor) {
        int valueX = x + Math.max(92, Math.min(LABEL_WIDTH, width / 2));
        int labelY = y + Math.max(3, (ROW_HEIGHT - textRenderer.fontHeight) / 2);
        addContentLabel(layout, spec("screen.label_colon", tr(label)), x, labelY, 0xFFC9D4DE, false);
        if (isVisibleInContentPartial(layout, y, ROW_HEIGHT)) {
            labels.add(new Label(value, valueX, labelY, valueColor, false, Math.max(40, width - (valueX - x))));
        }
        return nextRowY(y);
    }

    private int addInputRow(Layout layout, NumberField field, int x, int y, int width) {
        return addInputRow(layout, field, x, y, width, canManage);
    }

    private int addInputRow(Layout layout, NumberField field, int x, int y, int width, boolean editable) {
        int unitSpace = numberUnitSpace(field);
        int fieldWidth = numberFieldWidth(field, width, unitSpace);
        if (labelFits(field.label, formLabelWidth(width, fieldWidth, unitSpace))) {
            addContentNumberField(layout, field, x, y, width, CONTROL_HEIGHT, editable, true);
            return nextRowY(y);
        }

        addStackedLabel(layout, field.label, x, y, width);
        int controlRow = y + STACKED_LABEL_HEIGHT;
        addContentNumberField(layout, field, x, controlRow, width, CONTROL_HEIGHT, editable, false);
        return nextRowY(controlRow);
    }

    private int addInputRow(Layout layout, StringField field, int x, int y, int width) {
        int fieldWidth = textFieldWidth(width);
        if (labelFits(field.label, width - fieldWidth - 8)) {
            addContentStringField(layout, field, x, y, width, CONTROL_HEIGHT, true);
            return nextRowY(y);
        }

        addStackedLabel(layout, field.label, x, y, width);
        int controlRow = y + STACKED_LABEL_HEIGHT;
        addContentStringField(layout, field, x, controlRow, width, CONTROL_HEIGHT, false);
        return nextRowY(controlRow);
    }

    private boolean labelFits(String labelKey, int availableWidth) {
        return textRenderer.getWidth(tr(spec("screen.label_colon", labelKey))) <= availableWidth;
    }

    private void addStackedLabel(Layout layout, String labelKey, int x, int y, int width) {
        addContentLabel(layout, spec("screen.label_colon", labelKey), x, y + 2, 0xFFC9D4DE, false);
    }

    private int numberUnitSpace(NumberField field) {
        if (field.unit.isBlank()) {
            return 0;
        }
        int unitTextWidth = Math.min(UNIT_WIDTH, Math.max(18, textRenderer.getWidth(tr(field.unit))));
        return unitTextWidth + 8;
    }

    private int addCoordinateRow(Layout layout, NumberField xField, NumberField yField, NumberField zField, int x, int y, int width) {
        if (!isVisibleInContent(layout, y, ROW_HEIGHT)) {
            return nextRowY(y);
        }

        int labelWidth = width < 230 ? 32 : 52;
        int axisWidth = 8;
        int gap = 4;
        int availableWidth = width - labelWidth - axisWidth * 3 - gap * 5;
        int fieldWidth = Math.max(28, availableWidth / 3);
        int rowWidth = labelWidth + (axisWidth + fieldWidth) * 3 + gap * 5;
        if (rowWidth > width) {
            fieldWidth = Math.max(24, (width - labelWidth - axisWidth * 3 - gap * 5) / 3);
            rowWidth = labelWidth + (axisWidth + fieldWidth) * 3 + gap * 5;
        }

        int startX = x + Math.max(0, (width - rowWidth) / 2);
        int labelY = y + Math.max(3, (ROW_HEIGHT - textRenderer.fontHeight) / 2);
        labels.add(new Label(width < 230 ? key("screen.field.coordinates.short") : key("screen.field.coordinates"), startX, labelY, 0xFFC9D4DE, false, labelWidth));

        int currentX = startX + labelWidth + gap;
        currentX = addCoordinateInput(layout, key("screen.axis.x"), xField, currentX, controlY(y), axisWidth, fieldWidth, CONTROL_HEIGHT);
        currentX = addCoordinateInput(layout, key("screen.axis.y"), yField, currentX + gap, controlY(y), axisWidth, fieldWidth, CONTROL_HEIGHT);
        addCoordinateInput(layout, key("screen.axis.z"), zField, currentX + gap, controlY(y), axisWidth, fieldWidth, CONTROL_HEIGHT);
        return nextRowY(y);
    }

    private int addDropdownRow(Layout layout, DropdownField field, int x, int y, int width) {
        return addDropdownRow(layout, field, x, y, width, canManage);
    }

    private int addDropdownRow(Layout layout, DropdownField field, int x, int y, int width, boolean editable) {
        int dropdownWidth = dropdownWidth(width);
        int dropdownX = x + width - dropdownWidth;
        if (labelFits(field.label, dropdownX - x - 8)) {
            int labelY = y + Math.max(3, (ROW_HEIGHT - textRenderer.fontHeight) / 2);
            addContentLabel(layout, spec("screen.label_colon", field.label), x, labelY, 0xFFC9D4DE, false);
            addContentDropdownField(layout, field, dropdownX, controlY(y), dropdownWidth, CONTROL_HEIGHT, editable);
            return nextRowY(y);
        }

        addStackedLabel(layout, field.label, x, y, width);
        int controlRow = y + STACKED_LABEL_HEIGHT;
        int fullWidth = Math.min(width, Math.max(dropdownWidth, DROPDOWN_WIDTH + 40));
        addContentDropdownField(layout, field, x + width - fullWidth, controlY(controlRow), fullWidth, CONTROL_HEIGHT, editable);
        return nextRowY(controlRow);
    }

    private int addToggleRow(Layout layout, BooleanField field, int x, int y, int width) {
        addContentBooleanField(layout, field, x, y, width, BUTTON_HEIGHT);
        return nextRowY(y);
    }

    private int addButtonRow(Layout layout, List<ButtonSpec> buttons, int x, int y, int width, int requestedColumns, int maxButtonWidth) {
        if (buttons.isEmpty()) {
            return y;
        }

        int columns = Math.max(1, Math.min(requestedColumns, buttons.size()));
        int availableButtonWidth = Math.max(80, (width - (columns - 1) * BUTTON_GAP) / columns);
        int buttonWidth = maxButtonWidth <= 0 ? availableButtonWidth : Math.min(maxButtonWidth, availableButtonWidth);
        int rowWidth = buttonWidth * columns + (columns - 1) * BUTTON_GAP;
        int startX = x + Math.max(0, (width - rowWidth) / 2);
        int rows = (buttons.size() + columns - 1) / columns;
        for (int i = 0; i < buttons.size(); i++) {
            ButtonSpec button = buttons.get(i);
            int column = i % columns;
            int row = i / columns;
            addContentButton(
                    layout,
                    startX + column * (buttonWidth + BUTTON_GAP),
                    y + row * (BUTTON_HEIGHT + BUTTON_GAP),
                    buttonWidth,
                    BUTTON_HEIGHT,
                    button.title(),
                    button.tooltip(),
                    button.action(),
                    button.variant(),
                    button.enabled()
            );
        }
        return y + rows * BUTTON_HEIGHT + (rows - 1) * BUTTON_GAP + ROW_GAP;
    }

    private int nextRowY(int y) {
        return y + ROW_HEIGHT + ROW_GAP;
    }

    private int controlY(int rowY) {
        return rowY + Math.max(0, (ROW_HEIGHT - CONTROL_HEIGHT) / 2);
    }

    private int buttonY(int rowY) {
        return rowY + Math.max(0, (ROW_HEIGHT - BUTTON_HEIGHT) / 2);
    }

    private int rowContentBottom(int y) {
        return y + Math.max(Math.max(ROW_HEIGHT, CONTROL_HEIGHT), BUTTON_HEIGHT);
    }

    private int contentStartY(int cardY) {
        return cardY + CARD_PADDING_TOP + CARD_TITLE_HEIGHT;
    }

    private int formLabelWidth(int width, int controlWidth, int unitSpace) {
        return Math.max(10, width - controlWidth - unitSpace - 8);
    }

    private int numberFieldWidth(NumberField field, int width, int unitSpace) {
        int labelTarget = Math.min(LABEL_WIDTH, Math.max(72, width / 2));
        int minWidth = Math.min(46, Math.max(34, width - unitSpace));
        int maxBySpace = Math.max(minWidth, width - unitSpace);
        int maxByRow = width - labelTarget - unitSpace - 8;
        int preferred = field.allowsNegative() ? 96 : CONTROL_WIDTH;
        int target = Math.min(preferred, maxBySpace);
        if (maxByRow >= minWidth) {
            target = Math.min(target, maxByRow);
        }
        return Math.max(minWidth, Math.min(128, target));
    }

    private int textFieldWidth(int width) {
        int labelTarget = Math.min(LABEL_WIDTH, Math.max(72, width / 2));
        int minWidth = Math.min(82, Math.max(52, width));
        int maxByRow = width - labelTarget - 8;
        int target = Math.min(150, width);
        if (maxByRow >= minWidth) {
            target = Math.min(target, maxByRow);
        }
        return Math.max(minWidth, target);
    }

    private int dropdownWidth(int width) {
        int labelTarget = Math.min(LABEL_WIDTH, Math.max(72, width / 2));
        int minWidth = Math.min(100, Math.max(70, width));
        int maxByRow = width - labelTarget - 8;
        int target = Math.min(DROPDOWN_WIDTH, width);
        if (maxByRow >= minWidth) {
            target = Math.min(target, maxByRow);
        }
        return Math.max(minWidth, target);
    }

    private void addNumberField(NumberField field, int x, int y, int width, int fieldHeight) {
        addNumberField(field, x, y, width, fieldHeight, canEditConfig());
    }

    private void addNumberField(NumberField field, int x, int y, int width, int fieldHeight, boolean editable) {
        addNumberField(field, x, y, width, fieldHeight, editable, true);
    }

    private void addNumberField(NumberField field, int x, int y, int width, int fieldHeight, boolean editable, boolean showLabel) {
        boolean fieldEditable = editable && canEditField(field);
        int unitSpace = numberUnitSpace(field);
        int fieldWidth = showLabel
                ? numberFieldWidth(field, width, unitSpace)
                : Math.max(34, Math.min(field.allowsNegative() ? 96 : CONTROL_WIDTH, width - unitSpace));
        int fieldX = x + width - fieldWidth - unitSpace;
        int labelY = y + Math.max(3, (fieldHeight - textRenderer.fontHeight) / 2);
        if (showLabel) {
            labels.add(new Label(spec("screen.label_colon", field.label), x, labelY, 0xFFC9D4DE, false, formLabelWidth(width, fieldWidth, unitSpace)));
        }
        if (!field.unit.isBlank()) {
            labels.add(new Label(field.unit, fieldX + fieldWidth + 8, labelY, fieldEditable ? 0xFFC9D4DE : 0xFF7D8790));
        }

        registerNumberField(field, fieldX, y, fieldWidth, fieldHeight, fieldEditable);
    }

    /** Creates the widget for a number field, seeding it with any uncommitted text the user typed earlier. */
    private void registerNumberField(NumberField field, int x, int y, int width, int height, boolean editable) {
        TextFieldWidget textField = new TextFieldWidget(textRenderer, x, y, width, height, text(field.label));
        textField.setMaxLength(11);
        String pending = editable ? pendingNumberText.get(field) : null;
        textField.setText(pending != null ? pending : Integer.toString(getNumber(editableConfig, field)));
        textField.setEditable(editable);
        textField.active = editable;
        if (!editable) {
            pendingNumberText.remove(field);
        }
        textField.setChangedListener(value -> onNumberTextChanged(field, value));
        numberFields.put(field, textField);
        addDrawableChild(textField);
    }

    private void onNumberTextChanged(NumberField field, String value) {
        if (editableConfig == null || value.trim().equals(Integer.toString(getNumber(editableConfig, field)))) {
            pendingNumberText.remove(field);
        } else {
            pendingNumberText.put(field, value);
        }
    }

    private void onStringTextChanged(StringField field, String value) {
        if (editableConfig == null || value.trim().equals(getString(editableConfig, field))) {
            pendingStringText.remove(field);
        } else {
            pendingStringText.put(field, value);
        }
    }

    private int addCoordinateInput(Layout layout, String axis, NumberField field, int x, int y, int axisWidth, int fieldWidth, int fieldHeight) {
        int labelY = y + Math.max(3, (fieldHeight - textRenderer.fontHeight) / 2);
        labels.add(new Label(axis, x, labelY, 0xFF9FAAB4, false, axisWidth));

        int fieldX = x + axisWidth + 2;
        registerNumberField(field, fieldX, y, fieldWidth, fieldHeight, canEditField(field));
        return fieldX + fieldWidth;
    }

    private void addStringField(StringField field, int x, int y, int width, int fieldHeight, boolean showLabel) {
        int fieldWidth = showLabel ? textFieldWidth(width) : Math.min(width, 220);
        int fieldX = x + width - fieldWidth;
        int labelY = y + Math.max(3, (fieldHeight - textRenderer.fontHeight) / 2);
        if (showLabel) {
            labels.add(new Label(spec("screen.label_colon", field.label), x, labelY, 0xFFC9D4DE, false, Math.max(10, fieldX - x - 8)));
        }

        TextFieldWidget textField = new TextFieldWidget(textRenderer, fieldX, y, fieldWidth, fieldHeight, text(field.label));
        textField.setMaxLength(field.maxLength);
        boolean editable = canEditField(field);
        String pending = editable ? pendingStringText.get(field) : null;
        textField.setText(pending != null ? pending : getString(editableConfig, field));
        textField.setEditable(editable);
        textField.active = editable;
        if (!editable) {
            pendingStringText.remove(field);
        }
        textField.setChangedListener(value -> onStringTextChanged(field, value));
        stringFields.put(field, textField);
        addDrawableChild(textField);
    }

    private void addDropdownField(DropdownField field, int x, int y, int width, int height, boolean openUp, boolean enabled) {
        boolean editable = enabled && canEditField(field);
        DropdownWidget dropdown = new DropdownWidget(
                textRenderer,
                x,
                y,
                width,
                height,
                "",
                dropdownOptions(field, getDropdownValue(editableConfig, field)),
                getDropdownValue(editableConfig, field),
                editable,
                value -> selectDropdownField(field, value),
                this::closeDropdowns
        );
        dropdown.setOpenUp(openUp);
        dropdownFields.put(field, dropdown);
        addDrawableChild(dropdown);
    }

    private List<DropdownWidget.Option> dropdownOptions(DropdownField field, String currentValue) {
        if (currentValue == null || currentValue.isBlank()) {
            return field.options;
        }

        for (DropdownWidget.Option option : field.options) {
            if (option.value().equals(currentValue)) {
                return field.options;
            }
        }

        List<DropdownWidget.Option> options = new ArrayList<>(field.options);
        options.add(option(currentValue, currentValue));
        return options;
    }

    private StyledButtonWidget addToggleField(ToggleField field, int x, int y, int width, int height) {
        boolean enabled = getToggle(editableConfig, field);
        return addButton(
                x,
                y,
                width,
                height,
                spec("screen.toggle.field_state", field.label, enabled ? key("screen.toggle.enabled") : key("screen.toggle.disabled")),
                "",
                widget -> toggleField(field),
                enabled ? ButtonVariant.TOGGLE_ON : ButtonVariant.TOGGLE_OFF,
                canEditConfig()
        );
    }

    private StyledButtonWidget addBooleanField(BooleanField field, int x, int y, int width, int height) {
        boolean enabled = getBoolean(editableConfig, field);
        int buttonWidth = width < 320
                ? Math.max(78, Math.min(116, width / 2))
                : Math.min(150, Math.max(CONTROL_WIDTH, width / 2));
        buttonWidth = Math.min(buttonWidth, width);
        int buttonX = x + width - buttonWidth;
        int labelY = y + Math.max(3, (height - textRenderer.fontHeight) / 2);
        labels.add(new Label(spec("screen.label_colon", field.label), x, labelY, 0xFFC9D4DE, false, Math.max(10, buttonX - x - 8)));
        return addButton(
                buttonX,
                y,
                buttonWidth,
                height,
                enabled ? key("screen.toggle.enabled") : key("screen.toggle.disabled"),
                "",
                widget -> toggleBooleanField(field),
                enabled ? ButtonVariant.TOGGLE_ON : ButtonVariant.TOGGLE_OFF,
                canEditField(field)
        );
    }

    private boolean isRoundRunning() {
        return serverSync != null && serverSync.gameState() != GameState.WAITING;
    }

    private boolean canEditField(NumberField field) {
        return canEditConfig() && (field.live || !isRoundRunning());
    }

    private boolean canEditField(StringField field) {
        return canEditConfig() && (field.live || !isRoundRunning());
    }

    private boolean canEditField(DropdownField field) {
        return canEditConfig() && (field.live || !isRoundRunning());
    }

    private boolean canEditField(BooleanField field) {
        return canEditConfig() && (field.live || !isRoundRunning());
    }

    private void addContentLabel(Layout layout, String text, int x, int y, int color) {
        addContentLabel(layout, text, x, y, color, false);
    }

    private void addContentLabel(Layout layout, String text, int x, int y, int color, boolean shadow) {
        if (isVisibleInContentPartial(layout, y, textRenderer.fontHeight)) {
            labels.add(new Label(text, x, y, color, shadow));
        }
    }

    private void renderWrappedLabel(DrawContext context, Layout layout, WrappedLabel label) {
        int maxWidth = label.maxWidth(layout);
        List<String> lines = wrapLabelText(tr(label.text()), maxWidth, label.maxLines());
        int textHeight = lines.size() * textRenderer.fontHeight + Math.max(0, lines.size() - 1);
        int startY = label.y() + Math.max(0, (label.height() - textHeight) / 2);
        for (int i = 0; i < lines.size(); i++) {
            context.drawText(textRenderer, Text.literal(lines.get(i)), label.x(), startY + i * (textRenderer.fontHeight + 1), label.color(), label.shadow());
        }
    }

    private List<String> wrapLabelText(String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            String line = textRenderer.trimToWidth(remaining, maxWidth);
            if (line.isBlank()) {
                line = remaining.substring(0, 1);
            }
            remaining = remaining.substring(line.length()).trim();
            if (!remaining.isEmpty() && lines.size() == maxLines - 1) {
                line = textRenderer.trimToWidth(line + "...", maxWidth);
            }
            lines.add(line);
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private void addContentNumberField(Layout layout, NumberField field, int x, int y, int width, int fieldHeight) {
        addNumberField(field, x, controlY(y), width, fieldHeight);
        registerContentWidget(numberFields.get(field));
    }

    private void addContentNumberField(Layout layout, NumberField field, int x, int y, int width, int fieldHeight, boolean editable) {
        addContentNumberField(layout, field, x, y, width, fieldHeight, editable, true);
    }

    private void addContentNumberField(Layout layout, NumberField field, int x, int y, int width, int fieldHeight, boolean editable, boolean showLabel) {
        addNumberField(field, x, controlY(y), width, fieldHeight, editable, showLabel);
        registerContentWidget(numberFields.get(field));
    }

    private void addContentStringField(Layout layout, StringField field, int x, int y, int width, int fieldHeight) {
        addContentStringField(layout, field, x, y, width, fieldHeight, true);
    }

    private void addContentStringField(Layout layout, StringField field, int x, int y, int width, int fieldHeight, boolean showLabel) {
        addStringField(field, x, controlY(y), width, fieldHeight, showLabel);
        registerContentWidget(stringFields.get(field));
    }

    private void addContentDropdownField(Layout layout, DropdownField field, int x, int y, int width, int height) {
        addContentDropdownField(layout, field, x, y, width, height, canManage);
    }

    private void addContentDropdownField(Layout layout, DropdownField field, int x, int y, int width, int height, boolean editable) {
        int menuHeight = Math.max(18, height) * field.options.size();
        boolean openUp = y + height + 1 + menuHeight > layout.viewportBottom()
                && y - menuHeight - 1 >= layout.viewportTop();
        addDropdownField(field, x, y, width, height, openUp, editable);
        registerContentWidget(dropdownFields.get(field));
    }

    private void addContentToggleField(Layout layout, ToggleField field, int x, int y, int width, int height) {
        registerContentWidget(addToggleField(field, x, y, width, height));
    }

    private void addContentBooleanField(Layout layout, BooleanField field, int x, int y, int width, int height) {
        registerContentWidget(addBooleanField(field, x, buttonY(y), width, height));
    }

    private void addContentButton(Layout layout, int x, int y, int width, int height, String title, String description, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled) {
        registerContentWidget(addButton(x, y, width, height, title, description, action, variant, enabled));
    }

    private StyledButtonWidget addButton(int x, int y, int width, int height, String title, String description, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled) {
        String tooltip = description == null ? "" : description;
        if (!enabled && tooltip.isBlank() && isConfigEditPage()) {
            tooltip = configEditDeniedMessage();
        }

        StyledButtonWidget button = new StyledButtonWidget(x, y, width, height, title, tooltip, action, variant);
        button.active = enabled;
        addDrawableChild(button);
        return button;
    }

    private int pageTop(Layout layout) {
        return layout.viewportTop() - renderedScroll;
    }

    private void markContentBottom(Layout layout, int screenBottomY) {
        pageContentHeight = Math.max(pageContentHeight, screenBottomY - pageTop(layout));
    }

    /** Everything is built regardless of scroll position; the viewport scissor and widget visibility do the clipping. */
    private boolean isVisibleInContent(Layout layout, int y, int height) {
        return true;
    }

    private boolean isVisibleInContentPartial(Layout layout, int y, int height) {
        return true;
    }

    private boolean intersectsViewport(Layout layout, ClickableWidget widget) {
        return widget.getY() + widget.getHeight() > layout.viewportTop() && widget.getY() < layout.viewportBottom();
    }

    private void registerContentWidget(ClickableWidget widget) {
        if (widget != null) {
            contentWidgets.add(widget);
        }
    }

    private void updateContentWidgetVisibility(Layout layout) {
        for (ClickableWidget widget : contentWidgets) {
            widget.visible = intersectsViewport(layout, widget);
        }
    }

    /** Moves every content element by {@code delta} pixels vertically (scrolling without rebuilding the page). */
    private void shiftContent(int delta) {
        labels.replaceAll(label -> new Label(label.text(), label.x(), label.y() + delta, label.color(), label.shadow(), label.width()));
        wrappedLabels.replaceAll(label -> new WrappedLabel(label.text(), label.x(), label.y() + delta, label.height(), label.color(), label.shadow(), label.width(), label.maxLines()));
        boxes.replaceAll(box -> new Box(box.x(), box.y() + delta, box.width(), box.height(), box.color(), box.borderColor()));
        icons.replaceAll(icon -> new Icon(icon.stack(), icon.x(), icon.y() + delta));
        for (ClickableWidget widget : contentWidgets) {
            widget.setY(widget.getY() + delta);
        }
    }

    private boolean isInsideContent(Layout layout, double mouseX, double mouseY) {
        return mouseX >= layout.contentX()
                && mouseX <= layout.contentX() + layout.contentWidth()
                && mouseY >= layout.viewportTop()
                && mouseY <= layout.viewportBottom();
    }

    private void updateMaxScroll(Layout layout) {
        maxScroll = Math.max(0.0F, pageContentHeight - layout.viewportHeight());
        clampScroll(layout);
    }

    private boolean updateSmoothScroll(Layout layout, float delta) {
        updateMaxScroll(layout);
        float before = scrollOffset;
        float smoothing = 1.0F - (float) Math.pow(0.001F, Math.max(0.0F, delta) / 8.0F);
        if (Math.abs(targetScrollOffset - scrollOffset) < 0.5F) {
            scrollOffset = targetScrollOffset;
        } else {
            scrollOffset += (targetScrollOffset - scrollOffset) * Math.max(0.0F, Math.min(1.0F, smoothing));
        }

        clampScroll(layout);
        rememberCurrentScroll();
        int oldRenderedScroll = renderedScroll;
        renderedScroll = Math.round(scrollOffset);
        return Math.round(before) != renderedScroll || oldRenderedScroll != renderedScroll;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void clampScroll(Layout layout) {
        float nextMaxScroll = Math.max(0.0F, pageContentHeight - layout.viewportHeight());
        scrollOffset = clamp(scrollOffset, 0.0F, nextMaxScroll);
        targetScrollOffset = clamp(targetScrollOffset, 0.0F, nextMaxScroll);
        maxScroll = nextMaxScroll;
        renderedScroll = Math.round(scrollOffset);
    }

    /** Scroll positions are remembered per page and per open sub-page. */
    private String scrollKey() {
        if (currentPage == Page.RULES && rulesSubPage != null) {
            return currentPage.name() + "/" + rulesSubPage.name();
        }
        if (currentPage == Page.WILDCARD && selectedWildcardSettings != null) {
            return currentPage.name() + "/" + selectedWildcardSettings.name();
        }
        return currentPage.name();
    }

    private void rememberCurrentScroll() {
        String scrollKey = scrollKey();
        pageScrollOffsets.put(scrollKey, scrollOffset);
        pageTargetScrollOffsets.put(scrollKey, targetScrollOffset);
    }

    private void restorePageScroll(String scrollKey) {
        scrollOffset = pageScrollOffsets.getOrDefault(scrollKey, 0.0F);
        targetScrollOffset = pageTargetScrollOffsets.getOrDefault(scrollKey, scrollOffset);
        renderedScroll = Math.round(scrollOffset);
    }

    private void renderScrollBar(DrawContext context, Layout layout) {
        updateMaxScroll(layout);
        if (maxScroll <= 0.5F) {
            return;
        }

        int trackX = layout.scrollBarX();
        int trackTop = layout.viewportTop();
        int trackHeight = Math.max(16, layout.viewportHeight());
        int thumbHeight = Math.max(24, Math.round(trackHeight * (layout.viewportHeight() / (float) Math.max(layout.viewportHeight(), pageContentHeight))));
        int thumbY = trackTop + Math.round((trackHeight - thumbHeight) * (scrollOffset / maxScroll));
        context.fill(trackX, trackTop, trackX + 2, trackTop + trackHeight, 0x664C5A66);
        context.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, 0xCC7FC2FF);
    }

    private void updateNavigationScroll(Layout layout) {
        int pageCount = visiblePages().size();
        int buttonHeight = 24;
        int buttonGap = 6;
        int totalHeight = pageCount * buttonHeight + Math.max(0, pageCount - 1) * buttonGap;
        maxNavScroll = Math.max(0.0F, totalHeight - navigationHeight(layout));
        navScroll = clamp(navScroll, 0.0F, maxNavScroll);
    }

    private void renderNavigationScrollBar(DrawContext context, Layout layout) {
        updateNavigationScroll(layout);
        if (maxNavScroll <= 0.5F) {
            return;
        }

        int trackX = layout.panelX() + layout.navWidth() - 7;
        int trackTop = navigationTop(layout);
        int trackHeight = navigationHeight(layout);
        int totalHeight = Math.round(trackHeight + maxNavScroll);
        int thumbHeight = Math.max(20, Math.round(trackHeight * (trackHeight / (float) Math.max(trackHeight, totalHeight))));
        int thumbY = trackTop + Math.round((trackHeight - thumbHeight) * (navScroll / maxNavScroll));
        context.fill(trackX, trackTop, trackX + 2, trackTop + trackHeight, 0x554C5A66);
        context.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, 0xAA7FC2FF);
    }

    private int navigationTop(Layout layout) {
        return layout.panelY() + 48;
    }

    private int navigationBottom(Layout layout) {
        return layout.panelY() + layout.panelHeight() - 14;
    }

    private int navigationHeight(Layout layout) {
        return Math.max(24, navigationBottom(layout) - navigationTop(layout));
    }

    private boolean isInsideNavigation(Layout layout, double mouseX, double mouseY) {
        return mouseX >= layout.panelX()
                && mouseX <= layout.panelX() + layout.navWidth()
                && mouseY >= navigationTop(layout)
                && mouseY <= navigationBottom(layout);
    }

    private void renderDropdownOverlays(DrawContext context, int mouseX, int mouseY, float delta) {
        for (DropdownWidget dropdown : dropdownFields.values()) {
            dropdown.renderOverlay(context, mouseX, mouseY, delta);
        }
    }

    private void renderHoverTooltip(DrawContext context) {
        if (!hoverTooltip.isBlank()) {
            int tooltipWidth = Math.min(360, Math.max(160, width - 24));
            List<Text> lines = wrapTooltipText(tr(hoverTooltip), tooltipWidth);
            context.drawTooltip(textRenderer, lines, hoverTooltipX, hoverTooltipY);
        }
    }

    private List<Text> wrapTooltipText(String text, int maxWidth) {
        List<Text> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (!remaining.isEmpty()) {
            String line = textRenderer.trimToWidth(remaining, maxWidth);
            if (line.isBlank()) {
                line = remaining.substring(0, 1);
            }
            lines.add(Text.literal(line));
            remaining = remaining.substring(line.length()).trim();
        }
        return lines.isEmpty() ? List.of(Text.empty()) : lines;
    }

    private void setHoverTooltip(String tooltip, int mouseX, int mouseY) {
        if (tooltip == null || tooltip.isBlank()) {
            return;
        }

        hoverTooltip = tooltip;
        hoverTooltipX = mouseX;
        hoverTooltipY = mouseY;
    }

    private void renderToast(DrawContext context, Layout layout, float delta) {
        if (toastDurationMs <= 0L || toastMessage.isBlank()) {
            return;
        }

        long elapsedMs = System.currentTimeMillis() - toastStartTimeMs;
        long totalMs = toastDurationMs;
        if (elapsedMs >= totalMs) {
            toastDurationMs = 0L;
            return;
        }

        float age = Math.max(0L, elapsedMs);
        float alpha;
        if (age < TOAST_FADE_IN_MS) {
            alpha = smoothStep(age / TOAST_FADE_IN_MS);
        } else if (age > TOAST_FADE_IN_MS + TOAST_HOLD_MS) {
            alpha = 1.0F - smoothStep((age - TOAST_FADE_IN_MS - TOAST_HOLD_MS) / TOAST_FADE_OUT_MS);
        } else {
            alpha = 1.0F;
        }

        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        int maxWidth = Math.max(90, Math.min(260, layout.usableContentWidth() - 12));
        String text = trim(tr(toastMessage), maxWidth - 18);
        int toastWidth = Math.min(maxWidth, textRenderer.getWidth(text) + 18);
        int toastHeight = 24;
        int toastX = layout.usableContentWidth() < 320
                ? layout.contentX() + (layout.usableContentWidth() - toastWidth) / 2
                : layout.contentX() + layout.usableContentWidth() - toastWidth - 6;
        int toastY = layout.footerTop() - toastHeight - 8 + Math.round((1.0F - alpha) * 4.0F);

        context.fill(toastX, toastY, toastX + toastWidth, toastY + toastHeight, withAlpha(0xCC101820, alpha));
        context.fill(toastX, toastY, toastX + toastWidth, toastY + 1, withAlpha(toastKind.color, alpha));
        context.fill(toastX, toastY + toastHeight - 1, toastX + toastWidth, toastY + toastHeight, withAlpha(0xFF4C5A66, alpha));
        context.fill(toastX, toastY, toastX + 1, toastY + toastHeight, withAlpha(0xFF4C5A66, alpha));
        context.fill(toastX + toastWidth - 1, toastY, toastX + toastWidth, toastY + toastHeight, withAlpha(0xFF4C5A66, alpha));
        context.drawText(textRenderer, Text.literal(text), toastX + 9, toastY + 7, withAlpha(toastKind.color, alpha), true);
    }

    private float smoothStep(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }

    private void closeDropdowns() {
        for (DropdownWidget dropdown : dropdownFields.values()) {
            dropdown.close();
        }
        if (syncRebuildPending) {
            syncRebuildPending = false;
            clearAndInit();
        }
    }

    private boolean hasExpandedDropdown() {
        for (DropdownWidget dropdown : dropdownFields.values()) {
            if (dropdown.isExpanded()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test hook: switch to a page by name. Accepts the top-level pages (GAME, RULES, WILDCARD, DEBUG), a RULES
     * sub-page (TIME_BOUNDARY, VICTORY, RESPAWN, KILL_CREDIT, BALANCE) and the legacy names TEAM / BASIC.
     */
    public void selectPageForTesting(String pageName) {
        switch (pageName) {
            case "TEAM" -> switchPage(Page.GAME);
            case "BASIC" -> selectRulesSubPageForTesting(RulesSubPage.TIME_BOUNDARY);
            default -> {
                for (RulesSubPage subPage : RulesSubPage.values()) {
                    if (subPage.name().equals(pageName)) {
                        selectRulesSubPageForTesting(subPage);
                        return;
                    }
                }
                switchPage(Page.valueOf(pageName));
            }
        }
    }

    private void selectRulesSubPageForTesting(RulesSubPage subPage) {
        switchPage(Page.RULES);
        openRulesSubPage(subPage);
    }

    /** Test hook: scroll the content area by the given number of pixels. */
    public void scrollForTesting(float pixels) {
        targetScrollOffset = clamp(targetScrollOffset + pixels, 0.0F, maxScroll);
        scrollOffset = targetScrollOffset;
        clearAndInit();
    }

    private void switchPage(Page page) {
        if (page == currentPage) {
            return;
        }

        // Never blocked: invalid text is reverted (with a toast) and the switch proceeds.
        commitVisibleInputs();
        closeDropdowns();
        rememberCurrentScroll();
        currentPage = page;
        if (page != Page.WILDCARD) {
            selectedWildcardSettings = null;
        }
        restorePageScroll(scrollKey());
        clearAndInit();
    }

    private boolean beginConfigEdit() {
        if (!canEditConfig()) {
            showError(configEditDeniedMessage());
            return false;
        }
        if (editableConfig == null) {
            return false;
        }
        commitVisibleInputs();
        return true;
    }

    private void toggleField(ToggleField field) {
        if (!beginConfigEdit()) {
            return;
        }

        editableConfig = setToggle(editableConfig, field, !getToggle(editableConfig, field));
        clearAndInit();
    }

    private void setAllWildcards(boolean enabled) {
        if (!beginConfigEdit()) {
            return;
        }

        ConfigSnapshot updated = editableConfig;
        for (ToggleField field : ToggleField.values()) {
            updated = setToggle(updated, field, enabled);
        }
        editableConfig = updated;
        clearAndInit();
    }

    private void setCategoryWildcards(WildcardCategory category, boolean enabled) {
        if (!beginConfigEdit()) {
            return;
        }

        ConfigSnapshot updated = editableConfig;
        for (ToggleField field : ToggleField.inCategory(category)) {
            updated = setToggle(updated, field, enabled);
        }
        editableConfig = updated;
        clearAndInit();
    }

    private void openWildcardSettings(ToggleField field) {
        if (!hasWildcardSettings(field) || editableConfig == null) {
            return;
        }

        commitVisibleInputs();
        closeDropdowns();
        rememberCurrentScroll();
        selectedWildcardSettings = field;
        restorePageScroll(scrollKey());
        clearAndInit();
    }

    private void toggleBooleanField(BooleanField field) {
        if (!beginConfigEdit()) {
            return;
        }
        if (!canEditField(field)) {
            showError(key("screen.error.field_locked_live"));
            return;
        }

        editableConfig = setBoolean(editableConfig, field, !getBoolean(editableConfig, field));
        clearAndInit();
    }

    private void selectDropdownField(DropdownField field, String value) {
        if (!beginConfigEdit()) {
            return;
        }
        if (!canEditField(field)) {
            showError(key("screen.error.field_locked_live"));
            return;
        }

        editableConfig = setDropdownValue(editableConfig, field, value);
        clearAndInit();
    }

    private void setTargetToCurrentLocation() {
        if (!beginConfigEdit()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            showError(key("screen.error.no_world_for_location"));
            return;
        }

        BlockPos pos = client.player.getBlockPos();
        ModConfig copy = editableConfig.toConfig();
        copy.targetDimension = client.world.getRegistryKey().getValue().toString();
        copy.targetX = pos.getX();
        copy.targetY = pos.getY();
        copy.targetZ = pos.getZ();
        copy.validate();
        editableConfig = ConfigSnapshot.from(copy);
        showToast(key("screen.toast.location_set"), StatusKind.INFO);
        clearAndInit();
    }

    /**
     * Folds everything the user typed (including text in fields that have since scrolled out of view) into
     * {@link #editableConfig}. This never fails: unparsable or empty text reverts to the last valid value and values
     * below the minimum clamp to it, each with a toast, so navigation, scrolling, toggles and closing are never blocked.
     */
    private void commitVisibleInputs() {
        if (editableConfig == null) {
            pendingNumberText.clear();
            pendingStringText.clear();
            return;
        }

        ConfigSnapshot updated = editableConfig;
        String firstProblem = null;
        for (Map.Entry<NumberField, String> entry : new ArrayList<>(pendingNumberText.entrySet())) {
            NumberField field = entry.getKey();
            String raw = entry.getValue().trim();
            int value;
            try {
                value = Integer.parseInt(raw);
            } catch (NumberFormatException exception) {
                if (firstProblem == null) {
                    firstProblem = spec("screen.error.reverted_invalid", field.label);
                }
                continue;
            }

            if (value < field.minValue) {
                value = field.minValue;
                if (firstProblem == null) {
                    firstProblem = spec("screen.error.min_value", field.label, field.minValue);
                }
            }
            updated = setNumber(updated, field, value);
        }

        for (Map.Entry<StringField, String> entry : new ArrayList<>(pendingStringText.entrySet())) {
            String value = entry.getValue().trim();
            if (value.isBlank()) {
                if (firstProblem == null) {
                    firstProblem = spec("screen.error.required", entry.getKey().label);
                }
                continue;
            }
            updated = setString(updated, entry.getKey(), value);
        }

        pendingNumberText.clear();
        pendingStringText.clear();
        editableConfig = updated;
        syncWidgetsToConfig();
        if (firstProblem != null) {
            showError(firstProblem);
        }
    }

    /** Pushes the committed config back into the live widgets (e.g. after a revert or clamp). */
    private void syncWidgetsToConfig() {
        if (editableConfig == null) {
            return;
        }
        for (Map.Entry<NumberField, TextFieldWidget> entry : numberFields.entrySet()) {
            String expected = Integer.toString(getNumber(editableConfig, entry.getKey()));
            if (!entry.getValue().getText().equals(expected)) {
                entry.getValue().setText(expected);
            }
        }
        for (Map.Entry<StringField, TextFieldWidget> entry : stringFields.entrySet()) {
            String expected = getString(editableConfig, entry.getKey());
            if (!entry.getValue().getText().equals(expected)) {
                entry.getValue().setText(expected);
            }
        }
    }

    private void saveConfig() {
        if (!canEditConfig()) {
            showError(configEditDeniedMessage());
            return;
        }

        if (editableConfig == null) {
            return;
        }

        if (!canSaveConfig()) {
            showInfo(key("screen.info.no_unsaved_changes"));
            return;
        }

        commitVisibleInputs();
        if (!hasUnsavedChanges()) {
            showInfo(key("screen.info.no_unsaved_changes"));
            return;
        }

        cachedEditableConfig = editableConfig;
        manualSaveRequested = true;
        sendPayload(new HunterWildcardPackets.UpdateConfigPayload(editableConfig), HunterWildcardPackets.C2S_UPDATE_CONFIG, key("screen.toast.save_submitted"));
    }

    private void restoreDefaultConfig() {
        if (!canEditConfig()) {
            showError(configEditDeniedMessage());
            return;
        }

        ModConfig defaults = new ModConfig();
        defaults.validate();
        pendingNumberText.clear();
        pendingStringText.clear();
        editableConfig = ConfigSnapshot.from(defaults);
        cachedEditableConfig = editableConfig;
        manualReloadRequested = false;
        manualSaveRequested = false;
        showToast(key("screen.toast.default_restored"), StatusKind.INFO);
        clearAndInit();
    }

    private void reloadConfig() {
        if (!canEditConfig()) {
            showError(configEditDeniedMessage());
            return;
        }

        cachedEditableConfig = null;
        manualReloadRequested = true;
        sendPayload(new HunterWildcardPackets.ReloadConfigPayload(), HunterWildcardPackets.C2S_RELOAD_CONFIG, key("screen.toast.reload_submitted"));
    }

    private void requestConfig() {
        requestConfig(true);
    }

    private void requestConfig(boolean updateMessage) {
        if (updateMessage) {
            setFooterStatus(key("screen.status.requesting_server"), StatusKind.INFO);
        }
        sendPayload(new HunterWildcardPackets.RequestConfigPayload(), HunterWildcardPackets.C2S_REQUEST_CONFIG, key("screen.status.waiting_server_sync"), false);
    }

    private void sendDebugAction(DebugAction action) {
        if (!canManage) {
            showError(key("screen.error.debug_op_only"));
            return;
        }

        if (!isDebugPageEnabled()) {
            showError(key("screen.error.debug_menu_required"));
            return;
        }

        sendPayload(new HunterWildcardPackets.DebugActionPayload(action), HunterWildcardPackets.C2S_DEBUG_ACTION, key("screen.toast.debug_submitted"));
    }

    private void sendTestWildcard(ToggleField field) {
        if (!canManage) {
            showError(key("screen.error.test_wildcard_op_only"));
            return;
        }

        if (!isDebugPageEnabled()) {
            showError(key("screen.error.debug_menu_required"));
            return;
        }

        sendPayload(new HunterWildcardPackets.TestWildcardPayload(field.id), HunterWildcardPackets.C2S_TEST_WILDCARD, spec("screen.toast.test_wildcard_submitted", field.label));
    }

    private void sendTeamAction(TeamAction action) {
        sendPayload(new HunterWildcardPackets.TeamActionPayload(action), HunterWildcardPackets.C2S_TEAM_ACTION, key("screen.toast.team_submitted"));
    }

    private void sendGameAction(GameAction action) {
        sendPayload(new HunterWildcardPackets.GameActionPayload(action), HunterWildcardPackets.C2S_GAME_ACTION, key("screen.toast.game_submitted"));
    }

    private void sendPayload(CustomPayload payload, CustomPayload.Id<?> id, String successMessage) {
        sendPayload(payload, id, successMessage, true);
    }

    private void sendPayload(CustomPayload payload, CustomPayload.Id<?> id, String successMessage, boolean updateMessage) {
        if (!canSend(id)) {
            if (id.equals(HunterWildcardPackets.C2S_REQUEST_CONFIG)) {
                setFooterStatus(key("screen.status.sync_not_enabled"), StatusKind.ERROR);
            }
            if (updateMessage) {
                showError(key("screen.error.sync_not_enabled"));
            }
            return;
        }

        try {
            ClientPlayNetworking.send(payload);
            if (updateMessage) {
                showToast(successMessage, StatusKind.INFO);
            }
        } catch (IllegalStateException exception) {
            if (id.equals(HunterWildcardPackets.C2S_REQUEST_CONFIG)) {
                setFooterStatus(key("screen.status.sync_no_server"), StatusKind.ERROR);
            }
            if (updateMessage) {
                showError(key("screen.error.no_server"));
            }
        }
    }

    private boolean canSend(CustomPayload.Id<?> id) {
        try {
            return ClientPlayNetworking.canSend(id);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }

    private void showInfo(String message) {
        showToast(message, StatusKind.INFO);
    }

    private void showSuccess(String message) {
        showToast(message, StatusKind.SUCCESS);
    }

    private void showError(String message) {
        showToast(message, StatusKind.ERROR);
    }

    private void setFooterStatus(String message, StatusKind kind) {
        statusMessage = message;
        statusKind = kind;
    }

    private void showToast(String message, StatusKind kind) {
        toastMessage = message == null ? "" : message;
        toastKind = kind == null ? StatusKind.INFO : kind;
        toastStartTimeMs = System.currentTimeMillis();
        toastDurationMs = TOAST_FADE_IN_MS + TOAST_HOLD_MS + TOAST_FADE_OUT_MS;
    }

    private void applySync(SyncConfigPayload payload) {
        // A periodic sync must never clobber what the user is typing: keep local edits while there are unsaved
        // changes, uncommitted text or a focused text field.
        boolean preserveLocalEdits = payload.canManage()
                && editableConfig != null
                && !manualReloadRequested
                && !manualSaveRequested
                && (hasUnsavedChanges() || hasVisibleInputChanges() || hasFocusedTextField());
        serverSync = payload;
        canManage = payload.canManage();
        ensureVisiblePage();
        if (manualReloadRequested || manualSaveRequested || !preserveLocalEdits) {
            editableConfig = payload.config();
            cachedEditableConfig = null;
            pendingNumberText.clear();
            pendingStringText.clear();
        } else {
            cachedEditableConfig = editableConfig;
        }

        if (manualReloadRequested) {
            showToast(key("screen.toast.server_config_reloaded"), StatusKind.SUCCESS);
            setFooterStatus("", StatusKind.INFO);
        } else if (manualSaveRequested) {
            showToast(key("screen.toast.server_config_saved"), StatusKind.SUCCESS);
            setFooterStatus("", StatusKind.INFO);
        } else if (!hasSyncedOnce) {
            setFooterStatus("", StatusKind.INFO);
        } else if (key("screen.status.requesting_server").equals(statusMessage) || key("screen.status.waiting_server_sync").equals(statusMessage)) {
            setFooterStatus("", StatusKind.INFO);
        }

        hasSyncedOnce = true;
        manualReloadRequested = false;
        manualSaveRequested = false;
        if (hasExpandedDropdown()) {
            syncRebuildPending = true;
        } else {
            clearAndInit();
        }
    }

    private boolean isDebugPageEnabled() {
        return serverSync != null && serverSync.debugPageEnabled();
    }

    private boolean isGameActive() {
        return serverSync != null && serverSync.gameState() != GameState.WAITING;
    }

    private boolean canChangeTeam() {
        return serverSync != null && serverSync.gameState() == GameState.WAITING;
    }

    /** Operators may edit at any time; mid-round only fields flagged {@code live} are enabled (see canEditField). */
    private boolean canEditConfig() {
        return canManage && serverSync != null;
    }

    private String configEditDeniedMessage() {
        if (!canManage) {
            return key("screen.error.config_op_only");
        }

        return key("screen.error.field_locked_live");
    }

    private List<Page> visiblePages() {
        List<Page> pages = new ArrayList<>();
        pages.add(Page.GAME);
        pages.add(Page.RULES);
        pages.add(Page.WILDCARD);
        if (isDebugPageEnabled()) {
            pages.add(Page.DEBUG);
        }
        return pages;
    }

    private void ensureVisiblePage() {
        if (currentPage == Page.DEBUG && !isDebugPageEnabled()) {
            currentPage = Page.GAME;
            restorePageScroll(scrollKey());
        }
    }

    private boolean isConfigEditPage() {
        return currentPage == Page.RULES || currentPage == Page.WILDCARD;
    }

    private boolean isHunterKillCountMode() {
        return editableConfig != null
                && HunterVictoryType.fromConfig(editableConfig.hunterVictoryType(), HunterVictoryType.RUNNERS_OUT) == HunterVictoryType.RUNNER_KILL_COUNT;
    }

    private int getNumber(ConfigSnapshot config, NumberField field) {
        return switch (field) {
            case PREPARING_SECONDS -> config.preparingSeconds();
            case HUNTER_RESPAWN_SECONDS -> config.hunterRespawnSeconds();
            case WILDCARD_INTERVAL_SECONDS -> config.wildcardIntervalSeconds();
            case WILDCARD_DURATION_SECONDS -> config.wildcardDurationSeconds();
            case WILDCARD_INTERVAL_MIN_SECONDS -> config.wildcardIntervalMinSeconds();
            case WILDCARD_INTERVAL_MAX_SECONDS -> config.wildcardIntervalMaxSeconds();
            case WILDCARD_DURATION_MIN_SECONDS -> config.wildcardDurationMinSeconds();
            case WILDCARD_DURATION_MAX_SECONDS -> config.wildcardDurationMaxSeconds();
            case HUNTER_RADAR_WARNING_DISTANCE -> config.hunterRadarWarningDistance();
            case SPACE_SHIFT_INTERVAL_SECONDS -> config.spaceShiftIntervalSeconds();
            case SUPPLY_DROP_INTERVAL_SECONDS -> config.supplyDropIntervalSeconds();
            case BLOCK_DECAY_SECONDS -> config.blockDecaySeconds();
            case PEARL_FRENZY_MAX_PEARLS -> config.pearlFrenzyMaxPearls();
            case PEARL_FRENZY_INTERVAL_SECONDS -> config.pearlFrenzyIntervalSeconds();
            case WIND_CHARGE_BRAWL_INTERVAL_SECONDS -> config.windChargeBrawlIntervalSeconds();
            case WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT -> config.windChargeExplosionMultiplierPercent();
            case BACKROOMS_DURATION_SECONDS -> config.backroomsDurationSeconds();
            case HUNTER_PREPARE_BOUNDARY_RADIUS -> config.hunterPrepareBoundaryRadius();
            case PIGLIN_PEARL_CHANCE_PERCENT -> config.piglinPearlChancePercent();
            case HUNTER_DAMAGE_MULTIPLIER_PERCENT -> config.hunterDamageMultiplierPercent();
            case HUNTER_SPEED_PERCENT -> config.hunterSpeedPercent();
            case RUNNER_SPEED_PERCENT -> config.runnerSpeedPercent();
            case HUNTER_HIT_CREDIT_SECONDS -> config.hunterHitCreditSeconds();
            case ENVIRONMENT_DEATHS_PER_KILL -> config.environmentDeathsPerKill();
            case RUNNER_RESPAWN_DISTANCE -> config.runnerRespawnDistance();
            case HUNTER_RESPAWN_DISTANCE -> config.hunterRespawnDistance();
            case HUNTER_RESPAWN_RUNNER_CLEARANCE -> config.hunterRespawnRunnerClearance();
            case HUNTER_RESPAWN_PENALTY_SECONDS -> config.hunterRespawnPenaltySeconds();
            case SURVIVE_TIME_SECONDS -> config.surviveTimeSeconds();
            case SURVIVE_BORDER_RADIUS -> config.surviveBorderRadius();
            case TARGET_X -> config.targetX();
            case TARGET_Y -> config.targetY();
            case TARGET_Z -> config.targetZ();
            case TARGET_RADIUS -> config.targetRadius();
            case TARGET_ITEM_COUNT -> config.targetItemCount();
            case HUNTER_LIVES -> config.hunterLives();
            case RUNNER_LIVES -> config.runnerLives();
            case RUNNER_RESPAWN_SECONDS -> config.runnerRespawnSeconds();
            case HUNTER_RUNNER_KILL_TARGET -> config.hunterRunnerKillTarget();
        };
    }

    private ConfigSnapshot setNumber(ConfigSnapshot config, NumberField field, int value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case PREPARING_SECONDS -> copy.preparingSeconds = value;
            case HUNTER_RESPAWN_SECONDS -> copy.hunterRespawnSeconds = value;
            case WILDCARD_INTERVAL_SECONDS -> copy.wildcardIntervalSeconds = value;
            case WILDCARD_DURATION_SECONDS -> copy.wildcardDurationSeconds = value;
            case WILDCARD_INTERVAL_MIN_SECONDS -> copy.wildcardIntervalMinSeconds = value;
            case WILDCARD_INTERVAL_MAX_SECONDS -> copy.wildcardIntervalMaxSeconds = value;
            case WILDCARD_DURATION_MIN_SECONDS -> copy.wildcardDurationMinSeconds = value;
            case WILDCARD_DURATION_MAX_SECONDS -> copy.wildcardDurationMaxSeconds = value;
            case HUNTER_RADAR_WARNING_DISTANCE -> copy.hunterRadarWarningDistance = value;
            case SPACE_SHIFT_INTERVAL_SECONDS -> copy.spaceShiftIntervalSeconds = value;
            case SUPPLY_DROP_INTERVAL_SECONDS -> copy.supplyDropIntervalSeconds = value;
            case BLOCK_DECAY_SECONDS -> copy.blockDecaySeconds = value;
            case PEARL_FRENZY_MAX_PEARLS -> copy.pearlFrenzyMaxPearls = value;
            case PEARL_FRENZY_INTERVAL_SECONDS -> copy.pearlFrenzyIntervalSeconds = value;
            case WIND_CHARGE_BRAWL_INTERVAL_SECONDS -> copy.windChargeBrawlIntervalSeconds = value;
            case WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT -> copy.windChargeExplosionMultiplierPercent = value;
            case BACKROOMS_DURATION_SECONDS -> copy.backroomsDurationSeconds = value;
            case HUNTER_PREPARE_BOUNDARY_RADIUS -> copy.hunterPrepareBoundaryRadius = value;
            case PIGLIN_PEARL_CHANCE_PERCENT -> copy.piglinPearlChancePercent = value;
            case HUNTER_DAMAGE_MULTIPLIER_PERCENT -> copy.hunterDamageMultiplierPercent = value;
            case HUNTER_SPEED_PERCENT -> copy.hunterSpeedPercent = value;
            case RUNNER_SPEED_PERCENT -> copy.runnerSpeedPercent = value;
            case HUNTER_HIT_CREDIT_SECONDS -> copy.hunterHitCreditSeconds = value;
            case ENVIRONMENT_DEATHS_PER_KILL -> copy.environmentDeathsPerKill = value;
            case RUNNER_RESPAWN_DISTANCE -> copy.runnerRespawnDistance = value;
            case HUNTER_RESPAWN_DISTANCE -> copy.hunterRespawnDistance = value;
            case HUNTER_RESPAWN_RUNNER_CLEARANCE -> copy.hunterRespawnRunnerClearance = value;
            case HUNTER_RESPAWN_PENALTY_SECONDS -> copy.hunterRespawnPenaltySeconds = value;
            case SURVIVE_TIME_SECONDS -> copy.surviveTimeSeconds = value;
            case SURVIVE_BORDER_RADIUS -> copy.surviveBorderRadius = value;
            case TARGET_X -> copy.targetX = value;
            case TARGET_Y -> copy.targetY = value;
            case TARGET_Z -> copy.targetZ = value;
            case TARGET_RADIUS -> copy.targetRadius = value;
            case TARGET_ITEM_COUNT -> copy.targetItemCount = value;
            case HUNTER_LIVES -> copy.hunterLives = value;
            case RUNNER_LIVES -> copy.runnerLives = value;
            case RUNNER_RESPAWN_SECONDS -> copy.runnerRespawnSeconds = value;
            case HUNTER_RUNNER_KILL_TARGET -> copy.hunterRunnerKillTarget = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    private String getString(ConfigSnapshot config, StringField field) {
        return switch (field) {
            case TARGET_ITEM_ID -> config.targetItemId();
        };
    }

    private ConfigSnapshot setString(ConfigSnapshot config, StringField field, String value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case TARGET_ITEM_ID -> copy.targetItemId = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    private String getDropdownValue(ConfigSnapshot config, DropdownField field) {
        return switch (field) {
            case RUNNER_VICTORY_TYPE -> config.runnerVictoryType();
            case HUNTER_VICTORY_TYPE -> config.hunterVictoryType();
            case HUNTER_RESPAWN_MODE -> config.hunterRespawnMode();
            case RUNNER_RESPAWN_MODE -> config.runnerRespawnMode();
            case RUNNER_TEAM_LOSS_MODE -> config.runnerTeamLossMode();
            case TARGET_DIMENSION -> config.targetDimension();
            case WILDCARD_INTERVAL_MODE -> config.wildcardIntervalMode();
            case WILDCARD_DURATION_MODE -> config.wildcardDurationMode();
        };
    }

    private ConfigSnapshot setDropdownValue(ConfigSnapshot config, DropdownField field, String value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case RUNNER_VICTORY_TYPE -> copy.runnerVictoryType = value;
            case HUNTER_VICTORY_TYPE -> copy.hunterVictoryType = value;
            case HUNTER_RESPAWN_MODE -> copy.hunterRespawnMode = value;
            case RUNNER_RESPAWN_MODE -> copy.runnerRespawnMode = value;
            case RUNNER_TEAM_LOSS_MODE -> copy.runnerTeamLossMode = value;
            case TARGET_DIMENSION -> copy.targetDimension = value;
            case WILDCARD_INTERVAL_MODE -> copy.wildcardIntervalMode = value;
            case WILDCARD_DURATION_MODE -> copy.wildcardDurationMode = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    private boolean getToggle(ConfigSnapshot config, ToggleField field) {
        return config.enabledWildcards().getOrDefault(field.id, Boolean.TRUE);
    }

    private ConfigSnapshot setToggle(ConfigSnapshot config, ToggleField field, boolean value) {
        ModConfig copy = config.toConfig();
        copy.enabledWildcards.put(field.id, value);
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    private boolean getBoolean(ConfigSnapshot config, BooleanField field) {
        return switch (field) {
            case HUNTER_PREPARE_BOUNDARY_ENABLED -> config.hunterPrepareBoundaryEnabled();
            case ENVIRONMENT_KILLS_ENABLED -> config.environmentKillsEnabled();
            case RUNNER_DEATH_NO_DROPS -> config.runnerDeathNoDrops();
            case HUNTER_DEATH_NO_DROPS -> config.hunterDeathNoDrops();
            case PIGLIN_PEARL_BOOST_ENABLED -> config.piglinPearlBoostEnabled();
            case RANDOM_RESPAWN_ENABLED -> config.randomRespawnEnabled();
            case LOCATOR_BAR_TEAM_ONLY -> config.locatorBarTeamOnly();
            case SURVIVE_BORDER_ENABLED -> config.surviveBorderEnabled();
        };
    }

    private ConfigSnapshot setBoolean(ConfigSnapshot config, BooleanField field, boolean value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case HUNTER_PREPARE_BOUNDARY_ENABLED -> copy.hunterPrepareBoundaryEnabled = value;
            case ENVIRONMENT_KILLS_ENABLED -> copy.environmentKillsEnabled = value;
            case RUNNER_DEATH_NO_DROPS -> copy.runnerDeathNoDrops = value;
            case HUNTER_DEATH_NO_DROPS -> copy.hunterDeathNoDrops = value;
            case PIGLIN_PEARL_BOOST_ENABLED -> copy.piglinPearlBoostEnabled = value;
            case RANDOM_RESPAWN_ENABLED -> copy.randomRespawnEnabled = value;
            case LOCATOR_BAR_TEAM_ONLY -> copy.locatorBarTeamOnly = value;
            case SURVIVE_BORDER_ENABLED -> copy.surviveBorderEnabled = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    private String stateName(GameState state) {
        return key("state." + state.name().toLowerCase());
    }

    private int stateColor(GameState state) {
        return switch (state) {
            case WAITING -> 0xFF9FAAB4;
            case PREPARING -> 0xFFFFD966;
            case RUNNING -> 0xFF77E287;
            case ENDING -> 0xFFFF8A8A;
        };
    }

    private String formatSeconds(int seconds) {
        return seconds < 0 ? key("common.none") : spec("screen.time.seconds", seconds);
    }

    private String wildcardDisplayName() {
        if (serverSync == null || serverSync.activeWildcard() == null || serverSync.activeWildcard().isBlank()) {
            return key("screen.wildcard.none_active");
        }
        return HunterWildcardText.wildcardNameKey(serverSync.activeWildcard());
    }

    private String compactWildcardDisplayName() {
        String name = wildcardDisplayName();
        return key("screen.wildcard.none_active").equals(name) ? key("common.none") : name;
    }

    private boolean hasWildcardSettings(ToggleField field) {
        return field == ToggleField.HUNTER_RADAR
                || field == ToggleField.SUPPLY_DROP
                || field == ToggleField.SPACE_SHIFT
                || field == ToggleField.BLOCK_DECAY
                || field == ToggleField.PEARL_FRENZY
                || field == ToggleField.WIND_CHARGE_BRAWL
                || field == ToggleField.BACKROOMS;
    }

    private int enabledWildcardCount(ConfigSnapshot config) {
        int count = 0;
        for (ToggleField field : ToggleField.values()) {
            if (getToggle(config, field)) {
                count++;
            }
        }
        return count;
    }

    private String startGameTooltip(boolean waiting) {
        if (!canManage) {
            return key("screen.error.start_op_only");
        }

        if (!waiting) {
            return key("screen.error.start_waiting_only");
        }

        if (serverSync == null || serverSync.hunterCount() == 0 || serverSync.runnerCount() == 0) {
            return key("screen.error.start_need_teams");
        }

        return "";
    }

    private String startConditionDisplay(String startTooltip) {
        if (startTooltip == null || startTooltip.isBlank()) {
            return key("screen.start_condition.met");
        }

        if (key("screen.error.start_need_teams").equals(startTooltip)) {
            return key("screen.start_condition.need_teams");
        }
        if (key("screen.error.start_op_only").equals(startTooltip)) {
            return key("screen.start_condition.op_only");
        }
        if (key("screen.error.start_waiting_only").equals(startTooltip)) {
            return key("screen.start_condition.waiting_only");
        }
        return startTooltip;
    }

    private int wildcardToggleColumns(int width) {
        if (width >= 420) {
            return 2;
        }
        return 1;
    }

    private int teamButtonColumns(int width) {
        if (width >= 620) {
            return 3;
        }
        if (width >= 380) {
            return 2;
        }
        return 1;
    }

    private String trim(String text, int width) {
        return textRenderer.trimToWidth(text, Math.max(10, width));
    }

    private int withAlpha(int color, float alpha) {
        int baseAlpha = color >>> 24;
        int scaledAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * alpha)));
        return (color & 0x00FFFFFF) | (scaledAlpha << 24);
    }

    private void renderFooterStatus(DrawContext context, Layout layout) {
        List<StatusSegment> segments = footerSegments();
        if (segments.isEmpty()) {
            return;
        }

        int x = layout.contentX();
        int y = layout.footerTop() + 8;
        int right = layout.contentX() + layout.usableContentWidth();
        for (int i = 0; i < segments.size(); i++) {
            StatusSegment segment = segments.get(i);
            if (x >= right) {
                return;
            }

            String text = trim(tr(segment.text()), right - x);
            if (!text.isBlank()) {
                context.drawText(textRenderer, Text.literal(text), x, y, segment.color(), false);
                x += textRenderer.getWidth(text);
            }

            String separator = tr(key("screen.footer.separator"));
            if (i < segments.size() - 1 && x + textRenderer.getWidth(separator) < right) {
                context.drawText(textRenderer, Text.literal(separator), x, y, 0xFF6F7C86, false);
                x += textRenderer.getWidth(separator);
            }
        }
    }

    private List<StatusSegment> footerSegments() {
        List<StatusSegment> segments = new ArrayList<>();
        if (serverSync == null) {
            if (!statusMessage.isBlank()) {
                segments.add(new StatusSegment(statusMessage, statusKind.color));
            }
            return segments;
        }

        if (isConfigEditPage()) {
            String modeText = canEditConfig()
                    ? (isRoundRunning() ? key("screen.footer.config_live") : key("screen.footer.config_editable"))
                    : key("screen.footer.config_readonly");
            int modeColor = canEditConfig() ? 0xFF7FC2FF : 0xFF9FAAB4;
            segments.add(new StatusSegment(modeText, modeColor));
            if (hasUnsavedChanges()) {
                segments.add(new StatusSegment(key("screen.footer.unsaved"), 0xFFFFB347));
            }
            if (!statusMessage.isBlank() && (statusKind == StatusKind.ERROR || !hasUnsavedChanges())) {
                segments.add(new StatusSegment(statusMessage, statusKind.color));
            }
            return segments;
        }

        if (!statusMessage.isBlank()) {
            segments.add(new StatusSegment(statusMessage, statusKind.color));
        }
        return segments;
    }

    private boolean hasUnsavedChanges() {
        return canEditConfig() && editableConfig != null && serverSync != null && !editableConfig.equals(serverSync.config());
    }

    private boolean canSaveConfig() {
        return canEditConfig()
                && editableConfig != null
                && serverSync != null
                && (hasUnsavedChanges() || hasVisibleInputChanges());
    }

    private boolean hasVisibleInputChanges() {
        if (!canEditConfig() || editableConfig == null) {
            return false;
        }
        return !pendingNumberText.isEmpty() || !pendingStringText.isEmpty();
    }

    private boolean hasFocusedTextField() {
        if (focusedInputKey != null) {
            return true;
        }

        for (TextFieldWidget field : numberFields.values()) {
            if (field.isFocused()) {
                return true;
            }
        }

        for (TextFieldWidget field : stringFields.values()) {
            if (field.isFocused()) {
                return true;
            }
        }

        return false;
    }

    private void ensureFocusedInputVisible(Layout layout) {
        for (TextFieldWidget field : numberFields.values()) {
            if (field.isFocused()) {
                ensureVisible(layout, field.getY(), field.getHeight());
                return;
            }
        }

        for (TextFieldWidget field : stringFields.values()) {
            if (field.isFocused()) {
                ensureVisible(layout, field.getY(), field.getHeight());
                return;
            }
        }
    }

    private void ensureVisible(Layout layout, int widgetY, int widgetHeight) {
        int margin = 8;
        if (widgetY < layout.viewportTop() + margin) {
            targetScrollOffset -= layout.viewportTop() + margin - widgetY;
        } else if (widgetY + widgetHeight > layout.viewportBottom() - margin) {
            targetScrollOffset += widgetY + widgetHeight - (layout.viewportBottom() - margin);
        }

        updateMaxScroll(layout);
        targetScrollOffset = clamp(targetScrollOffset, 0.0F, maxScroll);
        rememberCurrentScroll();
    }

    private Layout layout() {
        int panelWidth = Math.min(1120, Math.max(360, width - 24));
        if (panelWidth > width - 8) {
            panelWidth = Math.max(220, width - 8);
        }

        int panelHeight = Math.min(520, Math.max(260, height - 24));
        if (panelHeight > height - 8) {
            panelHeight = Math.max(200, height - 8);
        }

        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int navWidth = panelWidth < 520 ? 92 : panelWidth < 760 ? 104 : 116;
        int contentGap = panelWidth < 760 ? 12 : 16;
        int rightPadding = panelWidth < 760 ? 10 : 12;
        int contentX = panelX + navWidth + contentGap;
        int contentY = panelY + 48;
        int contentWidth = panelX + panelWidth - contentX - rightPadding;
        int footerHeight = 50;
        int footerTop = panelY + panelHeight - footerHeight;
        int viewportTop = contentY;
        int viewportBottom = Math.max(viewportTop + 40, footerTop - 12);
        int scrollBarX = contentX + contentWidth - 6;
        return new Layout(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                navWidth,
                contentX,
                contentY,
                contentWidth,
                viewportTop,
                viewportBottom,
                footerTop,
                footerHeight,
                SCROLL_BAR_RESERVE,
                scrollBarX
        );
    }

    private class CardBuilder {
        private final Layout layout;
        private final int x;
        private final int y;
        private final int width;
        private final String title;
        private int cursorY;
        private int bottomY;
        private boolean finished;

        CardBuilder(Layout layout, int x, int y, int width, String title) {
            this.layout = layout;
            this.x = x;
            this.y = y;
            this.width = width;
            this.title = title;
            this.cursorY = contentStartY(y);
            this.bottomY = cursorY;
            addCardTitle(layout, title, x + CARD_PADDING_X, y + CARD_PADDING_TOP);
        }

        int width() {
            return width;
        }

        void info(String label, String value, int valueColor) {
            addInfoRow(layout, label, value, contentX(), cursorY, contentWidth(), valueColor);
            advanceRow();
        }

        void number(NumberField field) {
            number(field, canManage);
        }

        void number(NumberField field, boolean editable) {
            advanceTo(addInputRow(layout, field, contentX(), cursorY, contentWidth(), editable));
        }

        void coordinates(NumberField xField, NumberField yField, NumberField zField) {
            int nextY = addCoordinateRow(layout, xField, yField, zField, contentX(), cursorY, contentWidth());
            bottomY = Math.max(bottomY, nextY - ROW_GAP);
            cursorY = nextY;
        }

        void numberPair(NumberField left, NumberField right) {
            int contentWidth = contentWidth();
            if (contentWidth < 420) {
                number(left);
                number(right);
                return;
            }

            int gap = 12;
            int fieldWidth = (contentWidth - gap) / 2;
            addInputRow(layout, left, contentX(), cursorY, fieldWidth, canManage);
            addInputRow(layout, right, contentX() + fieldWidth + gap, cursorY, fieldWidth, canManage);
            advanceRow();
        }

        void string(StringField field) {
            advanceTo(addInputRow(layout, field, contentX(), cursorY, contentWidth()));
        }

        void dropdown(DropdownField field) {
            dropdown(field, canManage);
        }

        void dropdown(DropdownField field, boolean editable) {
            advanceTo(addDropdownRow(layout, field, contentX(), cursorY, contentWidth(), editable));
        }

        void booleanField(BooleanField field) {
            addToggleRow(layout, field, contentX(), cursorY, contentWidth());
            advanceRow();
        }

        void hint(String text) {
            int hintY = cursorY + 2;
            int hintHeight = hintHeight(text, contentWidth());
            addHintText(layout, text, contentX(), hintY, contentWidth());
            bottomY = Math.max(bottomY, hintY + hintHeight);
            cursorY = hintY + hintHeight + ROW_GAP;
        }

        void button(String title, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled, int maxWidth) {
            buttonGrid(List.of(new ButtonSpec(title, action, variant, enabled)), 1, maxWidth);
        }

        void buttonGrid(List<ButtonSpec> buttons, int requestedColumns, int maxButtonWidth) {
            int nextY = addButtonRow(layout, buttons, contentX(), cursorY, contentWidth(), requestedColumns, maxButtonWidth);
            bottomY = Math.max(bottomY, nextY - ROW_GAP);
            cursorY = nextY;
        }

        void gap(int height) {
            cursorY += Math.max(0, height);
            bottomY = Math.max(bottomY, cursorY);
        }

        /** Small buttons on the title row (right-aligned); falls back to a button row when the card is too narrow. */
        void titleButtons(List<ButtonSpec> buttons, int buttonWidth) {
            int buttonHeight = 18;
            int totalWidth = buttonWidth * buttons.size() + BUTTON_GAP * (buttons.size() - 1);
            int titleWidth = textRenderer.getWidth(tr(title));
            if (width >= CARD_PADDING_X * 2 + titleWidth + 12 + totalWidth) {
                int buttonX = x + width - CARD_PADDING_X - totalWidth;
                int buttonY = y + CARD_PADDING_TOP - 3;
                for (int i = 0; i < buttons.size(); i++) {
                    ButtonSpec button = buttons.get(i);
                    addContentButton(layout, buttonX + i * (buttonWidth + BUTTON_GAP), buttonY, buttonWidth, buttonHeight, button.title(), button.tooltip(), button.action(), button.variant(), button.enabled());
                }
                gap(8);
                return;
            }

            buttonGrid(buttons, Math.min(2, buttons.size()), 112);
        }

        void toggleGrid(ToggleField[] fields, int requestedColumns) {
            int gap = 8;
            int columns = Math.max(1, Math.min(requestedColumns, fields.length));
            while (columns > 1 && (contentWidth() - (columns - 1) * gap) / columns < 118) {
                columns--;
            }

            int itemWidth = Math.min(WILDCARD_TOGGLE_MAX_WIDTH, Math.max(80, (contentWidth() - (columns - 1) * gap) / columns));
            int gridWidth = itemWidth * columns + (columns - 1) * gap;
            int gridX = contentX() + Math.max(0, (contentWidth() - gridWidth) / 2);
            int rows = (fields.length + columns - 1) / columns;
            for (int i = 0; i < fields.length; i++) {
                ToggleField field = fields[i];
                int column = i % columns;
                int row = i / columns;
                addWildcardToggleTile(
                        layout,
                        field,
                        gridX + column * (itemWidth + gap),
                        cursorY + row * (WILDCARD_TOGGLE_HEIGHT + gap),
                        itemWidth,
                        WILDCARD_TOGGLE_HEIGHT
                );
            }
            int nextY = cursorY + rows * WILDCARD_TOGGLE_HEIGHT + (rows - 1) * gap + ROW_GAP;
            bottomY = Math.max(bottomY, nextY - ROW_GAP);
            cursorY = nextY;
        }

        int height() {
            return Math.max(CARD_PADDING_TOP + CARD_TITLE_HEIGHT + CARD_PADDING_BOTTOM, bottomY - y + CARD_PADDING_BOTTOM);
        }

        int finish() {
            return finish(height());
        }

        int finish(int forcedHeight) {
            if (!finished) {
                drawCardBorder(layout, x, y, width, forcedHeight);
                finished = true;
            }
            return y + forcedHeight;
        }

        private int contentX() {
            return x + CARD_PADDING_X;
        }

        private int contentWidth() {
            return Math.max(40, width - CARD_PADDING_X * 2);
        }

        private void advanceRow() {
            bottomY = Math.max(bottomY, rowContentBottom(cursorY));
            cursorY = nextRowY(cursorY);
        }

        /** For rows that report their own next-row y (they may be taller than a single ROW_HEIGHT). */
        private void advanceTo(int nextY) {
            bottomY = Math.max(bottomY, rowContentBottom(nextY - ROW_HEIGHT - ROW_GAP));
            cursorY = nextY;
        }
    }

    @FunctionalInterface
    private interface CardBody {
        void build(CardBuilder card);
    }

    private record ButtonSpec(String title, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled, String tooltip) {
        ButtonSpec(String title, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled) {
            this(title, action, variant, enabled, "");
        }
    }

    private enum Page {
        GAME(key("screen.page.game"), key("screen.page.game.description")),
        RULES(key("screen.page.rules"), key("screen.page.rules.description")),
        WILDCARD(key("screen.page.wildcard"), key("screen.page.wildcard.description")),
        DEBUG(key("screen.page.debug"), key("screen.page.debug.description"));

        private final String label;
        private final String description;

        Page(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    /** Sub-pages of the RULES hub, in display order. */
    private enum RulesSubPage {
        TIME_BOUNDARY(key("screen.rules.time_boundary"), key("screen.rules.time_boundary.description")),
        VICTORY(key("screen.rules.victory"), key("screen.rules.victory.description")),
        RESPAWN(key("screen.rules.respawn"), key("screen.rules.respawn.description")),
        KILL_CREDIT(key("screen.rules.kill_credit"), key("screen.rules.kill_credit.description")),
        BALANCE(key("screen.rules.balance"), key("screen.rules.balance.description"));

        private final String label;
        private final String description;

        RulesSubPage(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    /** {@code live}: may be changed by an operator while a round is PREPARING/RUNNING. */
    private enum NumberField {
        PREPARING_SECONDS(key("config.number.preparing_seconds"), key("unit.seconds"), 1, false),
        HUNTER_RESPAWN_SECONDS(key("config.number.hunter_respawn_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_INTERVAL_SECONDS(key("config.number.wildcard_interval_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_DURATION_SECONDS(key("config.number.wildcard_duration_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_INTERVAL_MIN_SECONDS(key("config.number.wildcard_interval_min_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_INTERVAL_MAX_SECONDS(key("config.number.wildcard_interval_max_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_DURATION_MIN_SECONDS(key("config.number.wildcard_duration_min_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_DURATION_MAX_SECONDS(key("config.number.wildcard_duration_max_seconds"), key("unit.seconds"), 1, true),
        HUNTER_RADAR_WARNING_DISTANCE(key("config.number.hunter_radar_warning_distance"), key("unit.blocks"), 1, true),
        SPACE_SHIFT_INTERVAL_SECONDS(key("config.number.space_shift_interval_seconds"), key("unit.seconds"), 1, true),
        SUPPLY_DROP_INTERVAL_SECONDS(key("config.number.supply_drop_interval_seconds"), key("unit.seconds"), 1, true),
        BLOCK_DECAY_SECONDS(key("config.number.block_decay_seconds"), key("unit.seconds"), 1, true),
        PEARL_FRENZY_MAX_PEARLS(key("config.number.pearl_frenzy_max_pearls"), key("unit.items"), 1, true),
        PEARL_FRENZY_INTERVAL_SECONDS(key("config.number.pearl_frenzy_interval_seconds"), key("unit.seconds"), 1, true),
        WIND_CHARGE_BRAWL_INTERVAL_SECONDS(key("config.number.wind_charge_brawl_interval_seconds"), key("unit.seconds"), 1, true),
        WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT(key("config.number.wind_charge_explosion_multiplier_percent"), key("unit.percent"), 1, true),
        BACKROOMS_DURATION_SECONDS(key("config.number.backrooms_duration_seconds"), key("unit.seconds"), 1, true),
        HUNTER_PREPARE_BOUNDARY_RADIUS(key("config.number.hunter_prepare_boundary_radius"), key("unit.blocks"), 1, false),
        PIGLIN_PEARL_CHANCE_PERCENT(key("config.number.piglin_pearl_chance_percent"), key("unit.percent"), 0, true),
        HUNTER_DAMAGE_MULTIPLIER_PERCENT(key("config.number.hunter_damage_multiplier_percent"), key("unit.percent"), 1, true),
        HUNTER_SPEED_PERCENT(key("config.number.hunter_speed_percent"), key("unit.percent"), 10, true),
        RUNNER_SPEED_PERCENT(key("config.number.runner_speed_percent"), key("unit.percent"), 10, true),
        HUNTER_HIT_CREDIT_SECONDS(key("config.number.hunter_hit_credit_seconds"), key("unit.seconds"), 0, true),
        ENVIRONMENT_DEATHS_PER_KILL(key("config.number.environment_deaths_per_kill"), key("unit.times"), 1, true),
        RUNNER_RESPAWN_DISTANCE(key("config.number.runner_respawn_distance"), key("unit.blocks"), 16, true),
        HUNTER_RESPAWN_DISTANCE(key("config.number.hunter_respawn_distance"), key("unit.blocks"), 16, true),
        HUNTER_RESPAWN_RUNNER_CLEARANCE(key("config.number.hunter_respawn_runner_clearance"), key("unit.blocks"), 0, true),
        HUNTER_RESPAWN_PENALTY_SECONDS(key("config.number.hunter_respawn_penalty_seconds"), key("unit.seconds"), 0, true),
        SURVIVE_TIME_SECONDS(key("config.number.survive_time_seconds"), key("unit.seconds"), 1, true),
        SURVIVE_BORDER_RADIUS(key("config.number.survive_border_radius"), key("unit.blocks"), 32, false),
        TARGET_X(key("config.number.target_x"), "", Integer.MIN_VALUE, true),
        TARGET_Y(key("config.number.target_y"), "", Integer.MIN_VALUE, true),
        TARGET_Z(key("config.number.target_z"), "", Integer.MIN_VALUE, true),
        TARGET_RADIUS(key("config.number.target_radius"), key("unit.blocks"), 1, true),
        TARGET_ITEM_COUNT(key("config.number.target_item_count"), key("unit.items"), 1, true),
        HUNTER_LIVES(key("config.number.hunter_lives"), key("unit.lives"), 0, false),
        RUNNER_LIVES(key("config.number.runner_lives"), key("unit.lives"), 1, false),
        RUNNER_RESPAWN_SECONDS(key("config.number.runner_respawn_seconds"), key("unit.seconds"), 1, true),
        HUNTER_RUNNER_KILL_TARGET(key("config.number.hunter_runner_kill_target"), key("unit.times"), 1, true);

        private final String label;
        private final String unit;
        private final int minValue;
        private final boolean live;

        NumberField(String label, String unit, int minValue, boolean live) {
            this.label = label;
            this.unit = unit;
            this.minValue = minValue;
            this.live = live;
        }

        private boolean allowsNegative() {
            return minValue < 0;
        }
    }

    private enum StringField {
        TARGET_ITEM_ID(key("config.string.target_item_id"), 128, true);

        private final String label;
        private final int maxLength;
        private final boolean live;

        StringField(String label, int maxLength, boolean live) {
            this.label = label;
            this.maxLength = maxLength;
            this.live = live;
        }
    }

    private enum DropdownField {
        WILDCARD_INTERVAL_MODE(key("config.dropdown.wildcard_interval_mode"), true, List.of(
                option("FIXED", key("config.timing_mode.fixed")),
                option("RANDOM", key("config.timing_mode.random"))
        )),
        WILDCARD_DURATION_MODE(key("config.dropdown.wildcard_duration_mode"), true, List.of(
                option("FIXED", key("config.timing_mode.fixed")),
                option("RANDOM", key("config.timing_mode.random"))
        )),
        RUNNER_VICTORY_TYPE(key("config.dropdown.runner_victory_type"), false, List.of(
                option(RunnerVictoryType.DRAGON.name(), RunnerVictoryType.DRAGON.getDisplayName()),
                option(RunnerVictoryType.SURVIVE_TIME.name(), RunnerVictoryType.SURVIVE_TIME.getDisplayName()),
                option(RunnerVictoryType.REACH_LOCATION.name(), RunnerVictoryType.REACH_LOCATION.getDisplayName()),
                option(RunnerVictoryType.COLLECT_ITEM.name(), RunnerVictoryType.COLLECT_ITEM.getDisplayName())
        )),
        HUNTER_VICTORY_TYPE(key("config.dropdown.hunter_victory_type"), false, List.of(
                option(HunterVictoryType.RUNNERS_OUT.name(), HunterVictoryType.RUNNERS_OUT.getDisplayName()),
                option(HunterVictoryType.RUNNER_KILL_COUNT.name(), HunterVictoryType.RUNNER_KILL_COUNT.getDisplayName())
        )),
        HUNTER_RESPAWN_MODE(key("config.dropdown.hunter_respawn_mode"), false, List.of(
                option(RespawnMode.INFINITE.name(), key("config.respawn_mode.infinite")),
                option(RespawnMode.LIMITED_LIVES.name(), key("config.respawn_mode.limited_lives")),
                option(RespawnMode.NO_RESPAWN.name(), key("config.respawn_mode.no_respawn"))
        )),
        RUNNER_RESPAWN_MODE(key("config.dropdown.runner_respawn_mode"), false, List.of(
                option(RespawnMode.INFINITE.name(), key("config.respawn_mode.infinite")),
                option(RespawnMode.LIMITED_LIVES.name(), key("config.respawn_mode.limited_lives")),
                option(RespawnMode.NO_RESPAWN.name(), key("config.respawn_mode.no_respawn"))
        )),
        RUNNER_TEAM_LOSS_MODE(key("config.dropdown.runner_team_loss_mode"), true, List.of(
                option(RunnerTeamLossMode.ANY_RUNNER_OUT.name(), key("config.runner_team_loss.any_runner_out")),
                option(RunnerTeamLossMode.ALL_RUNNERS_OUT.name(), key("config.runner_team_loss.all_runners_out"))
        )),
        TARGET_DIMENSION(key("config.dropdown.target_dimension"), true, List.of(
                option("minecraft:overworld", key("config.dimension.overworld")),
                option("minecraft:the_nether", key("config.dimension.the_nether")),
                option("minecraft:the_end", key("config.dimension.the_end"))
        ));

        private final String label;
        private final boolean live;
        private final List<DropdownWidget.Option> options;

        DropdownField(String label, boolean live, List<DropdownWidget.Option> options) {
            this.label = label;
            this.live = live;
            this.options = options;
        }
    }

    private static DropdownWidget.Option option(String value, String displayName) {
        return new DropdownWidget.Option(value, displayName);
    }

    private enum BooleanField {
        HUNTER_PREPARE_BOUNDARY_ENABLED(key("config.boolean.hunter_prepare_boundary_enabled"), false),
        RUNNER_DEATH_NO_DROPS(key("config.boolean.runner_death_no_drops"), true),
        HUNTER_DEATH_NO_DROPS(key("config.boolean.hunter_death_no_drops"), true),
        PIGLIN_PEARL_BOOST_ENABLED(key("config.boolean.piglin_pearl_boost_enabled"), true),
        RANDOM_RESPAWN_ENABLED(key("config.boolean.random_respawn_enabled"), true),
        LOCATOR_BAR_TEAM_ONLY(key("config.boolean.locator_bar_team_only"), true),
        SURVIVE_BORDER_ENABLED(key("config.boolean.survive_border_enabled"), false),
        ENVIRONMENT_KILLS_ENABLED(key("config.boolean.environment_kills_enabled"), true);

        private final String label;
        private final boolean live;

        BooleanField(String label, boolean live) {
            this.label = label;
            this.live = live;
        }
    }

    private enum WildcardCategory {
        COMBAT(key("screen.wildcard_category.combat")),
        MOBILITY(key("screen.wildcard_category.mobility")),
        VISION(key("screen.wildcard_category.vision")),
        WORLD(key("screen.wildcard_category.world"));

        private final String label;

        WildcardCategory(String label) {
            this.label = label;
        }
    }

    /** Same order as {@link WildcardIds#ALL}, grouped by category for the toggle page. */
    private enum ToggleField {
        BACKSTAB(WildcardIds.BACKSTAB, WildcardCategory.COMBAT),
        VAMPIRE(WildcardIds.VAMPIRE, WildcardCategory.COMBAT),
        BLOOD_RAGE(WildcardIds.BLOOD_RAGE, WildcardCategory.COMBAT),
        WEAPON_OVERHEAT(WildcardIds.WEAPON_OVERHEAT, WildcardCategory.COMBAT),
        STAY_AWAY(WildcardIds.STAY_AWAY, WildcardCategory.COMBAT),
        FRAGILE(WildcardIds.FRAGILE, WildcardCategory.COMBAT),
        EXPLOSIVE_DEATH(WildcardIds.EXPLOSIVE_DEATH, WildcardCategory.COMBAT),
        KEY_SCRAMBLE(WildcardIds.KEY_SCRAMBLE, WildcardCategory.COMBAT),
        FLASH(WildcardIds.FLASH, WildcardCategory.MOBILITY),
        SHADOW_STEP(WildcardIds.SHADOW_STEP, WildcardCategory.MOBILITY),
        HURT_TELEPORT(WildcardIds.HURT_TELEPORT, WildcardCategory.MOBILITY),
        SPACE_SHIFT(WildcardIds.SPACE_SHIFT, WildcardCategory.MOBILITY),
        PORTAL(WildcardIds.PORTAL, WildcardCategory.MOBILITY),
        PEARL_FRENZY(WildcardIds.PEARL_FRENZY, WildcardCategory.MOBILITY),
        WIND_CHARGE_BRAWL(WildcardIds.WIND_CHARGE_BRAWL, WildcardCategory.MOBILITY),
        LIGHT_LOAD(WildcardIds.LIGHT_LOAD, WildcardCategory.MOBILITY),
        HUNGER_CHASE(WildcardIds.HUNGER_CHASE, WildcardCategory.MOBILITY),
        STILL_GLOW(WildcardIds.STILL_GLOW, WildcardCategory.VISION),
        SNEAK_FREEZE(WildcardIds.SNEAK_FREEZE, WildcardCategory.VISION),
        HUNTER_RADAR(WildcardIds.HUNTER_RADAR, WildcardCategory.VISION),
        WHO_ARE_YOU(WildcardIds.WHO_ARE_YOU, WildcardCategory.VISION),
        TINY_PLAYERS(WildcardIds.TINY_PLAYERS, WildcardCategory.VISION),
        WORLD_TILT(WildcardIds.WORLD_TILT, WildcardCategory.VISION),
        SUPPLY_DROP(WildcardIds.SUPPLY_DROP, WildcardCategory.WORLD),
        DROP_BOMB(WildcardIds.DROP_BOMB, WildcardCategory.WORLD),
        CHAIN_MINING(WildcardIds.CHAIN_MINING, WildcardCategory.WORLD),
        BLOCK_DECAY(WildcardIds.BLOCK_DECAY, WildcardCategory.WORLD),
        BACKROOMS(WildcardIds.BACKROOMS, WildcardCategory.WORLD);

        private final String id;
        private final String label;
        private final String description;
        private final WildcardCategory category;

        ToggleField(String id, WildcardCategory category) {
            this.id = id;
            this.label = HunterWildcardText.wildcardNameKey(id);
            this.description = HunterWildcardText.wildcardDescriptionKey(id);
            this.category = category;
        }

        static ToggleField[] inCategory(WildcardCategory category) {
            List<ToggleField> fields = new ArrayList<>();
            for (ToggleField field : values()) {
                if (field.category == category) {
                    fields.add(field);
                }
            }
            return fields.toArray(new ToggleField[0]);
        }
    }

    private enum ButtonVariant {
        PRIMARY,
        NORMAL,
        SELECTED,
        DANGER,
        TOGGLE_ON,
        TOGGLE_OFF,
        DISABLED
    }

    private enum StatusKind {
        INFO(0xFFFFD966),
        SUCCESS(0xFF77E287),
        ERROR(0xFFFF8A8A);

        private final int color;

        StatusKind(int color) {
            this.color = color;
        }
    }

    private record Layout(
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int navWidth,
            int contentX,
            int contentY,
            int contentWidth,
            int viewportTop,
            int viewportBottom,
            int footerTop,
            int footerHeight,
            int scrollBarReserve,
            int scrollBarX
    ) {
        int usableContentWidth() {
            return Math.max(80, contentWidth - scrollBarReserve);
        }

        int viewportHeight() {
            return viewportBottom - viewportTop;
        }
    }

    private record StatusSegment(String text, int color) {
    }

    private record StatusBlock(String label, String value, int color) {
    }

    private record Label(String text, int x, int y, int color, boolean shadow, int width) {
        Label(String text, int x, int y, int color) {
            this(text, x, y, color, false, 0);
        }

        Label(String text, int x, int y, int color, boolean shadow) {
            this(text, x, y, color, shadow, 0);
        }

        int maxWidth(Layout layout) {
            return width > 0 ? width : Math.max(20, layout.panelX() + layout.panelWidth() - x - 12);
        }
    }

    private record WrappedLabel(String text, int x, int y, int height, int color, boolean shadow, int width, int maxLines) {
        int maxWidth(Layout layout) {
            return width > 0 ? width : Math.max(20, layout.panelX() + layout.panelWidth() - x - 12);
        }
    }

    private record Box(int x, int y, int width, int height, int color, int borderColor) {
    }

    private record Icon(ItemStack stack, int x, int y) {
    }

    private class StyledButtonWidget extends ButtonWidget {
        private final String description;
        private final ButtonVariant variant;

        StyledButtonWidget(int x, int y, int width, int height, String title, String description, ButtonWidget.PressAction action, ButtonVariant variant) {
            super(x, y, width, height, text(title), action, DEFAULT_NARRATION_SUPPLIER);
            this.description = description == null ? "" : description;
            this.variant = variant == null ? ButtonVariant.NORMAL : variant;
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
            ButtonVariant renderedVariant = active || variant == ButtonVariant.SELECTED ? variant : ButtonVariant.DISABLED;
            Palette palette = palette(renderedVariant, isHovered());
            if (isHovered() && !description.isBlank()) {
                setHoverTooltip(description, mouseX, mouseY);
            }
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();

            context.fill(x, y, x + width, y + height, palette.background);
            context.fill(x, y, x + width, y + 1, palette.border);
            context.fill(x, y + height - 1, x + width, y + height, palette.border);
            context.fill(x, y, x + 1, y + height, palette.border);
            context.fill(x + width - 1, y, x + width, y + height, palette.border);
            if (renderedVariant != ButtonVariant.NORMAL) {
                context.fill(x + 2, y + 2, x + 5, y + height - 2, palette.accent);
            }

            String title = trim(getMessage().getString(), width - 12);
            if (!description.isBlank() && height >= 34) {
                int textX = renderedVariant == ButtonVariant.NORMAL ? x + 8 : x + 11;
                context.drawText(textRenderer, net.minecraft.text.Text.literal(title), textX, y + 7, palette.titleColor, true);
                context.drawText(textRenderer, net.minecraft.text.Text.literal(trim(tr(description), width - 20)), textX, y + 23, palette.descriptionColor, false);
                return;
            }

            int titleX = x + Math.max(4, (width - textRenderer.getWidth(title)) / 2);
            int titlePadding = height <= 18 ? 1 : 4;
            int titleY = y + Math.max(titlePadding, (height - textRenderer.fontHeight) / 2);
            if ("i".equals(title) && height <= 14) {
                titleY = y + Math.max(0, (height - textRenderer.fontHeight + 1) / 2);
            }
            context.drawText(textRenderer, net.minecraft.text.Text.literal(title), titleX, titleY, palette.titleColor, true);
        }

        @Override
        protected void drawLabel(net.minecraft.client.font.DrawnTextConsumer textConsumer) {
        }

        private Palette palette(ButtonVariant variant, boolean hovered) {
            return switch (variant) {
                case PRIMARY -> new Palette(hovered ? 0xCC246C86 : 0xAA1F536A, hovered ? 0xFF7FE7FF : 0xFF54B8D6, 0xFF7FE7FF, 0xFFFFFFFF, 0xFFD7F8FF);
                case SELECTED -> new Palette(0xAA345B78, 0xFF7FC2FF, 0xFF7FC2FF, 0xFFFFFFFF, 0xFFD7ECFF);
                case DANGER -> new Palette(hovered ? 0xAA6D3434 : 0x8845292F, hovered ? 0xFFFF8A8A : 0xFFD76474, 0xFFFF8A8A, 0xFFFFFFFF, 0xFFFFC2C8);
                case TOGGLE_ON -> new Palette(hovered ? 0xAA2E5C49 : 0x88324B3F, hovered ? 0xFF77E287 : 0xFF55B978, 0xFF77E287, 0xFFFFFFFF, 0xFFD7F8E1);
                case TOGGLE_OFF -> new Palette(hovered ? 0xAA3A4652 : 0x88303A46, hovered ? 0xFF8A98A6 : 0xFF59636C, 0xFF8A98A6, 0xFFE1E6EB, 0xFF9FAAB4);
                case DISABLED -> new Palette(0x66303A46, 0xFF59636C, 0xFF59636C, 0xFF9FAAB4, 0xFF9FAAB4);
                default -> new Palette(hovered ? 0xAA3E5570 : 0x88303A46, hovered ? 0xFF74B6FF : 0xFF4C5A66, 0xFF74B6FF, 0xFFFFFFFF, 0xFFC9D4DE);
            };
        }
    }

    private record Palette(int background, int border, int accent, int titleColor, int descriptionColor) {
    }
}
