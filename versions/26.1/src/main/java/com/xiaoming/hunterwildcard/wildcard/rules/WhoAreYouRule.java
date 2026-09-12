package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
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
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_NAME);
        }
        team.setNameTagVisibility(Team.Visibility.NEVER);

        for (ServerPlayer player : context.getParticipants()) {
            String holder = player.getScoreboardName();
            PlayerTeam current = scoreboard.getPlayersTeam(holder);
            if (current != null && !current.getName().equals(TEAM_NAME)) {
                previousTeams.put(player.getUUID(), current.getName());
            }
            scoreboard.addPlayerToTeam(holder, team);
        }

        refreshNames(server);
    }

    @Override
    public void onStop(GameContext context) {
        active = false;
        MinecraftServer server = context.getServer();
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String holder = player.getScoreboardName();
                if (scoreboard.getPlayersTeam(holder) == team) {
                    scoreboard.removePlayerFromTeam(holder, team);
                }
                String previous = previousTeams.get(player.getUUID());
                if (previous != null) {
                    PlayerTeam previousTeam = scoreboard.getPlayerTeam(previous);
                    if (previousTeam != null) {
                        scoreboard.addPlayerToTeam(holder, previousTeam);
                    }
                }
            }
            scoreboard.removePlayerTeam(team);
        }
        previousTeams.clear();

        refreshNames(server);
    }

    /** Pushes the (possibly anonymised) tab-list names to every client and refreshes the HUD status sync. */
    private static void refreshNames(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), players);
        for (ServerPlayer player : players) {
            player.connection.send(packet);
        }
        HunterWildcardPackets.syncAll(server);
    }
}
