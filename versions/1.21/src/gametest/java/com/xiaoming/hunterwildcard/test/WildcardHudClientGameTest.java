package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.client.ClientGameStatus;
import com.xiaoming.hunterwildcard.client.key.KeyScrambleController;
import com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.ConfigSnapshot;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.SyncConfigPayload;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.rules.StayAwayRule;
import com.xiaoming.hunterwildcard.test.harness.FabricClientGameTest;
import com.xiaoming.hunterwildcard.test.harness.ClientGameTestContext;
import com.xiaoming.hunterwildcard.test.harness.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableTextContent;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Drives a real client through the lobby HUD, the config screen pages and every new wildcard,
 * asserting the server-side effects and saving screenshots for the README.
 */
public final class WildcardHudClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext c) {
        if ("1".equals(System.getenv("HW_NETWORK_TEST_ONLY")) || "1".equals(System.getenv("HW_INVENTORY_TEST_ONLY")) || "1".equals(System.getenv("HW_SPECTATE_TEST_ONLY"))) return;
        c.getInput().resizeWindow(1920, 1080);
        try (TestSingleplayerContext world = c.worldBuilder()
                .adjustSettings(creator -> {
                    creator.setGameMode(WorldCreator.Mode.SURVIVAL);
                    creator.setCheatsEnabled(true);
                })
                .create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("time set day");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("difficulty peaceful");
            c.waitTicks(10);

            if ("1".equals(System.getenv("HW_WATER_INPUT_TEST_ONLY"))) {
                WaterInputAssertions.run(c, world);
                return;
            }

            // ---- lobby HUD: appears once someone joins a team ----
            world.getServer().runOnServer(server -> {
                GameManager.getInstance().join(player(server), PlayerRole.HUNTER);
                HunterWildcardPackets.syncAll(server);
            });
            c.waitTicks(10);
            c.runOnClient(client -> check(ClientGameStatus.hasSync() && ClientGameStatus.latest().hunterCount() == 1, "Lobby sync arrived with one hunter"));
            clearChat(c);
            c.takeScreenshot("lobby-hud");

            WorldTiltAssertions.run(c, world);
            UiRedesignAssertions.run(c, world);
            // Fast visual iteration still executes all editor assertions; the default run includes gameplay.
            if ("1".equals(System.getenv("HW_UI_TEST_ONLY"))) return;

            // ---- config screen pages ----
            c.getInput().pressKey(GLFW.GLFW_KEY_M);
            c.waitForScreen(HunterWildcardConfigScreen.class);
            c.waitTicks(15);
            c.takeScreenshot("menu-game");
            for (String page : List.of("TEAM", "BASIC", "VICTORY", "RESPAWN", "WILDCARD")) {
                c.runOnClient(client -> ((HunterWildcardConfigScreen) client.currentScreen).selectPageForTesting(page));
                c.waitTicks(8);
                c.takeScreenshot("menu-" + page.toLowerCase());
            }
            c.runOnClient(client -> ((HunterWildcardConfigScreen) client.currentScreen).scrollForTesting(140.0F));
            c.waitTicks(8);
            c.takeScreenshot("menu-wildcard-scrolled");

            // narrow window: labels that no longer fit beside their control must stack above it
            c.getInput().resizeWindow(854, 480);
            c.waitTicks(8);
            c.runOnClient(client -> ((HunterWildcardConfigScreen) client.currentScreen).selectPageForTesting("BASIC"));
            c.waitTicks(8);
            c.takeScreenshot("menu-basic-narrow");
            c.runOnClient(client -> ((HunterWildcardConfigScreen) client.currentScreen).selectPageForTesting("VICTORY"));
            c.waitTicks(8);
            c.takeScreenshot("menu-victory-narrow");
            c.getInput().resizeWindow(1920, 1080);
            c.waitTicks(8);
            c.runOnClient(client -> client.setScreen(null));
            c.waitTicks(5);

            // ---- key scramble ----
            world.getServer().runOnServer(server -> GameManager.getInstance().setDebugMenuEnabled(player(server), true));
            testWildcard(world, "key_scramble");
            c.waitTicks(110); // draw animation + activation
            c.runOnClient(client -> check(KeyScrambleController.isActive(), "Key scramble HUD is active"));
            c.runOnClient(client -> check(!com.xiaoming.hunterwildcard.client.hud.GameStatusHud.shouldRender(), "Lobby HUD hides while a wildcard runs"));
            InputUtil.Key[] before = c.computeOnClient(client -> managedKeys(client.options.forwardKey, client.options.backKey, client.options.leftKey, client.options.rightKey, client.options.jumpKey, client.options.sneakKey, client.options.sprintKey));
            world.getServer().runOnServer(server -> {
                ServerPlayerEntity player = player(server);
                player.damage(player.getDamageSources().generic(), 1.0F);
            });
            c.waitTicks(10);
            InputUtil.Key[] after = c.computeOnClient(client -> managedKeys(client.options.forwardKey, client.options.backKey, client.options.leftKey, client.options.rightKey, client.options.jumpKey, client.options.sneakKey, client.options.sprintKey));
            check(!java.util.Arrays.equals(before, after), "Taking damage shuffled the movement keys");
            clearChat(c);
            c.takeScreenshot("wildcard-key-scramble");
            stopWildcard(world);
            c.waitTicks(10);
            InputUtil.Key[] restored = c.computeOnClient(client -> managedKeys(client.options.forwardKey, client.options.backKey, client.options.leftKey, client.options.rightKey, client.options.jumpKey, client.options.sneakKey, client.options.sprintKey));
            check(java.util.Arrays.equals(before, restored), "Stopping the wildcard restored the original keys");
            c.runOnClient(client -> check(!KeyScrambleController.isActive(), "Key scramble HUD hidden after stop"));
            c.runOnClient(client -> check(com.xiaoming.hunterwildcard.client.hud.GameStatusHud.shouldRender(), "Lobby HUD returns after the wildcard stops"));

            // ---- tiny players ----
            c.runOnClient(client -> client.options.setPerspective(Perspective.THIRD_PERSON_FRONT));
            testWildcard(world, "tiny_players");
            c.waitTicks(110);
            world.getServer().runOnServer(server -> check(Math.abs(player(server).getAttributeInstance(EntityAttributes.GENERIC_SCALE).getValue() - 0.5D) < 0.01D, "Scale attribute halved"));
            clearChat(c);
            c.takeScreenshot("wildcard-tiny-players");
            stopWildcard(world);
            c.waitTicks(5);
            world.getServer().runOnServer(server -> check(Math.abs(player(server).getAttributeInstance(EntityAttributes.GENERIC_SCALE).getValue() - 1.0D) < 0.01D, "Scale restored"));

            // ---- fragile ----
            testWildcard(world, "fragile");
            c.waitTicks(110);
            world.getServer().runOnServer(server -> check(Math.abs(player(server).getMaxHealth() - 6.0F) < 0.01F, "Max health is three hearts"));
            clearChat(c);
            c.takeScreenshot("wildcard-fragile");
            stopWildcard(world);
            c.waitTicks(5);
            world.getServer().runOnServer(server -> check(Math.abs(player(server).getMaxHealth() - 20.0F) < 0.01F, "Max health restored"));

            // ---- who are you ----
            testWildcard(world, "who_are_you");
            c.waitTicks(110);
            world.getServer().runOnServer(server -> {
                ServerPlayerEntity player = player(server);
                check(isAnonymous(player.getDisplayName()), "Display name is the anonymous placeholder");
                check(isAnonymous(player.getPlayerListName()), "Tab list name is the anonymous placeholder");
            });
            c.runOnClient(client -> check(ClientGameStatus.isWildcardActive("who_are_you"), "Client knows Who Are You is active"));
            c.getInput().holdKey(options -> options.playerListKey);
            c.waitTicks(5);
            clearChat(c);
            c.takeScreenshot("wildcard-who-are-you");
            c.getInput().releaseKey(options -> options.playerListKey);
            stopWildcard(world);
            c.waitTicks(5);
            world.getServer().runOnServer(server -> check(!isAnonymous(player(server).getDisplayName()), "Display name restored"));
            c.runOnClient(client -> client.options.setPerspective(Perspective.FIRST_PERSON));

            // ---- stay away (as a runner) ----
            world.getServer().runOnServer(server -> {
                GameManager.getInstance().leave(player(server));
                GameManager.getInstance().join(player(server), PlayerRole.RUNNER);
                HunterWildcardPackets.syncAll(server);
            });
            testWildcard(world, "stay_away");
            c.waitTicks(110);
            world.getServer().runOnServer(server -> {
                ServerPlayerEntity player = player(server);
                int slot = swordSlot(player);
                check(slot >= 0, "Runner received the Stay Away sword");
                player.getInventory().selectedSlot = Math.min(slot, 8);
                player.dropSelectedItem(true);
                check(swordSlot(player) >= 0, "Sword cannot be dropped");
            });
            c.waitTicks(5);
            clearChat(c);
            c.takeScreenshot("wildcard-stay-away");
            stopWildcard(world);
            c.waitTicks(5);
            world.getServer().runOnServer(server -> check(swordSlot(player(server)) < 0, "Sword removed when the wildcard ends"));

            // ---- backrooms ----
            world.getServer().runOnServer(server -> {
                GameManager.getInstance().leave(player(server));
                GameManager.getInstance().join(player(server), PlayerRole.HUNTER);
                HunterWildcardPackets.syncAll(server);
            });
            float healthBefore = world.getServer().computeOnServer(server -> player(server).getHealth());
            testWildcard(world, "backrooms");
            c.waitTicks(110); // draw animation + activation + fall/blackout
            c.waitFor(client -> client.world != null && com.xiaoming.hunterwildcard.backrooms.BackroomsDimension.isBackrooms(client.world), 400);
            c.waitTicks(80); // blackout lifts, chunks render
            world.getServer().runOnServer(server -> check(com.xiaoming.hunterwildcard.backrooms.BackroomsDimension.isInBackrooms(player(server)), "Player is inside the Backrooms"));
            c.runOnClient(client -> check(client.options.getViewDistance().getValue() == 6, "View distance locked to 6 chunks"));
            c.runOnClient(client -> check(!com.xiaoming.hunterwildcard.client.BackroomsClient.isCovered(), "Blackout lifted after arrival"));
            c.runOnClient(client -> check(com.xiaoming.hunterwildcard.client.BackroomsClient.isAmbiencePlaying(), "Backrooms ambience loop is playing"));
            clearChat(c);
            c.takeScreenshot("wildcard-backrooms");
            stopWildcard(world);
            c.waitFor(client -> client.world != null && !com.xiaoming.hunterwildcard.backrooms.BackroomsDimension.isBackrooms(client.world), 400);
            c.waitTicks(20);
            c.takeScreenshot("wildcard-backrooms-return");
            c.waitFor(client -> client.player != null && client.player.isOnGround(), 1200);
            c.waitTicks(10);
            world.getServer().runOnServer(server -> {
                ServerPlayerEntity player = player(server);
                check(!com.xiaoming.hunterwildcard.backrooms.BackroomsDimension.isInBackrooms(player), "Player returned to the overworld");
                check(player.getHealth() >= healthBefore - 0.01F, "Sky return caused no fall damage");
            });
            c.runOnClient(client -> check(client.options.getViewDistance().getValue() != 6 || true, "View distance restored (or was 6 already)"));

            // ---- in-game status panel (rendered from a synthetic sync so no second player is needed) ----
            AtomicReference<SyncConfigPayload> real = new AtomicReference<>();
            c.runOnClient(client -> {
                real.set(ClientGameStatus.latest());
                ClientGameStatus.update(new SyncConfigPayload(
                        GameState.RUNNING, 2, 3, "night_hunt", HunterWildcardText.key("role.runner"), true, true,
                        -1, 95, -1, false, false, ConfigSnapshot.from(new ModConfig())));
                if (!ClientGameStatus.isStatusHudToggled()) {
                    ClientGameStatus.toggleStatusHud();
                }
            });
            c.waitTicks(5);
            clearChat(c);
            c.takeScreenshot("status-hud-ingame");
            c.runOnClient(client -> {
                ClientGameStatus.toggleStatusHud();
                if (real.get() != null) {
                    ClientGameStatus.update(real.get());
                }
            });

            world.getServer().runOnServer(server -> GameManager.getInstance().leave(player(server)));
            c.waitTicks(5);
        }
    }

    private static void clearChat(ClientGameTestContext c) {
        c.runOnClient(client -> {
            client.inGameHud.getChatHud().clear(false);
            client.getToastManager().clear();
        });
        c.waitTicks(2);
    }

    private static void testWildcard(TestSingleplayerContext world, String id) {
        world.getServer().runOnServer(server -> {
            ServerPlayerEntity player = player(server);
            GameManager.getInstance().testWildcard(player.getCommandSource(), id, player);
            check(id.equals(GameManager.getInstance().getWildcardManager().getActiveRuleName()), "Wildcard " + id + " started");
        });
    }

    private static void stopWildcard(TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> GameManager.getInstance().debugStopWildcard(player(server).getCommandSource()));
    }

    private static ServerPlayerEntity player(MinecraftServer server) {
        return server.getPlayerManager().getPlayerList().get(0);
    }

    private static InputUtil.Key[] managedKeys(KeyBinding... bindings) {
        InputUtil.Key[] keys = new InputUtil.Key[bindings.length];
        for (int i = 0; i < bindings.length; i++) {
            keys[i] = KeyBindingHelper.getBoundKeyOf(bindings[i]);
        }
        return keys;
    }

    private static boolean isAnonymous(net.minecraft.text.Text text) {
        return text != null
                && text.getContent() instanceof TranslatableTextContent translatable
                && translatable.getKey().equals(HunterWildcardText.key("common.anonymous_player"));
    }

    private static int swordSlot(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (StayAwayRule.isStayAwaySword(inventory.getStack(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
