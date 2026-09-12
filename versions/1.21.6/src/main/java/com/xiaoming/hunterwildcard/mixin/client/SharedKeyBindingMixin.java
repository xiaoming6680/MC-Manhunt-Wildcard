package com.xiaoming.hunterwildcard.mixin.client;
import com.xiaoming.hunterwildcard.client.key.HunterWildcardKeyBindings;
import com.xiaoming.hunterwildcard.client.key.QueuedKeyPress;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
/** Older Minecraft stores one binding per key; preserve mod actions when vanilla owns the same key. */
@Mixin(KeyBinding.class)
public abstract class SharedKeyBindingMixin implements QueuedKeyPress {
    @Shadow @Final private static Map<InputUtil.Key,KeyBinding> KEY_TO_BINDINGS;
    @Shadow private int timesPressed;
    @Override public void hunterwildcard$queuePress(){timesPressed++;}
    @Inject(method="onKeyPressed",at=@At("TAIL"))
    private static void hunterwildcard$sharedKey(InputUtil.Key key,CallbackInfo ci){
        for(KeyBinding binding:HunterWildcardKeyBindings.bindings())
            if(binding!=null&&KEY_TO_BINDINGS.get(key)!=binding&&KeyBindingHelper.getBoundKeyOf(binding).equals(key))
                ((QueuedKeyPress)binding).hunterwildcard$queuePress();
    }
}