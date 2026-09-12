package com.xiaoming.hunterwildcard.team;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who is on which side, mirrored into two scoreboard teams so hunters read red and runners blue in
 * chat, the tab list, name tags and the locator bar.
 */
public class TeamManager {
    public static final String HUNTER_TEAM = "hw_hunters";
    public static final String RUNNER_TEAM = "hw_runners";

    private final Map<UUID, PlayerRole> playerRolesById = new HashMap<>();

    public void join(ServerPlayer player, PlayerRole role) {
        playerRolesById.put(player.getUUID(), role);
        syncScoreboardTeams(player.level().getServer());
    }

    public PlayerRole leave(ServerPlayer player) {
        PlayerRole role = playerRolesById.remove(player.getUUID());
        syncScoreboardTeams(player.level().getServer());
        return role;
    }

    public void remove(UUID playerId) {
        playerRolesById.remove(playerId);
    }

    public void clear() {
        playerRolesById.clear();
    }

    public PlayerRole getRole(ServerPlayer player) {
        return playerRolesById.get(player.getUUID());
    }

    public boolean isHunter(ServerPlayer player) {
        return getRole(player) == PlayerRole.HUNTER;
    }

    public boolean isRunner(ServerPlayer player) {
        return getRole(player) == PlayerRole.RUNNER;
    }

    public int count(PlayerRole role) {
        int count = 0;
        for (PlayerRole assignedRole : playerRolesById.values()) {
            if (assignedRole == role) {
                count++;
            }
        }
        return count;
    }

    public List<ServerPlayer> getHunters(MinecraftServer server) {
        return getPlayers(server, PlayerRole.HUNTER);
    }

    public List<ServerPlayer> getRunners(MinecraftServer server) {
        return getPlayers(server, PlayerRole.RUNNER);
    }

    public List<ServerPlayer> getParticipants(MinecraftServer server) {
        List<ServerPlayer> onlinePlayers = new ArrayList<>();
        for (UUID playerId : playerRolesById.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                onlinePlayers.add(player);
            }
        }
        return onlinePlayers;
    }

    /** Makes the scoreboard teams match the current roles (creating them on demand). */
    public void syncScoreboardTeams(MinecraftServer server) {
        if (server == null) {
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam hunters = ensureTeam(scoreboard, HUNTER_TEAM, ChatFormatting.RED);
        PlayerTeam runners = ensureTeam(scoreboard, RUNNER_TEAM, ChatFormatting.BLUE);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String holder = player.getScoreboardName();
            PlayerTeam current = scoreboard.getPlayersTeam(holder);
            PlayerRole role = playerRolesById.get(player.getUUID());
            PlayerTeam wanted = role == PlayerRole.HUNTER ? hunters : role == PlayerRole.RUNNER ? runners : null;
            if (wanted == null) {
                if (current == hunters || current == runners) {
                    scoreboard.removePlayerFromTeam(holder, current);
                }
                continue;
            }
            if (current != wanted && (current == null || current == hunters || current == runners)) {
                scoreboard.addPlayerToTeam(holder, wanted);
            }
        }
    }

    /** Removes the mod's scoreboard teams entirely (end of round / server stop). */
    public void clearScoreboardTeams(MinecraftServer server) {
        if (server == null) {
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        for (String name : new String[] {HUNTER_TEAM, RUNNER_TEAM}) {
            PlayerTeam team = scoreboard.getPlayerTeam(name);
            if (team != null) {
                scoreboard.removePlayerTeam(team);
            }
        }
    }

    private static PlayerTeam ensureTeam(Scoreboard scoreboard, String name, ChatFormatting color) {
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) {
            team = scoreboard.addPlayerTeam(name);
        }
        team.setColor(color);
        team.setAllowFriendlyFire(true);
        team.setCollisionRule(Team.CollisionRule.ALWAYS);
        return team;
    }

    private List<ServerPlayer> getPlayers(MinecraftServer server, PlayerRole role) {
        List<ServerPlayer> onlinePlayers = new ArrayList<>();
        for (Map.Entry<UUID, PlayerRole> entry : playerRolesById.entrySet()) {
            if (entry.getValue() != role) {
                continue;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                onlinePlayers.add(player);
            }
        }
        return onlinePlayers;
    }
}
