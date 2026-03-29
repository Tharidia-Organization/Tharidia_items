package com.THproject.tharidia_things.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.THproject.tharidia_things.compoundTag.ReviveAttachments;

import net.minecraft.client.Minecraft;
import yesman.epicfight.client.events.engine.ControlEngine;

/**
 * Cancels EpicFight's attack/key input processing when the local player is fallen.
 * Injects directly into ControlEngine.handleEpicFightKeyMappings() which is
 * the entry point for all EpicFight combat input (attacks, skills, etc.).
 */
@Mixin(value = ControlEngine.class, remap = false)
public class MinecraftFallenMixin {

    @Inject(method = "handleEpicFightKeyMappings", at = @At("HEAD"), cancellable = true, remap = false)
    private void preventFallenAttack(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getData(ReviveAttachments.REVIVE_DATA.get()).isFallen()) {
            ci.cancel();
        }
    }
}
