package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen;
import com.xiaoming.hunterwildcard.client.ui.ConfigDraft;
import com.xiaoming.hunterwildcard.wildcard.rules.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.client.screen.widget.DropdownWidget;
import net.minecraft.client.input.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.*;

/** Uses real screen input dispatch and compares rotated swimming against vanilla travel. */
final class WaterInputAssertions {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static void run(ClientGameTestContext c, TestSingleplayerContext world) {
        c.runOnClient(client -> {client.options.getGuiScale().setValue(3); client.setScreen(new HunterWildcardConfigScreen());});
        c.waitTicks(15);
        c.runOnClient(client -> ((HunterWildcardConfigScreen)client.currentScreen).selectPageForTesting("WILDCARD"));
        c.waitTicks(5);
        for (int index=0;index<2;index++) {
            final int selected=index;
            c.runOnClient(client -> {
                var s=client.currentScreen;
                var d=(DropdownWidget)s.children().stream().filter(e->e instanceof DropdownWidget).toList().get(selected);
                s.mouseClicked(new Click(d.getX()+5,d.getY()+5,new MouseInput(0,0)),false);
                s.mouseClicked(new Click(d.getX()+5,d.getY()+d.getHeight()*2+5,new MouseInput(0,0)),false);
            });
            c.waitTicks(4);
        }
        c.takeScreenshot("input-before");
        c.runOnClient(client -> {
            var screen=client.currentScreen;
            var field=(TextFieldWidget)screen.children().stream().filter(e->e instanceof TextFieldWidget f&&f.active&&f.visible).findFirst().orElseThrow();
            check(screen.mouseClicked(new Click(field.getX()+8,field.getY()+8,new MouseInput(0,0)),false),"Mouse click reaches time input");
            check(screen.getFocused()==field&&field.isFocused(),"Clicked time input receives focus");
            check(screen.keyPressed(new KeyInput(65,0,2)),"Ctrl+A selects time");
            for(char ch:"2:03".toCharArray())check(screen.charTyped(new CharInput(ch,0)),"Time accepts character "+ch);
            check(field.getText().equals("2:03"),"Time text is editable through keyboard");
        });
        c.waitTicks(25);
        c.takeScreenshot("input-edited");
        for (int index=0;index<4;index++) {
            final int selected=index;
            c.runOnClient(client -> {
                var s=client.currentScreen;
                var f=(TextFieldWidget)s.children().stream().filter(e->e instanceof TextFieldWidget).toList().get(selected);
                s.mouseClicked(new Click(f.getX()+6,f.getY()+6,new MouseInput(0,0)),false);
                check(s.getFocused()==f&&f.isFocused(),"Random time input "+selected+" gets focus");
                s.keyPressed(new KeyInput(65,0,2));
            });
            for(char ch:"2:03".toCharArray()) {
                c.waitTicks(3);
                c.runOnClient(client -> check(client.currentScreen.charTyped(new CharInput(ch,0)),"Typing after redraw in random field "+selected));
            }
        }
        c.runOnClient(client -> {
            var field=(TextFieldWidget)client.currentScreen.getFocused();
            check(field.getText().equals("2:03"),"Focus and raw time survive redraw and sync");
        });
        for(int index=0;index<5;index++) {
            final int selected=index;
            double[] point=c.computeOnClient(client -> {
                var f=(TextFieldWidget)client.currentScreen.children().stream().filter(e->e instanceof TextFieldWidget).toList().get(selected);
                return new double[]{(f.getX()+8)*(double)client.getWindow().getWidth()/client.currentScreen.width,(f.getY()+8)*(double)client.getWindow().getHeight()/client.currentScreen.height};
            });
            c.getInput().setCursorPos(point[0],point[1]);c.getInput().pressMouse(0);c.waitTicks(2);
            // Fabric's pressKey emits modifiers=0, even with holdControl; send the real Ctrl+A event.
            c.runOnClient(client -> client.currentScreen.keyPressed(new KeyInput(65,0,2)));
            c.getInput().typeChars(index==4?"backrooms":"2:04");c.waitTicks(5);
            c.takeScreenshot("input-native-"+index);
            c.runOnClient(client -> check(client.currentScreen.getFocused() instanceof TextFieldWidget f&&f.getText().equals(selected==4?"backrooms":"2:04"),"Native mouse and keyboard input "+selected+" focus="+client.currentScreen.getFocused()+" texts="+client.currentScreen.children().stream().filter(e->e instanceof TextFieldWidget).map(e->((TextFieldWidget)e).getText()).toList()));
        }
        c.takeScreenshot("input-native-edited");
        c.runOnClient(client -> {client.setScreen(null);ConfigDraft.clear();});
        testMinimum(c,world);
        world.getServer().runOnServer(server -> {
            var p=server.getPlayerManager().getPlayerList().getFirst();
            for(int x=-5;x<=5;x++)for(int y=195;y<=205;y++)for(int z=-5;z<=5;z++)
                p.getEntityWorld().setBlockState(new BlockPos(x,y,z),Blocks.WATER.getDefaultState());
        });
        c.waitTicks(10);
        c.runOnClient(client -> {
            var p=client.player;
            for(boolean sprint:new boolean[]{false,true})for(float pitch:new float[]{0,40,-40})
                for(int effects=0;effects<4;effects++) {
                    Vec3d vanilla=step(p,Direction.DOWN,sprint,pitch,effects);
                    for(Direction d:new Direction[]{Direction.EAST,Direction.WEST,Direction.NORTH,Direction.SOUTH}) {
                        Vec3d rotated=step(p,d,sprint,pitch,effects);
                        check(vanilla.distanceTo(rotated)<.00001,"Water matches vanilla in "+d+" sprint="+sprint+" pitch="+pitch+" effects="+effects+": "+vanilla+" vs "+rotated);
                    }
                }
            for(Direction d:new Direction[]{Direction.DOWN,Direction.EAST,Direction.WEST,Direction.NORTH,Direction.SOUTH}) {
                ((TiltTrackedGravity)p).hunterwildcard$gravity(d);p.setVelocity(Vec3d.ZERO);
                invoke(LivingEntity.class,"knockDownwards",p);
                check(TiltFrame.local(d,p.getVelocity()).distanceTo(new Vec3d(0,-.03999999910593033,0))<1e-9,"Dive follows gravity in "+d);
                try {var m=LivingEntity.class.getDeclaredMethod("swimUpward",net.minecraft.registry.tag.TagKey.class);m.setAccessible(true);m.invoke(p,FluidTags.WATER);}catch(Exception e){throw new AssertionError(e);}
                check(p.getVelocity().length()<1e-9,"Jump cancels dive in "+d);
            }
            ((TiltTrackedGravity)p).hunterwildcard$gravity(Direction.DOWN);p.calculateDimensions();p.setVelocity(Vec3d.ZERO);p.clearStatusEffects();
            p.getAttributeInstance(EntityAttributes.WATER_MOVEMENT_EFFICIENCY).setBaseValue(0);
        });
    }
    private static Vec3d step(PlayerEntity p,Direction d,boolean sprint,float pitch,int effects) {
        ((TiltTrackedGravity)p).hunterwildcard$gravity(d);p.setPosition(.5,200,.5);p.setPose(EntityPose.STANDING);p.calculateDimensions();
        p.setOnGround(false);p.horizontalCollision=false;p.verticalCollision=false;p.setNoGravity(false);p.getAbilities().flying=false;
        p.clearStatusEffects();p.getAttributeInstance(EntityAttributes.WATER_MOVEMENT_EFFICIENCY).setBaseValue(effects==1?1:0);
        if(effects==2)p.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE,100));
        if(effects==3)p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,100));
        p.setSprinting(sprint);p.setSwimming(sprint);p.setJumping(false);TiltFrame.look(p,30,pitch);
        invoke(Entity.class,"updateWaterState",p);check(p.isTouchingWater(),"Fixture is in water");
        p.setVelocity(TiltFrame.world(d,new Vec3d(.03,-.02,.07)));
        p.travel(new Vec3d(.2,0,.8));
        return TiltFrame.local(d,p.getVelocity());
    }
    private static void invoke(Class<?> owner,String name,Object instance) {
        try {var m=owner.getDeclaredMethod(name);m.setAccessible(true);m.invoke(instance);}catch(Exception e){throw new AssertionError(e);}
    }
    private static void testMinimum(ClientGameTestContext c,TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            var config=GameManager.getInstance().getConfig();config.wildcardDurationMode="RANDOM";
            config.wildcardDurationMinSeconds=90;config.wildcardDurationMaxSeconds=150;
            HunterWildcardPackets.syncAll(server);
        });
        c.waitTicks(5);
        c.runOnClient(client -> client.setScreen(new HunterWildcardConfigScreen()));c.waitTicks(10);
        c.runOnClient(client -> ((HunterWildcardConfigScreen)client.currentScreen).selectPageForTesting("WILDCARD"));
        String[] values={"1:31","2:00","1：45秒","3:00","５：００"};int[] seconds={91,120,105,180,300};
        for(int i=0;i<values.length;i++) {
            final int n=i;
            c.runOnClient(client -> typeTime(client,"wildcard_duration_min_seconds",values[n]));
            c.waitTicks(3);
            c.runOnClient(client -> apply(client));c.waitTicks(12);
            world.getServer().runOnServer(server -> {
                var config=GameManager.getInstance().getConfig();
                check(config.wildcardDurationMinSeconds==seconds[n],"Minimum saves beyond 90 seconds: "+values[n]+" actual="+config.wildcardDurationMinSeconds);
                check(config.wildcardDurationMaxSeconds==Math.max(150,seconds[n]),"Unedited maximum extends with minimum");
            });
        }
        c.takeScreenshot("minimum-five-minutes-saved");
        c.runOnClient(client -> {typeTime(client,"wildcard_duration_min_seconds","6:00");typeTime(client,"wildcard_duration_max_seconds","4:00");apply(client);});
        c.waitTicks(3);
        world.getServer().runOnServer(server -> check(GameManager.getInstance().getConfig().wildcardDurationMinSeconds==300,"Explicit reversed range is not silently clamped"));
        c.runOnClient(client -> {typeTime(client,"wildcard_duration_max_seconds","7:00");apply(client);});c.waitTicks(12);
        world.getServer().runOnServer(server -> check(GameManager.getInstance().getConfig().wildcardDurationMinSeconds==360&&GameManager.getInstance().getConfig().wildcardDurationMaxSeconds==420,"Both edited bounds save together after correction"));
        c.runOnClient(client -> {client.setScreen(null);ConfigDraft.clear();});
    }
    private static void typeTime(net.minecraft.client.MinecraftClient client,String key,String value) {
        var s=client.currentScreen;String label=Text.translatable("hunterwildcard.config.number."+key).getString();
        var f=(TextFieldWidget)s.children().stream().filter(e->e instanceof TextFieldWidget t&&t.getMessage().getString().equals(label)).findFirst().orElseThrow();
        s.mouseClicked(new Click(f.getX()+5,f.getY()+5,new MouseInput(0,0)),false);
        check(s.getFocused()==f&&f.isFocused(),"Time click focused "+key+" active="+f.active+" visible="+f.visible+" y="+f.getY());
        s.keyPressed(new KeyInput(65,0,2));
        for(char ch:value.toCharArray())check(s.charTyped(new CharInput(ch,0)),"Time character reaches field");
        check(f.getText().equals(value),"Raw time remains intact before saving: expected="+value+" actual="+f.getText());
    }
    private static void apply(net.minecraft.client.MinecraftClient client) {
        String label=Text.translatable("hunterwildcard.ui.apply").getString();
        var b=(ButtonWidget)client.currentScreen.children().stream().filter(e->e instanceof ButtonWidget t&&t.getMessage().getString().equals(label)).findFirst().orElseThrow();
        b.onPress(new KeyInput(257,0,0));
    }
}
