package com.xiaoming.hunterwildcard.test;

import com.mojang.authlib.GameProfile;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.UUID;

final class AdvancementResetAssertions {
    static void run(TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            GameManager manager = GameManager.getInstance();
            manager.stop(server.getCommandSource());
            var player = server.getPlayerManager().getPlayerList().getFirst();
            var tracker = player.getAdvancementTracker();
            AdvancementEntry completed = server.getAdvancementLoader().get(Identifier.ofVanilla("story/mine_stone"));
            AdvancementEntry partial = server.getAdvancementLoader().get(Identifier.ofVanilla("adventure/adventuring_time"));
            check(completed != null && partial != null, "Vanilla test advancements are loaded");
            grantProgress(tracker, completed, partial);

            // Rejected start: no teams yet, so existing player progress must be untouched.
            manager.start(server.getCommandSource());
            check(manager.getState() == GameState.WAITING && tracker.getProgress(completed).isDone(),
                    "A rejected start must not erase advancements");

            for (int round = 0; round < 2; round++) {
                // Leave the connected player unassigned: all online players includes spectators.
                for (PlayerRole role : PlayerRole.values()) {
                    var member = new ServerPlayerEntity(server, server.getOverworld(),
                            new GameProfile(UUID.randomUUID(), "Test" + role.name()), SyncedClientOptions.createDefault());
                    manager.getTeamManager().join(member, role);
                }
                manager.start(server.getCommandSource());
                check(manager.getState() == GameState.PREPARING, "Successful start enters preparation");
                check(manager.getTeamManager().getRole(player) == null, "Test player is an unassigned spectator");
                for (AdvancementEntry advancement : server.getAdvancementLoader().getAdvancements()) {
                    check(!tracker.getProgress(advancement).isAnyObtained(), "Progress survived round start: " + advancement.id());
                }

                // New progress can be earned again; another start command in this round must not clear it.
                grantProgress(tracker, completed, partial);
                manager.start(server.getCommandSource());
                check(tracker.getProgress(completed).isDone() && tracker.getProgress(partial).isAnyObtained(),
                        "Duplicate start during preparation must not reset new progress");
                manager.stop(server.getCommandSource());
            }
        });
    }

    private static void grantProgress(PlayerAdvancementTracker tracker, AdvancementEntry completed, AdvancementEntry partial) {
        for (String criterion : completed.value().criteria().keySet()) tracker.grantCriterion(completed, criterion);
        // Use a biome other than the one where this test world spawns, and leave the other criteria unfinished.
        tracker.grantCriterion(partial, "minecraft:badlands");
        check(tracker.getProgress(completed).isDone(), "Complete advancement was granted");
        check(tracker.getProgress(partial).isAnyObtained() && !tracker.getProgress(partial).isDone(), "Partial progress was granted");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
