package com.xiaoming.hunterwildcard.test.harness;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.util.ScreenshotRecorder;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
/** Runs assertions off the render thread, with bounded waits on real client ticks. */
public final class ClientGameTestContext {
    public static volatile long ticks;
    public void runOnClient(Consumer<MinecraftClient> action) {computeOnClient(c->{action.accept(c);return null;});}
    public <T> T computeOnClient(Function<MinecraftClient,T> action) {
        var client=MinecraftClient.getInstance();
        try { return client.submit(()->action.apply(client)).get(60,TimeUnit.SECONDS); }
        catch(Exception e){throw new AssertionError("Client action failed",e);}
    }
    public void waitTicks(int count) {long target=ticks+count,deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(Math.max(60,count/10));while(ticks<target){if(System.nanoTime()>deadline)throw new AssertionError("Client tick timeout");sleep();}}
    public void waitFor(Predicate<MinecraftClient> predicate,int limit){for(int i=0;i<limit;i++){if(computeOnClient(predicate::test))return;waitTicks(1);}throw new AssertionError("Client condition timed out");}
    public void waitForScreen(Class<? extends Screen> type){waitFor(c->type.isInstance(c.currentScreen),1200);}
    static void sleep(){try{Thread.sleep(10);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}}
    public TestInput getInput(){return new TestInput(this);}
    public void takeScreenshot(String name){runOnClient(c->ScreenshotRecorder.saveScreenshot(c.runDirectory,name+".png",c.getFramebuffer(),text->{}));}
    public WorldBuilder worldBuilder(){return new WorldBuilder();}
    public final class WorldBuilder {
        private Consumer<WorldCreator> settings=c->{};
        public WorldBuilder adjustSettings(Consumer<WorldCreator> settings){this.settings=settings;return this;}
        public TestSingleplayerContext create(){
            runOnClient(c->CreateWorldScreen.create(c,c.currentScreen));
            waitForScreen(CreateWorldScreen.class);
            runOnClient(c->{var screen=(CreateWorldScreen)c.currentScreen; var creator=screen.getWorldCreator();creator.setWorldName("Wildcard Test "+System.currentTimeMillis());settings.accept(creator);var options=creator.getGeneratorOptionsHolder(); var dimensions=options.selectedDimensions().toConfig(options.dimensionOptionsRegistry()); var registries=options.combinedDynamicRegistries().with(net.minecraft.registry.ServerDynamicRegistryType.DIMENSIONS,dimensions.toDynamicRegistryManager()); TestInput.invoke(screen,"startServer",new Class<?>[]{net.minecraft.world.level.LevelProperties.SpecialProperty.class,net.minecraft.registry.CombinedDynamicRegistries.class,com.mojang.serialization.Lifecycle.class},dimensions.specialWorldProperty(),registries,com.mojang.serialization.Lifecycle.stable());});
            waitFor(c->c.world!=null&&c.player!=null&&c.getServer()!=null,3600);
            waitTicks(30);runOnClient(c->c.setScreen(null));
            return new TestSingleplayerContext(ClientGameTestContext.this);
        }
    }
}