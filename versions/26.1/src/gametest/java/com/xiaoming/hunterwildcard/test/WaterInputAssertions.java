package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen;
import com.xiaoming.hunterwildcard.client.ui.ConfigDraft;
import com.xiaoming.hunterwildcard.wildcard.rules.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.client.screen.widget.DropdownWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Uses real screen input dispatch and compares rotated swimming against vanilla travel. */
final class WaterInputAssertions {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static void run(ClientGameTestContext c, TestSingleplayerContext world) {
        c.runOnClient(client -> {client.options.guiScale().set(3); client.setScreen(new HunterWildcardConfigScreen());});
        c.waitTicks(15);
        c.runOnClient(client -> ((HunterWildcardConfigScreen)client.screen).selectPageForTesting("WILDCARD"));
        c.waitTicks(5);
        for (int index=0;index<2;index++) {
            final int selected=index;
            c.runOnClient(client -> {
                var s=client.screen;
                var d=(DropdownWidget)s.children().stream().filter(e->e instanceof DropdownWidget).toList().get(selected);
                s.mouseClicked(new MouseButtonEvent(d.getX()+5,d.getY()+5,new MouseButtonInfo(0,0)),false);
                s.mouseClicked(new MouseButtonEvent(d.getX()+5,d.getY()+d.getHeight()*2+5,new MouseButtonInfo(0,0)),false);
            });
            c.waitTicks(4);
        }
        c.takeScreenshot("input-before");
        c.runOnClient(client -> {
            var screen=client.screen;
            var field=(EditBox)screen.children().stream().filter(e->e instanceof EditBox f&&f.active&&f.visible).findFirst().orElseThrow();
            check(screen.mouseClicked(new MouseButtonEvent(field.getX()+8,field.getY()+8,new MouseButtonInfo(0,0)),false),"Mouse click reaches time input");
            check(screen.getFocused()==field&&field.isFocused(),"Clicked time input receives focus");
            check(screen.keyPressed(new KeyEvent(65,0,2)),"Ctrl+A selects time");
            for(char ch:"2:03".toCharArray())check(screen.charTyped(new CharacterEvent(ch)),"Time accepts character "+ch);
            check(field.getValue().equals("2:03"),"Time text is editable through keyboard");
        });
        c.waitTicks(25);
        c.takeScreenshot("input-edited");
        for (int index=0;index<4;index++) {
            final int selected=index;
            c.runOnClient(client -> {
                var s=client.screen;
                var f=(EditBox)s.children().stream().filter(e->e instanceof EditBox).toList().get(selected);
                s.mouseClicked(new MouseButtonEvent(f.getX()+6,f.getY()+6,new MouseButtonInfo(0,0)),false);
                check(s.getFocused()==f&&f.isFocused(),"Random time input "+selected+" gets focus");
                s.keyPressed(new KeyEvent(65,0,2));
            });
            for(char ch:"2:03".toCharArray()) {
                c.waitTicks(3);
                c.runOnClient(client -> check(client.screen.charTyped(new CharacterEvent(ch)),"Typing after redraw in random field "+selected));
            }
        }
        c.runOnClient(client -> {
            var field=(EditBox)client.screen.getFocused();
            check(field.getValue().equals("2:03"),"Focus and raw time survive redraw and sync");
        });
        for(int index=0;index<5;index++) {
            final int selected=index;
            double[] point=c.computeOnClient(client -> {
                var f=(EditBox)client.screen.children().stream().filter(e->e instanceof EditBox).toList().get(selected);
                return new double[]{(f.getX()+8)*(double)client.getWindow().getScreenWidth()/client.screen.width,(f.getY()+8)*(double)client.getWindow().getScreenHeight()/client.screen.height};
            });
            c.getInput().setCursorPos(point[0],point[1]);c.getInput().pressMouse(0);c.waitTicks(2);
            // Fabric's pressKey emits modifiers=0, even with holdControl; send the real Ctrl+A event.
            c.runOnClient(client -> client.screen.keyPressed(new KeyEvent(65,0,2)));
            c.getInput().typeChars(index==4?"backrooms":"2:04");c.waitTicks(5);
            c.takeScreenshot("input-native-"+index);
            c.runOnClient(client -> check(client.screen.getFocused() instanceof EditBox f&&f.getValue().equals(selected==4?"backrooms":"2:04"),"Native mouse and keyboard input "+selected+" focus="+client.screen.getFocused()+" texts="+client.screen.children().stream().filter(e->e instanceof EditBox).map(e->((EditBox)e).getValue()).toList()));
        }
        c.takeScreenshot("input-native-edited");
        c.runOnClient(client -> {client.setScreen(null);ConfigDraft.clear();});
        testMinimum(c,world);
        world.getServer().runOnServer(server -> {
            var p=server.getPlayerList().getPlayers().getFirst();
            for(int x=-5;x<=5;x++)for(int y=195;y<=205;y++)for(int z=-5;z<=5;z++)
                p.level().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState());
        });
        c.waitTicks(10);
        c.runOnClient(client -> {
            var p=client.player;
            for(boolean sprint:new boolean[]{false,true})for(float pitch:new float[]{0,40,-40})
                for(int effects=0;effects<4;effects++) {
                    Vec3 vanilla=step(p,Direction.DOWN,sprint,pitch,effects);
                    for(Direction d:new Direction[]{Direction.EAST,Direction.WEST,Direction.NORTH,Direction.SOUTH}) {
                        Vec3 rotated=step(p,d,sprint,pitch,effects);
                        check(vanilla.distanceTo(rotated)<.00001,"Water matches vanilla in "+d+" sprint="+sprint+" pitch="+pitch+" effects="+effects+": "+vanilla+" vs "+rotated);
                    }
                }
            for(Direction d:new Direction[]{Direction.DOWN,Direction.EAST,Direction.WEST,Direction.NORTH,Direction.SOUTH}) {
                ((TiltTrackedGravity)p).hunterwildcard$gravity(d);p.setDeltaMovement(Vec3.ZERO);
                invoke(LivingEntity.class,"goDownInWater",p);
                check(TiltFrame.local(d,p.getDeltaMovement()).distanceTo(new Vec3(0,-.03999999910593033,0))<1e-9,"Dive follows gravity in "+d);
                try {var m=LivingEntity.class.getDeclaredMethod("jumpInLiquid",net.minecraft.tags.TagKey.class);m.setAccessible(true);m.invoke(p,FluidTags.WATER);}catch(Exception e){throw new AssertionError(e);}
                check(p.getDeltaMovement().length()<1e-9,"Jump cancels dive in "+d);
            }
            ((TiltTrackedGravity)p).hunterwildcard$gravity(Direction.DOWN);p.refreshDimensions();p.setDeltaMovement(Vec3.ZERO);p.removeAllEffects();
            p.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).setBaseValue(0);
        });
    }
    private static Vec3 step(Player p,Direction d,boolean sprint,float pitch,int effects) {
        ((TiltTrackedGravity)p).hunterwildcard$gravity(d);p.setPos(.5,200,.5);p.setPose(Pose.STANDING);p.refreshDimensions();
        p.setOnGround(false);p.horizontalCollision=false;p.verticalCollision=false;p.setNoGravity(false);p.getAbilities().flying=false;
        p.removeAllEffects();p.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).setBaseValue(effects==1?1:0);
        if(effects==2)p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,100));
        if(effects==3)p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,100));
        p.setSprinting(sprint);p.setSwimming(sprint);p.setJumping(false);TiltFrame.look(p,30,pitch);
        invoke(Entity.class,"updateFluidInteraction",p);check(p.isInWater(),"Fixture is in water");
        p.setDeltaMovement(TiltFrame.world(d,new Vec3(.03,-.02,.07)));
        p.travel(new Vec3(.2,0,.8));
        return TiltFrame.local(d,p.getDeltaMovement());
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
        c.runOnClient(client -> ((HunterWildcardConfigScreen)client.screen).selectPageForTesting("WILDCARD"));
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
    private static void typeTime(net.minecraft.client.Minecraft client,String key,String value) {
        var s=client.screen;String label=Component.translatable("hunterwildcard.config.number."+key).getString();
        var f=(EditBox)s.children().stream().filter(e->e instanceof EditBox t&&t.getMessage().getString().equals(label)).findFirst().orElseThrow();
        s.mouseClicked(new MouseButtonEvent(f.getX()+5,f.getY()+5,new MouseButtonInfo(0,0)),false);
        check(s.getFocused()==f&&f.isFocused(),"Time click focused "+key+" active="+f.active+" visible="+f.visible+" y="+f.getY());
        s.keyPressed(new KeyEvent(65,0,2));
        for(char ch:value.toCharArray())check(s.charTyped(new CharacterEvent(ch)),"Time character reaches field");
        check(f.getValue().equals(value),"Raw time remains intact before saving: expected="+value+" actual="+f.getValue());
    }
    private static void apply(net.minecraft.client.Minecraft client) {
        String label=Component.translatable("hunterwildcard.ui.apply").getString();
        var b=(Button)client.screen.children().stream().filter(e->e instanceof Button t&&t.getMessage().getString().equals(label)).findFirst().orElseThrow();
        b.onPress(new KeyEvent(257,0,0));
    }
}
