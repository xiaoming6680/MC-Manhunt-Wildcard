package com.xiaoming.hunterwildcard.client.screen;
import com.xiaoming.hunterwildcard.client.ui.*;
import com.xiaoming.hunterwildcard.client.ClientGameStatus;
import com.xiaoming.hunterwildcard.client.key.HunterWildcardKeyBindings;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class DisplaySettingsScreen extends Screen {
    private final Screen parent;
    private String error="";
    private int offset;
    public DisplaySettingsScreen(Screen parent){super(t("display"));this.parent=parent;}
    private static Component t(String k,Object... args){return Component.translatable("hunterwildcard.ui."+k,args);}
    private static Component onOff(boolean b){return Component.translatable(b?"options.on":"options.off");}
    @Override protected void init(){
        var p=DisplayPreferences.get;
        int x=Math.max(8,width/2-150),w=Math.min(300,width-16), y=44-offset;
        List<Runnable> actions=List.of(()->p.compact=!p.compact,()->p.scale=p.scale>=1.25F?0.75F:p.scale+0.25F,()->p.opacity=p.opacity>=100?50:Math.min(100,p.opacity+15),()->p.inset=p.inset>=30?6:p.inset+8,()->p.reducedMotion=!p.reducedMotion,()->p.killFlash=!p.killFlash,()->p.feedbackHoldSeconds=p.feedbackHoldSeconds>=8?3:p.feedbackHoldSeconds+1,ClientGameStatus::toggleStatusHud);
        List<Component> labels=List.of(t("display.compact",onOff(p.compact)),t("display.scale",Math.round(p.scale*100)),t("display.opacity",p.opacity),t("display.inset",p.inset),t("display.motion",onOff(p.reducedMotion)),t("display.flash",onOff(p.killFlash)),t("display.hold",p.feedbackHoldSeconds),t("display.hud",onOff(ClientGameStatus.isStatusHudToggled())));
        for(int i=0;i<labels.size();i++){Runnable action=actions.get(i);if(y>=38 && y+22<height-56)addRenderableWidget(ThemedButton.row(labels.get(i),x,y,w,22,b->{action.run();error=p.save()?"":"display.failed";rebuildWidgets();}));y+=28;}
        addRenderableWidget(ThemedButton.create(t("back"),x+w-76,height-30,76,20,b->onClose()));
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v){offset=Math.clamp(offset-(int)(v*28),0,Math.max(0,224-(height-100)));rebuildWidgets();return true;}
    @Override public void extractRenderState(GuiGraphicsExtractor c,int mx,int my,float d){
        c.fill(0,0,width,height,UiTheme.BACKGROUND);c.centeredText(font,title,width/2,14,UiTheme.TEXT);
        c.centeredText(font,t("display.keys",HunterWildcardKeyBindings.openPanelKeyName(),HunterWildcardKeyBindings.hudKeyName()),width/2,28,UiTheme.MUTED);
        c.centeredText(font,t(error.isEmpty()?"display.local":error),width/2,height-49,UiTheme.MUTED);super.extractRenderState(c,mx,my,d);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
