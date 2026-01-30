package com.THproject.tharidia_things.client.renderer;

import org.jetbrains.annotations.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sieve.SieveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SieveLeverRenderer extends GeoRenderLayer<SieveBlockEntity> {
    public SieveLeverRenderer(GeoRenderer<SieveBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, SieveBlockEntity animatable, BakedGeoModel bakedModel,
            @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
            float partialTick, int packedLight, int packedOverlay) {

        ResourceLocation modelLoc = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
                animatable.isActive() ? "geo/sieve_lever_active.geo.json" : "geo/sieve_lever_deactive.geo.json");

        getRenderer().reRender(getRenderer().getGeoModel().getBakedModel(modelLoc), poseStack, bufferSource, animatable,
                renderType, buffer, partialTick, packedLight, packedOverlay, 10);
    }
}
