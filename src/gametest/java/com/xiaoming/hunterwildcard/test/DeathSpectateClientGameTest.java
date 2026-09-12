package com.xiaoming.hunterwildcard.test;

import com.mojang.authlib.GameProfile;
import com.xiaoming.hunterwildcard.client.hud.DeathWaitOverlay;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** Real client camera packets and deaths, with server-side teammates registered in the online roster. */
public final class DeathSpectateClientGameTest implements FabricClientGameTest {
    private UUID viewerId;
    @Override
    public void runTest(ClientGameTestContext c) {
        if ("1".equals(System.getenv("HW_WATER_INPUT_TEST_ONLY"))) return;
        if ("1".equals(System.getenv("HW_NETWORK_TEST_ONLY")) || "1".equals(System.getenv("HW_INVENTORY_TEST_ONLY"))) return;
        try (TestSingleplayerContext world = c.worldBuilder().adjustSettings(creator -> creator.setCheatsEnabled(true)).create()) {
            world.getClientWorld().waitForChunksRender();
            c.waitTicks(20);
            world.getServer().runCommand("execute as @a at @s run spawnpoint @s ~ ~ ~");
            world.getServer().runCommand("execute as @a at @s run setworldspawn ~ ~ ~");
            DrawOverlayAssertions.run(c);
            c.getInput().resizeWindow(1280, 720);
            List<ServerPlayerEntity> bots = new ArrayList<>();
            AtomicReference<UUID> firstTarget = new AtomicReference<>();
            AtomicReference<Vec3d> originalSpawn = new AtomicReference<>();
            world.getServer().runOnServer(server -> {
                viewerId = server.getPlayerManager().getPlayerList().getFirst().getUuid();
                var manager = GameManager.getInstance();
                ModConfig config = new ModConfig();
                config.preparingSeconds = 1;
                config.runnerRespawnSeconds = 12;
                config.randomRespawnEnabled = false;
                config.hunterPrepareBoundaryEnabled = false;
                config.enabledWildcards.replaceAll((key, value) -> false);
                manager.applyConfig(config);
                var player = player(server);
                player.changeGameMode(GameMode.SURVIVAL);
                originalSpawn.set(player.getEntityPos());
                manager.join(player, PlayerRole.RUNNER);
                bots.add(addBot(server, "TeammateOne", PlayerRole.RUNNER, player.getEntityPos().add(10, 0, 0)));
                bots.add(addBot(server, "TeammateTwo", PlayerRole.RUNNER, player.getEntityPos().add(20, 0, 0)));
                bots.add(addBot(server, "EnemyHunter", PlayerRole.HUNTER, player.getEntityPos().add(30, 0, 0)));
                manager.start(server.getCommandSource());
            });
            c.waitTicks(40);
            world.getServer().runOnServer(server -> {
                check(GameManager.getInstance().getState() == GameState.RUNNING, "Game is running");
                player(server).kill(server.getOverworld());
            });
            c.waitTicks(10);
            world.getServer().runOnServer(server -> {
                var player = player(server);
                check(player.isAlive() && player.isSpectator(), "Death automatically enters spectator without clicking respawn");
                check(player.getCameraEntity() != player && GameManager.getInstance().getTeamManager().isRunner((ServerPlayerEntity) player.getCameraEntity()), "Initial camera follows a teammate");
                firstTarget.set(player.getCameraEntity().getUuid());
            });
            c.runOnClient(client -> {
                check(DeathWaitOverlay.isVisible() && DeathWaitOverlay.isSpectating(), "Countdown and spectator HUD are active");
                check(client.getCameraEntity() != client.player, "Client camera attached to teammate");
            });
            c.takeScreenshot("death-spectate-teammate");
            c.runOnClient(client -> {
                var screen = new com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen();
                client.setScreen(screen);
                try {
                    var format = screen.getClass().getDeclaredMethod("formatSeconds", int.class);
                    format.setAccessible(true);
                    String seconds = (String) format.invoke(screen, 14);
                    check(seconds.equals(net.minecraft.client.resource.language.I18n.translate("hunterwildcard.screen.time.seconds", 14))
                            && !seconds.contains("hunterwildcard.") && !seconds.contains("\u001f"), "Respawn duration is translated before concatenating member row");
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
            c.waitTicks(2);
            c.takeScreenshot("member-respawn-localized");
            c.runOnClient(client -> client.setScreen(null));
            c.getInput().pressKey(GLFW.GLFW_KEY_X);
            c.waitTicks(5);
            world.getServer().runOnServer(server -> check(!player(server).getCameraEntity().getUuid().equals(firstTarget.get()), "X cycles forward"));
            c.getInput().pressKey(GLFW.GLFW_KEY_Z);
            c.waitTicks(5);
            world.getServer().runOnServer(server -> {
                check(player(server).getCameraEntity().getUuid().equals(firstTarget.get()), "Z cycles backward");
                var target = (ServerPlayerEntity) player(server).getCameraEntity();
                target.teleport(server.getWorld(net.minecraft.world.World.NETHER), 100, 100, 100, java.util.Set.of(), 0, 0, true);
            });
            c.waitTicks(30);
            world.getServer().runOnServer(server -> check(player(server).getEntityWorld().getRegistryKey() == net.minecraft.world.World.NETHER, "Camera follows teammate across dimensions"));
            c.runOnClient(client -> check(client.world.getRegistryKey() == net.minecraft.world.World.NETHER && client.getCameraEntity() != client.player, "Client camera reattaches after dimension change"));
            world.getServer().runOnServer(server -> {
                // A teammate becoming a spectator must be excluded immediately.
                ((ServerPlayerEntity) player(server).getCameraEntity()).changeGameMode(GameMode.SPECTATOR);
            });
            c.waitTicks(5);
            world.getServer().runOnServer(server -> {
                check(!player(server).getCameraEntity().getUuid().equals(firstTarget.get()), "Unavailable target automatically changes");
                for (var bot : bots) if (GameManager.getInstance().getTeamManager().isRunner(bot)) bot.changeGameMode(GameMode.SPECTATOR);
            });
            c.waitTicks(5);
            AtomicReference<Vec3d> freePosition = new AtomicReference<>();
            world.getServer().runOnServer(server -> {
                var player = player(server);
                check(player.getCameraEntity() == player && player.isSpectator(), "No teammates means free spectator, never enemy camera");
                Vec3d moved = player.getEntityPos().add(35, 15, 35);
                player.teleport(server.getOverworld(), moved.x, moved.y, moved.z, java.util.Set.of(), 0, 0, true);
                freePosition.set(moved);
            });
            c.waitTicks(10);
            world.getServer().runOnServer(server -> check(player(server).getEntityPos().distanceTo(freePosition.get()) < 1, "Free spectator is not held at the death point"));
            c.takeScreenshot("death-spectate-free");
            c.waitTicks(230);
            world.getServer().runOnServer(server -> {
                var player = player(server);
                check(player.isAlive() && !player.isSpectator() && player.getCameraEntity() == player, "Original timer restores survival and own camera");
                check(player.getEntityPos().distanceTo(freePosition.get()) > 20, "Respawn does not use the free camera location");
                check(player.getEntityPos().distanceTo(originalSpawn.get()) < 10, "Non-random respawn returns to original world spawn");
            });
            c.runOnClient(client -> {
                check(!DeathWaitOverlay.isVisible() && !DeathWaitOverlay.isSpectating(), "Death HUD clears at respawn");
                ClientPlayNetworking.send(new HunterWildcardPackets.CycleDeathSpectatePayload(false));
            });
            c.waitTicks(5);
            world.getServer().runOnServer(server -> {
                check(player(server).getCameraEntity() == player(server), "Living player cannot use death-spectate controls");
                var manager = GameManager.getInstance();
                manager.stop(server.getCommandSource());
                ModConfig config = new ModConfig();
                config.copyFrom(manager.getConfig());
                config.runnerRespawnMode = "INFINITE";
                config.runnerRespawnSeconds = 60;
                manager.applyConfig(config);
                manager.join(player(server), PlayerRole.RUNNER);
                for (int i = 0; i < bots.size(); i++) {
                    bots.get(i).changeGameMode(GameMode.SURVIVAL);
                    manager.join(bots.get(i), i < 2 ? PlayerRole.RUNNER : PlayerRole.HUNTER);
                }
                manager.start(server.getCommandSource());
            });
            c.waitTicks(40);
            world.getServer().runOnServer(server -> player(server).kill(player(server).getEntityWorld()));
            c.waitTicks(60);
            testCameraTransitions(c, world, bots);
            world.getServer().runOnServer(server -> {
                check(player(server).isSpectator() && player(server).getCameraEntity() != player(server), "Waiting player watches teammates until respawn");
                GameManager.getInstance().cycleDeathSpectate(player(server), false);
                GameManager.getInstance().stop(server.getCommandSource());
                check(!player(server).isSpectator() && player(server).getCameraEntity() == player(server), "Stopping round clears spectator camera");
            });
            c.waitTicks(5);
            c.runOnClient(client -> check(!DeathWaitOverlay.isSpectating() && client.getCameraEntity() == client.player, "Client camera clears when round stops"));
            testEliminatedFreeSpectator(c, world, bots);
            testOverworldRespawns(c, world, bots, originalSpawn.get());
            PearlFrenzyAssertions.run(c, world, viewerId, bots.getFirst().getUuid());
            world.getServer().runOnServer(server -> {
                for (var bot : bots) removeBot(server, bot);
            });
        }
    }

    private void testOverworldRespawns(ClientGameTestContext c, TestSingleplayerContext world, List<ServerPlayerEntity> bots, Vec3d spawn) {
        for (var dimension : List.of(net.minecraft.world.World.NETHER, net.minecraft.world.World.END)) {
            for (boolean random : List.of(false, true)) {
                world.getServer().runOnServer(server -> {
                    var manager = GameManager.getInstance();
                    ModConfig config = new ModConfig();
                    config.preparingSeconds = 1;
                    config.runnerRespawnMode = "INFINITE";
                    config.runnerRespawnSeconds = 3;
                    config.runnerRespawnDistance = 12;
                    config.randomRespawnEnabled = random;
                    config.hunterPrepareBoundaryEnabled = false;
                    config.enabledWildcards.replaceAll((key, value) -> false);
                    manager.applyConfig(config);
                    manager.join(player(server), PlayerRole.RUNNER);
                    for (int i = 0; i < bots.size(); i++) {
                        bots.get(i).changeGameMode(GameMode.SURVIVAL);
                        manager.join(bots.get(i), i < 2 ? PlayerRole.RUNNER : PlayerRole.HUNTER);
                    }
                    manager.start(server.getCommandSource());
                });
                c.waitTicks(40);
                // A forced non-Overworld spawn exercises the respawn-anchor fallback too.
                world.getServer().runCommand("execute in " + dimension.getValue() + " run fill 100 100 100 100 102 100 minecraft:air");
                world.getServer().runCommand("execute in " + dimension.getValue() + " run spawnpoint Player0 100 100 100");
                world.getServer().runOnServer(server -> {
                    var player = player(server);
                    player.setNoGravity(true);
                    player.teleport(server.getWorld(dimension), 100.5, 100, 100.5, java.util.Set.of(), 0, 0, true);
                });
                c.waitTicks(30);
                world.getServer().runOnServer(server -> {
                    check(player(server).getEntityWorld().getRegistryKey().equals(dimension), "Death happens in requested dimension");
                    player(server).kill(player(server).getEntityWorld());
                });
                c.waitTicks(100);
                world.getServer().runOnServer(server -> {
                    var player = player(server);
                    check(player.isAlive() && !player.isSpectator() && player.getCameraEntity() == player, "Dimension death finishes original respawn timer");
                    check(player.getEntityWorld() == server.getOverworld(), "Nether/End deaths always respawn in Overworld, random=" + random);
                    check(player.getEntityPos().distanceTo(spawn) < 60, "Respawn uses Overworld spawn area, expected=" + spawn + ", actual=" + player.getEntityPos() + ", worldSpawn=" + server.getOverworld().getSpawnPoint());
                    GameManager.getInstance().stop(server.getCommandSource());
                });
                c.waitTicks(5);
                c.runOnClient(client -> check(client.world.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD) && !DeathWaitOverlay.isSpectating(), "Client returns to Overworld with own camera"));
            }
        }
    }

    private void testCameraTransitions(ClientGameTestContext c, TestSingleplayerContext world, List<ServerPlayerEntity> bots) {
        AtomicReference<UUID> expected = new AtomicReference<>();
        for (var dimension : List.of(net.minecraft.world.World.NETHER, net.minecraft.world.World.END, net.minecraft.world.World.OVERWORLD)) {
            world.getServer().runOnServer(server -> {
                var viewer = player(server);
                var target = (ServerPlayerEntity)viewer.getCameraEntity();
                expected.set(target.getUuid());
                Vec3d before = viewer.getEntityPos();
                var beforeWorld = viewer.getEntityWorld();
                target.teleport(server.getWorld(dimension), 520, 160, 520, java.util.Set.of(), 0, 0, true);
                // The hook must run before native camera-follow copies coordinates into the old world.
                GameManager.getInstance().beforeDeathSpectatorTick(viewer);
                check(viewer.getCameraEntity() == viewer && viewer.getEntityWorld() == beforeWorld
                        && viewer.getEntityPos().equals(before), "Target teleport detaches camera without moving through the old world");
            });
            c.waitTicks(50);
            assertCameraSettled(c, world, expected.get());
            world.getServer().runOnServer(server -> {
                var viewer = player(server);
                int beforeId = teleportId(viewer);
                for(int i=0;i<41;i++)GameManager.getInstance().cycleDeathSpectate(viewer,false);
                check(teleportId(viewer)-beforeId<=1,"Rapid camera cycling sends at most one teleport before confirmation");
                expected.set(bots.stream().filter(bot->bot!=viewer && GameManager.getInstance().getTeamManager().isRunner(bot)
                        && !bot.getUuid().equals(expected.get())).findFirst().orElseThrow().getUuid());
            });
            c.waitTicks(50);
            assertCameraSettled(c, world, expected.get());
        }
        // Same-dimension teleport into new chunks must move the spectator before reattaching too.
        world.getServer().runOnServer(server -> {
            var target=(ServerPlayerEntity)player(server).getCameraEntity();expected.set(target.getUuid());
            target.teleport(target.getEntityWorld(), 1040,160,1040,java.util.Set.of(),0,0,true);
        });
        c.waitTicks(50);assertCameraSettled(c,world,expected.get());
        AtomicReference<Integer> stableId=new AtomicReference<>();
        world.getServer().runOnServer(server -> stableId.set(teleportId(player(server))));
        c.getInput().holdKey(options->options.sneakKey);c.waitTicks(20);
        world.getServer().runOnServer(server -> check(player(server).getCameraEntity()==player(server)
                && teleportId(player(server))==stableId.get(),"Holding sneak does not create a detach/reattach teleport loop"));
        c.getInput().releaseKey(options->options.sneakKey);c.waitTicks(10);
        assertCameraSettled(c,world,expected.get());
        AtomicReference<ServerPlayerEntity> invalidated = new AtomicReference<>();
        world.getServer().runOnServer(server -> {
            var viewer=player(server);
            var next=bots.stream().filter(bot->GameManager.getInstance().getTeamManager().isRunner(bot)&&!bot.getUuid().equals(expected.get())).findFirst().orElseThrow();
            GameManager.getInstance().cycleDeathSpectate(viewer,false);
            next.changeGameMode(GameMode.SPECTATOR);invalidated.set(next);
        });
        c.waitTicks(40);assertCameraSettled(c,world,expected.get());
        world.getServer().runOnServer(server -> invalidated.get().changeGameMode(GameMode.SURVIVAL));
        c.waitTicks(5);
        c.takeScreenshot("death-spectate-after-transitions");
    }

    private void assertCameraSettled(ClientGameTestContext c,TestSingleplayerContext world,UUID expected) {
        world.getServer().runOnServer(server -> {
            var viewer=player(server);var target=server.getPlayerManager().getPlayer(expected);
            check(viewer.getCameraEntity()==target && viewer.getEntityWorld()==target.getEntityWorld(),"Server camera converges on latest teammate");
            check(((com.xiaoming.hunterwildcard.mixin.TeleportPendingAccessor)viewer.networkHandler).hunterwildcard$pendingTeleport()==null,"Spectator teleport confirmation completes");
        });
        c.runOnClient(client -> check(client.getCameraEntity()!=client.player && client.getCameraEntity()!=null
                && client.getCameraEntity().getUuid().equals(expected) && !client.getCameraEntity().isRemoved()
                && client.getCameraEntity().getEntityWorld()==client.world,"Client camera follows a loaded current-world teammate"));
    }

    private void testEliminatedFreeSpectator(ClientGameTestContext c, TestSingleplayerContext world, List<ServerPlayerEntity> bots) {
        world.getServer().runOnServer(server -> {
            var manager=GameManager.getInstance();var config=new ModConfig();config.copyFrom(manager.getConfig());
            config.runnerRespawnMode="NO_RESPAWN";manager.applyConfig(config);
            manager.join(player(server),PlayerRole.RUNNER);
            for(int i=0;i<bots.size();i++){bots.get(i).changeGameMode(GameMode.SURVIVAL);manager.join(bots.get(i),i<2?PlayerRole.RUNNER:PlayerRole.HUNTER);}
            manager.start(server.getCommandSource());
        });
        c.waitTicks(40);
        world.getServer().runOnServer(server -> player(server).kill(player(server).getEntityWorld()));c.waitTicks(30);
        world.getServer().runOnServer(server -> check(player(server).isSpectator()&&player(server).getCameraEntity()==player(server),"Eliminated player starts in free spectator despite living teammates"));
        c.runOnClient(client -> check(client.getCameraEntity()==client.player&&DeathWaitOverlay.isSpectating(),"Client elimination camera stays free"));
        var ordered=bots.stream().sorted(java.util.Comparator.comparing(ServerPlayerEntity::getUuid)).toList();
        world.getServer().runOnServer(server -> {
            bots.get(2).teleport(server.getWorld(net.minecraft.world.World.NETHER),200,150,200,java.util.Set.of(),0,0,true);
        });
        for(var target:ordered) {
            c.getInput().pressKey(GLFW.GLFW_KEY_X);c.waitTicks(35);
            world.getServer().runOnServer(server -> check(player(server).getCameraEntity()==player(server)
                    && player(server).getEntityWorld()==target.getEntityWorld()
                    && player(server).getEntityPos().distanceTo(target.getEntityPos())<2,"Eliminated X visits every team including hunters without attaching camera"));
            c.runOnClient(client -> check(client.getCameraEntity()==client.player,"Visiting a player preserves free client camera"));
        }
        AtomicReference<Vec3d> free=new AtomicReference<>();
        world.getServer().runOnServer(server -> {
            var viewer=player(server);free.set(viewer.getEntityPos().add(30,15,30));
            viewer.teleport(viewer.getEntityWorld(),free.get().x,free.get().y,free.get().z,java.util.Set.of(),0,0,true);
            var last=ordered.getLast();last.teleport(server.getWorld(net.minecraft.world.World.END),300,150,300,java.util.Set.of(),0,0,true);
        });
        c.waitTicks(30);
        world.getServer().runOnServer(server -> check(player(server).getEntityPos().distanceTo(free.get())<1&&player(server).getCameraEntity()==player(server),"Visited player movement never drags free spectator back"));
        c.getInput().pressKey(GLFW.GLFW_KEY_Z);c.waitTicks(35);
        world.getServer().runOnServer(server -> check(player(server).getEntityWorld()==ordered.get(ordered.size()-2).getEntityWorld()
                &&player(server).getEntityPos().distanceTo(ordered.get(ordered.size()-2).getEntityPos())<2,"Eliminated Z visits previous player"));
        c.takeScreenshot("eliminated-free-spectator");
        world.getServer().runOnServer(server -> GameManager.getInstance().stop(server.getCommandSource()));c.waitTicks(10);
        c.runOnClient(client -> check(!DeathWaitOverlay.isSpectating()&&client.getCameraEntity()==client.player,"Eliminated free spectator clears on round stop"));
    }

    private static int teleportId(ServerPlayerEntity player) {
        try {var field=ServerPlayNetworkHandler.class.getDeclaredField("requestedTeleportId");field.setAccessible(true);return field.getInt(player.networkHandler);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }

    private ServerPlayerEntity player(MinecraftServer server) { return server.getPlayerManager().getPlayer(viewerId); }

    private ServerPlayerEntity addBot(MinecraftServer server, String name, PlayerRole role, Vec3d pos) {
        var profile = new GameProfile(UUID.randomUUID(), name);
        var bot = new ServerPlayerEntity(server, server.getOverworld(), profile, SyncedClientOptions.createDefault());
        bot.networkHandler = new ServerPlayNetworkHandler(server, new ClientConnection(NetworkSide.SERVERBOUND), bot, ConnectedClientData.createDefault(profile, false));
        bot.changeGameMode(GameMode.SURVIVAL);
        bot.setPosition(pos);
        bot.setNoGravity(true);
        server.getPlayerManager().getPlayerList().add(bot);
        playerMap(server).put(bot.getUuid(), bot);
        player(server).networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(List.of(bot)));
        server.getOverworld().onPlayerConnected(bot);
        GameManager.getInstance().getTeamManager().join(bot, role);
        return bot;
    }

    private void removeBot(MinecraftServer server, ServerPlayerEntity bot) {
        server.getPlayerManager().getPlayerList().remove(bot);
        playerMap(server).remove(bot.getUuid());
        bot.getEntityWorld().removePlayer(bot, Entity.RemovalReason.DISCARDED);
        player(server).networkHandler.sendPacket(new PlayerRemoveS2CPacket(List.of(bot.getUuid())));
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ServerPlayerEntity> playerMap(MinecraftServer server) {
        try {
            var field = PlayerManager.class.getDeclaredField("playerMap");
            field.setAccessible(true);
            return (Map<UUID, ServerPlayerEntity>) field.get(server.getPlayerManager());
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
