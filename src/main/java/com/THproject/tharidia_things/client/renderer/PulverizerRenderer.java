package com.THproject.tharidia_things.client.renderer;

import org.jetbrains.annotations.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.pulverizer.PulverizerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class PulverizerRenderer extends GeoBlockRenderer<PulverizerBlockEntity> {
    @Override
    public void render(PulverizerBlockEntity animatable, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public PulverizerRenderer(BlockEntityRendererProvider.Context context) {
        super(new GeoModel<PulverizerBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(PulverizerBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/pulverizer.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(PulverizerBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/pulverizer.png");
            }

            @Override
            public ResourceLocation getAnimationResource(PulverizerBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "animations/pulverizer.animation.json");
            }
        });
    }

    @Override
    public void preRender(PoseStack poseStack, PulverizerBlockEntity animatable, BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        setBoneVisible(model, "grinder_left", animatable.hasGrinder());
        setBoneVisible(model, "grinder_right", animatable.hasGrinder());
    }

    private void setBoneVisible(BakedGeoModel model, String boneName, boolean visible) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            bone.setHidden(!visible);
        }
    }
}
