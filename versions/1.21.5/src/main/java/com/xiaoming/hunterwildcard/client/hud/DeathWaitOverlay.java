package com.xiaoming.hunterwildcard.client.hud;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.client.key.HunterWildcardKeyBindings;
import com.xiaoming.hunterwildcard.client.HunterWildcardClientText;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Compact death status shown over teammate cameras or free spectator flight. */
public final class DeathWaitOverlay {
    private static final int COLOR_TITLE = 0xFFFF5555;
    private static final int COLOR_COUNTDOWN = 0xFFFFFFFF;
    private static final int COLOR_LINE = 0xFFB0BAC4;

    private static boolean visible;
    private static boolean spectating;
    private static String targetName = "";
    private static int targetCount;
    private static int remainingSeconds;
    private static String line1 = "";
    private static String line2 = "";
    private static long lastUpdateMs;

    private DeathWaitOverlay() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    public static void update(boolean show, int seconds, String first, String second) {
        visible = show;
        remainingSeconds = Math.max(0, seconds);
        line1 = first == null ? "" : first;
        line2 = second == null ? "" : second;
        lastUpdateMs = System.currentTimeMillis();
    }

    public static void reset() {
        update(false, 0, "", "");
        updateSpectating(false, "", 0);
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void updateSpectating(boolean active, String name, int count) {
        spectating = active;
        targetName = name == null ? "" : name;
        targetCount = Math.max(0, count);
    }

    public static boolean isSpectating() { return spectating; }

    private static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.isDead() || client.options.hudHidden) return;
        var details = com.xiaoming.hunterwildcard.client.ClientGameStatus.details;
        var sync = com.xiaoming.hunterwildcard.client.ClientGameStatus.latest();
        boolean out = details != null && sync != null
                && sync.gameState() != com.xiaoming.hunterwildcard.game.GameState.WAITING
                && details.ownState().endsWith("ui.member.out");
        if (!visible && !spectating && !out) return;

        TextRenderer font = client.textRenderer;
        int width = Math.min(310, context.getScaledWindowWidth() - 24);
        int x = (context.getScaledWindowWidth() - width) / 2;
        int height = visible ? 76 : 50;
        int y = Math.max(10, context.getScaledWindowHeight() - height - 30);
        context.fill(x, y, x + width, y + height, 0xEE1C232C);
        context.fill(x, y, x + 2, y + height, COLOR_TITLE);
        int centerX = context.getScaledWindowWidth() / 2;
        String title;
        if (visible) {
            int elapsed = (int) ((System.currentTimeMillis() - lastUpdateMs) / 1000L);
            title = tr(HunterWildcardText.spec("hud.death_wait.countdown", Math.max(0, remainingSeconds - elapsed)));
        } else {
            title = tr(HunterWildcardText.key("ui.death.out"));
        }
        centered(context, font, title, centerX, y + 7, width - 16, COLOR_COUNTDOWN);
        String watching = out ? tr(HunterWildcardText.key("hud.death_out.free_spectator")) : spectating && !targetName.isBlank()
                ? tr(HunterWildcardText.spec("hud.death_wait.watching", targetName))
                : tr(HunterWildcardText.key("hud.death_wait.free_spectator"));
        centered(context, font, watching, centerX, y + 20, width - 16, COLOR_LINE);
        int hintY = y + 33;
        if (visible) {
            centered(context, font, tr(line1), centerX, y + 33, width - 16, COLOR_LINE);
            centered(context, font, tr(line2), centerX, y + 46, width - 16, COLOR_LINE);
            hintY = y + 59;
        }
        if (spectating && targetCount > (out ? 0 : 1)) {
            String hint = tr(HunterWildcardText.spec(out ? "hud.death_out.switch" : "hud.death_wait.switch", HunterWildcardKeyBindings.previousTeammateKeyName(), HunterWildcardKeyBindings.nextTeammateKeyName()));
            centered(context, font, hint, centerX, hintY, width - 16, 0xFF83C9F4);
        }
    }

    private static void centered(DrawContext context, TextRenderer font, String text, int x, int y, int width, int color) {
        if (font.getWidth(text) > width) text = font.trimToWidth(text, Math.max(1, width - font.getWidth("…"))) + "…";
        context.drawCenteredTextWithShadow(font, Text.literal(text), x, y, color);
    }

    private static String tr(String spec) {
        return HunterWildcardClientText.translate(spec);
    }
}
