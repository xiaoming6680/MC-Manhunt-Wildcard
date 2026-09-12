package com.xiaoming.hunterwildcard.client.key;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class HunterWildcardKeyBindings {
    private static final String CATEGORY = "key.category.hunterwildcard.hunter_wildcard";
    private static KeyBinding openPanelKey;
    private static KeyBinding toggleHudKey;
    private static KeyBinding previousTeammateKey, nextTeammateKey;

    private HunterWildcardKeyBindings() {
    }

    public static void register() {
        if (openPanelKey != null) {
            return;
        }

        openPanelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hunterwildcard.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                CATEGORY
        ));

        toggleHudKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hunterwildcard.toggle_hud",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_H,CATEGORY));
        previousTeammateKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hunterwildcard.previous_teammate",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_Z,CATEGORY));
        nextTeammateKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hunterwildcard.next_teammate",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_X,CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (previousTeammateKey.wasPressed()) cycleTeammate(client, true);
            while (nextTeammateKey.wasPressed()) cycleTeammate(client, false);
            while(toggleHudKey.wasPressed()) if(client.player!=null && client.currentScreen==null) com.xiaoming.hunterwildcard.client.ClientGameStatus.toggleStatusHud();
            while (openPanelKey.wasPressed()) {
                if (client.player == null || client.world == null || client.currentScreen != null) {
                    continue;
                }

                // The key always opens the panel; the status HUD toggle lives on the panel's GAME page.
                client.setScreen(new HunterWildcardConfigScreen());
            }
        });
    }

    public static KeyBinding[] bindings(){return new KeyBinding[]{openPanelKey,toggleHudKey,previousTeammateKey,nextTeammateKey};}
    public static String hudKeyName(){return toggleHudKey==null?"H":toggleHudKey.getBoundKeyLocalizedText().getString();}

    private static void cycleTeammate(net.minecraft.client.MinecraftClient client, boolean previous) {
        if (client.player == null || client.currentScreen != null || !com.xiaoming.hunterwildcard.client.hud.DeathWaitOverlay.isSpectating()) return;
        var id = com.xiaoming.hunterwildcard.network.HunterWildcardPackets.C2S_CYCLE_DEATH_SPECTATE;
        if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(id)) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.xiaoming.hunterwildcard.network.HunterWildcardPackets.CycleDeathSpectatePayload(previous));
        }
    }

    public static String previousTeammateKeyName(){return previousTeammateKey==null?"Z":previousTeammateKey.getBoundKeyLocalizedText().getString();}
    public static String nextTeammateKeyName(){return nextTeammateKey==null?"X":nextTeammateKey.getBoundKeyLocalizedText().getString();}

    public static String openPanelKeyName() {
        return openPanelKey == null ? "M" : openPanelKey.getBoundKeyLocalizedText().getString();
    }
}
