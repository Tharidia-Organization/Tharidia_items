package com.THproject.tharidia_things.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import virtuoel.pehkui.api.ScaleTypes;
import yesman.epicfight.client.renderer.FirstPersonRenderer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

/**
 * Fixes the vertical position mismatch between Pehkui's player scaling and
 * Epic Fight's first-person arm renderer.
 *
 * Root cause: Epic Fight positions the arm model by translating the PoseStack
 * by -eyeHeight (already Pehkui-scaled). However, the arm geometry in model
 * space is always at its default (scale=1) Y positions. When the camera moves
 * up/down with Pehkui scaling, the arms end up visually displaced:
 *   - Large player (scale > 1): camera higher, arms appear too low / disappear
 *   - Small player (scale < 1): camera lower, arms appear too high / block view
 *
 * Fix: pre-translate the PoseStack vertically by scaledEyeHeight * (1 - 1/scale)
 * so the arm geometry lines up with the scaled camera position.
 */
@Mixin(value = FirstPersonRenderer.class, remap = false)
public class EpicFightFirstPersonScaleMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void tharidia$fixPehkuiVerticalOffset(
            LocalPlayer entity,
            LocalPlayerPatch localPlayerPatch,
            LivingEntityRenderer<LocalPlayer, PlayerModel<LocalPlayer>> renderer,
            MultiBufferSource buffer,
            PoseStack poseStack,
            int packedLight,
            float partialTick,
            CallbackInfo ci) {
        float scale = ScaleTypes.BASE.getScaleData(entity).getScale(partialTick);
        if (Math.abs(scale - 1.0F) > 0.001F) {
            float scaledEyeHeight = entity.getDimensions(Pose.STANDING).eyeHeight();
            // Shift the model up/down so arms align with the Pehkui-scaled camera position.
            // offset = scaledEyeHeight * (1 - 1/scale)
            //   scale > 1: positive offset → moves arms up to meet higher camera
            //   scale < 1: negative offset → moves arms down to meet lower camera
            float offset = scaledEyeHeight * (1.0F - 1.0F / scale);
            poseStack.translate(0.0F, offset, 0.0F);
        }
    }
}
