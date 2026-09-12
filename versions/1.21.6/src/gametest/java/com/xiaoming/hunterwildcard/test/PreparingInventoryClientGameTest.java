package com.xiaoming.hunterwildcard.test;

import com.mojang.authlib.GameProfile;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.test.harness.FabricClientGameTest;
import com.xiaoming.hunterwildcard.test.harness.ClientGameTestContext;
import com.xiaoming.hunterwildcard.test.harness.TestSingleplayerContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import java.util.UUID;

/** Real deaths and respawns during the countdown, with each side's switch independently enabled/disabled. */
public final class PreparingInventoryClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if ("1".equals(System.getenv("HW_WATER_INPUT_TEST_ONLY"))) return;
        if ("1".equals(System.getenv("HW_NETWORK_TEST_ONLY")) || "1".equals(System.getenv("HW_SPECTATE_TEST_ONLY"))) return;
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(creator -> creator.setCheatsEnabled(true)).create()) {
            world.getClientWorld().waitForChunksRender();
            context.waitTicks(20);
            AdvancementResetAssertions.run(world);
            DrawOverlayAssertions.run(context);
            for (PlayerRole role : PlayerRole.values()) {
                for (boolean keep : new boolean[]{true, false}) {
                    world.getServer().runOnServer(server -> {
                        GameManager manager = GameManager.getInstance();
                        manager.stop(server.getCommandSource());
                        ModConfig config = new ModConfig();
                        config.preparingSeconds = 300;
                        config.hunterPrepareBoundaryEnabled = false;
                        config.hunterDeathNoDrops = role == PlayerRole.HUNTER ? keep : !keep;
                        config.runnerDeathNoDrops = role == PlayerRole.RUNNER ? keep : !keep;
                        manager.applyConfig(config);
                        ServerPlayerEntity player = player(server);
                        player.changeGameMode(GameMode.SURVIVAL);
                        manager.join(player, role);
                        // A second roster entry meets the normal start requirement; no fake network connection is needed.
                        ServerPlayerEntity opponent = new ServerPlayerEntity(server, server.getOverworld(),
                                new GameProfile(UUID.randomUUID(), "InventoryTest"), SyncedClientOptions.createDefault());
                        manager.getTeamManager().join(opponent, role == PlayerRole.HUNTER ? PlayerRole.RUNNER : PlayerRole.HUNTER);
                        server.getOverworld().getGameRules().get(GameRules.KEEP_INVENTORY).set(true, server);
                        manager.start(server.getCommandSource());
                        check(manager.getState() == GameState.PREPARING, "Test must remain in preparation");
                        check(!server.getOverworld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY), "Round takes over vanilla inventory rule");
                        player.getInventory().clear();
                        player.getInventory().setStack(0, new ItemStack(Items.DIAMOND, 7));
                        player.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                        player.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                    });
                    context.waitTicks(20);
                    world.getServer().runOnServer(server -> {
                        ServerPlayerEntity player = player(server);
                        // /kill bypasses the short invulnerability window on join and respawn.
                        player.kill(player.getWorld());
                        check(player.isDead(), "Player must actually die");
                        var drops = player.getWorld().getEntitiesByClass(ItemEntity.class,
                                player.getBoundingBox().expand(5), e -> e.getStack().isOf(Items.DIAMOND));
                        check(keep ? drops.isEmpty() : drops.stream().mapToInt(e -> e.getStack().getCount()).sum() == 7,
                                role + " preparing keep=" + keep + ": incorrect death drops");
                        // Remove dropped kits between cases so they cannot be picked up by a later respawn.
                        player.getWorld().getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(5), e -> true)
                                .forEach(ItemEntity::discard);
                    });
                    context.waitTicks(3);
                    context.runOnClient(client -> client.player.requestRespawn());
                    context.waitTicks(10);
                    world.getServer().runOnServer(server -> {
                        ServerPlayerEntity player = player(server);
                        check(!player.isDead(), "Respawn must complete");
                        check(GameManager.getInstance().getState() == GameState.PREPARING, "Countdown must continue");
                        check(player.getInventory().count(Items.DIAMOND) == (keep ? 7 : 0), role + ": main inventory after respawn");
                        check(player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.DIAMOND_HELMET) == keep, role + ": armor after respawn");
                        check(player.getOffHandStack().isOf(Items.SHIELD) == keep, role + ": offhand after respawn");
                    });
                }
            }
            world.getServer().runOnServer(server -> GameManager.getInstance().stop(server.getCommandSource()));
        }
    }

    private static ServerPlayerEntity player(MinecraftServer server) {
        return server.getPlayerManager().getPlayerList().getFirst();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
