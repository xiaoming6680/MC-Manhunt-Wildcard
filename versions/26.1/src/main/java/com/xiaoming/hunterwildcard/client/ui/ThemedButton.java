package com.xiaoming.hunterwildcard.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;

/** Shared flat button for the secondary pickers and display preferences. */
public final class ThemedButton extends Button {
    private boolean row;
    private ThemedButton(net.minecraft.network.chat.Component text, int x, int y, int width, int height, OnPress action) {
        super(x,y,width,height,text,action,DEFAULT_NARRATION);
    }
    public static ThemedButton create(net.minecraft.network.chat.Component text, int x, int y, int width, int height, OnPress action) {
        return new ThemedButton(text,x,y,width,height,action);
    }
    public static ThemedButton row(net.minecraft.network.chat.Component text, int x, int y, int width, int height, OnPress action) {
        ThemedButton button=create(text,x,y,width,height,action);button.row=true;return button;
    }
    @Override protected void extractContents(GuiGraphicsExtractor context,int mouseX,int mouseY,float delta) {
        boolean focus=isHovered()||isFocused();
        int border=focus?UiTheme.RUNNER:UiTheme.BORDER;
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        if(row) {
            if(focus)context.fill(x,y,x+w,y+h,UiTheme.RAISED);
            context.fill(x,y+h-1,x+w,y+h,focus?UiTheme.RUNNER:UiTheme.BORDER);
            var font=Minecraft.getInstance().font;
            context.text(font,net.minecraft.network.chat.Component.literal(font.plainSubstrByWidth(getMessage().getString(),w-16)),x+8,y+(h-font.lineHeight)/2,active?UiTheme.TEXT:UiTheme.MUTED,false);
            return;
        }
        context.fill(x,y,x+w,y+h,focus?UiTheme.RAISED:UiTheme.PANEL);
        context.fill(x,y,x+w,y+1,border);context.fill(x,y+h-1,x+w,y+h,border);
        context.fill(x,y,x+1,y+h,border);context.fill(x+w-1,y,x+w,y+h,border);
        if(isFocused())context.fill(x+2,y+2,x+4,y+h-2,border);
        var font=Minecraft.getInstance().font;
        String label=getMessage().getString();
        if(font.width(label)>w-16)label=font.plainSubstrByWidth(label,w-16-font.width("…"))+"…";
        context.centeredText(font,net.minecraft.network.chat.Component.literal(label),x+w/2,y+(h-font.lineHeight)/2,active?UiTheme.TEXT:UiTheme.MUTED);
    }
    @Override protected void extractDefaultLabel(net.minecraft.client.gui.ActiveTextCollector textConsumer) {}
}
