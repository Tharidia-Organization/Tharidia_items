package com.THproject.tharidia_things.client.renderer;

import org.jetbrains.annotations.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.pulverizer.PulverizerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class PulverizerGrinderRenderer extends GeoRenderLayer<PulverizerBlockEntity> {
    public PulverizerGrinderRenderer(GeoRenderer<PulverizerBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, PulverizerBlockEntity animatable, BakedGeoModel bakedModel,
            @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
            float partialTick, int packedLight, int packedOverlay) {

        ResourceLocation modelLoc = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
                "geo/pulverizer_grinder.geo.json");
        ResourceLocation textureLoc = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
                "textures/block/pulverizer.png");
        RenderType reType = RenderType.entityCutout(textureLoc);

        if (animatable.hasGrinder())
            getRenderer().reRender(
                    getRenderer().getGeoModel().getBakedModel(modelLoc),
                    poseStack,
                    bufferSource,
                    animatable,
                    reType,
                    bufferSource.getBuffer(reType),
                    partialTick,
                    packedLight,
                    packedOverlay,
                    -1);
    }
}
