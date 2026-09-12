package com.xiaoming.hunterwildcard.util;

import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.wildcard.rules.WhoAreYouRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class PlayerUtil {
    private PlayerUtil() {
    }

    /** Player name as a Text, or the anonymous placeholder while "Who are you?" hides identities. */
    public static Text displayNameText(ServerPlayerEntity player) {
        if (isAnonymous(player)) {
            return HunterWildcardText.translatable("common.anonymous_player");
        }
        return player.getName();
    }

    /** Player name for spec / packet strings; returns the translation key while anonymous so clients localise it. */
    public static String displayNameSpec(ServerPlayerEntity player) {
        if (isAnonymous(player)) {
            return HunterWildcardText.key("common.anonymous_player");
        }
        return player.getName().getString();
    }

    private static boolean isAnonymous(ServerPlayerEntity player) {
        return WhoAreYouRule.isActive() && GameManager.getInstance().isWildcardParticipant(player);
    }

    public static ServerPlayerEntity findNearestRunner(ServerPlayerEntity hunter, List<ServerPlayerEntity> runners) {
        ServerPlayerEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ServerPlayerEntity runner : runners) {
            double distance = distanceSquared(hunter, runner);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = runner;
            }
        }

        return nearest;
    }

    public static ServerPlayerEntity findNearestRunnerInSameWorld(ServerPlayerEntity hunter, List<ServerPlayerEntity> runners) {
        ServerPlayerEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ServerPlayerEntity runner : runners) {
            if (hunter.getWorld() != runner.getWorld()) {
                continue;
            }

            double distance = hunter.squaredDistanceTo(runner);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = runner;
            }
        }

        return nearest;
    }

    public static double distanceSquared(ServerPlayerEntity a, ServerPlayerEntity b) {
        if (a.getWorld() != b.getWorld()) {
            return Double.MAX_VALUE / 4.0;
        }

        return a.squaredDistanceTo(b);
    }

    public static int roundDistance(double distance) {
        if (Double.isInfinite(distance) || distance > 1_000_000) {
            return -1;
        }

        return Math.max(0, (int) Math.round(distance));
    }
}
