package com.THproject.tharidia_things.mixin;

import com.THproject.tharidia_things.TharidiaThings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to disable the player head rendering on the Mapwright map.
 * Injects into Rendering methods and cancels them entirely.
 */
@Mixin(targets = "wawa.mapwright.Rendering", remap = false)
public class MapwrightRenderingMixin {

    private static boolean logged = false;

    /**
     * Injects at the head of renderHead and cancels it to prevent player head rendering.
     */
    @Inject(method = "renderHead", at = @At("HEAD"), cancellable = true)
    private static void disableRenderHead(CallbackInfo ci) {
        if (!logged) {
            TharidiaThings.LOGGER.info("[MapwrightRenderingMixin] Disabling renderHead");
            logged = true;
        }
        ci.cancel();
    }

    /**
     * Backup injection on renderPlayerIcon in case renderHead doesn't get caught.
     */
    @Inject(method = "renderPlayerIcon", at = @At("HEAD"), cancellable = true)
    private static void disableRenderPlayerIcon(CallbackInfo ci) {
        if (!logged) {
            TharidiaThings.LOGGER.info("[MapwrightRenderingMixin] Disabling renderPlayerIcon");
            logged = true;
        }
        ci.cancel();
    }
}
