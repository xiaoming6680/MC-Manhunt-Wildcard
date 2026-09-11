package com.xiaoming.hunterwildcard.client.key;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.client.ClientGameStatus;
import com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class HunterWildcardKeyBindings {
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(HunterWildcardMod.MOD_ID, "hunter_wildcard"));
    private static KeyBinding openPanelKey;

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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPanelKey.wasPressed()) {
                if (client.player == null || client.world == null || client.currentScreen != null) {
                    continue;
                }

                // Operators always get the full menu. Everyone else gets the menu in the lobby (to join a team)
                // and a compact status HUD toggle once the round has started.
                if (ClientGameStatus.hasSync() && !ClientGameStatus.isWaiting() && !ClientGameStatus.canManage()) {
                    ClientGameStatus.toggleStatusHud();
                    continue;
                }

                client.setScreen(new HunterWildcardConfigScreen());
            }
        });
    }

    public static String openPanelKeyName() {
        return openPanelKey == null ? "M" : openPanelKey.getBoundKeyLocalizedText().getString();
    }
}
