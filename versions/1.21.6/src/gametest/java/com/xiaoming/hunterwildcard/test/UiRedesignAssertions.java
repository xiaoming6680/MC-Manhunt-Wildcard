package com.xiaoming.hunterwildcard.test;
import com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen;
import com.xiaoming.hunterwildcard.client.*;
import com.xiaoming.hunterwildcard.client.ui.*;
import com.xiaoming.hunterwildcard.client.screen.*;
import com.xiaoming.hunterwildcard.client.screen.widget.DropdownWidget;
import com.xiaoming.hunterwildcard.client.hud.*;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.test.harness.*;
import net.minecraft.client.gui.widget.*;

import net.minecraft.text.Text;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
final class UiRedesignAssertions {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static HunterWildcardConfigScreen screen(net.minecraft.client.MinecraftClient c){return (HunterWildcardConfigScreen)c.currentScreen;}
    private static TextFieldWidget field(net.minecraft.client.MinecraftClient c){return (TextFieldWidget)c.currentScreen.children().stream().filter(e->e instanceof TextFieldWidget).findFirst().orElseThrow();}
    private static void press(net.minecraft.client.MinecraftClient c,String key){String label=Text.translatable("hunterwildcard."+key).getString();ButtonWidget b=(ButtonWidget)c.currentScreen.children().stream().filter(e->e instanceof ButtonWidget button && button.getMessage().getString().equals(label)).findFirst().orElseThrow();check(b.active,"Button is enabled: "+key);b.onPress();}
    private static void waitForReload(ClientGameTestContext c) {
        for(int i=0;i<60;i++) {
            c.waitTicks(10);
            java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();
            c.runOnClient(client->ready.set(client.getOverlay()==null));
            if(ready.get())return;
        }
        throw new AssertionError("Resource reload finishes before screenshot");
    }
    static void run(ClientGameTestContext c,TestSingleplayerContext world){
        AtomicReference<java.util.function.Consumer<net.minecraft.client.MinecraftClient>> preview=new AtomicReference<>();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{var frame=preview.get();if(frame!=null)frame.accept(client);});
        c.runOnClient(client->{client.options.getGuiScale().setValue(2);client.setScreen(new HunterWildcardConfigScreen());});c.waitTicks(12);
        c.runOnClient(client->{screen(client).selectPageForTesting("BASIC");
            var apply=client.currentScreen.children().stream().filter(e->e instanceof ButtonWidget b && b.getMessage().getString().equals(Text.translatable("hunterwildcard.ui.apply").getString())).map(e->(ButtonWidget)e).findFirst().orElseThrow();
            check(apply.getWidth()<=76,"Apply stays compact on a wide screen");
            field(client).setText("2:03");screen(client).close();client.setScreen(new HunterWildcardConfigScreen());});c.waitTicks(10);
        c.runOnClient(client->{screen(client).selectPageForTesting("BASIC");check(field(client).getText().equals("2:03"),"Uncommitted draft survives close and reopen");press(client,"ui.apply");});c.waitTicks(15);
        world.getServer().runOnServer(server->check(GameManager.getInstance().getConfig().preparingSeconds==123,"Time input saved to server"));
        c.runOnClient(client->check(ConfigDraft.pendingId==0&&!ConfigDraft.failed,"Matching save acknowledgement"));
        c.runOnClient(client->field(client).setText("3:00"));
        world.getServer().runOnServer(server->{GameManager.getInstance().getConfig().preparingSeconds=140;HunterWildcardPackets.syncAll(server);});c.waitTicks(6);
        c.runOnClient(client->{check(field(client).getText().equals("3:00"),"Broadcast does not clobber raw input");press(client,"ui.apply");});c.waitTicks(12);
        c.runOnClient(client->{check(ConfigDraft.failed && ConfigDraft.value.preparingSeconds()==180,"Conflict retains submitted draft: pending="+ConfigDraft.pendingId+", failed="+ConfigDraft.failed+", preparing="+ConfigDraft.value.preparingSeconds()+", message="+ConfigDraft.message);screen(client).selectPageForTesting("CHANGES");});
        c.takeScreenshot("ui-save-conflict");
        c.runOnClient(client->{press(client,"ui.undo");screen(client).selectPageForTesting("BASIC");field(client).setText("invalid");screen(client).selectPageForTesting("GAME");screen(client).selectPageForTesting("BASIC");check(field(client).getText().equals("invalid"),"Invalid input survives navigation");});
        c.takeScreenshot("ui-input-error");
        c.runOnClient(client->{
            screen(client).selectPageForTesting("BALANCE");press(client,"ui.defaults.page");screen(client).selectPageForTesting("BASIC");
            check(field(client).getText().equals("invalid"),"Reset on another page preserves invalid raw input");
            press(client,"ui.undo");screen(client).selectPageForTesting("GAME");check(ClientGameStatus.details.members().size()==1,"Lobby roster includes joined hunter");});c.waitTicks(3);c.takeScreenshot("ui-lobby-wide");
        java.util.concurrent.atomic.AtomicReference<double[]> navCursor=new java.util.concurrent.atomic.AtomicReference<>();
        c.runOnClient(client->{var nav=(ButtonWidget)client.currentScreen.children().stream().filter(e->e instanceof ButtonWidget b&&b.getMessage().getString().equals(Text.translatable("hunterwildcard.screen.page.rules").getString())).findFirst().orElseThrow();navCursor.set(new double[]{(nav.getX()+10)*(double)client.getWindow().getWidth()/client.currentScreen.width,(nav.getY()+10)*(double)client.getWindow().getHeight()/client.currentScreen.height});});
        java.util.concurrent.atomic.AtomicReference<Object> stableNav=new java.util.concurrent.atomic.AtomicReference<>();
        c.runOnClient(client->stableNav.set(client.currentScreen.children().getFirst()));
        c.getInput().setCursorPos(navCursor.get()[0],navCursor.get()[1]);c.waitTicks(20);
        c.runOnClient(client->check(client.currentScreen.children().getFirst()==stableNav.get(),"Unchanged sync packets preserve menu widgets"));
        c.runOnClient(client->{check(screen(client).navigationProgressForTesting(Text.translatable("hunterwildcard.screen.page.rules").getString())>.9F,"Navigation hover remains settled across periodic refreshes");client.onResolutionChanged();});c.waitTicks(1);
        c.runOnClient(client->check(screen(client).navigationProgressForTesting(Text.translatable("hunterwildcard.screen.page.rules").getString())>.9F,"Rebuilding widgets retains navigation animation"));
        c.getInput().setCursorPos(0,0);
        c.runOnClient(client->screen(client).selectPageForTesting("RULES"));c.waitTicks(2);
        c.runOnClient(client->check(screen(client).rulesExpansionForTesting()>0&&screen(client).rulesExpansionForTesting()<1,"Rules navigation expands gradually"));c.waitTicks(12);
        c.runOnClient(client->screen(client).selectPageForTesting("GAME"));c.waitTicks(2);
        c.runOnClient(client->check(screen(client).rulesExpansionForTesting()>0&&screen(client).rulesExpansionForTesting()<1,"Rules navigation collapses gradually"));c.waitTicks(12);
        c.runOnClient(client->{
            screen(client).selectPageForTesting("RULES");
            for(String mode:List.of("classic","dragon","survive","collect")) {
                var button=(ButtonWidget)client.currentScreen.children().stream().filter(e->e instanceof ButtonWidget b&&b.getMessage().getString().equals(Text.translatable("hunterwildcard.ui.preset."+mode).getString())).findFirst().orElseThrow();
                var before=ConfigDraft.value;
                check(screen(client).presetDescriptionAt(button.getX()+2,button.getY()+2).equals("hunterwildcard.ui.preset."+mode+".description"),"Hover previews corresponding mode");
                check(before.equals(ConfigDraft.value),"Preview leaves rules unchanged");
                press(client,"ui.preset."+mode);
                check(client.currentScreen.children().stream().noneMatch(e->e instanceof TextFieldWidget),"Rules overview uses readable values rather than input boxes");
                if(mode.equals("classic")) {
                    var cfg=ConfigDraft.value;
                    check(cfg.runnerRespawnMode().equals("NO_RESPAWN")&&cfg.runnerVictoryType().equals("DRAGON")&&cfg.hunterRespawnMode().equals("INFINITE"),"Classic uses one-life dragon hunt with unlimited hunter respawns");
                    check(cfg.enabledWildcards().values().stream().noneMatch(Boolean::booleanValue),"Classic disables all wildcards");
                    check(!cfg.randomRespawnEnabled()&&!cfg.piglinPearlBoostEnabled()&&cfg.hunterDamageMultiplierPercent()==100&&cfg.hunterSpeedPercent()==100&&cfg.runnerSpeedPercent()==100,"Classic removes extra boosts and random respawns");
                }
                screen(client).selectPageForTesting("BASIC");
                check(client.currentScreen.children().stream().anyMatch(e->e instanceof TextFieldWidget),"Category remains editable");
                screen(client).selectPageForTesting("RULES");
            }
            press(client,"ui.undo");
        });
        c.runOnClient(client->screen(client).selectPageForTesting("WILDCARD"));c.takeScreenshot("ui-wildcards-wide");
        c.runOnClient(client->{
            String first=Text.translatable("hunterwildcard.wildcard.backstab.name").getString();
            check(client.currentScreen.children().stream().noneMatch(e->e instanceof ButtonWidget b && b.getMessage().getString().equals(first)),"Matrix names are not clickable buttons");
            check(client.currentScreen.children().stream().noneMatch(e->e instanceof ButtonWidget b && b.getMessage().getString().equals(Text.translatable("hunterwildcard.ui.filter.short.1").getString())),"Removed enabled-state filter is absent");
            press(client,"ui.bulk.off");
            var switchesBeforeSettings=new HashMap<>(ConfigDraft.value.enabledWildcards());
            press(client,"screen.button.settings");
            check(client.currentScreen.children().stream().noneMatch(e->e instanceof ButtonWidget b && List.of("screen.toggle.enable","screen.toggle.disable").stream().anyMatch(k->b.getMessage().getString().equals(Text.translatable("hunterwildcard."+k).getString()))),"Parameter page has no wildcard enable/disable action");
            field(client).setText("7");
            press(client,"ui.defaults.page");
            check(ConfigDraft.value.enabledWildcards().equals(switchesBeforeSettings),"Restoring parameter defaults preserves disabled wildcard switches");
            screen(client).keyPressed(256,0,0);
            check(client.currentScreen.children().stream().anyMatch(e->e instanceof TextFieldWidget),"Escape returns from settings to searchable matrix");
            check(client.currentScreen.children().stream().filter(e->e instanceof TextFieldWidget).count()>=3,"Timing inputs appear directly on wildcard page");
            press(client,"ui.bulk.off");
            check(ConfigDraft.value.enabledWildcards().values().stream().noneMatch(Boolean::booleanValue),"Global disable affects all cards in draft");
            press(client,"ui.bulk.on");
            var offButtons=client.currentScreen.children().stream().filter(e->e instanceof ButtonWidget b && b.getMessage().getString().equals(Text.translatable("hunterwildcard.ui.bulk.off").getString())).map(e->(ButtonWidget)e).toList();
            check(offButtons.size()==5,"Global and four category actions exist");
            offButtons.get(1).onPress();
            for(var f:RuleFields.ToggleField.values())check(RuleFields.getToggle(ConfigDraft.value,f)==(f.category!=RuleFields.WildcardCategory.COMBAT),"Category bulk operation is scoped");
            press(client,"ui.undo");
        });c.takeScreenshot("ui-wildcards-matrix");
        c.runOnClient(client->{((TextFieldWidget)client.currentScreen.children().stream().filter(e->e instanceof TextFieldWidget f && f.getMessage().getString().equals(Text.translatable("hunterwildcard.ui.wildcard.search").getString())).findFirst().orElseThrow()).setText("backrooms");});c.waitTicks(3);c.takeScreenshot("ui-wildcards-search");
        c.runOnClient(client->{
            AtomicReference<String> selected=new AtomicReference<>("a");
            DropdownWidget d=new DropdownWidget(client.textRenderer,0,0,100,20,"",List.of(new DropdownWidget.Option("a","A"),new DropdownWidget.Option("b","B")),"a",true,selected::set,()->{});
            d.setFocused(true);com.xiaoming.hunterwildcard.test.harness.TestInput.keyPressed(d,257,0,0);com.xiaoming.hunterwildcard.test.harness.TestInput.keyPressed(d,264,0,0);com.xiaoming.hunterwildcard.test.harness.TestInput.keyPressed(d,257,0,0);check(selected.get().equals("b"),"Dropdown supports keyboard selection");
        });
        c.runOnClient(client->{
            var snapshot=ConfigDraft.latest;
            long id=ConfigDraft.submit(snapshot);
            check(!ConfigDraft.result(new HunterWildcardPackets.OperationResultPayload(true,"old",id-1)),"Stale save response ignored");
            check(ConfigDraft.pendingId==id,"Unrelated result cannot finish pending request");
            check(ConfigDraft.result(new HunterWildcardPackets.OperationResultPayload(false,"hunterwildcard.ui.save.disk_failed",id)),"Disk failure matched");
            check(ConfigDraft.failed && ConfigDraft.value.equals(snapshot),"Disk failure preserves retryable draft");
            ConfigDraft.discard();
            var pair=RuleFields.setNumbers(snapshot,Map.of(RuleFields.NumberField.WILDCARD_INTERVAL_MIN_SECONDS,10,RuleFields.NumberField.WILDCARD_INTERVAL_MAX_SECONDS,20));
            check(pair.wildcardIntervalMinSeconds()==10 && pair.wildcardIntervalMaxSeconds()==20,"Timing range commits atomically");
        });
        c.getInput().resizeWindow(1280,720);
        c.runOnClient(client->{
            var old=ClientGameStatus.latest();
            var viewer=new HunterWildcardPackets.SyncConfigPayload(old.gameState(),old.hunterCount(),old.runnerCount(),old.activeWildcard(),old.playerRole(),old.playerInTeam(),old.activeWildcardRunning(),old.phaseRemainingSeconds(),old.activeWildcardRemainingSeconds(),old.nextWildcardSeconds(),false,false,old.config());
            ClientGameStatus.update(viewer);HunterWildcardConfigScreen.receiveSync(viewer);client.setScreen(new HunterWildcardConfigScreen());screen(client).selectPageForTesting("BASIC");
            check(client.currentScreen.children().stream().noneMatch(e->e instanceof TextFieldWidget),"Viewer reads rule values without disabled edit fields");
        });c.takeScreenshot("ui-viewer-1280");
        world.getServer().runOnServer(HunterWildcardPackets::syncAll);c.waitTicks(4);
        c.getInput().resizeWindow(854,480);c.runOnClient(client->{client.options.getGuiScale().setValue(2);List<HunterWildcardPackets.CompassTargetEntry> entries=new ArrayList<>();for(int i=0;i<20;i++)entries.add(new HunterWildcardPackets.CompassTargetEntry(UUID.randomUUID(),"Runner_Long_Name_"+i,100+i,i%2==0,false));CompassTargetScreen.open(new HunterWildcardPackets.CompassMenuPayload(entries,true));});c.waitTicks(4);
        c.runOnClient(client->{for(var e:client.currentScreen.children())if(e instanceof ClickableWidget w)check(w.getY()>=0&&w.getY()+w.getHeight()<=client.currentScreen.height,"Compass controls fit viewport");});c.takeScreenshot("ui-compass-small");
        c.runOnClient(client->client.setScreen(null));
        c.getInput().resizeWindow(1920,1080);
        c.runOnClient(client->{WildcardDrawOverlay.reset();CombatFeed.kill("HunterLongName123","RunnerLongName456",4,10,false);CombatFeed.notice("Respawn","Runner returned","3 lives","respawn");CombatFeed.kill("Environment","RunnerTwo",5,10,true);});c.waitTicks(8);c.takeScreenshot("ui-kill-feed");
        c.runOnClient(client->{
            CombatFeed.clear();
            CombatFeed.notice("hunterwildcard.hud.feedback.kill.title","hunterwildcard.hud.feedback.versus\u001FHunterOne\u001FRunnerOne","hunterwildcard.hud.feedback.runner_out","kill");
            check(CombatFeed.queuedCount()==1,"Life-mode kill stays a structured high-priority card");
            client.getLanguageManager().setLanguage("zh_cn");client.options.language="zh_cn";client.reloadResources();
        });waitForReload(c);
        c.runOnClient(client->{CombatFeed.clear();client.setScreen(new HunterWildcardConfigScreen());});c.waitTicks(10);
        c.takeScreenshot("ui-lobby-zh");
        c.runOnClient(client->screen(client).selectPageForTesting("RULES"));c.takeScreenshot("ui-rules-zh");
        c.runOnClient(client->press(client,"ui.preset.classic"));c.takeScreenshot("ui-classic-zh");
        c.getInput().resizeWindow(2560,1440);c.runOnClient(client->{client.options.getGuiScale().setValue(4);client.onResolutionChanged();});c.waitTicks(3);
        c.runOnClient(client->{var ys=new java.util.HashSet<Integer>();for(var e:client.currentScreen.children())if(e instanceof ButtonWidget b&&List.of("classic","dragon","survive","collect").stream().anyMatch(k->b.getMessage().getString().equals(Text.translatable("hunterwildcard.ui.preset."+k).getString())))ys.add(b.getY());check(ys.size()==1,"Four presets fit one row at GUI scale 4");});
        c.takeScreenshot("ui-rules-scale4");
        c.getInput().resizeWindow(1920,1080);c.runOnClient(client->{client.options.getGuiScale().setValue(2);client.onResolutionChanged();});c.waitTicks(3);
        c.runOnClient(client->press(client,"ui.undo"));
        c.runOnClient(client->{press(client,"ui.preset.collect");screen(client).selectPageForTesting("VICTORY");});c.takeScreenshot("ui-collect-zh");
        c.runOnClient(client->press(client,"ui.undo"));
        c.runOnClient(client->screen(client).selectPageForTesting("WILDCARD"));c.takeScreenshot("ui-wildcards-zh");
        c.runOnClient(client->{var lines=screen(client).wrapTooltipText("回头杀\n只有背后攻击有效，伤害翻倍。",80);check(lines.size()>=3&&lines.getFirst().getString().equals("回头杀"),"Tooltip honors explicit line breaks and width wrapping");for(var line:lines)check(!line.getString().contains("\n")&&client.textRenderer.getWidth(line)<=80,"Tooltip renders text without newline glyphs");
            var toggle=(ButtonWidget)client.currentScreen.children().stream().filter(e->e instanceof ButtonWidget b&&b.getMessage().getString().equals(Text.translatable("hunterwildcard.screen.toggle.on_short").getString())).findFirst().orElseThrow();navCursor.set(new double[]{(toggle.getX()-15)*(double)client.getWindow().getWidth()/client.currentScreen.width,(toggle.getY()+7)*(double)client.getWindow().getHeight()/client.currentScreen.height});});
        c.getInput().setCursorPos(navCursor.get()[0],navCursor.get()[1]);c.waitTicks(2);c.takeScreenshot("ui-wildcard-tooltip");c.getInput().setCursorPos(0,0);
        c.runOnClient(client->((TextFieldWidget)client.currentScreen.children().stream().filter(e->e instanceof TextFieldWidget f&&f.getMessage().getString().equals(Text.translatable("hunterwildcard.ui.wildcard.search").getString())).findFirst().orElseThrow()).setText("wind_charge_brawl"));
        c.waitTicks(3);c.runOnClient(client->press(client,"screen.button.settings"));c.waitTicks(3);c.takeScreenshot("ui-wildcard-parameters");
        c.runOnClient(client->client.setScreen(new ItemPickerScreen(client.currentScreen,id->{})));c.takeScreenshot("ui-items-zh");
        c.runOnClient(client->client.setScreen(new DisplaySettingsScreen(new HunterWildcardConfigScreen())));c.takeScreenshot("ui-display-zh");
        c.runOnClient(client->{
            client.setScreen(new HunterWildcardConfigScreen());
            var config=ClientGameStatus.latest().config();
            var sync=new HunterWildcardPackets.SyncConfigPayload(com.xiaoming.hunterwildcard.game.GameState.RUNNING,2,2,"fragile","hunterwildcard.role.runner",true,true,-1,42,60,false,false,config);
            var details=new HunterWildcardPackets.RoundDetailsPayload(List.of(new HunterWildcardPackets.MemberEntry("玩家一","hunterwildcard.role.runner","hunterwildcard.ui.member.alive",2,0)),"hunterwildcard.team.runners","hunterwildcard.ui.test.result",List.of(new HunterWildcardPackets.MemberEntry("玩家一","hunterwildcard.role.runner","hunterwildcard.ui.member.alive",2,0)),"存活目标：还需 04:35","猎人击杀：4 / 10","hunterwildcard.ui.member.alive",2);
            preview.set(mc->{ClientGameStatus.update(sync);ConfigDraft.sync(config);ClientGameStatus.details=details;HunterWildcardConfigScreen.receiveSync(sync);});
            preview.get().accept(client);screen(client).selectPageForTesting("GAME");
        });c.takeScreenshot("ui-running-zh");
        c.runOnClient(client->{preview.get().accept(client);screen(client).selectPageForTesting("RESULT");check(!ClientGameStatus.details.reason().isBlank(),"Result fixture survives live server broadcasts");});c.takeScreenshot("ui-result-zh");
        c.runOnClient(client->{client.setScreen(null);WildcardDrawOverlay.setIntro(true,"fragile","hunterwildcard.wildcard.fragile.description");WildcardDrawOverlay.setObjectiveStatus(true,"存活目标：还需 04:35","runner","猎人击杀：4 / 10");CombatFeed.kill("猎人甲","逃亡者乙",4,10,false);CombatFeed.notice("hunterwildcard.hud.feedback.kill.title","hunterwildcard.hud.feedback.versus\u001F猎人丙\u001F逃亡者丁","hunterwildcard.hud.feedback.runner_out","kill");});c.waitTicks(6);c.takeScreenshot("ui-kill-feed-zh");
        c.runOnClient(client->{DisplayPreferences.get.compact=false;check(WildcardDrawOverlay.isIntroExpanded(),"New wildcard introduction starts expanded");});
        c.waitTicks(125);
        c.runOnClient(client->{WildcardDrawOverlay.setIntro(true,"fragile","hunterwildcard.wildcard.fragile.description");check(WildcardDrawOverlay.isIntroExpanded(),"Introduction remains visible until wildcard ends, including after duplicate packets");DisplayPreferences.get.compact=true;});
        c.takeScreenshot("ui-wildcard-intro");
        c.runOnClient(client->{preview.set(null);WildcardDrawOverlay.reset();ConfigDraft.discard();client.setScreen(null);client.options.getGuiScale().setValue(0);client.getLanguageManager().setLanguage("en_us");client.options.language="en_us";client.reloadResources();});waitForReload(c);
        world.getServer().runOnServer(server->{GameManager.getInstance().getConfig().preparingSeconds=60;HunterWildcardPackets.syncAll(server);});c.waitTicks(5);
    }
}
