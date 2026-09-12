package com.xiaoming.hunterwildcard.client.screen;
import com.xiaoming.hunterwildcard.client.ui.UiTheme;
import com.xiaoming.hunterwildcard.client.ui.ThemedButton;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.Registries;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import java.util.*;
import java.util.function.Consumer;
public final class ItemPickerScreen extends Screen {
    private final Screen parent;private final Consumer<String> pick;
    private TextFieldWidget search;private String query="";private int offset;private boolean rebuild;
    private List<Item> matches=List.of();
    public ItemPickerScreen(Screen parent,Consumer<String> pick){super(Text.translatable("hunterwildcard.ui.item.search"));this.parent=parent;this.pick=pick;}
    @Override protected void init(){
        int panelWidth=Math.min(420,width-40),panelX=(width-panelWidth)/2;
        search=addDrawableChild(new TextFieldWidget(textRenderer,panelX,32,panelWidth,20,title));search.setMaxLength(128);search.setText(query);
        search.setChangedListener(q->{query=q;offset=0;rebuild=true;});setInitialFocus(search);
        String q=query.toLowerCase(Locale.ROOT);
        matches=Registries.ITEM.stream().filter(i->i!=Items.AIR).filter(i->Registries.ITEM.getId(i).toString().contains(q)||i.getName().getString().toLowerCase(Locale.ROOT).contains(q)).toList();
        int rows=Math.max(1,(height-96)/24);offset=Math.clamp(offset,0,Math.max(0,matches.size()-rows));
        for(int i=offset;i<Math.min(matches.size(),offset+rows);i++){
            Item item=matches.get(i);String id=Registries.ITEM.getId(item).toString();
            var button=ThemedButton.row(item.getName(),panelX+22,60+(i-offset)*24,panelWidth-22,22,b->{pick.accept(id);close();});
            button.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(id)));addDrawableChild(button);
        }
        addDrawableChild(ThemedButton.create(Text.translatable("gui.back"),width/2-38,height-28,76,20,b->close()));
    }
    @Override public void tick(){if(rebuild){rebuild=false;int cursor=search.getCursor();clearAndInit();search.setCursor(cursor,false);}}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){offset+=(v>0?-3:3);clearAndInit();return true;}
    @Override public void renderBackground(DrawContext c,int mx,int my,float d){}
    @Override public void render(DrawContext c,int mx,int my,float d){
        super.renderBackground(c,mx,my,d);c.fill(0,0,width,height,UiTheme.BACKGROUND);c.drawCenteredTextWithShadow(textRenderer,title,width/2,12,UiTheme.TEXT);super.render(c,mx,my,d);int rows=Math.max(1,(height-96)/24);for(int i=offset;i<Math.min(matches.size(),offset+rows);i++)c.drawItem(matches.get(i).getDefaultStack(),(width-Math.min(420,width-40))/2+1,63+(i-offset)*24);if(matches.isEmpty())c.drawCenteredTextWithShadow(textRenderer,Text.translatable("hunterwildcard.ui.search.empty"),width/2,70,UiTheme.MUTED);}
    @Override public boolean shouldPause(){return false;}
    @Override public void close(){client.setScreen(parent);}
}
