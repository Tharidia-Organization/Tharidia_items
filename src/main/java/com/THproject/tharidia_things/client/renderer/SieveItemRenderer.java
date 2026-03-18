package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sieve.SieveBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SieveItemRenderer extends GeoItemRenderer<SieveBlockItem> {
    private static final float SCALE = 0.35f;

    public SieveItemRenderer() {
        super(new GeoModel<SieveBlockItem>() {
            @Override
            public ResourceLocation getModelResource(SieveBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/sieve.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(SieveBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/sieve.png");
            }

            @Override
            public ResourceLocation getAnimationResource(SieveBlockItem animatable) {
                return null;
            };
        });
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, SieveBlockItem animatable,
            BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
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
