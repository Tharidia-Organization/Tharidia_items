package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.client.model.PietroItemModel;
import com.THproject.tharidia_things.item.PietroBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib renderer for PietroBlockItem in inventory/hand.
 * Scales the model down to fit nicely in GUI.
 */
public class PietroItemRenderer extends GeoItemRenderer<PietroBlockItem> {

    private static final float SCALE = 0.25f;

    public PietroItemRenderer() {
        super(new PietroItemModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
                                    PietroBlockItem animatable, BakedGeoModel model, boolean isReRender,
                                    float partialTick, int packedLight, int packedOverlay) {
        // Center the model horizontally (translate right to compensate for left offset)
        poseStack.translate(0.4, 0.0, 0.0);

        // Apply 1/4 scale for GUI display
        super.scaleModelForRender(SCALE, SCALE, poseStack, animatable, model, isReRender,
                partialTick, packedLight, packedOverlay);
    }
}
