package com.xiaoming.hunterwildcard.test.harness;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import com.xiaoming.hunterwildcard.test.*;
public final class LegacyTestRunner implements ClientModInitializer {
    private boolean started;
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(client->{ClientGameTestContext.ticks++;if(!started&&client.currentScreen != null && client.getOverlay() == null && ClientGameTestContext.ticks > 60){started=true;Thread.ofPlatform().name("Wildcard regression").start(()->run(client));}});}
    private void run(MinecraftClient client){try{var c=new ClientGameTestContext();c.waitTicks(10);client.submit(()->{client.options.pauseOnLostFocus=false;client.options.getViewDistance().setValue(4);client.options.getSimulationDistance().setValue(5);}).join();
        for(var test:new FabricClientGameTest[]{new DeathSpectateClientGameTest(),new PreparingInventoryClientGameTest(),new PacketProtocolClientGameTest(),new WildcardHudClientGameTest()}){System.out.println("WILDCARD_TEST_START "+test.getClass().getSimpleName());test.runTest(c);System.out.println("WILDCARD_TEST_PASS "+test.getClass().getSimpleName());}
        System.out.println("WILDCARD_ALL_TESTS_PASSED");client.execute(client::scheduleStop);
    }catch(Throwable failure){failure.printStackTrace();System.err.println("WILDCARD_TESTS_FAILED");System.exit(1);}}
}