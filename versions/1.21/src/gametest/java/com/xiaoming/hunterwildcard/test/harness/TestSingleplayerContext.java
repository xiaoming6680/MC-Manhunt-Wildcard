package com.xiaoming.hunterwildcard.test.harness;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.server.MinecraftServer;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
public final class TestSingleplayerContext implements AutoCloseable {
    private final ClientGameTestContext context;
    TestSingleplayerContext(ClientGameTestContext context){this.context=context;}
    public ServerContext getServer(){return new ServerContext();}
    public ClientWorldContext getClientWorld(){return new ClientWorldContext();}
    public final class ClientWorldContext {public void waitForChunksRender(){context.waitTicks(40);}}
    public final class ServerContext {
        public void runOnServer(Consumer<MinecraftServer> action){computeOnServer(s->{action.accept(s);return null;});}
        public <T>T computeOnServer(Function<MinecraftServer,T> action){var server=MinecraftClient.getInstance().getServer();if(server==null)throw new AssertionError("No integrated server");try{return server.submit(()->action.apply(server)).get(60,TimeUnit.SECONDS);}catch(Exception e){throw new AssertionError("Server action failed",e);}}
        public void runCommand(String command){runOnServer(s->s.getCommandManager().executeWithPrefix(s.getCommandSource(),command));}
    }
    @Override public void close(){context.runOnClient(c->{c.world.disconnect();c.disconnect();c.setScreen(new TitleScreen());});context.waitFor(c->c.getServer()==null,1200);context.waitTicks(5);}
}