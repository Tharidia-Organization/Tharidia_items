package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.entity.DiceEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public class DiceEntityRenderer extends EntityRenderer<DiceEntity> {
    private final ItemRenderer itemRenderer;

    public DiceEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(DiceEntity entity, float entityYaw, float partialTick,
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.translate(0.0F, 0.15F, 0.0F);

        if (!entity.isSettled()) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(entity.getYRot()));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(entity.getXRot()));
        } else {
            switch (entity.getFace()) {
                case 1 -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
                case 2 -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90));
                case 3 -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
                case 4 -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
                case 5 -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90));
                case 6 -> {}
                default -> {}
            }
        }

        itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, packedLight,
            OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(DiceEntity entity) {
        return Minecraft.getInstance().getItemRenderer()
            .getModel(entity.getItem(), entity.level(), null, entity.getId())
            .getParticleIcon()
            .contents()
            .name();
    }
}
