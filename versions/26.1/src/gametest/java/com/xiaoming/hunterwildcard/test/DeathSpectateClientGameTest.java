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
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
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
        try (TestSingleplayerContext world = c.worldBuilder().adjustSettings(creator -> creator.setAllowCommands(true)).create()) {
            world.getClientLevel().waitForChunksRender();
            c.waitTicks(20);
            world.getServer().runCommand("execute as @a at @s run spawnpoint @s ~ ~ ~");
            world.getServer().runCommand("execute as @a at @s run setworldspawn ~ ~ ~");
            DrawOverlayAssertions.run(c);
            c.getInput().resizeWindow(1280, 720);
            List<ServerPlayer> bots = new ArrayList<>();
            AtomicReference<UUID> firstTarget = new AtomicReference<>();
            AtomicReference<Vec3> originalSpawn = new AtomicReference<>();
            world.getServer().runOnServer(server -> {
                viewerId = server.getPlayerList().getPlayers().getFirst().getUUID();
                var manager = GameManager.getInstance();
                ModConfig config = new ModConfig();
                config.preparingSeconds = 1;
                config.runnerRespawnSeconds = 12;
                config.randomRespawnEnabled = false;
                config.hunterPrepareBoundaryEnabled = false;
                config.enabledWildcards.replaceAll((key, value) -> false);
                manager.applyConfig(config);
                var player = player(server);
                player.setGameMode(GameType.SURVIVAL);
                originalSpawn.set(player.position());
                manager.join(player, PlayerRole.RUNNER);
                bots.add(addBot(server, "TeammateOne", PlayerRole.RUNNER, player.position().add(10, 0, 0)));
                bots.add(addBot(server, "TeammateTwo", PlayerRole.RUNNER, player.position().add(20, 0, 0)));
                bots.add(addBot(server, "EnemyHunter", PlayerRole.HUNTER, player.position().add(30, 0, 0)));
                manager.start(server.createCommandSourceStack());
            });
            c.waitTicks(40);
            world.getServer().runOnServer(server -> {
                check(GameManager.getInstance().getState() == GameState.RUNNING, "Game is running");
                player(server).kill(server.overworld());
            });
            c.waitTicks(10);
            world.getServer().runOnServer(server -> {
                var player = player(server);
                check(player.isAlive() && player.isSpectator(), "Death automatically enters spectator without clicking respawn");
                check(player.getCamera() != player && GameManager.getInstance().getTeamManager().isRunner((ServerPlayer) player.getCamera()), "Initial camera follows a teammate");
                firstTarget.set(player.getCamera().getUUID());
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
                    check(seconds.equals(net.minecraft.client.resources.language.I18n.get("hunterwildcard.screen.time.seconds", 14))
                            && !seconds.contains("hunterwildcard.") && !seconds.contains("\u001f"), "Respawn duration is translated before concatenating member row");
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
            c.waitTicks(2);
            c.takeScreenshot("member-respawn-localized");
            c.runOnClient(client -> client.setScreen(null));
            c.getInput().pressKey(GLFW.GLFW_KEY_X);
            c.waitTicks(5);
            world.getServer().runOnServer(server -> check(!player(server).getCamera().getUUID().equals(firstTarget.get()), "X cycles forward"));
            c.getInput().pressKey(GLFW.GLFW_KEY_Z);
            c.waitTicks(5);
            world.getServer().runOnServer(server -> {
                check(player(server).getCamera().getUUID().equals(firstTarget.get()), "Z cycles backward");
                var target = (ServerPlayer) player(server).getCamera();
                target.teleportTo(server.getLevel(net.minecraft.world.level.Level.NETHER), 100, 100, 100, java.util.Set.of(), 0, 0, true);
            });
            c.waitTicks(30);
            world.getServer().runOnServer(server -> check(player(server).level().dimension() == net.minecraft.world.level.Level.NETHER, "Camera follows teammate across dimensions"));
            c.runOnClient(client -> check(client.level.dimension() == net.minecraft.world.level.Level.NETHER && client.getCameraEntity() != client.player, "Client camera reattaches after dimension change"));
            world.getServer().runOnServer(server -> {
                // A teammate becoming a spectator must be excluded immediately.
                ((ServerPlayer) player(server).getCamera()).setGameMode(GameType.SPECTATOR);
            });
            c.waitTicks(5);
            world.getServer().runOnServer(server -> {
                check(!player(server).getCamera().getUUID().equals(firstTarget.get()), "Unavailable target automatically changes");
                for (var bot : bots) if (GameManager.getInstance().getTeamManager().isRunner(bot)) bot.setGameMode(GameType.SPECTATOR);
            });
            c.waitTicks(5);
            AtomicReference<Vec3> freePosition = new AtomicReference<>();
            world.getServer().runOnServer(server -> {
                var player = player(server);
                check(player.getCamera() == player && player.isSpectator(), "No teammates means free spectator, never enemy camera");
                Vec3 moved = player.position().add(35, 15, 35);
                player.teleportTo(server.overworld(), moved.x, moved.y, moved.z, java.util.Set.of(), 0, 0, true);
                freePosition.set(moved);
            });
            c.waitTicks(10);
            world.getServer().runOnServer(server -> check(player(server).position().distanceTo(freePosition.get()) < 1, "Free spectator is not held at the death point"));
            c.takeScreenshot("death-spectate-free");
            c.waitTicks(230);
            world.getServer().runOnServer(server -> {
                var player = player(server);
                check(player.isAlive() && !player.isSpectator() && player.getCamera() == player, "Original timer restores survival and own camera");
                check(player.position().distanceTo(freePosition.get()) > 20, "Respawn does not use the free camera location");
                check(player.position().distanceTo(originalSpawn.get()) < 10, "Non-random respawn returns to original world spawn");
            });
            c.runOnClient(client -> {
                check(!DeathWaitOverlay.isVisible() && !DeathWaitOverlay.isSpectating(), "Death HUD clears at respawn");
                ClientPlayNetworking.send(new HunterWildcardPackets.CycleDeathSpectatePayload(false));
            });
            c.waitTicks(5);
            world.getServer().runOnServer(server -> {
                check(player(server).getCamera() == player(server), "Living player cannot use death-spectate controls");
                var manager = GameManager.getInstance();
                manager.stop(server.createCommandSourceStack());
                ModConfig config = new ModConfig();
                config.copyFrom(manager.getConfig());
                config.runnerRespawnMode = "INFINITE";
                config.runnerRespawnSeconds = 60;
                manager.applyConfig(config);
                manager.join(player(server), PlayerRole.RUNNER);
                for (int i = 0; i < bots.size(); i++) {
                    bots.get(i).setGameMode(GameType.SURVIVAL);
                    manager.join(bots.get(i), i < 2 ? PlayerRole.RUNNER : PlayerRole.HUNTER);
                }
                manager.start(server.createCommandSourceStack());
            });
            c.waitTicks(40);
            world.getServer().runOnServer(server -> player(server).kill(player(server).level()));
            c.waitTicks(60);
            testCameraTransitions(c, world, bots);
            world.getServer().runOnServer(server -> {
                check(player(server).isSpectator() && player(server).getCamera() != player(server), "Waiting player watches teammates until respawn");
                GameManager.getInstance().cycleDeathSpectate(player(server), false);
                GameManager.getInstance().stop(server.createCommandSourceStack());
                check(!player(server).isSpectator() && player(server).getCamera() == player(server), "Stopping round clears spectator camera");
            });
            c.waitTicks(5);
            c.runOnClient(client -> check(!DeathWaitOverlay.isSpectating() && client.getCameraEntity() == client.player, "Client camera clears when round stops"));
            testEliminatedFreeSpectator(c, world, bots);
            testOverworldRespawns(c, world, bots, originalSpawn.get());
            PearlFrenzyAssertions.run(c, world, viewerId, bots.getFirst().getUUID());
            world.getServer().runOnServer(server -> {
                for (var bot : bots) removeBot(server, bot);
            });
        }
    }

    private void testOverworldRespawns(ClientGameTestContext c, TestSingleplayerContext world, List<ServerPlayer> bots, Vec3 spawn) {
        for (var dimension : List.of(net.minecraft.world.level.Level.NETHER, net.minecraft.world.level.Level.END)) {
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
                        bots.get(i).setGameMode(GameType.SURVIVAL);
                        manager.join(bots.get(i), i < 2 ? PlayerRole.RUNNER : PlayerRole.HUNTER);
                    }
                    manager.start(server.createCommandSourceStack());
                });
                c.waitTicks(40);
                // A forced non-Overworld spawn exercises the respawn-anchor fallback too.
                world.getServer().runCommand("execute in " + dimension.identifier() + " run fill 100 100 100 100 102 100 minecraft:air");
                world.getServer().runCommand("execute in " + dimension.identifier() + " run spawnpoint Player0 100 100 100");
                world.getServer().runOnServer(server -> {
                    var player = player(server);
                    player.setNoGravity(true);
                    player.teleportTo(server.getLevel(dimension), 100.5, 100, 100.5, java.util.Set.of(), 0, 0, true);
                });
                c.waitTicks(30);
                world.getServer().runOnServer(server -> {
                    check(player(server).level().dimension().equals(dimension), "Death happens in requested dimension");
                    player(server).kill(player(server).level());
                });
                c.waitTicks(100);
                world.getServer().runOnServer(server -> {
                    var player = player(server);
                    check(player.isAlive() && !player.isSpectator() && player.getCamera() == player, "Dimension death finishes original respawn timer");
                    check(player.level() == server.overworld(), "Nether/End deaths always respawn in Overworld, random=" + random);
                    check(player.position().distanceTo(spawn) < 60, "Respawn uses Overworld spawn area, expected=" + spawn + ", actual=" + player.position() + ", worldSpawn=" + server.overworld().getRespawnData());
                    GameManager.getInstance().stop(server.createCommandSourceStack());
                });
                c.waitTicks(5);
                c.runOnClient(client -> check(client.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD) && !DeathWaitOverlay.isSpectating(), "Client returns to Overworld with own camera"));
            }
        }
    }

    private void testCameraTransitions(ClientGameTestContext c, TestSingleplayerContext world, List<ServerPlayer> bots) {
        AtomicReference<UUID> expected = new AtomicReference<>();
        for (var dimension : List.of(net.minecraft.world.level.Level.NETHER, net.minecraft.world.level.Level.END, net.minecraft.world.level.Level.OVERWORLD)) {
            world.getServer().runOnServer(server -> {
                var viewer = player(server);
                var target = (ServerPlayer)viewer.getCamera();
                expected.set(target.getUUID());
                Vec3 before = viewer.position();
                var beforeWorld = viewer.level();
                target.teleportTo(server.getLevel(dimension), 520, 160, 520, java.util.Set.of(), 0, 0, true);
                // The hook must run before native camera-follow copies coordinates into the old world.
                GameManager.getInstance().beforeDeathSpectatorTick(viewer);
                check(viewer.getCamera() == viewer && viewer.level() == beforeWorld
                        && viewer.position().equals(before), "Target teleport detaches camera without moving through the old world");
            });
            c.waitTicks(50);
            assertCameraSettled(c, world, expected.get());
            world.getServer().runOnServer(server -> {
                var viewer = player(server);
                int beforeId = teleportId(viewer);
                for(int i=0;i<41;i++)GameManager.getInstance().cycleDeathSpectate(viewer,false);
                check(teleportId(viewer)-beforeId<=1,"Rapid camera cycling sends at most one teleport before confirmation");
                expected.set(bots.stream().filter(bot->bot!=viewer && GameManager.getInstance().getTeamManager().isRunner(bot)
                        && !bot.getUUID().equals(expected.get())).findFirst().orElseThrow().getUUID());
            });
            c.waitTicks(50);
            assertCameraSettled(c, world, expected.get());
        }
        // Same-dimension teleport into new chunks must move the spectator before reattaching too.
        world.getServer().runOnServer(server -> {
            var target=(ServerPlayer)player(server).getCamera();expected.set(target.getUUID());
            target.teleportTo(target.level(), 1040,160,1040,java.util.Set.of(),0,0,true);
        });
        c.waitTicks(50);assertCameraSettled(c,world,expected.get());
        AtomicReference<Integer> stableId=new AtomicReference<>();
        world.getServer().runOnServer(server -> stableId.set(teleportId(player(server))));
        c.getInput().holdKey(options->options.keyShift);c.waitTicks(20);
        world.getServer().runOnServer(server -> check(player(server).getCamera()==player(server)
                && teleportId(player(server))==stableId.get(),"Holding sneak does not create a detach/reattach teleport loop"));
        c.getInput().releaseKey(options->options.keyShift);c.waitTicks(10);
        assertCameraSettled(c,world,expected.get());
        AtomicReference<ServerPlayer> invalidated = new AtomicReference<>();
        world.getServer().runOnServer(server -> {
            var viewer=player(server);
            var next=bots.stream().filter(bot->GameManager.getInstance().getTeamManager().isRunner(bot)&&!bot.getUUID().equals(expected.get())).findFirst().orElseThrow();
            GameManager.getInstance().cycleDeathSpectate(viewer,false);
            next.setGameMode(GameType.SPECTATOR);invalidated.set(next);
        });
        c.waitTicks(40);assertCameraSettled(c,world,expected.get());
        world.getServer().runOnServer(server -> invalidated.get().setGameMode(GameType.SURVIVAL));
        c.waitTicks(5);
        c.takeScreenshot("death-spectate-after-transitions");
    }

    private void assertCameraSettled(ClientGameTestContext c,TestSingleplayerContext world,UUID expected) {
        world.getServer().runOnServer(server -> {
            var viewer=player(server);var target=server.getPlayerList().getPlayer(expected);
            check(viewer.getCamera()==target && viewer.level()==target.level(),"Server camera converges on latest teammate");
            check(((com.xiaoming.hunterwildcard.mixin.TeleportPendingAccessor)viewer.connection).hunterwildcard$pendingTeleport()==null,"Spectator teleport confirmation completes");
        });
        c.runOnClient(client -> check(client.getCameraEntity()!=client.player && client.getCameraEntity()!=null
                && client.getCameraEntity().getUUID().equals(expected) && !client.getCameraEntity().isRemoved()
                && client.getCameraEntity().level()==client.level,"Client camera follows a loaded current-world teammate"));
    }

    private void testEliminatedFreeSpectator(ClientGameTestContext c, TestSingleplayerContext world, List<ServerPlayer> bots) {
        world.getServer().runOnServer(server -> {
            var manager=GameManager.getInstance();var config=new ModConfig();config.copyFrom(manager.getConfig());
            config.runnerRespawnMode="NO_RESPAWN";manager.applyConfig(config);
            manager.join(player(server),PlayerRole.RUNNER);
            for(int i=0;i<bots.size();i++){bots.get(i).setGameMode(GameType.SURVIVAL);manager.join(bots.get(i),i<2?PlayerRole.RUNNER:PlayerRole.HUNTER);}
            manager.start(server.createCommandSourceStack());
        });
        c.waitTicks(40);
        world.getServer().runOnServer(server -> player(server).kill(player(server).level()));c.waitTicks(30);
        world.getServer().runOnServer(server -> check(player(server).isSpectator()&&player(server).getCamera()==player(server),"Eliminated player starts in free spectator despite living teammates"));
        c.runOnClient(client -> check(client.getCameraEntity()==client.player&&DeathWaitOverlay.isSpectating(),"Client elimination camera stays free"));
        var ordered=bots.stream().sorted(java.util.Comparator.comparing(ServerPlayer::getUUID)).toList();
        world.getServer().runOnServer(server -> {
            bots.get(2).teleportTo(server.getLevel(net.minecraft.world.level.Level.NETHER),200,150,200,java.util.Set.of(),0,0,true);
        });
        for(var target:ordered) {
            c.getInput().pressKey(GLFW.GLFW_KEY_X);c.waitTicks(35);
            world.getServer().runOnServer(server -> check(player(server).getCamera()==player(server)
                    && player(server).level()==target.level()
                    && player(server).position().distanceTo(target.position())<2,"Eliminated X visits every team including hunters without attaching camera"));
            c.runOnClient(client -> check(client.getCameraEntity()==client.player,"Visiting a player preserves free client camera"));
        }
        AtomicReference<Vec3> free=new AtomicReference<>();
        world.getServer().runOnServer(server -> {
            var viewer=player(server);free.set(viewer.position().add(30,15,30));
            viewer.teleportTo(viewer.level(),free.get().x,free.get().y,free.get().z,java.util.Set.of(),0,0,true);
            var last=ordered.getLast();last.teleportTo(server.getLevel(net.minecraft.world.level.Level.END),300,150,300,java.util.Set.of(),0,0,true);
        });
        c.waitTicks(30);
        world.getServer().runOnServer(server -> check(player(server).position().distanceTo(free.get())<1&&player(server).getCamera()==player(server),"Visited player movement never drags free spectator back"));
        c.getInput().pressKey(GLFW.GLFW_KEY_Z);c.waitTicks(35);
        world.getServer().runOnServer(server -> check(player(server).level()==ordered.get(ordered.size()-2).level()
                &&player(server).position().distanceTo(ordered.get(ordered.size()-2).position())<2,"Eliminated Z visits previous player"));
        c.takeScreenshot("eliminated-free-spectator");
        world.getServer().runOnServer(server -> GameManager.getInstance().stop(server.createCommandSourceStack()));c.waitTicks(10);
        c.runOnClient(client -> check(!DeathWaitOverlay.isSpectating()&&client.getCameraEntity()==client.player,"Eliminated free spectator clears on round stop"));
    }

    private static int teleportId(ServerPlayer player) {
        try {var field=ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");field.setAccessible(true);return field.getInt(player.connection);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }

    private ServerPlayer player(MinecraftServer server) { return server.getPlayerList().getPlayer(viewerId); }

    private ServerPlayer addBot(MinecraftServer server, String name, PlayerRole role, Vec3 pos) {
        var profile = new GameProfile(UUID.randomUUID(), name);
        var bot = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
        bot.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), bot, CommonListenerCookie.createInitial(profile, false));
        bot.setGameMode(GameType.SURVIVAL);
        bot.setPos(pos);
        bot.setNoGravity(true);
        server.getPlayerList().getPlayers().add(bot);
        playerMap(server).put(bot.getUUID(), bot);
        player(server).connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(bot)));
        server.overworld().addNewPlayer(bot);
        GameManager.getInstance().getTeamManager().join(bot, role);
        return bot;
    }

    private void removeBot(MinecraftServer server, ServerPlayer bot) {
        server.getPlayerList().getPlayers().remove(bot);
        playerMap(server).remove(bot.getUUID());
        bot.level().removePlayerImmediately(bot, Entity.RemovalReason.DISCARDED);
        player(server).connection.send(new ClientboundPlayerInfoRemovePacket(List.of(bot.getUUID())));
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ServerPlayer> playerMap(MinecraftServer server) {
        try {
            var field = PlayerList.class.getDeclaredField("playersByUUID");
            field.setAccessible(true);
            return (Map<UUID, ServerPlayer>) field.get(server.getPlayerList());
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
