package com.xiaoming.hunterwildcard.client.hud;
import com.xiaoming.hunterwildcard.client.ui.DisplayPreferences;
import net.minecraft.client.Minecraft;
/** Shared virtual viewport for local HUD scale and safe screen-edge spacing. */
public final class HudLayout {
    private HudLayout(){}
    public static float scale(){return DisplayPreferences.get.scale;}
    public static int width(){return (int)(Minecraft.getInstance().getWindow().getGuiScaledWidth()/scale());}
    public static int height(){return (int)(Minecraft.getInstance().getWindow().getGuiScaledHeight()/scale());}
    public static int background(){return (DisplayPreferences.get.opacity*255/100<<24)|0x1C232C;}
}
