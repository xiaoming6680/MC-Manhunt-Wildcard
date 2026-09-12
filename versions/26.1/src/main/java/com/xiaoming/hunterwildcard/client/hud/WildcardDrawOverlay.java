package com.xiaoming.hunterwildcard.client.hud;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.client.ui.DisplayPreferences;
import com.xiaoming.hunterwildcard.client.HunterWildcardClientText;
import com.xiaoming.hunterwildcard.client.key.KeyScrambleController;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardIds;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WildcardDrawOverlay {
    private static final long DRAW_FADE_IN_MS = 180L;
    private static final long DRAW_SPIN_MS = 4300L;
    private static final long DRAW_FADE_OUT_MS = 300L;
    private static final long DRAW_TOTAL_MS = 5000L;
    private static final long FEEDBACK_IN_MS = 260L;
    private static final long FEEDBACK_HOLD_MS = 2300L;
    private static final long FEEDBACK_OUT_MS = 420L;
    private static final long FEEDBACK_TOTAL_MS = FEEDBACK_IN_MS + FEEDBACK_HOLD_MS + FEEDBACK_OUT_MS;
    private static final long INTRO_SLIDE_MS = 260L;
    private static final long OBJECTIVE_SLIDE_MS = 260L;
    private static final long OBJECTIVE_NOTICE_IN_MS = 260L;
    private static final long OBJECTIVE_NOTICE_HOLD_MS = 2300L;
    private static final long OBJECTIVE_NOTICE_OUT_MS = 420L;
    private static final long OBJECTIVE_NOTICE_TOTAL_MS = OBJECTIVE_NOTICE_IN_MS + OBJECTIVE_NOTICE_HOLD_MS + OBJECTIVE_NOTICE_OUT_MS;
    private static final String[] SPIN_NAMES = WildcardIds.ALL.toArray(new String[0]);
    private static final int KEY_PANEL_WIDTH = 146;
    private static final int KEY_PANEL_ROW_HEIGHT = 11;
    private static final int KEY_PANEL_MARGIN = 6;
    private static final int KEY_PANEL_TOP = 34; // below the boss bar rows so the countdown text stays readable

    private static long drawStartTimeMs = -1L;
    private static String finalWildcard = "";
    private static boolean revealSoundPlayed;
    private static boolean introVisible;
    private static int introPanelBottom;
    private static boolean introHiding;
    private static long introTransitionStartTimeMs = -1L;
    private static String introName = "";
    private static String introDescription = "";
    private static boolean weaponOverheatVisible;
    private static int weaponOverheatHeat;
    private static int weaponOverheatMaxHeat = 1;
    private static final long KILL_FLASH_MS = 450L;
    private static final float KILL_PANEL_SCALE = 1.0F;
    private static long killFlashStartMs = -1L;
    private static boolean objectiveVisible;
    private static boolean objectiveHiding;
    private static long objectiveTransitionStartTimeMs = -1L;
    private static String objectiveText = "";
    private static String objectiveHunterText = "";
    private static String objectiveStyle = "runner";
    private static final List<ObjectiveNoticeEntry> objectiveNoticeEntries = new ArrayList<>();

    private WildcardDrawOverlay() {
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                Identifier.fromNamespaceAndPath(HunterWildcardMod.MOD_ID, "wildcard_draw_overlay"),
                WildcardDrawOverlay::render
        );
    }

    public static void start(String wildcardName) {
        finalWildcard = wildcardName == null ? "" : wildcardName;
        drawStartTimeMs = System.currentTimeMillis();
        revealSoundPlayed = false;

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
    }

    public static void showKillFeedback(String hunterName,String runnerName,int remaining,int current,int target){
        CombatFeed.kill(hunterName,runnerName,current,target,HunterWildcardText.key("common.environment").equals(hunterName));
    }
    public static void showFeedback(String title,String line1,String line2,String style){CombatFeed.notice(title,line1,line2,style);}
    public static void reset(){
        drawStartTimeMs=-1; introVisible=false; introHiding=false; introName=""; introDescription="";
        objectiveVisible=false; objectiveHiding=false;objectiveText="";objectiveHunterText="";weaponOverheatVisible=false;
        objectiveNoticeEntries.clear();CombatFeed.clear();introPanelBottom=0;
    }

    public static void setObjectiveStatus(boolean visible, String text, String style, String hunterText) {
        long now = System.currentTimeMillis();
        if (visible) {
            boolean wasFullyVisible = objectiveVisible && !objectiveHiding;
            objectiveVisible = true;
            objectiveHiding = false;
            if (!wasFullyVisible) {
                objectiveTransitionStartTimeMs = now;
            }
            objectiveText = text == null ? "" : text;
            objectiveHunterText = hunterText == null ? "" : hunterText;
            objectiveStyle = style == null || style.isBlank() ? "runner" : style;
            return;
        }

        if (objectiveVisible && !objectiveHiding) {
            objectiveHiding = true;
            objectiveTransitionStartTimeMs = now;
        }
    }

    public static void showObjectiveNotice(String message, String style) {
        if(message!=null && !message.isBlank()) CombatFeed.notice(HunterWildcardText.key("hud.objective.notice_title"),message,"",style);
    }

    public static void setIntro(boolean visible, String wildcardName, String description) {
        if (visible) {
            if(introVisible && !introHiding && java.util.Objects.equals(introName,wildcardName) && java.util.Objects.equals(introDescription,description))return;
            introVisible = true;
            introHiding = false;
            introTransitionStartTimeMs = System.currentTimeMillis();
            introName = wildcardName == null ? "" : wildcardName;
            introDescription = description == null ? "" : description;
            return;
        }

        if (introVisible && !introHiding) {
            introHiding = true;
            introTransitionStartTimeMs = System.currentTimeMillis();
        }
    }

    public static void setWeaponOverheat(int heat, int maxHeat, boolean visible) {
        weaponOverheatVisible = visible;
        weaponOverheatHeat = Math.max(0, heat);
        weaponOverheatMaxHeat = Math.max(1, maxHeat);
    }

    private static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().options.hideGui || DeathWaitOverlay.isVisible() || com.xiaoming.hunterwildcard.client.BackroomsClient.isCoverVisible()) return;
        context.pose().pushMatrix();
        context.pose().scale(HudLayout.scale(),HudLayout.scale());
        introPanelBottom = 0;
        renderDrawPanel(context);
        // Keep the HUD cards' state, but hide them until the draw finishes.
        if (drawStartTimeMs < 0L) {
            renderIntroPanel(context);
            if (GameStatusHud.shouldRender()) {
                GameStatusHud.render(context, introPanelBottom > 0 ? introPanelBottom + 4 : 6);
            }
            renderObjectiveStatusPanel(context);
        }
        renderWeaponOverheatBar(context);

        renderKeyScramblePanel(context);
        context.pose().popMatrix();
        CombatFeed.render(context, Math.max(60, Math.round((KEY_PANEL_TOP + keyScrambleReservedHeight()) * HudLayout.scale())));
    }

    private static int keyScramblePanelHeight() {
        int rows = KeyScrambleController.size();
        return rows == 0 ? 0 : 16 + rows * KEY_PANEL_ROW_HEIGHT + 3;
    }

    /** Vertical space the key panel occupies at the top-right, so other top-right panels can move below it. */
    private static int keyScrambleReservedHeight() {
        if (!KeyScrambleController.isActive()) {
            return 0;
        }
        return KEY_PANEL_TOP - 10 + keyScramblePanelHeight() + 4;
    }

    private static void renderKeyScramblePanel(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (!KeyScrambleController.isActive() || client.options.hideGui) {
            return;
        }

        Font textRenderer = client.font;
        int screenWidth = HudLayout.width();
        int rows = KeyScrambleController.size();
        int panelWidth = Math.min(KEY_PANEL_WIDTH, Math.max(80, screenWidth - 12));
        int panelHeight = keyScramblePanelHeight();
        int panelX = screenWidth - panelWidth - KEY_PANEL_MARGIN;
        int panelY = KEY_PANEL_TOP;
        int accent = 0xFFFFB84D;
        long now = System.currentTimeMillis();

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, HudLayout.background());
        context.fill(panelX + panelWidth - 2, panelY, panelX + panelWidth, panelY + panelHeight, accent);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, accent);

        String title = tr(HunterWildcardText.key("hud.key_scramble.title"));
        context.text(textRenderer, Component.literal(trim(textRenderer, title, panelWidth - 10)), panelX + 5, panelY + 4, accent, false);

        int labelWidth = panelWidth - 10;
        for (int i = 0; i < rows; i++) {
            KeyMapping binding = KeyScrambleController.binding(i);
            String action = Component.translatable(binding.getName()).getString();
            String key = binding.getTranslatedKeyMessage().getString();
            boolean flash = KeyScrambleController.isRecentlyChanged(i, now);
            int rowY = panelY + 16 + i * KEY_PANEL_ROW_HEIGHT;
            int keyWidth = textRenderer.width(key);
            int keyX = panelX + panelWidth - 6 - keyWidth;
            if (flash) {
                context.fill(panelX + 3, rowY - 1, panelX + panelWidth - 3, rowY + 9, 0x66FFB84D);
            }
            context.text(textRenderer, Component.literal(trim(textRenderer, action, labelWidth - keyWidth - 6)), panelX + 5, rowY, 0xFFC9D4DE, false);
            context.text(textRenderer, Component.literal(key), keyX, rowY, flash ? 0xFFFFFFFF : 0xFFFFD966, true);
        }
    }

    private static void renderWeaponOverheatBar(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (!weaponOverheatVisible || client.options.hideGui) {
            return;
        }

        Font textRenderer = client.font;
        int screenWidth = HudLayout.width();
        int screenHeight = HudLayout.height();
        int barWidth = 64;
        int barHeight = 6;
        int x = screenWidth / 2 - barWidth / 2;
        int y = screenHeight / 2 + 14;
        int heat = Math.min(weaponOverheatHeat, weaponOverheatMaxHeat);
        int fillWidth = Math.round(barWidth * (heat / (float) weaponOverheatMaxHeat));
        int fillColor = weaponHeatColor(heat, weaponOverheatMaxHeat);
        boolean overheated = heat >= weaponOverheatMaxHeat;
        if (overheated && (System.currentTimeMillis() / 150L) % 2 == 0) {
            fillColor = 0xFFFFFFFF;
        }

        context.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xA0000000);
        context.fill(x, y, x + barWidth, y + barHeight, 0x90161B22);
        if (fillWidth > 0) {
            context.fill(x, y, x + fillWidth, y + barHeight, fillColor);
        }
        for (int i = 1; i < weaponOverheatMaxHeat; i++) {
            int segmentX = x + Math.round(barWidth * (i / (float) weaponOverheatMaxHeat));
            context.fill(segmentX, y, segmentX + 1, y + barHeight, 0x90000000);
        }

        String label = tr(HunterWildcardText.spec(overheated ? "hud.weapon_overheat.overheated" : "hud.weapon_overheat.label", heat, weaponOverheatMaxHeat));
        int labelWidth = textRenderer.width(label);
        context.text(textRenderer, Component.literal(label), screenWidth / 2 - labelWidth / 2, y + barHeight + 3, overheated ? 0xFFFF6B6B : 0xFFE6EDF3, true);
    }

    public static boolean isIntroExpanded() {
        return introVisible && !introHiding;
    }

    private static void renderIntroPanel(GuiGraphicsExtractor context) {
        if (!introVisible || introName.isBlank()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;
        int screenWidth = HudLayout.width();
        String introTitle = tr(HunterWildcardText.spec("hud.intro.title", wildcardDisplayName(introName)));
        String introDescriptionText = tr(introDescription);
        int maxAvailableWidth = Math.max(80, screenWidth - 6);
        int compactWidth = Math.min(Math.min(164, maxAvailableWidth), Math.max(112, screenWidth / 2));
        int wideWidth = Math.max(compactWidth, Math.min(224, maxAvailableWidth));
        boolean needsWide = textRenderer.width(introTitle) > compactWidth - 12
                || (!introDescriptionText.isBlank() && textRenderer.width(introDescriptionText) > compactWidth - 12);
        int panelWidth = needsWide ? wideWidth : compactWidth;
        int maxDescriptionLines = needsWide ? 3 : 2;
        List<String> descriptionLines = introDescriptionText.isBlank()
                ? List.of()
                : wrap(textRenderer, introDescriptionText, panelWidth - 12, maxDescriptionLines);
        int panelHeight = descriptionLines.isEmpty() ? 20 : 20 + descriptionLines.size() * 11;
        long elapsed = introTransitionStartTimeMs < 0L ? INTRO_SLIDE_MS : System.currentTimeMillis() - introTransitionStartTimeMs;
        if (introHiding && elapsed >= INTRO_SLIDE_MS) {
            introVisible = false;
            introHiding = false;
            introTransitionStartTimeMs = -1L;
            introName = "";
            introDescription = "";
            return;
        }

        float progress = DisplayPreferences.get.reducedMotion ? 1 : smooth(Math.min(1.0F, elapsed / (float) INTRO_SLIDE_MS));
        if (introHiding) {
            progress = 1.0F - progress;
        }
        float alpha = Math.max(0.0F, Math.min(1.0F, progress));
        int panelX = -Math.round((panelWidth + 2) * (1.0F - progress));
        int panelY = DisplayPreferences.get.inset + 18;

        introPanelBottom = panelY + panelHeight;
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, withAlpha(HudLayout.background(), alpha));
        context.fill(panelX, panelY, panelX + 2, panelY + panelHeight, withAlpha(0xFF7FC2FF, alpha));
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, withAlpha(0xAA7FC2FF, alpha));

        context.text(textRenderer, Component.literal(trim(textRenderer, introTitle, panelWidth - 12)), panelX + 5, panelY + 3, withAlpha(0xFFFFFFFF, alpha), true);
        for (int i = 0; i < descriptionLines.size(); i++) {
            context.text(textRenderer, Component.literal(descriptionLines.get(i)), panelX + 5, panelY + 16 + i * 11, withAlpha(0xFFC9D4DE, alpha), false);
        }
    }

    private static void renderObjectiveStatusPanel(GuiGraphicsExtractor context) {
        if (!objectiveVisible) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui) {
            return;
        }

        Font textRenderer = client.font;
        int screenWidth = HudLayout.width();
        int maxAvailableWidth = Math.max(120, Math.min(280,(int)(screenWidth*0.48F)));
        String objectiveDisplayText = objectiveText.isBlank() ? "" : tr(objectiveText);
        String hunterDisplayText = objectiveHunterText.isBlank() ? "" : tr(objectiveHunterText);
        int widest = Math.max(textRenderer.width(objectiveDisplayText), textRenderer.width(hunterDisplayText));
        int panelWidth = Math.min(maxAvailableWidth, Math.max(184, widest + 20));
        List<String> objectiveLines=objectiveDisplayText.isBlank()?List.of():wrap(textRenderer,objectiveDisplayText,panelWidth-18,2);
        List<String> hunterLines=hunterDisplayText.isBlank()?List.of():wrap(textRenderer,hunterDisplayText,panelWidth-18,2);
        int panelHeight=24+(objectiveLines.size()+hunterLines.size())*12+(com.xiaoming.hunterwildcard.client.ClientGameStatus.details==null?0:15);
        long elapsed = objectiveTransitionStartTimeMs < 0L ? OBJECTIVE_SLIDE_MS : System.currentTimeMillis() - objectiveTransitionStartTimeMs;
        if (objectiveHiding && elapsed >= OBJECTIVE_SLIDE_MS) {
            objectiveVisible = false;
            objectiveHiding = false;
            objectiveTransitionStartTimeMs = -1L;
            objectiveText = "";
            return;
        }

        float progress = smooth(Math.min(1.0F, elapsed / (float) OBJECTIVE_SLIDE_MS));
        if (objectiveHiding) {
            progress = 1.0F - progress;
        }
        float alpha = Math.max(0.0F, Math.min(1.0F, progress));
        int panelX = DisplayPreferences.get.inset - (DisplayPreferences.get.reducedMotion?0:Math.round((panelWidth + DisplayPreferences.get.inset) * (1.0F - progress)));
        int panelY = Math.max(DisplayPreferences.get.inset, (HudLayout.height() - panelHeight) / 2);
        int accent = objectiveAccent(objectiveStyle);

        renderObjectiveCard(context, textRenderer, panelX, panelY, panelWidth, panelHeight, accent, alpha, objectiveLines, hunterLines);
    }

    private static void renderObjectiveCard(GuiGraphicsExtractor context, Font font, int x, int y, int width, int height, int accent, float alpha, List<String> objectiveLines, List<String> hunterLines) {
        // Keep the normal font size. Card size follows the text, never the reverse.
        context.fill(x,y,x+width,y+height,withAlpha(HudLayout.background(),alpha));
        context.fill(x,y,x+2,y+height,withAlpha(accent,alpha));
        context.item(objectiveIcon(objectiveStyle),x+7,y+4);
        context.text(font,Component.literal(tr(HunterWildcardText.key("hud.objective.status_title"))),x+28,y+7,withAlpha(accent,alpha),false);
        int lineY=y+24;
        for(String line:objectiveLines) {context.text(font,Component.literal(line),x+9,lineY,withAlpha(0xFFE8EDF2,alpha),true);lineY+=12;}
        for(String line:hunterLines) {context.text(font,Component.literal(line),x+9,lineY,withAlpha(0xFFEF7181,alpha),true);lineY+=12;}
        var detail=com.xiaoming.hunterwildcard.client.ClientGameStatus.details;
        if(detail!=null){
            String lives=detail.ownLives()<0?tr(HunterWildcardText.key("common.infinite")):Integer.toString(detail.ownLives());
            context.text(font,Component.literal(trim(font,tr(detail.ownState())+(detail.ownLives()==-2?"":" · ♥ "+lives),width-18)),x+9,lineY+2,withAlpha(0xFFA6B1BD,alpha),false);
        }
    }

    private static void renderDrawPanel(GuiGraphicsExtractor context) {
        if (drawStartTimeMs < 0L) {
            return;
        }

        long elapsed = System.currentTimeMillis() - drawStartTimeMs;
        if (elapsed >= DRAW_TOTAL_MS) {
            drawStartTimeMs = -1L;
            return;
        }

        if (elapsed >= DRAW_SPIN_MS && !revealSoundPlayed) {
            revealSoundPlayed = true;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F));
        }

        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;
        int screenWidth = HudLayout.width();
        int panelX = 10;
        int panelY = 10;
        int panelWidth = Math.min(146, Math.max(118, screenWidth - 20));
        int panelHeight = 38;
        float alpha = drawAlpha(elapsed);
        String displayedId = DisplayPreferences.get.reducedMotion && elapsed < DRAW_SPIN_MS ? "" : displayedName(elapsed);
        String displayedName = wildcardDisplayName(displayedId);
        boolean revealed = elapsed >= DRAW_SPIN_MS;
        int accent = revealed ? 0xFF77E287 : 0xFF7FC2FF;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, withAlpha(HudLayout.background(), alpha));
        context.fill(panelX, panelY, panelX + 3, panelY + panelHeight, withAlpha(accent, alpha));
        context.fill(panelX, panelY + panelHeight - 2, panelX + Math.round(panelWidth * Math.min(1.0F, elapsed / (float) DRAW_TOTAL_MS)), panelY + panelHeight, withAlpha(accent, alpha));

        int iconX = panelX + 8;
        int iconY = panelY + 11;
        context.item(WildcardIcons.iconFor(displayedId), iconX, iconY);

        context.text(textRenderer, Component.literal(tr(revealed ? HunterWildcardText.key("hud.draw.revealed") : HunterWildcardText.key("hud.draw.drawing"))), panelX + 30, panelY + 6, withAlpha(0xFFC9D4DE, alpha), false);
        context.text(textRenderer, Component.literal(trim(textRenderer, displayedName, panelWidth - 38)), panelX + 30, panelY + 21, withAlpha(0xFFFFFFFF, alpha), true);
    }

    private static String displayedName(long elapsed) {
        if (elapsed >= DRAW_SPIN_MS) {
            return finalWildcard;
        }

        int index = (int) (elapsed / 95L) % SPIN_NAMES.length;
        return SPIN_NAMES[index];
    }

    private static String wildcardDisplayName(String wildcardId) {
        if (wildcardId == null || wildcardId.isBlank()) {
            return tr(HunterWildcardText.key("hud.wildcard.unknown"));
        }
        return tr(HunterWildcardText.wildcardNameKey(wildcardId));
    }

    private static String tr(String spec) {
        return HunterWildcardClientText.translate(spec);
    }

    private static float drawAlpha(long elapsed) {
        if (elapsed < DRAW_FADE_IN_MS) {
            return smooth(elapsed / (float) DRAW_FADE_IN_MS);
        }

        long fadeStart = DRAW_TOTAL_MS - DRAW_FADE_OUT_MS;
        if (elapsed > fadeStart) {
            return Math.max(0.0F, 1.0F - smooth((elapsed - fadeStart) / (float) DRAW_FADE_OUT_MS));
        }

        return 1.0F;
    }

    private static int feedbackAccent(String style) {
        return switch (style) {
            case "runner" -> 0xFF7FC2FF;
            case "respawn" -> 0xFF77E287;
            case "hunter" -> 0xFFFF8A8A;
            case "kill" -> 0xFFFF4D4D;
            default -> 0xFFC9D4DE;
        };
    }

    private static int objectiveBaseY(int screenHeight, int panelHeight) {
        return Math.max(24, screenHeight / 2 - panelHeight / 2 - 18);
    }

    private static int objectiveAccent(String style) {
        return switch (style) {
            case "time" -> 0xFFFFD966;
            case "item" -> 0xFF77E287;
            case "coordinate" -> 0xFF7FC2FF;
            case "hunter" -> 0xFFFF8A8A;
            default -> 0xFF7FC2FF;
        };
    }

    private static int weaponHeatColor(int heat, int maxHeat) {
        if (heat <= 0) {
            return 0x00000000;
        }
        if (heat >= maxHeat) {
            return 0xFFFF4C4C;
        }
        if (heat >= 3) {
            return 0xFFFF9F3F;
        }
        if (heat >= 2) {
            return 0xFFFFD966;
        }
        return 0xFF7FC2FF;
    }

    private static ItemStack feedbackIcon(String style) {
        return switch (style) {
            case "runner" -> new ItemStack(Items.DIAMOND);
            case "respawn" -> new ItemStack(Items.TOTEM_OF_UNDYING);
            case "hunter" -> new ItemStack(Items.IRON_SWORD);
            case "kill" -> new ItemStack(Items.NETHERITE_SWORD);
            default -> new ItemStack(Items.NETHER_STAR);
        };
    }

    private static ItemStack objectiveIcon(String style) {
        return switch (style) {
            case "time" -> new ItemStack(Items.CLOCK);
            case "item" -> new ItemStack(Items.DIAMOND);
            case "coordinate" -> new ItemStack(Items.COMPASS);
            default -> new ItemStack(Items.NETHER_STAR);
        };
    }

    private static float smooth(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static String trim(Font textRenderer, String text, int maxWidth) {
        String safeText = text == null ? "" : text;
        if (textRenderer.width(safeText) <= maxWidth) {
            return safeText;
        }

        return textRenderer.plainSubstrByWidth(safeText, Math.max(8, maxWidth - textRenderer.width("..."))) + "...";
    }

    private static List<String> wrap(Font textRenderer, String text, int maxWidth, int maxLines) {
        String safeText = text == null ? "" : text;
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < safeText.length(); i++) {
            char c = safeText.charAt(i);
            String candidate = current.toString() + c;
            if (current.length() > 0 && textRenderer.width(candidate) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
                if (lines.size() >= maxLines - 1) {
                    String remainder = safeText.substring(i);
                    lines.add(trim(textRenderer, remainder, maxWidth));
                    return lines;
                }
            }
            current.append(c);
        }
        if (current.length() > 0 && lines.size() < maxLines) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static int withAlpha(int color, float alpha) {
        int baseAlpha = color >>> 24;
        int scaledAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * alpha)));
        return (color & 0x00FFFFFF) | (scaledAlpha << 24);
    }

    private static final class ObjectiveNoticeEntry {
        private final String message;
        private final String style;
        private final long startTimeMs;
        private long topStartTimeMs = -1L;
        private float currentY = Float.NaN;

        private ObjectiveNoticeEntry(String message, String style, long startTimeMs) {
            this.message = message;
            this.style = style;
            this.startTimeMs = startTimeMs;
        }
    }
}
