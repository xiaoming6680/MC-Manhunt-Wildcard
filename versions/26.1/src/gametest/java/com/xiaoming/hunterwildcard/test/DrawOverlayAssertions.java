package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.client.hud.WildcardDrawOverlay;
import com.xiaoming.hunterwildcard.client.hud.CombatFeed;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class DrawOverlayAssertions {
    static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(1920, 1080);
        context.runOnClient(client -> {
            client.options.guiScale().set(3);
            client.setScreen(null);
            WildcardDrawOverlay.setIntro(true, "fragile", "hunterwildcard.wildcard.fragile.description");
            WildcardDrawOverlay.setObjectiveStatus(true, "Survive for 04:35", "runner", "Hunter kills: 4 / 10");
            CombatFeed.kill("HunterLongName123", "RunnerLongName456", 4, 10, false);
            CombatFeed.notice("Respawn", "Runner returned to the chase", "3 lives remaining", "respawn");
        });
        waitMillis(context, 350);
        context.takeScreenshot("hud-layout-wide");
        context.getInput().resizeWindow(1280, 720);
        context.waitTicks(5);
        context.takeScreenshot("draw-objective-before");
        context.runOnClient(client -> WildcardDrawOverlay.start("fragile"));
        waitMillis(context, 350);
        context.takeScreenshot("draw-objective-hidden");
        // Simulate an objective sync arriving during the draw: it should appear only after the animation.
        context.runOnClient(client -> WildcardDrawOverlay.setObjectiveStatus(true, "Survive for 04:30", "runner", "Hunter kills: 5 / 10"));
        waitMillis(context, 5100);
        context.takeScreenshot("draw-objective-restored");
        context.runOnClient(client -> WildcardDrawOverlay.reset());
    }

    private static void waitMillis(ClientGameTestContext context, long millis) {
        long until = System.currentTimeMillis() + millis;
        context.waitFor(client -> System.currentTimeMillis() >= until, 2000);
    }
}
