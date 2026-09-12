package com.xiaoming.hunterwildcard.game;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.team.TeamManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class GameContext {
    private final MinecraftServer server;
    private final ModConfig config;
    private final TeamManager teamManager;
    private final Random random;
    private final List<ServerPlayer> extraParticipants;

    public GameContext(MinecraftServer server, ModConfig config, TeamManager teamManager, Random random) {
        this(server, config, teamManager, random, List.of());
    }

    public GameContext(MinecraftServer server, ModConfig config, TeamManager teamManager, Random random, List<ServerPlayer> extraParticipants) {
        this.server = server;
        this.config = config;
        this.teamManager = teamManager;
        this.random = random;
        this.extraParticipants = extraParticipants;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ModConfig getConfig() {
        return config;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public Random getRandom() {
        return random;
    }

    public List<ServerPlayer> getParticipants() {
        if (extraParticipants.isEmpty()) {
            return teamManager.getParticipants(server);
        }

        List<ServerPlayer> participants = new ArrayList<>(teamManager.getParticipants(server));
        Set<UUID> seen = new HashSet<>();
        for (ServerPlayer participant : participants) {
            seen.add(participant.getUUID());
        }
        for (ServerPlayer extraParticipant : extraParticipants) {
            if (extraParticipant != null && seen.add(extraParticipant.getUUID())) {
                participants.add(extraParticipant);
            }
        }
        return participants;
    }

    public List<ServerPlayer> getHunters() {
        return teamManager.getHunters(server);
    }

    public List<ServerPlayer> getRunners() {
        return teamManager.getRunners(server);
    }
}
