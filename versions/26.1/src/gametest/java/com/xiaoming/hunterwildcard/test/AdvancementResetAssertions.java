package com.xiaoming.hunterwildcard.test;

import com.mojang.authlib.GameProfile;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

final class AdvancementResetAssertions {
    static void run(TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            GameManager manager = GameManager.getInstance();
            manager.stop(server.createCommandSourceStack());
            var player = server.getPlayerList().getPlayers().getFirst();
            var tracker = player.getAdvancements();
            AdvancementHolder completed = server.getAdvancements().get(Identifier.withDefaultNamespace("story/mine_stone"));
            AdvancementHolder partial = server.getAdvancements().get(Identifier.withDefaultNamespace("adventure/adventuring_time"));
            check(completed != null && partial != null, "Vanilla test advancements are loaded");
            grantProgress(tracker, completed, partial);

            // Rejected start: no teams yet, so existing player progress must be untouched.
            manager.start(server.createCommandSourceStack());
            check(manager.getState() == GameState.WAITING && tracker.getOrStartProgress(completed).isDone(),
                    "A rejected start must not erase advancements");

            for (int round = 0; round < 2; round++) {
                // Leave the connected player unassigned: all online players includes spectators.
                for (PlayerRole role : PlayerRole.values()) {
                    var member = new ServerPlayer(server, server.overworld(),
                            new GameProfile(UUID.randomUUID(), "Test" + role.name()), ClientInformation.createDefault());
                    manager.getTeamManager().join(member, role);
                }
                manager.start(server.createCommandSourceStack());
                check(manager.getState() == GameState.PREPARING, "Successful start enters preparation");
                check(manager.getTeamManager().getRole(player) == null, "Test player is an unassigned spectator");
                for (AdvancementHolder advancement : server.getAdvancements().getAllAdvancements()) {
                    check(!tracker.getOrStartProgress(advancement).hasProgress(), "Progress survived round start: " + advancement.id());
                }

                // New progress can be earned again; another start command in this round must not clear it.
                grantProgress(tracker, completed, partial);
                manager.start(server.createCommandSourceStack());
                check(tracker.getOrStartProgress(completed).isDone() && tracker.getOrStartProgress(partial).hasProgress(),
                        "Duplicate start during preparation must not reset new progress");
                manager.stop(server.createCommandSourceStack());
            }
        });
    }

    private static void grantProgress(PlayerAdvancements tracker, AdvancementHolder completed, AdvancementHolder partial) {
        for (String criterion : completed.value().criteria().keySet()) tracker.award(completed, criterion);
        // Use a biome other than the one where this test world spawns, and leave the other criteria unfinished.
        tracker.award(partial, "minecraft:badlands");
        check(tracker.getOrStartProgress(completed).isDone(), "Complete advancement was granted");
        check(tracker.getOrStartProgress(partial).hasProgress() && !tracker.getOrStartProgress(partial).isDone(), "Partial progress was granted");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
