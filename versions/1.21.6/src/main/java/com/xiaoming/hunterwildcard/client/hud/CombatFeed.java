package com.xiaoming.hunterwildcard.client.hud;

import com.xiaoming.hunterwildcard.client.HunterWildcardClientText;
import com.xiaoming.hunterwildcard.client.ui.*;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import java.util.*;

/** A bounded, priority-aware event feed; lifetime starts only when a card becomes visible. */
public final class CombatFeed {
    private static final List<Entry> entries=new ArrayList<>();
    private CombatFeed() {}
    public static void clear(){entries.clear();}
    private static String tr(String s){return HunterWildcardClientText.translate(s);}
    private static String key(String s){return HunterWildcardText.key(s);}
    public static void kill(String hunter,String runner,int current,int target,boolean environment){
        Entry e=new Entry(key(environment?"hud.feedback.env_kill.title":"hud.feedback.kill.title"),hunter,runner,
            "kill",90,current,Math.max(1,target));
        e.environment=environment;enqueue(e);
        var sm=MinecraftClient.getInstance().getSoundManager();
        sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_ARROW_HIT_PLAYER,environment?0.75F:1.0F));
    }
    public static void notice(String title,String a,String b,String style){
        if ("kill".equals(style) && a != null && a.startsWith(key("hud.feedback.versus")+HunterWildcardText.SPEC_SEPARATOR)) {
            String[] parts=a.split(HunterWildcardText.SPEC_SEPARATOR,-1);
            if(parts.length==3){Entry e=new Entry(title,parts[1],parts[2],"kill",90,0,0);e.status=b;e.environment=title.contains("env_kill");enqueue(e);MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ARROW_HIT_PLAYER,e.environment?0.75F:1F));return;}
        }
        int priority=title.contains("victory")?100:style.equals("kill")||title.contains("eliminat")||title.contains("out")?80:style.equals("respawn")?60:20;
        if(priority<60) for(Entry e:entries) if(e.title.equals(title)&&e.a.equals(a)&&e.b.equals(b)){e.count++;return;}
        enqueue(new Entry(title,a,b,style,priority,-1,-1));
    }
    private static void enqueue(Entry e){
        if(entries.size()>=64){Entry remove=entries.stream().filter(x->x.shown<0).min(Comparator.comparingInt(x->x.priority)).orElse(entries.get(entries.size()-1));entries.remove(remove);}
        int index=0;while(index<entries.size() && (entries.get(index).shown>=0 || entries.get(index).priority>=e.priority))index++;
        entries.add(index,e);
    }
    public static int queuedCount(){return entries.size();}
    public static void render(DrawContext c,int reservedTop){
        var mc=MinecraftClient.getInstance();if(mc.player==null||mc.options.hudHidden||entries.isEmpty())return;
        var prefs=DisplayPreferences.get;long now=System.currentTimeMillis();
        int lifetime=prefs.feedbackHoldSeconds*1000+400;
        entries.removeIf(e->e.shown>=0 && now-e.shown>=lifetime);
        int sw=c.getScaledWindowWidth(),sh=c.getScaledWindowHeight();
        float scale=Math.min(prefs.scale,Math.max(0.6F,(sw-16)/240F));
        int height=60;
        int top=Math.max(prefs.inset,reservedTop), bottom=sh-50;
        scale=Math.min(scale,Math.max(0.6F,(sh-top-26)/(float)height));
        // Keep the original horizontal length; tighten only vertical spacing.
        int width=Math.min(260,Math.max(168,(int)(sw*0.48F/scale)));
        if(mc.world!=null && mc.world.getScoreboard().getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR)!=null) bottom=Math.min(bottom,sh/2-8);
        int count=Math.min(2,Math.max(0,(bottom-top)/(Math.round(height*scale)+6)));
        // On small windows prefer one readable card, below mechanic key hints.
        if(count==0 && top+height*scale<sh-24)count=1;
        for(int i=0;i<Math.min(count,entries.size());i++){
            Entry e=entries.get(i);if(e.shown<0)e.shown=now;
            long age=now-e.shown;float alpha=Math.min(1,age/140F)*Math.min(1,(lifetime-age)/220F);
            int x=sw-prefs.inset-Math.round(width*scale);
            if(!prefs.reducedMotion)x+=Math.round((1-alpha)*(width*scale+8));
            int y=top+i*(Math.round(height*scale)+6);
            var m=c.getMatrices();m.pushMatrix();m.translate(x,y);m.scale(scale,scale);
            int accent=e.environment?UiTheme.WARNING:e.style.equals("kill")?UiTheme.HUNTER:e.style.equals("runner")?UiTheme.RUNNER:e.style.equals("respawn")?UiTheme.RUNNER:UiTheme.WILDCARD;
            int bg=(Math.round(255*prefs.opacity/100F)<<24)|0x1C232C;
            c.fill(0,0,width,height,bg);c.fill(0,0,3,height,accent);
            if(prefs.killFlash&&!prefs.reducedMotion&&e.style.equals("kill")&&age<240)c.fill(3,0,width,height,((int)(55*(1-age/240F))<<24)|(accent&0xFFFFFF));
            c.drawItem((e.style.equals("kill")?Items.IRON_SWORD:Items.PAPER).getDefaultStack(),9,4);
            text(c,tr(e.title)+(e.count>1?" ×"+e.count:""),31,6,width-40,accent);
            if(e.target>=0){
                text(c,tr(key("ui.kill.attacker"))+" "+tr(e.a),10,22,width-20,UiTheme.HUNTER);
                text(c,tr(key("ui.kill.victim"))+" "+tr(e.b),10,33,width-20,UiTheme.RUNNER);
                String progress=e.target==0?tr(e.status):e.current>=e.target?tr(key("hud.feedback.kill_target.complete")):e.current+" / "+e.target+" · "+tr(HunterWildcardText.spec("ui.kill.remaining",Math.max(0,e.target-e.current)));
                text(c,progress,10,44,width-20,UiTheme.WARNING);
                if(e.target>0){c.fill(10,55,width-10,57,UiTheme.BORDER);c.fill(10,55,10+Math.round((width-20)*Math.clamp(e.current/(float)e.target,0,1)),57,accent);}
            }else{
                var lines=mc.textRenderer.wrapLines(Text.literal(tr(e.a)),width-20);
                for(int j=0;j<Math.min(2,lines.size());j++)c.drawText(mc.textRenderer,lines.get(j),10,22+j*11,UiTheme.TEXT,false);
                text(c,tr(e.b),10,44,width-20,UiTheme.WARNING);
            }
            // Thin lifetime indicator is separate from the kill-target progress.
            c.fill(0,height-1,Math.round(width*Math.max(0,1-age/(float)lifetime)),height,UiTheme.MUTED);
            m.popMatrix();
        }
        if(count>0 && entries.size()>count) c.drawText(mc.textRenderer,Text.translatable("hunterwildcard.ui.feed.queued",entries.size()-count),sw-prefs.inset-92,top+count*(Math.round(height*scale)+6),UiTheme.TEXT,true);
    }
    private static void text(DrawContext c,String value,int x,int y,int width,int color){
        var font=MinecraftClient.getInstance().textRenderer;
        if(font.getWidth(value)>width)value=font.trimToWidth(value,Math.max(1,width-font.getWidth("…")))+"…";
        c.drawText(font,Text.literal(value),x,y,color,false);
    }
    private static final class Entry{
        final String title,a,b,style;final int priority,current,target;long shown=-1;int count=1;boolean environment;String status="";
        Entry(String title,String a,String b,String style,int priority,int current,int target){this.title=title==null?"":title;this.a=a==null?"":a;this.b=b==null?"":b;this.style=style==null?"neutral":style;this.priority=priority;this.current=current;this.target=target;}
    }
}
