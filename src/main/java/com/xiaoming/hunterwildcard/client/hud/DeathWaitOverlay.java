package com.xiaoming.hunterwildcard.client.hud;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.client.HunterWildcardClientText;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Full-screen cover shown while waiting to respawn: nothing of the world is visible, only the
 * countdown, how you died and the lives you have left. The server drives it and clears it.
 */
public final class DeathWaitOverlay {
    private static final int COLOR_TITLE = 0xFFFF5555;
    private static final int COLOR_COUNTDOWN = 0xFFFFFFFF;
    private static final int COLOR_LINE = 0xFFB0BAC4;

    private static boolean visible;
    private static int remainingSeconds;
    private static String line1 = "";
    private static String line2 = "";
    private static long lastUpdateMs;

    private DeathWaitOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(Identifier.of(HunterWildcardMod.MOD_ID, "death_wait_cover"), (context, tickCounter) -> render(context));
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
    }

    public static boolean isVisible() {
        return visible;
    }

    private static void render(DrawContext context) {
        if (!visible) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.isDead()) {
            // The vanilla death screen handles the moment of death; the cover takes over after respawn.
            return;
        }
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        context.fill(0, 0, width, height, 0xFF000000);

        TextRenderer textRenderer = client.textRenderer;
        // Count down locally between server updates so the number never looks stuck.
        int elapsed = (int) ((System.currentTimeMillis() - lastUpdateMs) / 1000L);
        int seconds = Math.max(0, remainingSeconds - elapsed);
        String title = tr(HunterWildcardText.key("hud.death_wait.title"));
        String countdown = tr(HunterWildcardText.spec("hud.death_wait.countdown", seconds));
        int centerX = width / 2;
        int y = height / 2 - 30;

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX, y);
        matrices.scale(2.0F, 2.0F);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(title), 0, 0, COLOR_TITLE);
        matrices.popMatrix();

        matrices.pushMatrix();
        matrices.translate(centerX, y + 28);
        matrices.scale(1.5F, 1.5F);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(countdown), 0, 0, COLOR_COUNTDOWN);
        matrices.popMatrix();

        if (!line1.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(tr(line1)), centerX, y + 52, COLOR_LINE);
        }
        if (!line2.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(tr(line2)), centerX, y + 64, COLOR_LINE);
        }
    }

    private static String tr(String spec) {
        return HunterWildcardClientText.translate(spec);
    }
}
