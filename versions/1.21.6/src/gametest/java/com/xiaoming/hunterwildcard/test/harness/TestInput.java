package com.xiaoming.hunterwildcard.test.harness;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import java.util.function.Function;
public final class TestInput {
    public static final ThreadLocal<Integer> MODIFIERS = new ThreadLocal<>();
    public static boolean keyPressed(net.minecraft.client.gui.Element screen,int key,int scan,int modifiers) {
        MODIFIERS.set(modifiers);
        try { return screen.keyPressed(key,scan,modifiers); } finally { MODIFIERS.remove(); }
    }
    private final ClientGameTestContext context;
    TestInput(ClientGameTestContext context){this.context=context;}
    static Object invoke(Object target,String name,Class<?>[] types,Object...args){try{var method=target.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(target,args);}catch(Exception e){throw new AssertionError("Native input/world action "+name,e);}}
    public void resizeWindow(int width,int height){context.runOnClient(c->GLFW.glfwSetWindowSize(c.getWindow().getHandle(),width,height));context.waitTicks(3);}
    public void pressKey(int key){context.runOnClient(c->c.keyboard.onKey(c.getWindow().getHandle(),key,0,GLFW.GLFW_PRESS,0));context.waitTicks(1);context.runOnClient(c->c.keyboard.onKey(c.getWindow().getHandle(),key,0,GLFW.GLFW_RELEASE,0));}
    public void holdKey(Function<GameOptions,KeyBinding> binding){context.runOnClient(c->binding.apply(c.options).setPressed(true));}
    public void releaseKey(Function<GameOptions,KeyBinding> binding){context.runOnClient(c->binding.apply(c.options).setPressed(false));}
    public void setCursorPos(double x,double y){context.runOnClient(c->{GLFW.glfwSetCursorPos(c.getWindow().getHandle(),x,y);invoke(c.mouse,"onCursorPos",new Class[]{long.class,double.class,double.class},c.getWindow().getHandle(),x,y);});}
    public void pressMouse(int button){for(int action:new int[]{GLFW.GLFW_PRESS,GLFW.GLFW_RELEASE}){context.runOnClient(c->invoke(c.mouse,"onMouseButton",new Class[]{long.class,int.class,int.class,int.class},c.getWindow().getHandle(),button,action,0));context.waitTicks(1);}}
    public void typeChars(String value){for(int ch:value.codePoints().toArray()){context.runOnClient(c->invoke(c.keyboard,"onChar",new Class[]{long.class,int.class,int.class},c.getWindow().getHandle(),ch,0));context.waitTicks(1);}}
}