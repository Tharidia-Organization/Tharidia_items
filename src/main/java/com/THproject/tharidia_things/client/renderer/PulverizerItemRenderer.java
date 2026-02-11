package com.THproject.tharidia_things.client.renderer;

import org.jetbrains.annotations.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.pulverizer.PulverizerBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PulverizerItemRenderer extends GeoItemRenderer<PulverizerBlockItem> {
    private static final float SCALE = 0.35f;

    public PulverizerItemRenderer() {
        super(new GeoModel<PulverizerBlockItem>() {
            @Override
            public ResourceLocation getModelResource(PulverizerBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/pulverizer.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(PulverizerBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/pulverizer.png");
            }

            @Override
            public ResourceLocation getAnimationResource(PulverizerBlockItem animatable) {
                return null;
            }
        });
    }

    @Override
    public void preRender(PoseStack poseStack, PulverizerBlockItem animatable, BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        setBoneVisible(model, "grinder_left", false);
        setBoneVisible(model, "grinder_right", false);
    }

    private void setBoneVisible(BakedGeoModel model, String boneName, boolean visible) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            bone.setHidden(!visible);
        }
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
            PulverizerBlockItem animatable, BakedGeoModel model, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay) {
        // Center the model
        poseStack.translate(0.25, 0.1, 0.0);
        // Isometric rotation (~40 degrees on X and Y axes)
        poseStack.mulPose(Axis.XP.rotationDegrees(40f));
        poseStack.mulPose(Axis.YP.rotationDegrees(40f));

        // Apply scale for GUI display
        super.scaleModelForRender(SCALE, SCALE, poseStack, animatable, model, isReRender,
                partialTick, packedLight, packedOverlay);
    }
}
