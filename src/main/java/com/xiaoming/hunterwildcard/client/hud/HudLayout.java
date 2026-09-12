package com.xiaoming.hunterwildcard.client.hud;
import com.xiaoming.hunterwildcard.client.ui.DisplayPreferences;
import net.minecraft.client.MinecraftClient;
/** Shared virtual viewport for local HUD scale and safe screen-edge spacing. */
public final class HudLayout {
    private HudLayout(){}
    public static float scale(){return DisplayPreferences.get.scale;}
    public static int width(){return (int)(MinecraftClient.getInstance().getWindow().getScaledWidth()/scale());}
    public static int height(){return (int)(MinecraftClient.getInstance().getWindow().getScaledHeight()/scale());}
    public static int background(){return (DisplayPreferences.get.opacity*255/100<<24)|0x1C232C;}
}
