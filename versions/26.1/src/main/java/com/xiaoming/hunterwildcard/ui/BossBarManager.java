package com.xiaoming.hunterwildcard.ui;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

public class BossBarManager {
    private ServerBossEvent prepareBar;
    private ServerBossEvent wildcardBar;

    public void updatePrepareBar(GameContext context, int remainingTicks, int totalTicks) {
        if (prepareBar == null) {
            prepareBar = new ServerBossEvent(java.util.UUID.randomUUID(), HunterWildcardText.translatable("hud.bossbar.prepare.title"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        }

        prepareBar.removeAllPlayers();
        for (ServerPlayer player : context.getParticipants()) {
            prepareBar.addPlayer(player);
        }

        int seconds = Math.max(0, (remainingTicks + 19) / 20);
        float percent = totalTicks <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, remainingTicks / (float) totalTicks));
        prepareBar.setName(HunterWildcardText.translatable("hud.bossbar.prepare.countdown", seconds));
        prepareBar.setProgress(percent);
        prepareBar.setVisible(true);
    }

    public void updateWildcardBar(GameContext context, String ruleId, int remainingTicks, int totalTicks) {
        if (wildcardBar == null) {
            wildcardBar = new ServerBossEvent(java.util.UUID.randomUUID(), HunterWildcardText.translatable("hud.bossbar.wildcard.title"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        }

        wildcardBar.removeAllPlayers();
        for (ServerPlayer player : context.getParticipants()) {
            wildcardBar.addPlayer(player);
        }

        int seconds = Math.max(0, remainingTicks / 20);
        float percent = totalTicks <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, remainingTicks / (float) totalTicks));
        wildcardBar.setName(HunterWildcardText.translatable("hud.bossbar.wildcard.countdown", HunterWildcardText.wildcardName(ruleId), seconds));
        wildcardBar.setProgress(percent);
        wildcardBar.setVisible(true);
    }

    public void clearPrepareBar() {
        if (prepareBar != null) {
            prepareBar.removeAllPlayers();
            prepareBar.setVisible(false);
        }
    }

    public void clearWildcardBar() {
        if (wildcardBar != null) {
            wildcardBar.removeAllPlayers();
            wildcardBar.setVisible(false);
        }
    }

    public void clear() {
        clearPrepareBar();
        clearWildcardBar();
    }
}
