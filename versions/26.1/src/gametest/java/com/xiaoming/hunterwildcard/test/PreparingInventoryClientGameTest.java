package com.xiaoming.hunterwildcard.test;

import com.mojang.authlib.GameProfile;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import java.util.UUID;

/** Real deaths and respawns during the countdown, with each side's switch independently enabled/disabled. */
public final class PreparingInventoryClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if ("1".equals(System.getenv("HW_WATER_INPUT_TEST_ONLY"))) return;
        if ("1".equals(System.getenv("HW_NETWORK_TEST_ONLY")) || "1".equals(System.getenv("HW_SPECTATE_TEST_ONLY"))) return;
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(creator -> creator.setAllowCommands(true)).create()) {
            world.getClientLevel().waitForChunksRender();
            context.waitTicks(20);
            AdvancementResetAssertions.run(world);
            DrawOverlayAssertions.run(context);
            for (PlayerRole role : PlayerRole.values()) {
                for (boolean keep : new boolean[]{true, false}) {
                    world.getServer().runOnServer(server -> {
                        GameManager manager = GameManager.getInstance();
                        manager.stop(server.createCommandSourceStack());
                        ModConfig config = new ModConfig();
                        config.preparingSeconds = 300;
                        config.hunterPrepareBoundaryEnabled = false;
                        config.hunterDeathNoDrops = role == PlayerRole.HUNTER ? keep : !keep;
                        config.runnerDeathNoDrops = role == PlayerRole.RUNNER ? keep : !keep;
                        manager.applyConfig(config);
                        ServerPlayer player = player(server);
                        player.setGameMode(GameType.SURVIVAL);
                        manager.join(player, role);
                        // A second roster entry meets the normal start requirement; no fake network connection is needed.
                        ServerPlayer opponent = new ServerPlayer(server, server.overworld(),
                                new GameProfile(UUID.randomUUID(), "InventoryTest"), ClientInformation.createDefault());
                        manager.getTeamManager().join(opponent, role == PlayerRole.HUNTER ? PlayerRole.RUNNER : PlayerRole.HUNTER);
                        server.overworld().getGameRules().set(GameRules.KEEP_INVENTORY, true, server);
                        manager.start(server.createCommandSourceStack());
                        check(manager.getState() == GameState.PREPARING, "Test must remain in preparation");
                        check(!server.overworld().getGameRules().get(GameRules.KEEP_INVENTORY), "Round takes over vanilla inventory rule");
                        player.getInventory().clearContent();
                        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
                        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                    });
                    context.waitTicks(20);
                    world.getServer().runOnServer(server -> {
                        ServerPlayer player = player(server);
                        // /kill bypasses the short invulnerability window on join and respawn.
                        player.kill(player.level());
                        check(player.isDeadOrDying(), "Player must actually die");
                        var drops = player.level().getEntitiesOfClass(ItemEntity.class,
                                player.getBoundingBox().inflate(5), e -> e.getItem().is(Items.DIAMOND));
                        check(keep ? drops.isEmpty() : drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 7,
                                role + " preparing keep=" + keep + ": incorrect death drops");
                        // Remove dropped kits between cases so they cannot be picked up by a later respawn.
                        player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(5), e -> true)
                                .forEach(ItemEntity::discard);
                    });
                    context.waitTicks(3);
                    context.runOnClient(client -> client.player.respawn());
                    context.waitTicks(10);
                    world.getServer().runOnServer(server -> {
                        ServerPlayer player = player(server);
                        check(!player.isDeadOrDying(), "Respawn must complete");
                        check(GameManager.getInstance().getState() == GameState.PREPARING, "Countdown must continue");
                        check(player.getInventory().countItem(Items.DIAMOND) == (keep ? 7 : 0), role + ": main inventory after respawn");
                        check(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET) == keep, role + ": armor after respawn");
                        check(player.getOffhandItem().is(Items.SHIELD) == keep, role + ": offhand after respawn");
                    });
                }
            }
            world.getServer().runOnServer(server -> GameManager.getInstance().stop(server.createCommandSourceStack()));
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
