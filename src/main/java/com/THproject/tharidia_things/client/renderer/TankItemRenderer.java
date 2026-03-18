package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.tank.TankBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TankItemRenderer extends GeoItemRenderer<TankBlockItem> {
    private static final float SCALE = 0.35f;

    public TankItemRenderer() {
        super(new GeoModel<TankBlockItem>() {
            @Override
            public ResourceLocation getModelResource(TankBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/tank.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(TankBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/tank.png");
            }

            @Override
            public ResourceLocation getAnimationResource(TankBlockItem animatable) {
                return null;
            }
        });
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, TankBlockItem animatable,
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
