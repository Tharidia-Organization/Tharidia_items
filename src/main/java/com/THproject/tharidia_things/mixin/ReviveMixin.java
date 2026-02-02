package com.THproject.tharidia_things.mixin;

import com.THproject.tharidia_things.features.Revive;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class ReviveMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void cancelAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player != null && Revive.isPlayerFallen(mc.player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void cancelUseItem(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player != null && Revive.isPlayerFallen(mc.player)) {
            ci.cancel();
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void cancelContinueAttack(boolean p_91387_, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player != null && Revive.isPlayerFallen(mc.player)) {
            ci.cancel();
        }
    }
}
