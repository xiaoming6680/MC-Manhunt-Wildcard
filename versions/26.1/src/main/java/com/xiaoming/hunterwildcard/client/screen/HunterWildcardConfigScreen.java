package com.xiaoming.hunterwildcard.client.screen;

import com.xiaoming.hunterwildcard.client.ClientGameStatus;
import com.xiaoming.hunterwildcard.client.ui.ConfigDraft;
import com.xiaoming.hunterwildcard.client.ui.RuleFields.BooleanField;
import com.xiaoming.hunterwildcard.client.ui.RuleFields.DropdownField;
import com.xiaoming.hunterwildcard.client.ui.RuleFields.NumberField;
import com.xiaoming.hunterwildcard.client.ui.RuleFields.StringField;
import com.xiaoming.hunterwildcard.client.ui.RuleFields.ToggleField;
import com.xiaoming.hunterwildcard.client.ui.RuleFields.WildcardCategory;

import static com.xiaoming.hunterwildcard.client.ui.RuleFields.*;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
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
    private boolean draggingScroll;
    private double scrollGrab;

    private final List<Label> labels = new ArrayList<>();
    private final List<WrappedLabel> wrappedLabels = new ArrayList<>();
    private final List<Box> boxes = new ArrayList<>();
    private final List<Icon> icons = new ArrayList<>();
    /** Every widget that lives inside the scrolling content area; moved on scroll instead of rebuilt. */
    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private int builtScroll;
    private final List<NavigationEntry> navigationEntries = new ArrayList<>();
    private record NavigationEntry(StyledButtonWidget button, Page page, RulesSubPage sub) {}
    private float rulesExpansion;
    private long rulesAnimationFrame;
    private boolean syncRebuildPending;
    private HunterWildcardPackets.RoundDetailsPayload displayedDetails;
    private final Map<NumberField, EditBox> numberFields = new EnumMap<>(NumberField.class);
    private final Map<StringField, EditBox> stringFields = new EnumMap<>(StringField.class);
    private final Map<DropdownField, DropdownWidget> dropdownFields = new EnumMap<>(DropdownField.class);
    private final Map<String, Float> pageScrollOffsets = new HashMap<>();
    private final Map<String, Float> pageTargetScrollOffsets = new HashMap<>();
    /** Raw text the user typed into number fields; survives rebuilds until committed to {@link #editableConfig}. */
    private final Map<NumberField, String> pendingNumberText = new EnumMap<>(NumberField.class);
    private final Map<StringField, String> pendingStringText = new EnumMap<>(StringField.class);

    private boolean confirmStop;
    private Page currentPage = Page.GAME;
    /** Open RULES sub-page, or null while the hub is shown. Per-instance: resets whenever the screen is reopened. */
    private RulesSubPage rulesSubPage;
    private boolean overviewReadOnly;
    private String hoveredNavigation = "";
    private long navigationSoundAt;
    private final Map<String, NavigationAnimation> navigationAnimations = new HashMap<>();
    private static final class NavigationAnimation {float hover;long frameAt;}
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
    private StyledButtonWidget changesLink;
    private final java.util.List<InfoHint> infoHints=new java.util.ArrayList<>();
    private record InfoHint(int x,int y,String tooltip) {}
    private ToggleField selectedWildcardSettings;
    private boolean advancedItemId;
    private String wildcardQuery="";
    private boolean searchRebuild;
    private EditBox wildcardSearch;

    public HunterWildcardConfigScreen() {
        super(Component.translatable(HunterWildcardText.key("screen.title")));
        editableConfig = ConfigDraft.value;
        for (NumberField f : NumberField.values()) if (ConfigDraft.raw.containsKey(f.name())) pendingNumberText.put(f, ConfigDraft.raw.get(f.name()));
        for (StringField f : StringField.values()) if (ConfigDraft.raw.containsKey(f.name())) pendingStringText.put(f, ConfigDraft.raw.get(f.name()));
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

    private static Component text(String spec) {
        return HunterWildcardClientText.text(spec);
    }

    public static void receiveSync(SyncConfigPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof HunterWildcardConfigScreen screen) {
            screen.applySync(payload);
        }
    }

    public static void receiveOperationResult(OperationResultPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof HunterWildcardConfigScreen screen) {
            if (payload.requestId() != 0) {
                screen.manualSaveRequested = ConfigDraft.pendingId != 0;
                screen.editableConfig = ConfigDraft.value;
                if (payload.success()) { screen.pendingNumberText.clear(); screen.pendingStringText.clear(); }
            }
            screen.showToast(payload.message(), payload.success() ? StatusKind.SUCCESS : StatusKind.ERROR);
            screen.rebuildWidgets();
        }
    }

    public static void closeFromServer() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof HunterWildcardConfigScreen) {
            ((HunterWildcardConfigScreen) client.screen).onClose();
        }
    }

    @Override
    protected void init() {
        displayedDetails = ClientGameStatus.details;
        captureInputState();
        syncRebuildPending = false;
        if (canManage && editableConfig != null && ConfigDraft.pendingId == 0) ConfigDraft.value = editableConfig;
        labels.clear();
        wrappedLabels.clear();
        boxes.clear();
        icons.clear();infoHints.clear();
        numberFields.clear();
        stringFields.clear();
        dropdownFields.clear();
        wildcardSearch = null;
        saveButton = null;changesLink=null;

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
            case CHANGES -> buildChangesPage(layout);
            case RESULT -> buildResultPage(layout);
        }

        updateMaxScroll(layout);
        if (renderedScroll != buildScroll) {
            rebuildWidgets();
            return;
        }

        if (canEditConfig() && ((currentPage==Page.RULES && rulesSubPage!=null) || (currentPage==Page.WILDCARD && selectedWildcardSettings!=null)))
            addButton(layout.contentX()+layout.usableContentWidth()-76,layout.panelY()+28,76,16,key("ui.defaults.page"),"",b->restoreDefaultConfig(),ButtonVariant.LINK,true);
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
        for (Map.Entry<NumberField, EditBox> entry : numberFields.entrySet()) {
            if (entry.getValue().isFocused()) {
                rememberFocus(entry.getKey(), entry.getValue());
                sawFocused = true;
            } else if (entry.getKey() == focusedInputKey) {
                focusedInputKey = null;
            }
        }
        for (Map.Entry<StringField, EditBox> entry : stringFields.entrySet()) {
            if (entry.getValue().isFocused()) {
                rememberFocus(entry.getKey(), entry.getValue());
                sawFocused = true;
            } else if (entry.getKey() == focusedInputKey) {
                focusedInputKey = null;
            }
        }
        if (!sawFocused && getFocused() != null && !(getFocused() instanceof EditBox)) {
            // Focus moved to a button/dropdown: the remembered field must not steal it back.
            focusedInputKey = null;
        }
    }

    private void rememberFocus(Object key, EditBox widget) {
        focusedInputKey = key;
        focusedCursor = widget.getCursorPosition();
        focusedSelectionStart = selectionAnchor(widget);
    }

    private int selectionAnchor(EditBox widget) {
        // TextFieldWidget exposes no selection getter; keep the cursor and let the restore collapse the selection.
        return widget.getCursorPosition();
    }

    private void restoreInputState() {
        if (focusedInputKey == null) {
            return;
        }

        EditBox widget = focusedInputKey instanceof NumberField numberField
                ? numberFields.get(numberField)
                : focusedInputKey instanceof StringField stringField ? stringFields.get(stringField) : null;
        if (widget == null || !widget.active) {
            return;
        }

        setFocused(widget);
        widget.setFocused(true);
        int length = widget.getValue().length();
        int cursor = focusedCursor < 0 ? length : Math.min(focusedCursor, length);
        widget.moveCursorTo(cursor, false);
        if (focusedSelectionStart >= 0 && focusedSelectionStart != cursor) {
            widget.setCursorPosition(Math.min(focusedSelectionStart, length));
            widget.setHighlightPos(cursor);
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
            return selectedWildcardSettings.description;
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
        rebuildWidgets();
    }

    private void openRulesSubPage(RulesSubPage subPage) {
        commitVisibleInputs();
        closeDropdowns();
        rememberCurrentScroll();
        rulesSubPage = subPage;
        restorePageScroll(scrollKey());
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Layout layout = layout();
        updateNavigationLayout(layout);
        updateSmoothScroll(layout, delta);
        int scrollDelta = renderedScroll - builtScroll;
        if (scrollDelta != 0) {
            shiftContent(-scrollDelta);
            builtScroll = renderedScroll;
            updateContentWidgetVisibility(layout);
        }

        context.fill(0, 0, width, height, 0x88000000);
        context.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.panelWidth(), layout.panelY() + layout.panelHeight(), 0xF512161C);
        context.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.navWidth(), layout.panelY() + layout.panelHeight(), 0xFF171D25);
        context.fill(layout.panelX() + layout.navWidth(), layout.panelY(), layout.panelX() + layout.navWidth() + 1, layout.panelY() + layout.panelHeight(), 0xFF35404B);

        context.text(font, Component.literal("MANHUNT"), layout.panelX() + 10, layout.panelY() + 10, 0xFFE8EDF2, true);
        context.text(font, Component.literal("WILDCARD"), layout.panelX()+10,layout.panelY()+23,0xFFB99AFF,false);
        int headerX = hasOpenSubPage() ? layout.contentX() + 24 : layout.contentX();
        int headerWidth = Math.max(20, layout.usableContentWidth() - (headerX - layout.contentX()) - ((currentPage == Page.RULES && rulesSubPage != null || currentPage == Page.WILDCARD && selectedWildcardSettings != null) && canManage ? 80 : 0));
        context.text(font, Component.literal(trim(tr(headerTitle()), headerWidth)), headerX, layout.panelY() + 14, 0xFFFFFFFF, true);
        context.text(font, Component.literal(trim(tr(headerDescription()), headerWidth)), headerX, layout.panelY() + 29, 0xFF9FAAB4, false);

        context.enableScissor(layout.contentX(), layout.viewportTop(), layout.contentX() + layout.usableContentWidth(), layout.viewportBottom());
        for (Box box : boxes) {
            context.fill(box.x, box.y, box.x + box.width, box.y + box.height, box.color);
            context.fill(box.x, box.y, box.x + 2, box.y + box.height, box.borderColor);
            context.fill(box.x, box.y + box.height - 1, box.x + box.width, box.y + box.height, box.borderColor);


        }

        for (Label label : labels) {
            context.text(font, Component.literal(trim(tr(label.text), label.maxWidth(layout))), label.x, label.y, label.color, label.shadow);
        }
        for (WrappedLabel label : wrappedLabels) {
            if (label.text().equals(key("ui.preset.hint"))) {
                label = new WrappedLabel(presetDescriptionAt(mouseX, mouseY), label.x(), label.y(), label.height(), label.color(), label.shadow(), label.width(), label.maxLines());
            }
            renderWrappedLabel(context, layout, label);
        }
        for (Icon icon : icons) {
            context.item(icon.stack, icon.x, icon.y);
        }
        context.disableScissor();

        renderFooterStatus(context, layout);
        renderInputErrors(context);
        renderScrollBar(context, layout);
        renderNavigationScrollBar(context, layout);
        updateNavigationFeedback(mouseX,mouseY);

        hoverTooltip = "";
        for(InfoHint hint:infoHints)if(isInsideContent(layout,mouseX,mouseY) && mouseX>=hint.x && mouseX<hint.x+14 && mouseY>=hint.y && mouseY<hint.y+20)setHoverTooltip(hint.tooltip,mouseX,mouseY);
        if(changesLink!=null)changesLink.setMessage(text(spec("ui.changes.count",changeCount())));
        if (saveButton != null) {
            saveButton.active = canSaveConfig();
        }
        for (AbstractWidget widget : contentWidgets) {
            widget.visible = false;
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.enableScissor(layout.contentX(), layout.viewportTop(), layout.contentX() + layout.usableContentWidth(), layout.viewportBottom());
        for (AbstractWidget widget : contentWidgets) {
            if (intersectsViewport(layout, widget)) {
                widget.visible = true;
                widget.extractRenderState(context, mouseX, mouseY, delta);
            }
        }
        context.disableScissor();
        renderToast(context, layout, delta);
        renderDropdownOverlays(context, mouseX, mouseY, delta);
        renderHoverTooltip(context);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        Layout sl = layout();
        if (click.button() == 0 && maxScroll > 0 && Math.abs(mouseX - sl.scrollBarX()) < 7
                && mouseY >= sl.viewportTop() && mouseY <= sl.viewportBottom()) {
            draggingScroll = true;
            dragScroll(mouseY);
            return true;
        }

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
        List<AbstractWidget> hidden = new ArrayList<>();
        if (!inside) {
            for (AbstractWidget widget : contentWidgets) {
                if (widget.visible) {
                    widget.visible = false;
                    hidden.add(widget);
                }
            }
        }
        boolean handled = super.mouseClicked(click, doubled);
        for (AbstractWidget widget : hidden) {
            widget.visible = true;
        }
        if (handled && inside) {
            ensureFocusedInputVisible(layout());
        }
        return handled;
    }

    private void dragScroll(double y) {
        Layout l = layout();
        float ratio = (float) ((y - l.viewportTop()) / Math.max(1, l.viewportHeight()));
        targetScrollOffset = scrollOffset = clamp(ratio * maxScroll, 0, maxScroll);
        rememberCurrentScroll(); rebuildWidgets();
    }
    @Override public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (draggingScroll) { dragScroll(click.y()); return true; }
        return super.mouseDragged(click, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent click) {
        draggingScroll = false; return super.mouseReleased(click);
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent input) {
        for (DropdownWidget dropdown : dropdownFields.values())
            if (dropdown.isExpanded() && dropdown.keyPressed(input)) return true;
        if (input.key() == 256 && hasOpenSubPage()) { closeSubPage(); return true; }
        return super.keyPressed(input);
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
                rebuildWidgets();
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
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        // Preserve raw input as well as committed values for this connection.
        if (editableConfig != null && canManage) {
            ConfigDraft.value = editableConfig;
            ConfigDraft.raw.clear();
            pendingNumberText.forEach((f, v) -> ConfigDraft.raw.put(f.name(), v));
            pendingStringText.forEach((f, v) -> ConfigDraft.raw.put(f.name(), v));
        }

        closeDropdowns();
        super.onClose();
    }

    @Override
    public void tick() {
        if (manualSaveRequested && ConfigDraft.pendingId == 0) {manualSaveRequested=false;showError(ConfigDraft.message);rebuildWidgets();}
        if(searchRebuild){searchRebuild=false;int cursor=wildcardSearch==null?0:wildcardSearch.getCursorPosition();rebuildWidgets();if(wildcardSearch!=null){setFocused(wildcardSearch);wildcardSearch.moveCursorTo(cursor,false);}}
        if (syncRebuildPending && !hasFocusedTextField() && !hasExpandedDropdown()) rebuildWidgets();

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
        if (ConfigDraft.pendingId != 0) return 40;
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

    private void updateNavigationFeedback(int mouseX,int mouseY) {
        String hovered="";
        for(var child:children())if(child instanceof StyledButtonWidget button && button.visible && (button.variant==ButtonVariant.NAV || button.variant==ButtonVariant.NAV_SELECTED) && button.isMouseOver(mouseX,mouseY)){hovered=button.getMessage().getString();break;}
        long now=System.currentTimeMillis();
        if(!hovered.isEmpty() && !hovered.equals(hoveredNavigation) && now-navigationSoundAt>=90){
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),1.6F,0.12F));navigationSoundAt=now;
        }
        hoveredNavigation=hovered;
    }

    private void buildNavigation(Layout l) {
        navigationEntries.clear();
        int x=l.panelX()+8, w=l.navWidth()-16;
        for(Page page:visiblePages()) {
            var button=addButton(x,0,w,24,page.label,"",b->switchPage(page),page==currentPage?ButtonVariant.NAV_SELECTED:ButtonVariant.NAV,true);
            navigationEntries.add(new NavigationEntry(button,page,null));
            if(page==Page.RULES) for(RulesSubPage sub:RulesSubPage.values()) {
                var child=addButton(x+4,0,w-4,20,key("ui.nav."+sub.name().toLowerCase(java.util.Locale.ROOT)),sub.description,b->openRulesSubPage(sub),currentPage==Page.RULES && rulesSubPage==sub?ButtonVariant.NAV_SELECTED:ButtonVariant.NAV,true);
                navigationEntries.add(new NavigationEntry(child,page,sub));
            }
        }
        updateNavigationLayout(l);
    }

    public float rulesExpansionForTesting() { return rulesExpansion; }

    private void updateNavigationLayout(Layout l) {
        long now=System.nanoTime();
        float elapsed=rulesAnimationFrame==0?0:(now-rulesAnimationFrame)/1_000_000_000F;
        rulesAnimationFrame=now;
        float target=currentPage==Page.RULES?1:0;
        rulesExpansion=com.xiaoming.hunterwildcard.client.ui.DisplayPreferences.get.reducedMotion?target:rulesExpansion+(target-rulesExpansion)*(1-(float)Math.exp(-Math.min(elapsed,.1F)*18));
        if(Math.abs(rulesExpansion-target)<.002F)rulesExpansion=target;
        float y=navigationTop(l)-navScroll;
        for(var entry:navigationEntries) {
            var button=entry.button();
            button.setY(Math.round(y));
            button.visible=y>=navigationTop(l)-button.getHeight() && y+button.getHeight()<=navigationBottom(l)
                    && (entry.sub()==null || rulesExpansion>.05F);
            button.active=entry.sub()==null || (currentPage==Page.RULES && rulesExpansion>.95F);
            button.navigationReveal=entry.sub()==null?1:rulesExpansion;
            y+=entry.sub()==null?30:24*rulesExpansion;
        }
        maxNavScroll=Math.max(0,y+navScroll-navigationBottom(l));
    }

    private void buildGamePage(Layout layout) {
        int x = layout.contentX(), y = pageTop(layout), w = layout.usableContentWidth();
        if (serverSync == null) { CardBuilder c = addCard(layout,x,y,w,key("screen.card.game_status")); c.hint(key("screen.hint.waiting_server_sync")); markContentBottom(layout,c.finish()); return; }
        boolean waiting = serverSync.gameState() == GameState.WAITING;
        if (waiting) {
            y = addTwoColumnCards(layout,x,y,w,MEDIUM_CARD_MAX_WIDTH,key("team.hunters"),c -> lobbyTeamCard(c,PlayerRole.HUNTER),key("team.runners"),c -> lobbyTeamCard(c,PlayerRole.RUNNER));
            CardBuilder summary = addCard(layout,x,y,w,key("screen.card.rules_summary"));
            summary.info(key("team.runners"),runnerWinSummary(serverSync.config()),0xFF78B8FA);
            summary.info(key("team.hunters"),hunterWinSummary(serverSync.config()),0xFFEF7181);
            summary.inlineAction(key("ui.rules.open"),b -> switchPage(Page.RULES),ButtonVariant.LINK,true,100);
            summary.endActions();
            y = summary.finish();
        } else {
            CardBuilder status = addCard(layout,x,y,w,stateName(serverSync.gameState()));
            if (serverSync.gameState() == GameState.PREPARING) status.hint(spec("ui.release_in", formatSeconds(serverSync.phaseRemainingSeconds())));
            if (ClientGameStatus.details != null) {
                var d = ClientGameStatus.details;
                status.hint(d.objective()); status.hint(d.hunterObjective());
                status.info(key("screen.field.my_team"),serverSync.playerRole(),0xFFE8EDF2);
                status.info(key("ui.member.status"),d.ownState(),0xFFF0C76B);
                status.info(key("ui.lives"),d.ownLives()==-2 ? "—" : d.ownLives()<0 ? key("common.infinite") : Integer.toString(d.ownLives()),0xFFE8EDF2);
            }
            status.info(key("screen.field.current_wildcard"),wildcardDisplayName(),0xFFB99AFF);
            status.info(key("screen.field.remaining"),formatSeconds(serverSync.activeWildcardRunning()?serverSync.activeWildcardRemainingSeconds():serverSync.nextWildcardSeconds()),0xFFB99AFF);
            y = status.finish()+CARD_GAP;
            y = addTwoColumnCards(layout,x,y,w,MEDIUM_CARD_MAX_WIDTH,key("team.hunters"),c -> rosterCard(c,PlayerRole.HUNTER),key("team.runners"),c -> rosterCard(c,PlayerRole.RUNNER));
        }
        if (ClientGameStatus.details != null && !ClientGameStatus.details.reason().isBlank()) {
            y = addButtonRow(layout,List.of(new ButtonSpec(key("ui.result"),b->switchPage(Page.RESULT),ButtonVariant.NORMAL,true)),x,y+8,w,1,160);
        }
        markContentBottom(layout,y);
    }

    private void lobbyTeamCard(CardBuilder card,PlayerRole role) {
        boolean current=serverSync.playerRole().equals(role.getTranslationKey());
        card.button(key(current?"screen.team.leave":role==PlayerRole.HUNTER?"screen.team.join_hunter":"screen.team.join_runner"),b->sendTeamAction(current?TeamAction.LEAVE:role==PlayerRole.HUNTER?TeamAction.JOIN_HUNTER:TeamAction.JOIN_RUNNER),current?ButtonVariant.DANGER:role==PlayerRole.HUNTER?ButtonVariant.TEAM_HUNTER:ButtonVariant.TEAM_RUNNER,true,112);
        if(current)card.hint(key("screen.team.current_side"));
        rosterCard(card,role);
    }

    private void rosterCard(CardBuilder card, PlayerRole role) {
        var details = ClientGameStatus.details;
        if (details == null) { card.hint(key("screen.hint.waiting_server_sync")); return; }
        var members = details.members().stream().filter(m -> m.role().equals(role.getTranslationKey())).toList();
        if (members.isEmpty()) card.hint(key("ui.team.empty"));
        for (var member : members) {
            card.hint(tr(member.name()) + "  ·  " + tr(member.state()) + (member.respawnSeconds()>0 ? "  " + formatSeconds(member.respawnSeconds()) : member.lives()>=0 && serverSync.gameState()!=GameState.WAITING ? "  ♥ " + member.lives() : ""));
        }
    }

    private void buildResultPage(Layout layout) {
        CardBuilder c = addCard(layout,layout.contentX(),pageTop(layout),layout.usableContentWidth(),key("ui.result"));
        var d = ClientGameStatus.details;
        if (d == null || d.reason().isBlank()) c.hint(key("ui.result.empty"));
        else {
            c.info(key("ui.winner"),d.winner(),0xFFF0C76B); c.hint(d.reason());
            for (var member:d.resultMembers()) c.hint(tr(member.role())+" · "+tr(member.name()));
            c.button(key("ui.play_again"), b->switchPage(Page.GAME), ButtonVariant.PRIMARY, true,180);
        }
        markContentBottom(layout,c.finish());
    }



    private void toggleStatusHud() {
        ClientGameStatus.toggleStatusHud();
        rebuildWidgets();
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
        if (!isRoundRunning() && canManage) {
            CardBuilder presets=addCard(layout,x,currentY,w,key("ui.presets"));
            presets.buttonGrid(List.of(
                new ButtonSpec(key("ui.preset.classic"),b->applyPreset("CLASSIC"),ButtonVariant.NORMAL,true),
                new ButtonSpec(key("ui.preset.dragon"),b->applyPreset("DRAGON"),ButtonVariant.NORMAL,true),
                new ButtonSpec(key("ui.preset.survive"),b->applyPreset("SURVIVE_TIME"),ButtonVariant.NORMAL,true),
                new ButtonSpec(key("ui.preset.collect"),b->applyPreset("COLLECT_ITEM"),ButtonVariant.NORMAL,true)),w>=360?4:2,112);
            wrappedLabels.add(new WrappedLabel(key("ui.preset.hint"),presets.contentX(),presets.cursorY,26,0xFF9FAAB4,false,presets.contentWidth(),2));
            presets.gap(30);currentY=presets.finish()+CARD_GAP;
        }
        if (isRoundRunning()) {
            CardBuilder note = addCard(layout, x, currentY, w, key("screen.card.live_rules"));
            note.hint(key("screen.hint.live_rules"));
            currentY = note.finish() + CARD_GAP;
        }

        for (RulesSubPage section : List.of(RulesSubPage.VICTORY, RulesSubPage.TIME_BOUNDARY, RulesSubPage.RESPAWN, RulesSubPage.KILL_CREDIT, RulesSubPage.BALANCE)) {
            addContentLabel(layout,section.label,x,currentY+5,0xFFB99AFF,false);
            addContentButton(layout,x+w-52,currentY,52,18,key(canManage?"ui.rules.adjust":"ui.view"),"",b->openRulesSubPage(section),ButtonVariant.LINK,true);
            currentY+=24;
            overviewReadOnly=true;
            try {
                currentY=switch(section) {
                    case VICTORY -> buildVictorySubPage(layout,x,currentY,w);
                    case TIME_BOUNDARY -> buildTimeBoundarySubPage(layout,x,currentY,w);
                    case RESPAWN -> buildRespawnSubPage(layout,x,currentY,w);
                    case KILL_CREDIT -> buildKillCreditSubPage(layout,x,currentY,w);
                    case BALANCE -> buildBalanceSubPage(layout,x,currentY,w);
                };
            } finally {overviewReadOnly=false;}
        }
        CardBuilder wildcards=addCard(layout,x,currentY,w,key("screen.page.wildcard"));
        long enabled=java.util.Arrays.stream(ToggleField.values()).filter(f->getToggle(editableConfig,f)).count();
        wildcards.hint(enabled==0?key("ui.wildcards.none"):spec("ui.wildcards.enabled_count",enabled,ToggleField.values().length));
        if(enabled>0) {
            String interval="RANDOM".equals(editableConfig.wildcardIntervalMode())?formatNumber(NumberField.WILDCARD_INTERVAL_MIN_SECONDS,editableConfig.wildcardIntervalMinSeconds())+"–"+formatNumber(NumberField.WILDCARD_INTERVAL_MAX_SECONDS,editableConfig.wildcardIntervalMaxSeconds()):formatNumber(NumberField.WILDCARD_INTERVAL_SECONDS,editableConfig.wildcardIntervalSeconds());
            String duration="RANDOM".equals(editableConfig.wildcardDurationMode())?formatNumber(NumberField.WILDCARD_DURATION_MIN_SECONDS,editableConfig.wildcardDurationMinSeconds())+"–"+formatNumber(NumberField.WILDCARD_DURATION_MAX_SECONDS,editableConfig.wildcardDurationMaxSeconds()):formatNumber(NumberField.WILDCARD_DURATION_SECONDS,editableConfig.wildcardDurationSeconds());
            wildcards.hint(spec("ui.wildcards.timing_summary",interval,duration));
        }
        wildcards.inlineAction(key(canManage?"ui.rules.adjust":"ui.view"),b->switchPage(Page.WILDCARD),ButtonVariant.LINK,true,52);
        wildcards.endActions();currentY=wildcards.finish();
        markContentBottom(layout, currentY);
    }

    public float navigationProgressForTesting(String title) {
        NavigationAnimation state=navigationAnimations.get(title);return state==null?0:state.hover;
    }

    public String presetDescriptionAt(int mouseX, int mouseY) {
        for (int pass=0;pass<2;pass++) for (var child : children()) if (child instanceof Button button && button.visible && (pass==0 ? button.isMouseOver(mouseX, mouseY) : button.isFocused())) {
            for (String preset : List.of("classic", "dragon", "survive", "collect")) {
                if (button.getMessage().getString().equals(tr(key("ui.preset." + preset)))) return key("ui.preset." + preset + ".description");
            }
        }
        return key("ui.preset.hint");
    }

    private void applyPreset(String type) {
        if (!canEditConfig() || isRoundRunning()) return;
        commitVisibleInputs();
        if(type.equals("CLASSIC")) {
            ModConfig classic=editableConfig.toConfig();
            classic.runnerVictoryType="DRAGON";classic.hunterVictoryType="RUNNERS_OUT";
            classic.runnerRespawnMode="NO_RESPAWN";classic.runnerLives=1;classic.runnerTeamLossMode="ANY_RUNNER_OUT";
            classic.hunterRespawnMode="INFINITE";classic.hunterRespawnSeconds=1;classic.hunterRespawnPenaltySeconds=0;
            classic.preparingSeconds=1;classic.hunterPrepareBoundaryEnabled=false;classic.surviveBorderEnabled=false;
            classic.randomRespawnEnabled=false;classic.runnerDeathNoDrops=false;classic.hunterDeathNoDrops=false;
            classic.hunterDamageMultiplierPercent=100;classic.hunterSpeedPercent=100;classic.runnerSpeedPercent=100;
            classic.piglinPearlBoostEnabled=false;classic.blazeRodChanceEnabled=false;classic.compassUpdateSeconds=1;
            for(ToggleField field:ToggleField.values())classic.enabledWildcards.put(field.id,false);
            classic.validate();editableConfig=ConfigSnapshot.from(classic);ConfigDraft.value=editableConfig;openRulesSubPage(null);return;
        }
        editableConfig=setDropdownValue(editableConfig,DropdownField.RUNNER_VICTORY_TYPE,type);
        editableConfig=setDropdownValue(editableConfig,DropdownField.HUNTER_VICTORY_TYPE,"RUNNERS_OUT");
        editableConfig=setDropdownValue(editableConfig,DropdownField.HUNTER_RESPAWN_MODE,"INFINITE");
        editableConfig=setDropdownValue(editableConfig,DropdownField.RUNNER_RESPAWN_MODE,"LIMITED_LIVES");
        editableConfig=setNumber(editableConfig,NumberField.RUNNER_LIVES,3);
        if(type.equals("SURVIVE_TIME")) {editableConfig=setNumber(editableConfig,NumberField.SURVIVE_TIME_SECONDS,900);editableConfig=setBoolean(editableConfig,BooleanField.SURVIVE_BORDER_ENABLED,true);}
        if(type.equals("COLLECT_ITEM")) {editableConfig=setString(editableConfig,StringField.TARGET_ITEM_ID,"minecraft:diamond");editableConfig=setNumber(editableConfig,NumberField.TARGET_ITEM_COUNT,16);}
        ConfigDraft.value=editableConfig;openRulesSubPage(null);
    }

    private void buildRulesHubCard(CardBuilder card, RulesSubPage subPage) {
        card.titleButtons(List.of(new ButtonSpec(key(canManage ? "screen.button.edit" : "ui.view"), widget -> openRulesSubPage(subPage), ButtonVariant.PRIMARY, true, subPage.description)), 60);
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

    private String readableTime(int seconds) {
        return seconds%60==0?tr(spec("ui.time.minutes",seconds/60)):seconds<60?tr(spec("ui.time.seconds",seconds)):tr(spec("ui.time.minutes_seconds",seconds/60,seconds%60));
    }
    private String itemName(String raw) {
        var id=net.minecraft.resources.Identifier.tryParse(raw);
        return id==null?raw:net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id).getDefaultInstance().getHoverName().getString();
    }
    private String runnerWinSummary(ConfigSnapshot config) {
        RunnerVictoryType type = RunnerVictoryType.fromConfig(config.runnerVictoryType(), RunnerVictoryType.DRAGON);
        String base = tr(type.getTranslationKey());
        return switch (type) {
            case DRAGON -> base;
            case SURVIVE_TIME -> tr(spec("ui.goal.survive", readableTime(config.surviveTimeSeconds())));
            case REACH_LOCATION -> base + " (" + config.targetX() + ", " + config.targetY() + ", " + config.targetZ() + ")";
            case COLLECT_ITEM -> tr(spec("ui.goal.collect", config.targetItemCount(), itemName(config.targetItemId())));
        };
    }

    private String hunterWinSummary(ConfigSnapshot config) {
        HunterVictoryType type = HunterVictoryType.fromConfig(config.hunterVictoryType(), HunterVictoryType.RUNNERS_OUT);
        String base = tr(type.getTranslationKey());
        return type == HunterVictoryType.RUNNER_KILL_COUNT
                ? tr(spec("ui.goal.hunter_kills",config.hunterRunnerKillTarget()))
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

    private int buildTimeBoundarySubPage(Layout layout, int x, int y, int w) {
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
        return bottom;
    }

    private int buildVictorySubPage(Layout layout, int x, int y, int w) {
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
        return bottom;
    }

    private int buildRespawnSubPage(Layout layout, int x, int y, int w) {
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
                        card.info(key("config.dropdown.runner_respawn_mode"), key("screen.respawn.infinite_locked"), 0xFF78B8FA);
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
        return bottom;
    }

    private int buildKillCreditSubPage(Layout layout, int x, int y, int w) {
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
        int bottom = card.finish() + CARD_GAP;
        markContentBottom(layout, bottom);
        return bottom;
    }

    private int buildBalanceSubPage(Layout layout, int x, int y, int w) {
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
        CardBuilder blazeCard = addCard(layout, cardX, bottom, cardWidth, key("screen.card.blaze_drops"));
        blazeCard.booleanField(BooleanField.BLAZE_ROD_CHANCE_ENABLED);
        blazeCard.number(NumberField.BLAZE_ROD_CHANCE_PERCENT, editableConfig.blazeRodChanceEnabled());
        blazeCard.hint(editableConfig.blazeRodChanceEnabled() ? key("screen.hint.blaze_drops") : key("screen.hint.blaze_drops_disabled"));
        bottom = blazeCard.finish() + CARD_GAP;
        CardBuilder locatorCard = addCard(layout, cardX, bottom, cardWidth, key("screen.card.locator_bar"));
        locatorCard.booleanField(BooleanField.LOCATOR_BAR_TEAM_ONLY);
        locatorCard.hint(key("screen.hint.locator_bar"));
        bottom = locatorCard.finish() + CARD_GAP;
        markContentBottom(layout, bottom);
        return bottom;
    }

    private void buildWildcardPage(Layout l) {
        int x=l.contentX(),y=pageTop(l),w=l.usableContentWidth();
        if(editableConfig==null){CardBuilder c=addCard(l,x,y,w,key("screen.card.wildcard_rules"));c.hint(key("screen.hint.waiting_config_sync"));markContentBottom(l,c.finish());return;}
        if(selectedWildcardSettings!=null){markContentBottom(l,addWildcardSettingsCard(l,x,y,w,selectedWildcardSettings));return;}
        addContentLabel(l,key("ui.timing"),x,y+6,0xFFB99AFF,false);
        addWildcardBulkActions(l,x+w-136,y,ToggleField.values());
        y+=26;
        y=addTwoColumnCards(l,x,y,w,MEDIUM_CARD_MAX_WIDTH,
            key("ui.timing.interval"),t->{
                t.dropdown(DropdownField.WILDCARD_INTERVAL_MODE);
                if("RANDOM".equals(editableConfig.wildcardIntervalMode()))t.numberPair(NumberField.WILDCARD_INTERVAL_MIN_SECONDS,NumberField.WILDCARD_INTERVAL_MAX_SECONDS);else t.number(NumberField.WILDCARD_INTERVAL_SECONDS);
            },key("ui.timing.duration"),t->{
                t.dropdown(DropdownField.WILDCARD_DURATION_MODE);
                if("RANDOM".equals(editableConfig.wildcardDurationMode()))t.numberPair(NumberField.WILDCARD_DURATION_MIN_SECONDS,NumberField.WILDCARD_DURATION_MAX_SECONDS);else t.number(NumberField.WILDCARD_DURATION_SECONDS);
            });
        wildcardSearch=new EditBox(font,x,y,w,20,text(key("ui.wildcard.search")));
        wildcardSearch.setHint(text(key("ui.wildcard.search")));wildcardSearch.setMaxLength(80);wildcardSearch.setValue(wildcardQuery);
        wildcardSearch.setResponder(q->{wildcardQuery=q;searchRebuild=true;});addRenderableWidget(wildcardSearch);registerContentWidget(wildcardSearch);y+=28;
        String query=wildcardQuery.toLowerCase(java.util.Locale.ROOT);
        int total=0;
        for(WildcardCategory category:WildcardCategory.values()) {
            List<ToggleField> fields=java.util.Arrays.stream(ToggleField.inCategory(category))
                .filter(f->tr(f.label).toLowerCase(java.util.Locale.ROOT).contains(query)||f.id.contains(query)||tr(f.description).toLowerCase(java.util.Locale.ROOT).contains(query)).toList();
            if(fields.isEmpty())continue;
            total+=fields.size();
            int columns=w>=420?2:1,gap=8,padding=10;
            int cellWidth=(w-padding*2-(columns-1)*gap)/columns;
            int rows=(fields.size()+columns-1)/columns,cardHeight=30+rows*36+8;
            boxes.add(new Box(x,y,w,cardHeight,0xF01C232C,0xFF374351));
            addContentLabel(l,category.label,x+padding,y+9,0xFFB99AFF,false);
            addWildcardBulkActions(l,x+w-padding-136,y+2,ToggleField.inCategory(category));
            for(int i=0;i<fields.size();i++) {
                ToggleField f=fields.get(i);boolean enabled=getToggle(editableConfig,f);
                int tx=x+padding+(i%columns)*(cellWidth+gap),ty=y+28+(i/columns)*36;
                boolean settings=hasWildcardSettings(f),test=canManage&&isDebugPageEnabled();
                int settingsWidth=settings||test?48:0;
                int switchX=tx+cellWidth-48,settingsX=switchX-settingsWidth-5,infoX=settingsX-17;
                int labelWidth=Math.max(24,infoX-(tx+26)-4);
                boxes.add(new Box(tx,ty,cellWidth,32,0x5527323E,0xFF374351));
                icons.add(new Icon(WildcardIcons.iconFor(f.id),tx+5,ty+8));
                wrappedLabels.add(new WrappedLabel(f.label,tx+26,ty+4,24,0xFFE8EDF2,false,labelWidth,2));
                addContentLabel(l,"i",infoX+5,ty+11,0xFF78B8FA,false);
                infoHints.add(new InfoHint(infoX,ty+6,tr(f.description)));
                if(settings||test)addContentButton(l,settingsX,ty+5,settingsWidth,22,key(settings?"screen.button.settings":"ui.wildcard.test_short"),"",b->{if(settings)openWildcardSettings(f);else sendTestWildcard(f);},ButtonVariant.LINK,true);
                addContentButton(l,switchX,ty+5,48,22,key(enabled?"screen.toggle.on_short":"screen.toggle.off_short"),spec("screen.tooltip.wildcard_toggle",f.label,key(enabled?"screen.toggle.enabled":"screen.toggle.disabled")),b->toggleField(f),enabled?ButtonVariant.SWITCH_ON:ButtonVariant.SWITCH_OFF,canEditConfig());
            }
            y+=cardHeight+10;
        }
        if(total==0){addHintText(l,key("ui.search.empty"),x,y,w);y+=24;}
        markContentBottom(l,y);
    }

    private void addWildcardBulkActions(Layout layout,int x,int y,ToggleField[] fields) {
        if(!canManage)return;
        addContentButton(layout,x,y,64,22,key("ui.bulk.on"),"",b->setWildcardGroup(fields,true),ButtonVariant.LINK,canEditConfig());
        addContentButton(layout,x+72,y,64,22,key("ui.bulk.off"),"",b->setWildcardGroup(fields,false),ButtonVariant.LINK,canEditConfig());
    }

    private void setWildcardGroup(ToggleField[] fields,boolean enabled) {
        if(!canEditConfig())return;
        commitVisibleInputs();
        for(ToggleField field:fields)editableConfig=setToggle(editableConfig,field,enabled);
        ConfigDraft.value=editableConfig;
        rebuildWidgets();
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
                        canEditConfig() && editableConfig != null && minecraft != null && minecraft.player != null && minecraft.level != null,
                        key("screen.tooltip.set_current_location")
                )), 1, 132);
            }
            case COLLECT_ITEM -> {
                var itemId = net.minecraft.resources.Identifier.tryParse(editableConfig.targetItemId());
                if(itemId!=null) card.hint(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(itemId).getDefaultInstance().getHoverName().getString());
                if(!overviewReadOnly) card.inlineAction(key("ui.item.search"),b->minecraft.setScreen(new ItemPickerScreen(this,id->{editableConfig=setString(editableConfig,StringField.TARGET_ITEM_ID,id);ConfigDraft.value=editableConfig;pendingStringText.remove(StringField.TARGET_ITEM_ID);ConfigDraft.raw.remove(StringField.TARGET_ITEM_ID.name());})),ButtonVariant.LINK,canEditConfig(),86);
                if(canManage && !overviewReadOnly) card.inlineAction(key("ui.item.advanced"),b->{advancedItemId=!advancedItemId;rebuildWidgets();},ButtonVariant.LINK,true,112);
                card.endActions();
                if(!overviewReadOnly && (advancedItemId || !canManage)) card.string(StringField.TARGET_ITEM_ID);
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

    private void buildDebugPage(Layout l) {
        CardBuilder c=addCard(l,l.contentX(),pageTop(l),l.usableContentWidth(),key("ui.debug.tools"));
        c.hint(key("ui.debug.hint"));
        c.button(key("screen.button.roll_wildcard"),b->sendDebugAction(DebugAction.ROLL_WILDCARD),ButtonVariant.NORMAL,canManage&&serverSync!=null&&serverSync.gameState()==GameState.RUNNING,180);
        c.button(key("screen.button.stop_wildcard"),b->sendDebugAction(DebugAction.STOP_WILDCARD),ButtonVariant.DANGER,canManage&&serverSync!=null&&serverSync.activeWildcardRunning(),180);
        c.button(key("ui.debug.browse"),b->switchPage(Page.WILDCARD),ButtonVariant.PRIMARY,true,180);
        markContentBottom(l,c.finish());
    }

    private void buildFooter(Layout l) {
        int y=l.panelY()+l.panelHeight()-32, x=l.contentX(), w=l.usableContentWidth();
        if (isConfigEditPage() || currentPage==Page.CHANGES) {
            int bw=Math.min(76,Math.max(48,(w-92)/3)),groupWidth=3*bw+12;
            int start=x+w-groupWidth;
            if(canManage) {
                changesLink=addButton(x,y,Math.max(56,start-x-8),24,spec("ui.changes.count",changeCount()),"",b->switchPage(Page.CHANGES),ButtonVariant.LINK,true);
                saveButton=addButton(start,y,bw,24,ConfigDraft.pendingId!=0?key("ui.saving"):key("ui.apply"),"",b->saveConfig(),ButtonVariant.PRIMARY,canSaveConfig());
                addButton(start+bw+6,y,bw,24,key("ui.undo"),"",b->discardChanges(),ButtonVariant.NORMAL,ConfigDraft.pendingId==0);
            }
            addButton(x+w-bw,y,bw,24,key("screen.button.close"),"",b->onClose(),ButtonVariant.NORMAL,true);
        } else if (currentPage==Page.GAME && serverSync!=null) {
            int bw=Math.min(96,Math.max(50,(w-12)/3));
            x+=w-(3*bw+12);
            boolean waiting=serverSync.gameState()==GameState.WAITING;
            if(canManage) addButton(x,y,bw,24,waiting?key("screen.button.start_game"):key(confirmStop?"ui.stop.confirm":"screen.button.stop_game"),startGameTooltip(waiting),b->{
                if(waiting) sendGameAction(GameAction.START_GAME);
                else if(confirmStop) {sendGameAction(GameAction.STOP_GAME);confirmStop=false;}
                else {confirmStop=true;rebuildWidgets();}
            },waiting?ButtonVariant.PRIMARY:ButtonVariant.DANGER,waiting?startGameTooltip(true).isBlank():true);
            addButton(x+bw+6,y,bw,24,key("ui.display"),"",b->minecraft.setScreen(new DisplaySettingsScreen(this)),ButtonVariant.NORMAL,true);
            addButton(x+2*(bw+6),y,bw,24,key("screen.button.close"),"",b->onClose(),ButtonVariant.NORMAL,true);
        } else addButton(x+w-86,y,86,24,key("screen.button.close"),"",b->onClose(),ButtonVariant.NORMAL,true);
    }

    private List<String> changes() {
        List<String> list=new ArrayList<>();
        if(editableConfig==null || ConfigDraft.base==null) return list;
        ConfigSnapshot base=ConfigDraft.base;
        for(NumberField f:NumberField.values()) if(getNumber(base,f)!=getNumber(editableConfig,f) || pendingNumberText.containsKey(f))
            list.add(tr(f.label)+": "+formatNumber(f,getNumber(base,f))+" → "+pendingNumberText.getOrDefault(f,formatNumber(f,getNumber(editableConfig,f)))+ (isRoundRunning()?tr(key(f.live?"ui.live":"ui.locked")):""));
        for(StringField f:StringField.values()) if(!getString(base,f).equals(getString(editableConfig,f)) || pendingStringText.containsKey(f)) list.add(tr(f.label)+": "+getString(base,f)+" → "+pendingStringText.getOrDefault(f,getString(editableConfig,f)));
        for(DropdownField f:DropdownField.values()) if(!getDropdownValue(base,f).equals(getDropdownValue(editableConfig,f))) list.add(tr(f.label)+": "+dropdownLabel(f,getDropdownValue(base,f))+" → "+dropdownLabel(f,getDropdownValue(editableConfig,f)));
        for(BooleanField f:BooleanField.values()) if(getBoolean(base,f)!=getBoolean(editableConfig,f)) list.add(tr(f.label)+": "+tr(key(getBoolean(editableConfig,f)?"screen.toggle.enabled":"screen.toggle.disabled")));
        for(ToggleField f:ToggleField.values()) if(getToggle(base,f)!=getToggle(editableConfig,f)) list.add(tr(f.label)+": "+tr(key(getToggle(editableConfig,f)?"screen.toggle.enabled":"screen.toggle.disabled")));
        return list;
    }
    private String dropdownLabel(DropdownField f,String value) { return f.options.stream().filter(o->o.value().equals(value)).map(o->tr(o.displayName())).findFirst().orElse(value); }
    private int changeCount() { return changes().size(); }
    private void discardChanges() {
        toastMessage="";statusMessage="";ConfigDraft.discard(); editableConfig=ConfigDraft.value; pendingNumberText.clear(); pendingStringText.clear(); rebuildWidgets();
    }
    private void buildChangesPage(Layout l) {
        CardBuilder c=addCard(l,l.contentX(),pageTop(l),l.usableContentWidth(),spec("ui.changes.count",changeCount()));
        if(ConfigDraft.pendingId!=0) c.hint(key("ui.saving"));
        if(ConfigDraft.conflict()) c.hint(key("ui.save.conflict"));
        if(!ConfigDraft.message.isBlank()) c.hint(ConfigDraft.message);
        c.hint(key("ui.draft.explanation"));
        for(String change:changes()) c.hint(change);
        if(changes().isEmpty()) c.hint(key("screen.info.no_unsaved_changes"));
        markContentBottom(l,c.finish());
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
            int rowHeight = Math.max(leftCard.height(), rightCard.height());
            int leftBottom = leftCard.finish(rowHeight);
            int rightBottom = rightCard.finish(rowHeight);
            return Math.max(leftBottom, rightBottom) + CARD_GAP;
        }

        int cardWidth = width;
        int cardX = x + Math.max(0, (width - cardWidth) / 2);
        CardBuilder leftCard = addCard(layout, cardX, y, cardWidth, leftTitle);
        leftBody.build(leftCard);
        int nextY = leftCard.finish() + CARD_GAP;
        CardBuilder rightCard = addCard(layout, cardX, nextY, cardWidth, rightTitle);
        rightBody.build(rightCard);
        return rightCard.finish() + CARD_GAP;
    }



    private int addWildcardSettingsCard(Layout layout, int x, int y, int width, ToggleField field) {
        int cardWidth = Math.min(width, MEDIUM_CARD_MAX_WIDTH);
        int cardX = x;
        CardBuilder card = addCard(layout, cardX, y, cardWidth, key("ui.wildcard.parameters"));
        switch (field) {
            case HUNTER_RADAR -> {
                card.number(NumberField.HUNTER_RADAR_WARNING_DISTANCE);
            }
            case SPACE_SHIFT -> {
                card.number(NumberField.SPACE_SHIFT_INTERVAL_SECONDS);
            }
            case SUPPLY_DROP -> {
                card.number(NumberField.SUPPLY_DROP_INTERVAL_SECONDS);
                card.hint(key("screen.hint.supply_drop_settings"));
            }
            case BLOCK_DECAY -> {
                card.number(NumberField.BLOCK_DECAY_SECONDS);
            }
            case PEARL_FRENZY -> {
                card.number(NumberField.PEARL_FRENZY_MAX_PEARLS);
                card.number(NumberField.PEARL_FRENZY_INTERVAL_SECONDS);
            }
            case WIND_CHARGE_BRAWL -> {
                card.number(NumberField.WIND_CHARGE_BRAWL_INTERVAL_SECONDS);
                card.number(NumberField.WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT);
            }
            case BACKROOMS -> {
                card.number(NumberField.BACKROOMS_DURATION_SECONDS);
                card.hint(key("screen.hint.backrooms_settings"));
            }
            default -> card.hint(key("screen.hint.no_wildcard_settings"));
        }
        if (canManage && isDebugPageEnabled()) {
            card.inlineAction(key("screen.card.test_wildcard"), b -> sendTestWildcard(field), ButtonVariant.LINK, getToggle(editableConfig, field), 140);
            card.endActions();
        }
        return card.finish() + CARD_GAP;
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
            boxes.add(new Box(x, y, width, height, 0xF01C232C, 0xFF27323E));
        }
    }

    private void drawCardBorder(Layout layout, int x, int y, int width, int height) {
        drawCardBackground(layout, x, y, width, height);
    }

    private int teamAccent(String title) {
        return title.equals(key("team.hunters"))?0xFFEF7181:title.equals(key("team.runners"))?0xFF78B8FA:0xFFFFFFFF;
    }
    private void addCardTitle(Layout layout, String title, int x, int y) {
        addContentLabel(layout, title, x, y, teamAccent(title), true);
    }

    private void addHintText(Layout layout, String text, int x, int y, int width) {
        int height = hintHeight(text, width);
        if (isVisibleInContentPartial(layout, y, height)) {
            wrappedLabels.add(new WrappedLabel(text, x, y, height, 0xFF9FAAB4, false, width, HINT_MAX_LINES));
        }
    }

    private int hintHeight(String text, int width) {
        int lines = wrapLabelText(tr(text), width, HINT_MAX_LINES).size();
        return lines * font.lineHeight + Math.max(0, lines - 1) * 2;
    }

    private int addInfoRow(Layout layout, String label, String value, int x, int y, int width, int valueColor) {
        String caption = tr(spec("screen.label_colon", tr(label)));
        int labelWidth = font.width(caption);
        int valueWidth = font.width(tr(value));
        boolean stacked = labelWidth + valueWidth + 12 > width;
        int labelY = y + Math.max(3, (ROW_HEIGHT - font.lineHeight) / 2);
        int valueX = stacked ? x : x + Math.max(labelWidth + 12, width - valueWidth);
        int valueY = stacked ? labelY + font.lineHeight + 5 : labelY;
        int rowHeight = stacked ? ROW_HEIGHT + font.lineHeight + 5 : ROW_HEIGHT;
        if (isVisibleInContentPartial(layout, y, rowHeight)) {
            labels.add(new Label(caption, x, labelY, 0xFFC9D4DE, false, stacked ? width : valueX - x - 8));
            labels.add(new Label(value, valueX, valueY, valueColor, false, width - (valueX - x)));
        }
        return y + rowHeight + ROW_GAP;
    }

    private int addInputRow(Layout layout, NumberField field, int x, int y, int width) {
        return addInputRow(layout, field, x, y, width, canManage);
    }

    private int addInputRow(Layout layout, NumberField field, int x, int y, int width, boolean editable) {
        if (!canManage || overviewReadOnly) return addInfoRow(layout,field.label,(field.name().endsWith("SECONDS")?readableTime(getNumber(editableConfig,field)):formatNumber(field,getNumber(editableConfig,field))+" "+tr(numberUnit(field))),x,y,width,0xFFE8EDF2);
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
        if (!canManage || overviewReadOnly) return addInfoRow(layout,field.label,getString(editableConfig,field),x,y,width,0xFFE8EDF2);
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
        return font.width(tr(spec("screen.label_colon", labelKey))) <= availableWidth;
    }

    private void addStackedLabel(Layout layout, String labelKey, int x, int y, int width) {
        addContentLabel(layout, spec("screen.label_colon", labelKey), x, y + 2, 0xFFC9D4DE, false);
    }

    private int numberUnitSpace(NumberField field) {
        if (field.unit.isBlank()) {
            return 0;
        }
        int unitTextWidth = Math.min(UNIT_WIDTH, Math.max(18, font.width(tr(numberUnit(field)))));
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
        int labelY = y + Math.max(3, (ROW_HEIGHT - font.lineHeight) / 2);
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
        if (!canManage || overviewReadOnly) return addInfoRow(layout,field.label,dropdownLabel(field,getDropdownValue(editableConfig,field)),x,y,width,0xFFE8EDF2);
        int dropdownWidth = dropdownWidth(width);
        int dropdownX = x + width - dropdownWidth;
        if (labelFits(field.label, dropdownX - x - 8)) {
            int labelY = y + Math.max(3, (ROW_HEIGHT - font.lineHeight) / 2);
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
        if (!canManage || overviewReadOnly) return addInfoRow(layout,field.label,key(getBoolean(editableConfig,field)?"screen.toggle.enabled":"screen.toggle.disabled"),x,y,width,0xFFE8EDF2);
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
        int labelY = y + Math.max(3, (fieldHeight - font.lineHeight) / 2);
        if (showLabel) {
            labels.add(new Label(spec("screen.label_colon", field.label), x, labelY, 0xFFC9D4DE, false, formLabelWidth(width, fieldWidth, unitSpace)));
        }
        if (!field.unit.isBlank()) {
            labels.add(new Label(numberUnit(field), fieldX + fieldWidth + 8, labelY, fieldEditable ? 0xFFC9D4DE : 0xFF7D8790));
        }

        registerNumberField(field, fieldX, y, fieldWidth, fieldHeight, fieldEditable);
    }

    /** Creates the widget for a number field, seeding it with any uncommitted text the user typed earlier. */
    private void registerNumberField(NumberField field, int x, int y, int width, int height, boolean editable) {
        EditBox textField = new EditBox(font, x, y, width, height, text(field.label));
        textField.setMaxLength(18);
        String pending = editable ? pendingNumberText.get(field) : null;
        textField.setValue(pending != null ? pending : formatNumber(field, getNumber(editableConfig, field)));
        textField.setEditable(editable);
        textField.active = editable;
        if (!editable) {
            pendingNumberText.remove(field);
        }
        textField.setResponder(value -> onNumberTextChanged(field, value));
        numberFields.put(field, textField);
        addRenderableWidget(textField);
    }

    private void onNumberTextChanged(NumberField field, String value) {
        if (editableConfig == null || value.trim().equals(formatNumber(field, getNumber(editableConfig, field)))) {
            pendingNumberText.remove(field);
        } else {
            pendingNumberText.put(field, value);
        }
        if(pendingNumberText.containsKey(field))ConfigDraft.raw.put(field.name(),value);else ConfigDraft.raw.remove(field.name());
    }

    private void onStringTextChanged(StringField field, String value) {
        if (editableConfig == null || value.trim().equals(getString(editableConfig, field))) {
            pendingStringText.remove(field);
        } else {
            pendingStringText.put(field, value);
        }
        if(pendingStringText.containsKey(field))ConfigDraft.raw.put(field.name(),value);else ConfigDraft.raw.remove(field.name());
    }

    private int addCoordinateInput(Layout layout, String axis, NumberField field, int x, int y, int axisWidth, int fieldWidth, int fieldHeight) {
        int labelY = y + Math.max(3, (fieldHeight - font.lineHeight) / 2);
        labels.add(new Label(axis, x, labelY, 0xFF9FAAB4, false, axisWidth));

        int fieldX = x + axisWidth + 2;
        registerNumberField(field, fieldX, y, fieldWidth, fieldHeight, canEditField(field));
        return fieldX + fieldWidth;
    }

    private void addStringField(StringField field, int x, int y, int width, int fieldHeight, boolean showLabel) {
        int fieldWidth = showLabel ? textFieldWidth(width) : Math.min(width, 220);
        int fieldX = x + width - fieldWidth;
        int labelY = y + Math.max(3, (fieldHeight - font.lineHeight) / 2);
        if (showLabel) {
            labels.add(new Label(spec("screen.label_colon", field.label), x, labelY, 0xFFC9D4DE, false, Math.max(10, fieldX - x - 8)));
        }

        EditBox textField = new EditBox(font, fieldX, y, fieldWidth, fieldHeight, text(field.label));
        textField.setMaxLength(field.maxLength);
        boolean editable = canEditField(field);
        String pending = editable ? pendingStringText.get(field) : null;
        textField.setValue(pending != null ? pending : getString(editableConfig, field));
        textField.setEditable(editable);
        textField.active = editable;
        if (!editable) {
            pendingStringText.remove(field);
        }
        textField.setResponder(value -> onStringTextChanged(field, value));
        stringFields.put(field, textField);
        addRenderableWidget(textField);
    }

    private void addDropdownField(DropdownField field, int x, int y, int width, int height, boolean openUp, boolean enabled) {
        boolean editable = enabled && canEditField(field);
        DropdownWidget dropdown = new DropdownWidget(
                font,
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
        addRenderableWidget(dropdown);
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



    private StyledButtonWidget addBooleanField(BooleanField field, int x, int y, int width, int height) {
        boolean enabled = getBoolean(editableConfig, field);
        int buttonWidth = Math.min(48, width);
        int buttonX = x + width - buttonWidth;
        int labelY = y + Math.max(3, (height - font.lineHeight) / 2);
        labels.add(new Label(spec("screen.label_colon", field.label), x, labelY, 0xFFC9D4DE, false, Math.max(10, buttonX - x - 8)));
        return addButton(
                buttonX,
                y,
                buttonWidth,
                height,
                enabled ? key("screen.toggle.on_short") : key("screen.toggle.off_short"),
                "",
                widget -> toggleBooleanField(field),
                enabled ? ButtonVariant.SWITCH_ON : ButtonVariant.SWITCH_OFF,
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
        if (isVisibleInContentPartial(layout, y, font.lineHeight)) {
            labels.add(new Label(text, x, y, color, shadow));
        }
    }

    private void renderWrappedLabel(GuiGraphicsExtractor context, Layout layout, WrappedLabel label) {
        int maxWidth = label.maxWidth(layout);
        List<String> lines = wrapLabelText(tr(label.text()), maxWidth, label.maxLines());
        int textHeight = lines.size() * font.lineHeight + Math.max(0, lines.size() - 1);
        int startY = label.y() + Math.max(0, (label.height() - textHeight) / 2);
        for (int i = 0; i < lines.size(); i++) {
            context.text(font, Component.literal(lines.get(i)), label.x(), startY + i * (font.lineHeight + 1), label.color(), label.shadow());
        }
    }

    private List<String> wrapLabelText(String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            String line = font.plainSubstrByWidth(remaining, maxWidth);
            if (line.isBlank()) {
                line = remaining.substring(0, 1);
            }
            remaining = remaining.substring(line.length()).trim();
            if (!remaining.isEmpty() && lines.size() == maxLines - 1) {
                line = font.plainSubstrByWidth(line + "...", maxWidth);
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



    private void addContentBooleanField(Layout layout, BooleanField field, int x, int y, int width, int height) {
        registerContentWidget(addBooleanField(field, x, buttonY(y), width, height));
    }

    private void addContentButton(Layout layout, int x, int y, int width, int height, String title, String description, Button.OnPress action, ButtonVariant variant, boolean enabled) {
        registerContentWidget(addButton(x, y, width, height, title, description, action, variant, enabled));
    }

    private StyledButtonWidget addButton(int x, int y, int width, int height, String title, String description, Button.OnPress action, ButtonVariant variant, boolean enabled) {
        String tooltip = description == null ? "" : description;
        if (!enabled && tooltip.isBlank() && isConfigEditPage()) {
            tooltip = configEditDeniedMessage();
        }

        StyledButtonWidget button = new StyledButtonWidget(x, y, width, height, title, tooltip, action, variant);
        button.active = enabled;
        addRenderableWidget(button);
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

    private boolean intersectsViewport(Layout layout, AbstractWidget widget) {
        return widget.getY() + widget.getHeight() > layout.viewportTop() && widget.getY() < layout.viewportBottom();
    }

    private void registerContentWidget(AbstractWidget widget) {
        if (widget != null) {
            contentWidgets.add(widget);
        }
    }

    private void updateContentWidgetVisibility(Layout layout) {
        for (AbstractWidget widget : contentWidgets) {
            widget.visible = intersectsViewport(layout, widget);
        }
    }

    /** Moves every content element by {@code delta} pixels vertically (scrolling without rebuilding the page). */
    private void shiftContent(int delta) {
        infoHints.replaceAll(hint->new InfoHint(hint.x,hint.y+delta,hint.tooltip));
        labels.replaceAll(label -> new Label(label.text(), label.x(), label.y() + delta, label.color(), label.shadow(), label.width()));
        wrappedLabels.replaceAll(label -> new WrappedLabel(label.text(), label.x(), label.y() + delta, label.height(), label.color(), label.shadow(), label.width(), label.maxLines()));
        boxes.replaceAll(box -> new Box(box.x(), box.y() + delta, box.width(), box.height(), box.color(), box.borderColor()));
        icons.replaceAll(icon -> new Icon(icon.stack(), icon.x(), icon.y() + delta));
        for (AbstractWidget widget : contentWidgets) {
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

    private void renderScrollBar(GuiGraphicsExtractor context, Layout layout) {
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
        float totalHeight = visiblePages().size()*30-6 + RulesSubPage.values().length*24*rulesExpansion;
        maxNavScroll = Math.max(0.0F, totalHeight - navigationHeight(layout));
        navScroll = clamp(navScroll, 0.0F, maxNavScroll);
    }

    private void renderNavigationScrollBar(GuiGraphicsExtractor context, Layout layout) {
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

    private void renderDropdownOverlays(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        for (DropdownWidget dropdown : dropdownFields.values()) {
            dropdown.renderOverlay(context, mouseX, mouseY, delta);
        }
    }

    private void renderHoverTooltip(GuiGraphicsExtractor context) {
        if (!hoverTooltip.isBlank()) {
            int tooltipWidth = Math.min(320, Math.max(40, width - 24));
            List<Component> lines = wrapTooltipText(tr(hoverTooltip), tooltipWidth);
            context.setComponentTooltipForNextFrame(font, lines, hoverTooltipX, hoverTooltipY);
        }
    }

    public List<Component> wrapTooltipText(String text, int maxWidth) {
        List<Component> lines = new ArrayList<>();
        for(String paragraph:(text==null?"":text).split("\\R",-1)) {
            String remaining=paragraph.trim();
            if(remaining.isEmpty()){lines.add(Component.empty());continue;}
            while(!remaining.isEmpty()) {
                String line=font.plainSubstrByWidth(remaining,Math.max(1,maxWidth));
                if(line.isEmpty())line=remaining.substring(0,Character.charCount(remaining.codePointAt(0)));
                lines.add(Component.literal(line));remaining=remaining.substring(line.length()).stripLeading();
            }
        }
        return lines.isEmpty() ? List.of(Component.empty()) : lines;
    }

    private void setHoverTooltip(String tooltip, int mouseX, int mouseY) {
        if (tooltip == null || tooltip.isBlank()) {
            return;
        }

        hoverTooltip = tooltip;
        hoverTooltipX = mouseX;
        hoverTooltipY = mouseY;
    }

    private void renderToast(GuiGraphicsExtractor context, Layout layout, float delta) {
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
        int toastWidth = Math.min(maxWidth, font.width(text) + 18);
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
        context.text(font, Component.literal(text), toastX + 9, toastY + 7, withAlpha(toastKind.color, alpha), true);
    }

    private float smoothStep(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }

    private void closeDropdowns() {
        for (DropdownWidget dropdown : dropdownFields.values()) {
            dropdown.close();
        }
        // A click still needs the current widget positions and identities to assign focus.
        // Apply deferred server refreshes in tick, after input dispatch has completed.
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
        rebuildWidgets();
    }

    private void switchPage(Page page) {
        if (page == currentPage) {
            if(page==Page.RULES && rulesSubPage!=null) closeSubPage();
            return;
        }

        // Never blocked: invalid text is reverted (with a toast) and the switch proceeds.
        commitVisibleInputs();
        closeDropdowns();
        rememberCurrentScroll();
        currentPage = page;
        if(page==Page.RULES) rulesSubPage=null;
        if (page != Page.WILDCARD) {
            selectedWildcardSettings = null;
        }
        restorePageScroll(scrollKey());
        rebuildWidgets();
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
        rebuildWidgets();
    }





    private void openWildcardSettings(ToggleField field) {
        if (editableConfig == null) {
            return;
        }

        commitVisibleInputs();
        closeDropdowns();
        rememberCurrentScroll();
        selectedWildcardSettings = field;
        restorePageScroll(scrollKey());
        rebuildWidgets();
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
        rebuildWidgets();
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
        rebuildWidgets();
    }

    private void setTargetToCurrentLocation() {
        if (!beginConfigEdit()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            showError(key("screen.error.no_world_for_location"));
            return;
        }

        BlockPos pos = client.player.blockPosition();
        ModConfig copy = editableConfig.toConfig();
        copy.targetDimension = client.level.dimension().identifier().toString();
        copy.targetX = pos.getX();
        copy.targetY = pos.getY();
        copy.targetZ = pos.getZ();
        copy.validate();
        editableConfig = ConfigSnapshot.from(copy);
        showToast(key("screen.toast.location_set"), StatusKind.INFO);
        rebuildWidgets();
    }

    /** Commits valid edits together; invalid raw values remain available for correction. */
    private void commitVisibleInputs() {
        if (editableConfig == null) {
            pendingNumberText.clear();
            pendingStringText.clear();
            return;
        }

        ConfigSnapshot updated = editableConfig;
        java.util.Map<NumberField,Integer> numbers = new java.util.EnumMap<>(NumberField.class);
        java.util.Set<NumberField> invalidRanges = java.util.EnumSet.noneOf(NumberField.class);
        for (NumberField f : pendingNumberText.keySet()) if (!validRange(f)) invalidRanges.add(f);
        String firstProblem = null;
        for (Map.Entry<NumberField, String> entry : new ArrayList<>(pendingNumberText.entrySet())) {
            NumberField field = entry.getKey();
            String raw = entry.getValue().trim();
            int value;
            try {
                value = parseNumber(field, raw);
            } catch (NumberFormatException exception) {
                if (firstProblem == null) {
                    firstProblem = spec("ui.invalid_field", field.label);
                }
                continue;
            }

            if (value < field.minValue) continue;
            if (value > maxNumber(field)) continue;
            if (invalidRanges.contains(field)) continue;
            numbers.put(field,value);
            pendingNumberText.remove(field);
            ConfigDraft.raw.remove(field.name());
        }

        updated = setNumbers(updated,numbers);
        for (Map.Entry<StringField, String> entry : new ArrayList<>(pendingStringText.entrySet())) {
            String value = entry.getValue().trim();
            if (value.isBlank()) {
                if (firstProblem == null) {
                    firstProblem = spec("screen.error.required", entry.getKey().label);
                }
                continue;
            }
            if (!validItem(value)) continue;
            updated = setString(updated, entry.getKey(), value);
            pendingStringText.remove(entry.getKey());
            ConfigDraft.raw.remove(entry.getKey().name());
        }

        editableConfig = updated;
        ConfigDraft.value = updated;
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
        for (Map.Entry<NumberField, EditBox> entry : numberFields.entrySet()) {
            if (pendingNumberText.containsKey(entry.getKey())) continue;
            String expected = formatNumber(entry.getKey(), getNumber(editableConfig, entry.getKey()));
            if (!entry.getValue().getValue().equals(expected)) {
                entry.getValue().setValue(expected);
            }
        }
        for (Map.Entry<StringField, EditBox> entry : stringFields.entrySet()) {
            if (pendingStringText.containsKey(entry.getKey())) continue;
            String expected = getString(editableConfig, entry.getKey());
            if (!entry.getValue().getValue().equals(expected)) {
                entry.getValue().setValue(expected);
            }
        }
    }

    private String numberHelp(NumberField field) {
        if (field.name().endsWith("SECONDS")) return key("ui.number_help.time");
        if (multiplier(field)) return key("ui.number_help.multiplier");
        return tr(field.label) + " · " + tr(spec("ui.number_range", formatNumber(field, field.minValue), formatNumber(field, maxNumber(field)))) + " " + tr(numberUnit(field));
    }

    private boolean validRange(NumberField field) {
        NumberField min, max;
        switch (field) {
            case WILDCARD_INTERVAL_MIN_SECONDS, WILDCARD_INTERVAL_MAX_SECONDS -> { min=NumberField.WILDCARD_INTERVAL_MIN_SECONDS; max=NumberField.WILDCARD_INTERVAL_MAX_SECONDS; }
            case WILDCARD_DURATION_MIN_SECONDS, WILDCARD_DURATION_MAX_SECONDS -> { min=NumberField.WILDCARD_DURATION_MIN_SECONDS; max=NumberField.WILDCARD_DURATION_MAX_SECONDS; }
            default -> { return true; }
        }
        // A new minimum may extend the previous range; setNumbers raises the unchanged maximum
        // on commit. When both ends were explicitly edited, retain both for correction.
        try { return parseNumber(min,pendingNumberText.getOrDefault(min,formatNumber(min,getNumber(editableConfig,min)))) <= parseNumber(max,pendingNumberText.getOrDefault(max,formatNumber(max,getNumber(editableConfig,max))))
                || (pendingNumberText.containsKey(min) && !pendingNumberText.containsKey(max)); }
        catch (NumberFormatException e) { return false; }
    }

    private boolean validateInputs() {
        for (var e : pendingNumberText.entrySet()) {
            if (!validNumber(e.getKey(), e.getValue())) { showError(spec("ui.invalid_field", e.getKey().label)); return false; }
            if (!validRange(e.getKey())) { showError(key("ui.time_range.invalid")); return false; }
        }
        for (var e : pendingStringText.entrySet()) if (!validItem(e.getValue())) { showError(key("ui.invalid_item")); return false; }
        return true;
    }
    private void renderInputErrors(GuiGraphicsExtractor context) {
        for (var e : numberFields.entrySet()) {
            boolean invalid = !validNumber(e.getKey(), e.getValue().getValue()) || !validRange(e.getKey());
            e.getValue().setTextColor(invalid ? 0xFFEF7181 : 0xFFE8EDF2);
            String help = !validRange(e.getKey()) ? key("ui.time_range.invalid")
                    : invalid ? spec("ui.number_range", formatNumber(e.getKey(), e.getKey().minValue), formatNumber(e.getKey(), maxNumber(e.getKey())))
                    : e.getKey()==NumberField.WILDCARD_DURATION_MIN_SECONDS || e.getKey()==NumberField.WILDCARD_INTERVAL_MIN_SECONDS
                        ? key("ui.time_range.minimum_help") : numberHelp(e.getKey());
            e.getValue().setTooltip(net.minecraft.client.gui.components.Tooltip.create(text(help)));
            if (invalid && e.getValue().visible) context.fill(e.getValue().getX(), e.getValue().getY() + e.getValue().getHeight(), e.getValue().getX() + e.getValue().getWidth(), e.getValue().getY() + e.getValue().getHeight() + 1, 0xFFEF7181);
        }
        for (var e : stringFields.entrySet()) {
            boolean valid = validItem(e.getValue().getValue());
            e.getValue().setTextColor(valid ? 0xFFE8EDF2 : 0xFFEF7181);
            e.getValue().setTooltip(valid ? null : net.minecraft.client.gui.components.Tooltip.create(text(key("ui.invalid_item"))));
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

        if (!validateInputs()) return;
        commitVisibleInputs();
        if (!hasUnsavedChanges() && !ConfigDraft.failed) {
            showInfo(key("screen.info.no_unsaved_changes"));
            return;
        }

        ConfigDraft.value = editableConfig;
        ConfigDraft.raw.clear();
        manualSaveRequested = true;
        sendPayload(new HunterWildcardPackets.UpdateConfigPayload(editableConfig, ConfigDraft.base, ConfigDraft.submit(editableConfig)), HunterWildcardPackets.C2S_UPDATE_CONFIG, key("screen.toast.save_submitted"));
    }

    private void restoreDefaultConfig() {
        if (!canEditConfig()) {
            showError(configEditDeniedMessage());
            return;
        }

        ModConfig defaults = new ModConfig(); defaults.validate();
        ConfigSnapshot d=ConfigSnapshot.from(defaults);
        for(NumberField f:new ArrayList<>(numberFields.keySet())) if(canEditField(f)) editableConfig=setNumber(editableConfig,f,getNumber(d,f));
        for(StringField f:new ArrayList<>(stringFields.keySet())) if(canEditField(f)) editableConfig=setString(editableConfig,f,getString(d,f));
        for(DropdownField f:new ArrayList<>(dropdownFields.keySet())) if(canEditField(f)) editableConfig=setDropdownValue(editableConfig,f,getDropdownValue(d,f));
        // Boolean scope follows the active rule subsection; unrelated settings stay untouched.
        for(BooleanField f:BooleanField.values()) if(canEditField(f) && booleanOnPage(f)) editableConfig=setBoolean(editableConfig,f,getBoolean(d,f));
        for (NumberField f : numberFields.keySet()) { pendingNumberText.remove(f); ConfigDraft.raw.remove(f.name()); }
        for (StringField f : stringFields.keySet()) { pendingStringText.remove(f); ConfigDraft.raw.remove(f.name()); }
        ConfigDraft.value=editableConfig;
        manualReloadRequested = false;
        manualSaveRequested = false;
        showToast(key("screen.toast.default_restored"), StatusKind.INFO);
        rebuildWidgets();
    }

    private boolean booleanOnPage(BooleanField f) {
        if(currentPage!=Page.RULES) return false;
        if(rulesSubPage==null) return true;
        return switch(rulesSubPage) {
            case TIME_BOUNDARY -> f==BooleanField.HUNTER_PREPARE_BOUNDARY_ENABLED;
            case VICTORY -> f==BooleanField.SURVIVE_BORDER_ENABLED;
            case RESPAWN -> f==BooleanField.RANDOM_RESPAWN_ENABLED || f==BooleanField.RUNNER_DEATH_NO_DROPS || f==BooleanField.HUNTER_DEATH_NO_DROPS;
            case KILL_CREDIT -> f==BooleanField.ENVIRONMENT_KILLS_ENABLED;
            case BALANCE -> f==BooleanField.PIGLIN_PEARL_BOOST_ENABLED || f==BooleanField.BLAZE_ROD_CHANCE_ENABLED || f==BooleanField.LOCATOR_BAR_TEAM_ONLY;
        };
    }

    private void reloadConfig() {
        if (!canEditConfig()) {
            showError(configEditDeniedMessage());
            return;
        }

        /* draft lifecycle handled by ConfigDraft */
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

    private void sendPayload(CustomPacketPayload payload, CustomPacketPayload.Type<?> id, String successMessage) {
        sendPayload(payload, id, successMessage, true);
    }

    private void sendPayload(CustomPacketPayload payload, CustomPacketPayload.Type<?> id, String successMessage, boolean updateMessage) {
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

    private boolean canSend(CustomPacketPayload.Type<?> id) {
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
        boolean first = serverSync == null;
        boolean changed = first || !payload.config().equals(serverSync.config())
                || payload.canManage() != serverSync.canManage()
                || payload.debugPageEnabled() != serverSync.debugPageEnabled()
                || payload.gameState() != serverSync.gameState()
                || (isRealtimeStatusPage() && (!payload.equals(serverSync)
                    || !java.util.Objects.equals(displayedDetails, ClientGameStatus.details)));
        serverSync = payload;
        canManage = payload.canManage();
        ensureVisiblePage();
        if (!canManage) {
            editableConfig = payload.config();
            pendingNumberText.clear(); pendingStringText.clear();
        } else {
            if (editableConfig == null || (!hasUnsavedChanges() && !hasVisibleInputChanges() && !hasFocusedTextField()))
                editableConfig = ConfigDraft.value == null ? payload.config() : ConfigDraft.value;
        }
        if (first && !ConfigDraft.message.isBlank()) setFooterStatus(ConfigDraft.message, ConfigDraft.failed ? StatusKind.ERROR : StatusKind.SUCCESS);
        else if (first) setFooterStatus("", StatusKind.INFO);
        hasSyncedOnce = true;
        manualSaveRequested = ConfigDraft.pendingId != 0;
        if (!changed) return;
        if (hasExpandedDropdown() || hasFocusedTextField()) syncRebuildPending = true;
        else rebuildWidgets();
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
        return canManage && serverSync != null && ConfigDraft.pendingId == 0;
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
        return currentPage == Page.RULES || currentPage == Page.WILDCARD || currentPage == Page.CHANGES;
    }

    private boolean isHunterKillCountMode() {
        return editableConfig != null
                && HunterVictoryType.fromConfig(editableConfig.hunterVictoryType(), HunterVictoryType.RUNNERS_OUT) == HunterVictoryType.RUNNER_KILL_COUNT;
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
        return tr(seconds < 0 ? key("common.none") : spec("screen.time.seconds", seconds));
    }

    private String wildcardDisplayName() {
        if (serverSync == null || serverSync.activeWildcard() == null || serverSync.activeWildcard().isBlank()) {
            return key("screen.wildcard.none_active");
        }
        return HunterWildcardText.wildcardNameKey(serverSync.activeWildcard());
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



    private int wildcardToggleColumns(int width) {
        if (width >= 420) {
            return 2;
        }
        return 1;
    }



    private String trim(String text, int width) {
        return font.plainSubstrByWidth(text, Math.max(10, width));
    }

    private int withAlpha(int color, float alpha) {
        int baseAlpha = color >>> 24;
        int scaledAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * alpha)));
        return (color & 0x00FFFFFF) | (scaledAlpha << 24);
    }

    private void renderFooterStatus(GuiGraphicsExtractor context, Layout layout) {
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
                context.text(font, Component.literal(text), x, y, segment.color(), false);
                x += font.width(text);
            }

            String separator = tr(key("screen.footer.separator"));
            if (i < segments.size() - 1 && x + font.width(separator) < right) {
                context.text(font, Component.literal(separator), x, y, 0xFF6F7C86, false);
                x += font.width(separator);
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
            if(ConfigDraft.pendingId!=0){segments.add(new StatusSegment(key("ui.saving"),0xFFF0C76B));return segments;}
            if(ConfigDraft.conflict()){segments.add(new StatusSegment(key("ui.save.conflict"),0xFFEF7181));return segments;}
            String modeText = canEditConfig()
                    ? (isRoundRunning() ? key("screen.footer.config_live") : key("screen.footer.config_editable"))
                    : key("screen.footer.config_readonly");
            int modeColor = canEditConfig() ? 0xFF78B8FA : 0xFF9FAAB4;
            segments.add(new StatusSegment(modeText, modeColor));
            if (!statusMessage.isBlank() && (statusKind == StatusKind.ERROR || !hasUnsavedChanges())) {
                segments.add(new StatusSegment(statusMessage, statusKind.color));
            }
            return segments;
        }

        if(currentPage==Page.GAME && serverSync.gameState()==GameState.WAITING && !startGameTooltip(true).isBlank()) segments.add(new StatusSegment(startGameTooltip(true),0xFFF0C76B));
        else if (!statusMessage.isBlank()) {
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
                && (hasUnsavedChanges() || hasVisibleInputChanges() || ConfigDraft.failed);
    }

    private boolean hasVisibleInputChanges() {
        if (!canEditConfig() || editableConfig == null) {
            return false;
        }
        return !pendingNumberText.isEmpty() || !pendingStringText.isEmpty();
    }

    private boolean hasFocusedTextField() {
        if (wildcardSearch != null && wildcardSearch.isFocused()) return true;
        for (EditBox field : numberFields.values()) {
            if (field.isFocused()) {
                return true;
            }
        }

        for (EditBox field : stringFields.values()) {
            if (field.isFocused()) {
                return true;
            }
        }

        return false;
    }

    private void ensureFocusedInputVisible(Layout layout) {
        for (EditBox field : numberFields.values()) {
            if (field.isFocused()) {
                ensureVisible(layout, field.getY(), field.getHeight());
                return;
            }
        }

        for (EditBox field : stringFields.values()) {
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
        int panelWidth = Math.min(720, Math.max(360, width - 48));
        if (panelWidth > width - 8) {
            panelWidth = Math.max(220, width - 8);
        }

        int panelHeight = Math.min(420, Math.max(260, height - 48));
        if (panelHeight > height - 8) {
            panelHeight = Math.max(200, height - 8);
        }

        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int navWidth = panelWidth < 520 ? 88 : 96;
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
            advanceTo(addInfoRow(layout, label, value, contentX(), cursorY, contentWidth(), valueColor));
        }

        void number(NumberField field) {
            number(field, canManage);
        }

        void number(NumberField field, boolean editable) {
            if(overviewReadOnly && !editable)return;
            advanceTo(addInputRow(layout, field, contentX(), cursorY, contentWidth(), editable));
        }

        void coordinates(NumberField xField, NumberField yField, NumberField zField) {
            if(overviewReadOnly){info(key("screen.field.coordinates"),getNumber(editableConfig,xField)+", "+getNumber(editableConfig,yField)+", "+getNumber(editableConfig,zField),0xFFE8EDF2);return;}
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
            advanceTo(addToggleRow(layout, field, contentX(), cursorY, contentWidth()));
        }

        void hint(String text) {
            if(overviewReadOnly)return;
            int hintY = cursorY + 2;
            int hintHeight = hintHeight(text, contentWidth());
            addHintText(layout, text, contentX(), hintY, contentWidth());
            bottomY = Math.max(bottomY, hintY + hintHeight);
            cursorY = hintY + hintHeight + ROW_GAP;
        }

        private int actionOffset;
        void inlineAction(String title, Button.OnPress action, ButtonVariant variant, boolean enabled, int maxWidth) {
            int actionWidth=Math.min(contentWidth(),Math.min(maxWidth,font.width(tr(title))+8));
            if(actionOffset+actionWidth>contentWidth()) endActions();
            addContentButton(layout,contentX()+actionOffset,cursorY,actionWidth,20,title,"",action,variant,enabled);
            actionOffset+=actionWidth+12;
        }
        void endActions() {
            if(actionOffset>0){gap(24);actionOffset=0;}
        }

        void button(String title, Button.OnPress action, ButtonVariant variant, boolean enabled, int maxWidth) {
            int buttonWidth=Math.min(contentWidth(),Math.min(maxWidth,Math.max(60,font.width(tr(title))+20)));
            addContentButton(layout,contentX(),cursorY,buttonWidth,20,title,"",action,variant,enabled);
            gap(24);
        }

        void buttonGrid(List<ButtonSpec> buttons, int requestedColumns, int maxButtonWidth) {
            if(overviewReadOnly) return;
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
            int titleWidth = font.width(tr(title));
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



        int height() {
            return Math.max(CARD_PADDING_TOP + CARD_TITLE_HEIGHT + CARD_PADDING_BOTTOM, bottomY - y + CARD_PADDING_BOTTOM);
        }

        int finish() {
            return finish(height());
        }

        int finish(int forcedHeight) {
            if (!finished) {
                int accent=teamAccent(title);
                if(accent!=0xFFFFFFFF) boxes.add(new Box(x,y,width,forcedHeight,accent==0xFFEF7181?0xF02C222B:0xF01E2B39,accent));
                else drawCardBorder(layout, x, y, width, forcedHeight);
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

    private record ButtonSpec(String title, Button.OnPress action, ButtonVariant variant, boolean enabled, String tooltip) {
        ButtonSpec(String title, Button.OnPress action, ButtonVariant variant, boolean enabled) {
            this(title, action, variant, enabled, "");
        }
    }

    private enum Page {
        RESULT(key("ui.result"), key("ui.result.description")),
        CHANGES(key("ui.changes"), key("ui.changes.description")),
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
    private enum ButtonVariant {
        TEAM_HUNTER, TEAM_RUNNER,
        NAV, NAV_SELECTED,
        SWITCH_ON, SWITCH_OFF,
        LINK,
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

    private class StyledButtonWidget extends Button {
        private final String description;
        private final ButtonVariant variant;
        private float navigationReveal=1;


        StyledButtonWidget(int x, int y, int width, int height, String title, String description, Button.OnPress action, ButtonVariant variant) {
            super(x, y, width, height, text(title), action, DEFAULT_NARRATION);
            this.description = description == null ? "" : description;
            this.variant = variant == null ? ButtonVariant.NORMAL : variant;
        }

        @Override
        public void playDownSound(net.minecraft.client.sounds.SoundManager manager) {
            if(variant==ButtonVariant.NAV || variant==ButtonVariant.NAV_SELECTED)manager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),1.1F,0.3F));
            else super.playDownSound(manager);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            if(variant==ButtonVariant.NAV || variant==ButtonVariant.NAV_SELECTED) {
                boolean clipped=navigationReveal<1;
                if(clipped)context.enableScissor(getX(),getY(),getX()+getWidth(),getY()+Math.max(1,Math.round(24*navigationReveal)));
                boolean selected=variant==ButtonVariant.NAV_SELECTED;
                NavigationAnimation animation=navigationAnimations.computeIfAbsent(getMessage().getString(),ignored->new NavigationAnimation());
                long now=System.nanoTime();float elapsed=animation.frameAt==0?0:(now-animation.frameAt)/1_000_000_000F;animation.frameAt=now;
                float target=isMouseOver(mouseX,mouseY)||isFocused()?1F:0F;
                animation.hover=com.xiaoming.hunterwildcard.client.ui.DisplayPreferences.get.reducedMotion?target:animation.hover+(target-animation.hover)*(1F-(float)Math.exp(-Math.min(elapsed,0.1F)*16F));
                int alpha=Math.round((selected?136:68*animation.hover)*navigationReveal);
                if(alpha>0)context.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),(alpha<<24)|(selected?0x345B78:0x374351));
                if(selected)context.fill(getX(),getY()+4,getX()+2,getY()+getHeight()-4,withAlpha(0xFF78B8FA,navigationReveal));
                int slide=Math.round(animation.hover*3);
                context.text(font,net.minecraft.network.chat.Component.literal(trim(getMessage().getString(),getWidth()-19)),getX()+8+slide,getY()+(getHeight()-font.lineHeight)/2,withAlpha(selected?0xFFB9DDFF:0xFFCAD3DC,navigationReveal),false);
                if(clipped)context.disableScissor();
                if(isHovered()&&!description.isBlank())setHoverTooltip(description,mouseX,mouseY);
                return;
            }
            if(variant==ButtonVariant.SWITCH_ON || variant==ButtonVariant.SWITCH_OFF) {
                boolean on=variant==ButtonVariant.SWITCH_ON,focus=isHovered()||isFocused();
                int x=getX(),y=getY(),color=active?(on?0xFF77D69A:0xFF8996A3):0xFF58616B;
                if(focus)context.fill(x,y,x+getWidth(),y+getHeight(),0x55374351);
                context.fill(x+2,y+6,x+24,y+16,on&&active?0xFF335A49:0xFF374351);
                int knob=on?x+15:x+4;
                context.fill(knob,y+8,knob+7,y+14,color);
                context.text(font,getMessage(),x+28,y+7,active?0xFFE8EDF2:0xFF8996A3,false);
                if(focus)context.fill(x,y+getHeight()-1,x+getWidth(),y+getHeight(),0xFF78B8FA);
                if(isHovered()&&!description.isBlank())setHoverTooltip(description,mouseX,mouseY);
                return;
            }
            if(variant==ButtonVariant.LINK) {
                String title=trim(getMessage().getString(),getWidth());
                int ty=getY()+(getHeight()-font.lineHeight)/2;
                int color=!active?0xFF58616B:isHovered()||isFocused()?0xFF78B8FA:0xFFA6B1BD;
                context.text(font,net.minecraft.network.chat.Component.literal(title),getX(),ty,color,false);
                if(active&&(isHovered()||isFocused()))context.fill(getX(),ty+font.lineHeight+1,getX()+font.width(title),ty+font.lineHeight+2,color);
                return;
            }
            ButtonVariant renderedVariant = active || variant == ButtonVariant.SELECTED ? variant : ButtonVariant.DISABLED;
            Palette palette = palette(renderedVariant, isHovered() || isFocused());
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
                context.text(font, net.minecraft.network.chat.Component.literal(title), textX, y + 7, palette.titleColor, true);
                context.text(font, net.minecraft.network.chat.Component.literal(trim(tr(description), width - 20)), textX, y + 23, palette.descriptionColor, false);
                return;
            }

            int titleX = x + Math.max(4, (width - font.width(title)) / 2);
            int titlePadding = height <= 18 ? 1 : 4;
            int titleY = y + Math.max(titlePadding, (height - font.lineHeight) / 2);
            if ("i".equals(title) && height <= 14) {
                titleY = y + Math.max(0, (height - font.lineHeight + 1) / 2);
            }
            context.text(font, net.minecraft.network.chat.Component.literal(title), titleX, titleY, palette.titleColor, true);
        }

        @Override
        protected void extractDefaultLabel(net.minecraft.client.gui.ActiveTextCollector textConsumer) {
        }

        private Palette palette(ButtonVariant variant, boolean hovered) {
            return switch (variant) {
                case TEAM_HUNTER -> new Palette(hovered ? 0xCC713844 : 0xAA502C35, 0xFFEF7181, 0xFFFF9CAB, 0xFFFFDCE1, 0xFFFFDCE1);
                case TEAM_RUNNER -> new Palette(hovered ? 0xCC315D87 : 0xAA253F5D, 0xFF78B8FA, 0xFFA3D0FF, 0xFFDEEEFF, 0xFFDEEEFF);
                case PRIMARY -> new Palette(hovered ? 0xCC246C86 : 0xAA1F536A, hovered ? 0xFF7FE7FF : 0xFF54B8D6, 0xFF7FE7FF, 0xFFFFFFFF, 0xFFD7F8FF);
                case SELECTED -> new Palette(0xAA345B78, 0xFF78B8FA, 0xFF78B8FA, 0xFFFFFFFF, 0xFFD7ECFF);
                case DANGER -> new Palette(hovered ? 0xAA6D3434 : 0x8845292F, hovered ? 0xFFFF8A8A : 0xFFEF7181, 0xFFFF8A8A, 0xFFFFFFFF, 0xFFFFC2C8);
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
