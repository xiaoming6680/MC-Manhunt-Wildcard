package com.xiaoming.hunterwildcard.team;

import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

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

    public void join(ServerPlayerEntity player, PlayerRole role) {
        playerRolesById.put(player.getUuid(), role);
        syncScoreboardTeams(player.getEntityWorld().getServer());
    }

    public PlayerRole leave(ServerPlayerEntity player) {
        PlayerRole role = playerRolesById.remove(player.getUuid());
        syncScoreboardTeams(player.getEntityWorld().getServer());
        return role;
    }

    public void remove(UUID playerId) {
        playerRolesById.remove(playerId);
    }

    public void clear() {
        playerRolesById.clear();
    }

    public PlayerRole getRole(ServerPlayerEntity player) {
        return playerRolesById.get(player.getUuid());
    }

    public boolean isHunter(ServerPlayerEntity player) {
        return getRole(player) == PlayerRole.HUNTER;
    }

    public boolean isRunner(ServerPlayerEntity player) {
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

    public List<ServerPlayerEntity> getHunters(MinecraftServer server) {
        return getPlayers(server, PlayerRole.HUNTER);
    }

    public List<ServerPlayerEntity> getRunners(MinecraftServer server) {
        return getPlayers(server, PlayerRole.RUNNER);
    }

    public List<ServerPlayerEntity> getParticipants(MinecraftServer server) {
        List<ServerPlayerEntity> onlinePlayers = new ArrayList<>();
        for (UUID playerId : playerRolesById.keySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
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
        Team hunters = ensureTeam(scoreboard, HUNTER_TEAM, Formatting.RED);
        Team runners = ensureTeam(scoreboard, RUNNER_TEAM, Formatting.BLUE);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String holder = player.getNameForScoreboard();
            Team current = scoreboard.getScoreHolderTeam(holder);
            PlayerRole role = playerRolesById.get(player.getUuid());
            Team wanted = role == PlayerRole.HUNTER ? hunters : role == PlayerRole.RUNNER ? runners : null;
            if (wanted == null) {
                if (current == hunters || current == runners) {
                    scoreboard.removeScoreHolderFromTeam(holder, current);
                }
                continue;
            }
            if (current != wanted && (current == null || current == hunters || current == runners)) {
                scoreboard.addScoreHolderToTeam(holder, wanted);
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
            Team team = scoreboard.getTeam(name);
            if (team != null) {
                scoreboard.removeTeam(team);
            }
        }
    }

    private static Team ensureTeam(Scoreboard scoreboard, String name, Formatting color) {
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.addTeam(name);
        }
        team.setColor(color);
        team.setFriendlyFireAllowed(true);
        team.setCollisionRule(AbstractTeam.CollisionRule.ALWAYS);
        return team;
    }

    private List<ServerPlayerEntity> getPlayers(MinecraftServer server, PlayerRole role) {
        List<ServerPlayerEntity> onlinePlayers = new ArrayList<>();
        for (Map.Entry<UUID, PlayerRole> entry : playerRolesById.entrySet()) {
            if (entry.getValue() != role) {
                continue;
            }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                onlinePlayers.add(player);
            }
        }
        return onlinePlayers;
    }
}
