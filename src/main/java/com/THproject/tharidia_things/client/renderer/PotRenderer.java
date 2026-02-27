package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.pot.PotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class PotRenderer extends GeoBlockRenderer<PotBlockEntity> {
    public PotRenderer() {
        super(new GeoModel<PotBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(PotBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/pot.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(PotBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/pot.png");
            }

            @Override
            public ResourceLocation getAnimationResource(PotBlockEntity animatable) {
                return null;
            }
        });
    }

    @Override
    public void render(PotBlockEntity animatable, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
