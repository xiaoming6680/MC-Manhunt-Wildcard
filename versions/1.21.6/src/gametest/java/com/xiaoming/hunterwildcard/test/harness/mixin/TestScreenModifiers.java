package com.xiaoming.hunterwildcard.test.harness.mixin;
import com.xiaoming.hunterwildcard.test.harness.TestInput;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Emulates held native modifier keys for synthetic keyboard callbacks in the regression runner. */
@Mixin(Screen.class)
public abstract class TestScreenModifiers {
    @Inject(method="hasControlDown",at=@At("HEAD"),cancellable=true)
    private static void control(CallbackInfoReturnable<Boolean> cir){Integer m=TestInput.MODIFIERS.get();if(m!=null)cir.setReturnValue((m&2)!=0);}
    @Inject(method="hasShiftDown",at=@At("HEAD"),cancellable=true)
    private static void shift(CallbackInfoReturnable<Boolean> cir){Integer m=TestInput.MODIFIERS.get();if(m!=null)cir.setReturnValue((m&1)!=0);}
    @Inject(method="hasAltDown",at=@At("HEAD"),cancellable=true)
    private static void alt(CallbackInfoReturnable<Boolean> cir){Integer m=TestInput.MODIFIERS.get();if(m!=null)cir.setReturnValue((m&4)!=0);}
}