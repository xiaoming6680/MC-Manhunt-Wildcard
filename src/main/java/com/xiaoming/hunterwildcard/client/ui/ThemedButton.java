package com.xiaoming.hunterwildcard.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

/** Shared flat button for the secondary pickers and display preferences. */
public final class ThemedButton extends ButtonWidget {
    private boolean row;
    private ThemedButton(net.minecraft.text.Text text, int x, int y, int width, int height, PressAction action) {
        super(x,y,width,height,text,action,DEFAULT_NARRATION_SUPPLIER);
    }
    public static ThemedButton create(net.minecraft.text.Text text, int x, int y, int width, int height, PressAction action) {
        return new ThemedButton(text,x,y,width,height,action);
    }
    public static ThemedButton row(net.minecraft.text.Text text, int x, int y, int width, int height, PressAction action) {
        ThemedButton button=create(text,x,y,width,height,action);button.row=true;return button;
    }
    @Override protected void drawIcon(DrawContext context,int mouseX,int mouseY,float delta) {
        boolean focus=isHovered()||isFocused();
        int border=focus?UiTheme.RUNNER:UiTheme.BORDER;
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        if(row) {
            if(focus)context.fill(x,y,x+w,y+h,UiTheme.RAISED);
            context.fill(x,y+h-1,x+w,y+h,focus?UiTheme.RUNNER:UiTheme.BORDER);
            var font=MinecraftClient.getInstance().textRenderer;
            context.drawText(font,net.minecraft.text.Text.literal(font.trimToWidth(getMessage().getString(),w-16)),x+8,y+(h-font.fontHeight)/2,active?UiTheme.TEXT:UiTheme.MUTED,false);
            return;
        }
        context.fill(x,y,x+w,y+h,focus?UiTheme.RAISED:UiTheme.PANEL);
        context.fill(x,y,x+w,y+1,border);context.fill(x,y+h-1,x+w,y+h,border);
        context.fill(x,y,x+1,y+h,border);context.fill(x+w-1,y,x+w,y+h,border);
        if(isFocused())context.fill(x+2,y+2,x+4,y+h-2,border);
        var font=MinecraftClient.getInstance().textRenderer;
        String label=getMessage().getString();
        if(font.getWidth(label)>w-16)label=font.trimToWidth(label,w-16-font.getWidth("…"))+"…";
        context.drawCenteredTextWithShadow(font,net.minecraft.text.Text.literal(label),x+w/2,y+(h-font.fontHeight)/2,active?UiTheme.TEXT:UiTheme.MUTED);
    }
    @Override protected void drawLabel(net.minecraft.client.font.DrawnTextConsumer textConsumer) {}
}
