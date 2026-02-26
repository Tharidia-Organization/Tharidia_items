package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.herbalist_tree.HerbalistTreeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HerbalistTreeRenderer extends GeoBlockRenderer<HerbalistTreeBlockEntity> {
    public HerbalistTreeRenderer() {
        super(new GeoModel<HerbalistTreeBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(HerbalistTreeBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/herbalist_tree.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(HerbalistTreeBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/herbalist_tree.png");
            }

            @Override
            public ResourceLocation getAnimationResource(HerbalistTreeBlockEntity animatable) {
                return null;
            }
        });
    }

    @Override
    public void render(HerbalistTreeBlockEntity animatable, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
