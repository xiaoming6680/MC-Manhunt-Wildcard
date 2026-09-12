package com.xiaoming.hunterwildcard.client.screen;
import com.xiaoming.hunterwildcard.client.ui.UiTheme;
import com.xiaoming.hunterwildcard.client.ui.ThemedButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.*;
import java.util.function.Consumer;
public final class ItemPickerScreen extends Screen {
    private final Screen parent;private final Consumer<String> pick;
    private EditBox search;private String query="";private int offset;private boolean rebuild;
    private List<Item> matches=List.of();
    public ItemPickerScreen(Screen parent,Consumer<String> pick){super(Component.translatable("hunterwildcard.ui.item.search"));this.parent=parent;this.pick=pick;}
    @Override protected void init(){
        int panelWidth=Math.min(420,width-40),panelX=(width-panelWidth)/2;
        search=addRenderableWidget(new EditBox(font,panelX,32,panelWidth,20,title));search.setMaxLength(128);search.setValue(query);
        search.setResponder(q->{query=q;offset=0;rebuild=true;});setInitialFocus(search);
        String q=query.toLowerCase(Locale.ROOT);
        matches=BuiltInRegistries.ITEM.stream().filter(i->i!=Items.AIR).filter(i->BuiltInRegistries.ITEM.getKey(i).toString().contains(q)||i.getDefaultInstance().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).toList();
        int rows=Math.max(1,(height-96)/24);offset=Math.clamp(offset,0,Math.max(0,matches.size()-rows));
        for(int i=offset;i<Math.min(matches.size(),offset+rows);i++){
            Item item=matches.get(i);String id=BuiltInRegistries.ITEM.getKey(item).toString();
            var button=ThemedButton.row(item.getDefaultInstance().getHoverName(),panelX+22,60+(i-offset)*24,panelWidth-22,22,b->{pick.accept(id);onClose();});
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(id)));addRenderableWidget(button);
        }
        addRenderableWidget(ThemedButton.create(Component.translatable("gui.back"),width/2-38,height-28,76,20,b->onClose()));
    }
    @Override public void tick(){if(rebuild){rebuild=false;int cursor=search.getCursorPosition();rebuildWidgets();search.moveCursorTo(cursor,false);}}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){offset+=(v>0?-3:3);rebuildWidgets();return true;}
    @Override public void extractRenderState(GuiGraphicsExtractor c,int mx,int my,float d){c.fill(0,0,width,height,UiTheme.BACKGROUND);c.centeredText(font,title,width/2,12,UiTheme.TEXT);super.extractRenderState(c,mx,my,d);int rows=Math.max(1,(height-96)/24);for(int i=offset;i<Math.min(matches.size(),offset+rows);i++)c.item(matches.get(i).getDefaultInstance(),(width-Math.min(420,width-40))/2+1,63+(i-offset)*24);if(matches.isEmpty())c.centeredText(font,Component.translatable("hunterwildcard.ui.search.empty"),width/2,70,UiTheme.MUTED);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
