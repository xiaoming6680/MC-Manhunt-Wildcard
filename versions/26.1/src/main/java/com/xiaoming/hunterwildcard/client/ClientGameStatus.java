package com.xiaoming.hunterwildcard.client;

import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.ConfigSnapshot;
import com.xiaoming.hunterwildcard.wildcard.WildcardIds;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.SyncConfigPayload;

/**
 * Client-side copy of the latest server sync, kept even while no screen is open so HUDs can read it.
 */
public final class ClientGameStatus {
    private static SyncConfigPayload latest;
    public static com.xiaoming.hunterwildcard.network.HunterWildcardPackets.RoundDetailsPayload details;

    private ClientGameStatus() {
    }

    public static void update(SyncConfigPayload payload) {
        latest = payload;
    }

    public static void clear() {
        latest = null;
        details = null;
    }

    public static SyncConfigPayload latest() {
        return latest;
    }

    public static boolean hasSync() {
        return latest != null;
    }

    public static boolean isWaiting() {
        return latest == null || latest.gameState() == GameState.WAITING;
    }

    public static boolean isWildcardActive(String wildcardId) {
        return latest != null && latest.activeWildcardRunning() && wildcardId.equals(latest.activeWildcard());
    }

    public static boolean canManage() {
        return latest != null && latest.canManage();
    }

    public static boolean isStatusHudToggled() {
        return com.xiaoming.hunterwildcard.client.ui.DisplayPreferences.get.statusHud;
    }

    public static void toggleStatusHud() {
        var prefs=com.xiaoming.hunterwildcard.client.ui.DisplayPreferences.get;
        prefs.statusHud = !prefs.statusHud;
        prefs.save();
    }

    public static int enabledWildcardCount(ConfigSnapshot config) {
        int count = 0;
        for (String id : WildcardIds.ALL) {
            if (config.enabledWildcards().getOrDefault(id, Boolean.TRUE)) {
                count++;
            }
        }
        return count;
    }

    public static int totalWildcardCount() {
        return WildcardIds.ALL.size();
    }
}
