package com.xiaoming.hunterwildcard.util;

import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.wildcard.rules.WhoAreYouRule;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerUtil {
    private PlayerUtil() {
    }

    /** Player name as a Text, or the anonymous placeholder while "Who are you?" hides identities. */
    public static Component displayNameText(ServerPlayer player) {
        if (isAnonymous(player)) {
            return HunterWildcardText.translatable("common.anonymous_player");
        }
        return player.getName();
    }

    /** Player name for spec / packet strings; returns the translation key while anonymous so clients localise it. */
    public static String displayNameSpec(ServerPlayer player) {
        if (isAnonymous(player)) {
            return HunterWildcardText.key("common.anonymous_player");
        }
        return player.getName().getString();
    }

    private static boolean isAnonymous(ServerPlayer player) {
        return WhoAreYouRule.isActive() && GameManager.getInstance().isWildcardParticipant(player);
    }

    public static ServerPlayer findNearestRunner(ServerPlayer hunter, List<ServerPlayer> runners) {
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ServerPlayer runner : runners) {
            double distance = distanceSquared(hunter, runner);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = runner;
            }
        }

        return nearest;
    }

    public static ServerPlayer findNearestRunnerInSameWorld(ServerPlayer hunter, List<ServerPlayer> runners) {
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ServerPlayer runner : runners) {
            if (hunter.level() != runner.level()) {
                continue;
            }

            double distance = hunter.distanceToSqr(runner);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = runner;
            }
        }

        return nearest;
    }

    public static double distanceSquared(ServerPlayer a, ServerPlayer b) {
        if (a.level() != b.level()) {
            return Double.MAX_VALUE / 4.0;
        }

        return a.distanceToSqr(b);
    }

    public static int roundDistance(double distance) {
        if (Double.isInfinite(distance) || distance > 1_000_000) {
            return -1;
        }

        return Math.max(0, (int) Math.round(distance));
    }
}
