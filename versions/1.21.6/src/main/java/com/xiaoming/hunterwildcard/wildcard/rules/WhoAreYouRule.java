package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Who are you?": every participant looks like Steve, name tags are hidden and every name reads as "Player".
 * <p>
 * Server side this rule owns a scoreboard team (name tags off) and forces the display / tab-list name via mixins.
 * The client swaps skins for the default Steve while the wildcard is reported active in the status sync.
 */
public class WhoAreYouRule implements WildcardRule {
    public static final String ID = "who_are_you";
    private static final String TEAM_NAME = "hw_anonymous";

    private static boolean active;

    private final Map<UUID, String> previousTeams = new HashMap<>();

    public static boolean isActive() {
        return active;
    }

    @Override
    public void onStart(GameContext context) {
        active = true;
        previousTeams.clear();
        MinecraftServer server = context.getServer();
        Scoreboard scoreboard = server.getScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.addTeam(TEAM_NAME);
        }
        team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);

        for (ServerPlayerEntity player : context.getParticipants()) {
            String holder = player.getNameForScoreboard();
            Team current = scoreboard.getScoreHolderTeam(holder);
            if (current != null && !current.getName().equals(TEAM_NAME)) {
                previousTeams.put(player.getUuid(), current.getName());
            }
            scoreboard.addScoreHolderToTeam(holder, team);
        }

        refreshNames(server);
    }

    @Override
    public void onStop(GameContext context) {
        active = false;
        MinecraftServer server = context.getServer();
        Scoreboard scoreboard = server.getScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                String holder = player.getNameForScoreboard();
                if (scoreboard.getScoreHolderTeam(holder) == team) {
                    scoreboard.removeScoreHolderFromTeam(holder, team);
                }
                String previous = previousTeams.get(player.getUuid());
                if (previous != null) {
                    Team previousTeam = scoreboard.getTeam(previous);
                    if (previousTeam != null) {
                        scoreboard.addScoreHolderToTeam(holder, previousTeam);
                    }
                }
            }
            scoreboard.removeTeam(team);
        }
        previousTeams.clear();

        refreshNames(server);
    }

    /** Pushes the (possibly anonymised) tab-list names to every client and refreshes the HUD status sync. */
    private static void refreshNames(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        PlayerListS2CPacket packet = new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME), players);
        for (ServerPlayerEntity player : players) {
            player.networkHandler.sendPacket(packet);
        }
        HunterWildcardPackets.syncAll(server);
    }
}
