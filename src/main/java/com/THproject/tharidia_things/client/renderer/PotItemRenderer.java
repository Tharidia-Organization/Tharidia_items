package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.pot.PotBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PotItemRenderer extends GeoItemRenderer<PotBlockItem> {
    private static final float SCALE = 0.75f;

    public PotItemRenderer() {
        super(new GeoModel<PotBlockItem>() {

            @Override
            public ResourceLocation getModelResource(PotBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/pot.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(PotBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/pot.png");
            }

            @Override
            public ResourceLocation getAnimationResource(PotBlockItem animatable) {
                return null;
            }
        });
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, PotBlockItem animatable,
            BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        // Center the model
        poseStack.translate(0.0, 0.1, 0.0);
        // Isometric rotation (~40 degrees on X and Y axes)
        poseStack.mulPose(Axis.XP.rotationDegrees(40f));
        poseStack.mulPose(Axis.YP.rotationDegrees(40f));

        // Apply scale for GUI display
        super.scaleModelForRender(SCALE, SCALE, poseStack, animatable, model, isReRender,
                partialTick, packedLight, packedOverlay);
    }
}
