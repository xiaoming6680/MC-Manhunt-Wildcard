package com.xiaoming.hunterwildcard.client.screen;
import com.xiaoming.hunterwildcard.client.ui.*;
import com.xiaoming.hunterwildcard.client.ClientGameStatus;
import com.xiaoming.hunterwildcard.client.key.HunterWildcardKeyBindings;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.util.List;
public final class DisplaySettingsScreen extends Screen {
    private final Screen parent;
    private String error="";
    private int offset;
    public DisplaySettingsScreen(Screen parent){super(t("display"));this.parent=parent;}
    private static Text t(String k,Object... args){return Text.translatable("hunterwildcard.ui."+k,args);}
    private static Text onOff(boolean b){return Text.translatable(b?"options.on":"options.off");}
    @Override protected void init(){
        var p=DisplayPreferences.get;
        int x=Math.max(8,width/2-150),w=Math.min(300,width-16), y=44-offset;
        List<Runnable> actions=List.of(()->p.compact=!p.compact,()->p.scale=p.scale>=1.25F?0.75F:p.scale+0.25F,()->p.opacity=p.opacity>=100?50:Math.min(100,p.opacity+15),()->p.inset=p.inset>=30?6:p.inset+8,()->p.reducedMotion=!p.reducedMotion,()->p.killFlash=!p.killFlash,()->p.feedbackHoldSeconds=p.feedbackHoldSeconds>=8?3:p.feedbackHoldSeconds+1,ClientGameStatus::toggleStatusHud);
        List<Text> labels=List.of(t("display.compact",onOff(p.compact)),t("display.scale",Math.round(p.scale*100)),t("display.opacity",p.opacity),t("display.inset",p.inset),t("display.motion",onOff(p.reducedMotion)),t("display.flash",onOff(p.killFlash)),t("display.hold",p.feedbackHoldSeconds),t("display.hud",onOff(ClientGameStatus.isStatusHudToggled())));
        for(int i=0;i<labels.size();i++){Runnable action=actions.get(i);if(y>=38 && y+22<height-56)addDrawableChild(ThemedButton.row(labels.get(i),x,y,w,22,b->{action.run();error=p.save()?"":"display.failed";clearAndInit();}));y+=28;}
        addDrawableChild(ThemedButton.create(t("back"),x+w-76,height-30,76,20,b->close()));
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v){offset=Math.clamp(offset-(int)(v*28),0,Math.max(0,224-(height-100)));clearAndInit();return true;}
    @Override public void render(DrawContext c,int mx,int my,float d){
        c.fill(0,0,width,height,UiTheme.BACKGROUND);c.drawCenteredTextWithShadow(textRenderer,title,width/2,14,UiTheme.TEXT);
        c.drawCenteredTextWithShadow(textRenderer,t("display.keys",HunterWildcardKeyBindings.openPanelKeyName(),HunterWildcardKeyBindings.hudKeyName()),width/2,28,UiTheme.MUTED);
        c.drawCenteredTextWithShadow(textRenderer,t(error.isEmpty()?"display.local":error),width/2,height-49,UiTheme.MUTED);super.render(c,mx,my,d);
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void close(){client.setScreen(parent);}
}
