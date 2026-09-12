package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.client.ClientGameStatus;
import com.xiaoming.hunterwildcard.client.ui.ConfigDraft;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.UnknownCustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Set;

/** Exercises the real Fabric-patched packet decoder; local integrated-server sends can bypass it. */
public final class PacketProtocolClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if ("1".equals(System.getenv("HW_WATER_INPUT_TEST_ONLY"))) return;
        if ("1".equals(System.getenv("HW_INVENTORY_TEST_ONLY")) || "1".equals(System.getenv("HW_SPECTATE_TEST_ONLY"))) return;
        ConfigSnapshot base = ConfigSnapshot.from(new ModConfig());
        ModConfig changed = base.toConfig();
        changed.environmentKillsEnabled = !changed.environmentKillsEnabled;
        changed.targetDimension = "minecraft:the_nether";
        changed.targetItemId = "minecraft:ender_pearl";
        changed.targetX = -12345;
        changed.blazeRodChanceEnabled = true;
        changed.blazeRodChancePercent = 87;
        changed.enabledWildcards.put("backrooms", false);
        ConfigSnapshot edited = ConfigSnapshot.from(changed);
        for (ConfigSnapshot config : List.of(base, edited)) {
            roundTripS2C(new SyncConfigPayload(GameState.WAITING, 2, 3, "地动山摇", "HUNTER",
                    true, false, 17, 0, 120, true, false, config));
            roundTrip(UpdateConfigPayload.CODEC, new UpdateConfigPayload(config, base, 123456789L));
        }
        roundTripS2C(new OperationResultPayload(true, "hunterwildcard.msg.config.saved", 123456789L));
        roundTripS2C(new OperationResultPayload(false, "hunterwildcard.ui.save.conflict", 987654321L));
        roundTripS2C(new WorldTiltPayload(true, 60, 0.7, -0.3));
        roundTripS2C(new DeathSpectatePayload(true, "Teammate", 2));
        roundTripS2C(new DeathSpectatePayload(false, "", 0));
        roundTrip(CycleDeathSpectatePayload.CODEC, new CycleDeathSpectatePayload(true));

        // Old channels must never select the new codecs, even if an old peer sends without canSend().
        for (String name : List.of("sync_config", "operation_result", "world_tilt", "v2/sync_config", "v2/update_config")) {
            RegistryByteBuf buffer = buffer();
            try {
                Identifier legacyId = Identifier.of("hunterwildcard", name);
                buffer.writeIdentifier(legacyId);
                buffer.writeZero(355);
                CustomPayload payload = CustomPayloadS2CPacket.PLAY_CODEC.decode(buffer).payload();
                check(payload instanceof UnknownCustomPayload, "Legacy packet entered a current decoder: " + name);
                check(payload.getId().id().equals(legacyId), "Legacy channel identity changed");
                check(!buffer.isReadable(), "Legacy packet bytes were left unread");
            } finally {
                buffer.release();
            }
        }

        for (Identifier required : List.of(HunterWildcardPackets.C2S_REQUEST_CONFIG.id(),
                HunterWildcardPackets.S2C_SYNC_CONFIG.id())) {
            check(HunterWildcardPackets.hasIncompatibleProtocol(Set.of(Identifier.of("hunterwildcard", "sync_config")), required),
                    "Old protocol must report mismatch");
            check(!HunterWildcardPackets.hasIncompatibleProtocol(Set.of(required), required), "Matching peer rejected");
            check(!HunterWildcardPackets.hasIncompatibleProtocol(Set.of(Identifier.of("othermod", "test")), required),
                    "A peer without this mod is not a protocol mismatch");
            check(HunterWildcardPackets.hasIncompatibleProtocol(Set.of(Identifier.of("hunterwildcard", "v999/test")), required),
                    "Future incompatible protocol must report mismatch");
        }

        // Also exercise channel negotiation, config delivery and a real save acknowledgement.
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(creator -> creator.setCheatsEnabled(true)).create()) {
            context.waitTicks(10);
            context.runOnClient(client -> {
                check(ClientGameStatus.hasSync(), "Compatible connection did not receive initial config");
                check(ClientPlayNetworking.canSend(HunterWildcardPackets.C2S_UPDATE_CONFIG), "Save channel not negotiated");
                ConfigSnapshot current = ClientGameStatus.latest().config();
                ModConfig update = current.toConfig();
                update.preparingSeconds = 37;
                update.blazeRodChanceEnabled = true;
                update.blazeRodChancePercent = 87;
                ConfigSnapshot submitted = ConfigSnapshot.from(update);
                ClientPlayNetworking.send(new UpdateConfigPayload(submitted, current, ConfigDraft.submit(submitted)));
            });
            context.waitTicks(15);
            world.getServer().runOnServer(server -> check(GameManager.getInstance().getConfig().preparingSeconds == 37,
                    "Server did not apply the config update"));
            context.runOnClient(client -> {
                check(ConfigDraft.pendingId == 0 && !ConfigDraft.failed, "Save acknowledgement was not accepted");
                check(ClientGameStatus.latest().config().preparingSeconds() == 37, "Updated config did not return to client");
                check(ClientGameStatus.latest().config().blazeRodChanceEnabled() && ClientGameStatus.latest().config().blazeRodChancePercent() == 87, "Blaze probability returns in saved config");
            });
            context.getInput().resizeWindow(1920, 1080);
            context.runOnClient(client -> {
                var screen = new com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen();
                client.options.getGuiScale().setValue(3);
                client.setScreen(screen);
                screen.selectPageForTesting("BALANCE");
                screen.scrollForTesting(100);
            });
            context.waitTicks(3);
            context.takeScreenshot("blaze-drop-config");
            context.runOnClient(client -> client.setScreen(null));
            world.getServer().runOnServer(server -> BlazeRodAssertions.run(server.getPlayerManager().getPlayerList().getFirst()));
            context.runOnClient(client -> {
                ClientGameStatus.update(new SyncConfigPayload(GameState.RUNNING, 2, 1, "", "hunterwildcard.role.hunter", true, false, 0, 0, 89, true, false, ClientGameStatus.latest().config()));
                ClientGameStatus.details = new RoundDetailsPayload(List.of(
                        new MemberEntry("QIAN996", "hunterwildcard.role.hunter", "hunterwildcard.ui.member.respawning", -1, 14),
                        new MemberEntry("Teammate", "hunterwildcard.role.hunter", "hunterwildcard.ui.member.alive", -1, 0),
                        new MemberEntry("Runner", "hunterwildcard.role.runner", "hunterwildcard.ui.member.hidden", -2, 0)),
                        "", "", List.of(), "", "", "hunterwildcard.ui.member.alive", -1);
                var screen = new com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen();
                client.setScreen(screen);
                com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen.receiveSync(ClientGameStatus.latest());
                screen.scrollForTesting(1000);
            });
            context.takeScreenshot("member-time-fixed");
            context.runOnClient(client -> client.setScreen(null));
            world.getServer().runOnServer(server -> PortalSafetyAssertions.run(server.getPlayerManager().getPlayerList().getFirst()));
            context.waitTicks(20);
            context.runOnClient(client -> check(client.world.getRegistryKey().equals(net.minecraft.world.World.NETHER) && client.player.getY() < 121, "Client portal arrival stays below the Nether roof"));
        }
    }

    private static RegistryByteBuf buffer() {
        return new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
    }

    private static void roundTripS2C(CustomPayload payload) {
        roundTrip(CustomPayloadS2CPacket.PLAY_CODEC, new CustomPayloadS2CPacket(payload));
    }

    private static <T> void roundTrip(PacketCodec<RegistryByteBuf, T> codec, T value) {
        RegistryByteBuf buffer = buffer();
        try {
            codec.encode(buffer, value);
            check(value.equals(codec.decode(buffer)), "Packet fields changed in transit: " + value);
            check(!buffer.isReadable(), "Packet decoder left trailing data");
        } finally {
            buffer.release();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
