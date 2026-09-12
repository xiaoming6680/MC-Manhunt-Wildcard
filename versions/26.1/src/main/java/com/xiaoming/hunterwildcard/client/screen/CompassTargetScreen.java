package com.xiaoming.hunterwildcard.client.screen;

import com.xiaoming.hunterwildcard.client.HunterWildcardClientText;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.CompassMenuPayload;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.CompassSelectPayload;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.CompassTargetEntry;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.ArrayList;
import java.util.List;

/**
 * Small centred picker opened when a hunter uses the tracking compass: "nearest runner (auto)" plus one row per runner.
 * Visual style matches {@link HunterWildcardConfigScreen} (dark panel, blue accent).
 */
public class CompassTargetScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_PADDING = 12;
    private static final int TITLE_HEIGHT = 22;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int MAX_VISIBLE_BUTTONS = 10;
    private static final int COLOR_PANEL = 0xF0161B22;
    private static final int COLOR_BORDER = 0xFF35404B;
    private static final int COLOR_ACCENT = 0xFF7FC2FF;
    private static final int COLOR_HINT = 0xFF7D8790;

    private final CompassMenuPayload payload;
    private int panelX;
    private int panelY;
    private int panelHeight;
    private int scrollRows;
    private int visibleRows;
    private int selectedRow;
    private String hoverTooltip = "";
    private int hoverTooltipX;
    private int hoverTooltipY;

    private CompassTargetScreen(CompassMenuPayload payload) {
        super(Component.translatable(HunterWildcardText.key("screen.compass.title")));
        this.payload = payload;
        if (!payload.nearestSelected()) for(int i=0;i<payload.entries().size();i++) if(payload.entries().get(i).selected()) selectedRow=i+1;
    }

    public static void open(CompassMenuPayload payload) {
        Minecraft.getInstance().setScreen(new CompassTargetScreen(payload));
    }

    @Override
    protected void init() {
        List<Row> rows = rows();
        visibleRows = Math.min(rows.size(), Math.max(1, Math.min(MAX_VISIBLE_BUTTONS, (height - 76) / (BUTTON_HEIGHT + BUTTON_GAP))));
        int contentHeight = visibleRows * BUTTON_HEIGHT + Math.max(0, visibleRows - 1) * BUTTON_GAP;
        panelHeight = PANEL_PADDING + TITLE_HEIGHT + contentHeight + 14 + PANEL_PADDING;
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(160, width - 16));
        panelX = (width - panelWidth) / 2;
        panelY = Math.max(8, (height - panelHeight) / 2);

        scrollRows = Math.max(0, Math.min(scrollRows, rows.size() - visibleRows));
        int buttonX = panelX + PANEL_PADDING;
        int buttonWidth = panelWidth - PANEL_PADDING * 2;
        int y = panelY + PANEL_PADDING + TITLE_HEIGHT;
        for (int i = scrollRows; i < scrollRows + visibleRows; i++) {
            Row row = rows.get(i);
            PickButton button = new PickButton(buttonX, y, buttonWidth, BUTTON_HEIGHT, row.label(), row.tooltip(), row.selected(), widget -> select(row.payload()));
            addRenderableWidget(button);
            y += BUTTON_HEIGHT + BUTTON_GAP;
        }
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(
                tr(HunterWildcardText.key("screen.compass.nearest")),
                tr(HunterWildcardText.key("screen.compass.nearest_tooltip")),
                payload.nearestSelected(),
                CompassSelectPayload.trackNearest()
        ));
        for (CompassTargetEntry entry : payload.entries()) {
            String name = tr(entry.nameSpec());
            String label = entry.sameDimension()
                    ? tr(HunterWildcardText.spec("screen.compass.entry_distance", name, entry.distance()))
                    : tr(HunterWildcardText.spec("screen.compass.entry_other_dimension", name));
            rows.add(new Row(label, label, entry.selected() && !payload.nearestSelected(), CompassSelectPayload.track(entry.playerId())));
        }
        return rows;
    }

    private void select(CustomPacketPayload selection) {
        try {
            ClientPlayNetworking.send(selection);
        } catch (IllegalStateException ignored) {
            // Not connected any more; nothing to send.
        }
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x66000000);
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(160, width - 16));
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, COLOR_ACCENT);
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, COLOR_BORDER);
        context.fill(panelX, panelY, panelX + 1, panelY + panelHeight, COLOR_BORDER);
        context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_BORDER);

        String title = font.plainSubstrByWidth(tr(HunterWildcardText.key("screen.compass.title")), panelWidth - PANEL_PADDING * 2);
        context.text(font, Component.literal(title), panelX + PANEL_PADDING, panelY + PANEL_PADDING + 2, 0xFFFFFFFF, true);
        if (payload.entries().isEmpty()) {
            context.text(font, Component.literal(tr(HunterWildcardText.key("screen.compass.empty"))), panelX + PANEL_PADDING, panelY + PANEL_PADDING + TITLE_HEIGHT + BUTTON_HEIGHT + 6, COLOR_HINT, false);
        }
        String hint = font.plainSubstrByWidth(tr(HunterWildcardText.key("screen.compass.hint")), panelWidth - PANEL_PADDING * 2);
        context.text(font, Component.literal(hint), panelX + PANEL_PADDING, panelY + panelHeight - PANEL_PADDING - 8, COLOR_HINT, false);

        if (rows().size() > visibleRows) {
            int trackHeight = Math.max(20, panelHeight - 60);
            int thumb = Math.max(8, trackHeight * visibleRows / rows().size());
            int sy = panelY + 34 + (trackHeight - thumb) * scrollRows / Math.max(1, rows().size() - visibleRows);
            context.fill(panelX + panelWidth - 5, sy, panelX + panelWidth - 3, sy + thumb, COLOR_ACCENT);
        }
        hoverTooltip = "";
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (!hoverTooltip.isBlank()) {
            context.setComponentTooltipForNextFrame(font, List.of(Component.literal(hoverTooltip)), hoverTooltipX, hoverTooltipY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int rows = rows().size();
        if (rows <= visibleRows) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int next = Math.max(0, Math.min(rows - visibleRows, scrollRows - (verticalAmount > 0 ? 1 : -1)));
        if (next != scrollRows) {
            scrollRows = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent input) {
        int key = input.key();
        int focusedIndex=children().indexOf(getFocused());
        if(focusedIndex>=0)selectedRow=scrollRows+focusedIndex;
        if (key == 264 || key == 265) {
            selectedRow = Math.floorMod(selectedRow + (key == 264 ? 1 : -1), rows().size());
            scrollRows = Math.max(0, Math.min(scrollRows, selectedRow));
            if (selectedRow >= scrollRows + visibleRows) scrollRows = selectedRow - visibleRows + 1;
            rebuildWidgets();
            if (selectedRow - scrollRows < children().size()) setFocused(children().get(selectedRow - scrollRows));
            return true;
        }
        if ((key == 257 || key == 335) && getFocused()==null) { select(rows().get(selectedRow).payload()); return true; }
        return super.keyPressed(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String tr(String spec) {
        return HunterWildcardClientText.translate(spec);
    }

    private record Row(String label, String tooltip, boolean selected, CustomPacketPayload payload) {
    }

    private class PickButton extends Button {
        private final String tooltip;
        private final boolean selected;

        PickButton(int x, int y, int width, int height, String label, String tooltip, boolean selected, OnPress action) {
            super(x, y, width, height, net.minecraft.network.chat.Component.literal(label), action, DEFAULT_NARRATION);
            this.tooltip = tooltip == null ? "" : tooltip;
            this.selected = selected;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            boolean hovered = isHovered() || isFocused();
            int background = selected ? 0xAA345B78 : hovered ? 0xAA3E5570 : 0x88303A46;
            int border = selected ? COLOR_ACCENT : hovered ? 0xFF74B6FF : 0xFF4C5A66;
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            context.fill(x, y, x + w, y + h, background);
            context.fill(x, y, x + w, y + 1, border);
            context.fill(x, y + h - 1, x + w, y + h, border);
            context.fill(x, y, x + 1, y + h, border);
            context.fill(x + w - 1, y, x + w, y + h, border);
            if (selected) {
                context.fill(x + 2, y + 2, x + 5, y + h - 2, COLOR_ACCENT);
            }
            if (hovered && !tooltip.isBlank()) {
                hoverTooltip = tooltip;
                hoverTooltipX = mouseX;
                hoverTooltipY = mouseY;
            }

            int textX = selected ? x + 9 : x + 6;
            String label = font.plainSubstrByWidth(getMessage().getString(), w - (textX - x) - 6);
            context.text(font, net.minecraft.network.chat.Component.literal(label), textX, y + Math.max(2, (h - font.lineHeight) / 2), selected ? 0xFFFFFFFF : 0xFFE1E6EB, true);
        }

        @Override
        protected void extractDefaultLabel(net.minecraft.client.gui.ActiveTextCollector textConsumer) {
        }
    }
}
