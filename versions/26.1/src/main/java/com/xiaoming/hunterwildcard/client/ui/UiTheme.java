package com.xiaoming.hunterwildcard.client.ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class UiTheme {
    public static final int BACKGROUND=0xF512161C, PANEL=0xF01C232C, RAISED=0xFF27323E, BORDER=0xFF374351;
    public static final int TEXT=0xFFE8EDF2, MUTED=0xFFA6B1BD, HUNTER=0xFFEF7181, RUNNER=0xFF78B8FA, WILDCARD=0xFFB99AFF, WARNING=0xFFF0C76B;
    private UiTheme() {}
    public static void panel(GuiGraphicsExtractor c,int x,int y,int w,int h,int accent) {
        c.fill(x,y,x+w,y+h,PANEL);c.fill(x,y,x+2,y+h,accent);c.fill(x+2,y+h-1,x+w,y+h,BORDER);
    }
}
