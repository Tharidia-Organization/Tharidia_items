package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.herbalist_tree.HerbalistTreeBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HerbalistTreeItemRenderer extends GeoItemRenderer<HerbalistTreeBlockItem> {
    private static final float SCALE = 0.25f;

    public HerbalistTreeItemRenderer() {
        super(new GeoModel<HerbalistTreeBlockItem>() {
            @Override
            public ResourceLocation getModelResource(HerbalistTreeBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/herbalist_tree.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(HerbalistTreeBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/herbalist_tree.png");
            }

            @Override
            public ResourceLocation getAnimationResource(HerbalistTreeBlockItem animatable) {
                return null;
            }
        });
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
            HerbalistTreeBlockItem animatable, BakedGeoModel model, boolean isReRender, float partialTick,
            int packedLight, int packedOverlay) {
        // Center the model
        poseStack.translate(0.3, 0.1, 0.0);
        // Isometric rotation (~40 degrees on X and Y axes)
        poseStack.mulPose(Axis.XP.rotationDegrees(40f));
        poseStack.mulPose(Axis.YP.rotationDegrees(40f));

        // Apply scale for GUI display
        super.scaleModelForRender(SCALE, SCALE, poseStack, animatable, model, isReRender,
                partialTick, packedLight, packedOverlay);
    }
}
