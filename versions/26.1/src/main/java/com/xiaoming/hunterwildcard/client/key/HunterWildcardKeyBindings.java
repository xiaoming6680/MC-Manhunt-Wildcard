package com.xiaoming.hunterwildcard.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class HunterWildcardKeyBindings {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(HunterWildcardMod.MOD_ID, "hunter_wildcard"));
    private static KeyMapping openPanelKey;
    private static KeyMapping toggleHudKey;
    private static KeyMapping previousTeammateKey, nextTeammateKey;

    private HunterWildcardKeyBindings() {
    }

    public static void register() {
        if (openPanelKey != null) {
            return;
        }

        openPanelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.hunterwildcard.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                CATEGORY
        ));

        toggleHudKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hunterwildcard.toggle_hud",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_H,CATEGORY));
        previousTeammateKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hunterwildcard.previous_teammate",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_Z,CATEGORY));
        nextTeammateKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hunterwildcard.next_teammate",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_X,CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (previousTeammateKey.consumeClick()) cycleTeammate(client, true);
            while (nextTeammateKey.consumeClick()) cycleTeammate(client, false);
            while(toggleHudKey.consumeClick()) if(client.player!=null && client.screen==null) com.xiaoming.hunterwildcard.client.ClientGameStatus.toggleStatusHud();
            while (openPanelKey.consumeClick()) {
                if (client.player == null || client.level == null || client.screen != null) {
                    continue;
                }

                // The key always opens the panel; the status HUD toggle lives on the panel's GAME page.
                client.setScreen(new HunterWildcardConfigScreen());
            }
        });
    }

    public static String hudKeyName(){return toggleHudKey==null?"H":toggleHudKey.getTranslatedKeyMessage().getString();}

    private static void cycleTeammate(net.minecraft.client.Minecraft client, boolean previous) {
        if (client.player == null || client.screen != null || !com.xiaoming.hunterwildcard.client.hud.DeathWaitOverlay.isSpectating()) return;
        var id = com.xiaoming.hunterwildcard.network.HunterWildcardPackets.C2S_CYCLE_DEATH_SPECTATE;
        if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(id)) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.xiaoming.hunterwildcard.network.HunterWildcardPackets.CycleDeathSpectatePayload(previous));
        }
    }

    public static String previousTeammateKeyName(){return previousTeammateKey==null?"Z":previousTeammateKey.getTranslatedKeyMessage().getString();}
    public static String nextTeammateKeyName(){return nextTeammateKey==null?"X":nextTeammateKey.getTranslatedKeyMessage().getString();}

    public static String openPanelKeyName() {
        return openPanelKey == null ? "M" : openPanelKey.getTranslatedKeyMessage().getString();
    }
}
