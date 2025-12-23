package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.client.model.TrebuchetModel;
import com.THproject.tharidia_things.entity.TrebuchetEntity;
import com.THproject.tharidia_things.entity.animation.TrebuchetAnimator;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;

public class TrebuchetRenderer extends AzEntityRenderer<TrebuchetEntity> {

    private static final float SCALE = (float) Math.cbrt(3.0);

    public TrebuchetRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<TrebuchetEntity>builder(TrebuchetModel.MODEL, TrebuchetModel.TEXTURE)
                        .setAnimatorProvider(TrebuchetAnimator::new)
                        .setShadowRadius(0.9f * SCALE)
                        .build(),
                context);
    }

    @Override
    public void render(
            TrebuchetEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        poseStack.pushPose();
        poseStack.scale(SCALE, SCALE, SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
