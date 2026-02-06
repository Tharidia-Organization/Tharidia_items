package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.client.model.SmithingFurnaceItemModel;
import com.THproject.tharidia_things.item.SmithingFurnaceBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib renderer for SmithingFurnaceBlockItem in inventory/hand.
 * Scales the model down to fit nicely in GUI and hand.
 * Shows only base and tiny_crucible bones.
 */
public class SmithingFurnaceItemRenderer extends GeoItemRenderer<SmithingFurnaceBlockItem> {

    // Scale factor - 1.5x for better visibility
    private static final float SCALE = 0.18f;

    public SmithingFurnaceItemRenderer() {
        super(new SmithingFurnaceItemModel());
    }

    @Override
    public void preRender(PoseStack poseStack, SmithingFurnaceBlockItem animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        // Show only base and tiny_crucible, hide all stage and coal bones
        setBoneVisible(model, "base", true);
        setBoneVisible(model, "tiny_crucible", true);
        setBoneVisible(model, "stage_1", false);
        setBoneVisible(model, "stage_2", false);
        setBoneVisible(model, "stage_3", false);
        setBoneVisible(model, "stage_4", false);
        // Hide coal bones
        setBoneVisible(model, "coal_1", false);
        setBoneVisible(model, "coal_2", false);
        setBoneVisible(model, "coal_3", false);
        setBoneVisible(model, "coal_4", false);
        // Hide ash bones
        setBoneVisible(model, "st_1", false);
        setBoneVisible(model, "st_2", false);
        setBoneVisible(model, "st_3", false);
        setBoneVisible(model, "st_4", false);
        setBoneVisible(model, "st_5", false);
        setBoneVisible(model, "st_6", false);
        // Hide fluid bones
        setBoneVisible(model, "fluid", false);
        setBoneVisible(model, "iron", false);
        setBoneVisible(model, "gold", false);
        setBoneVisible(model, "copper", false);
        setBoneVisible(model, "tin", false);
        setBoneVisible(model, "steel", false);
        setBoneVisible(model, "dark_steel", false);
        // Hide cast molten bones
        setBoneVisible(model, "molten", false);
        setBoneVisible(model, "molten_tin", false);
        setBoneVisible(model, "molten_gold", false);
        setBoneVisible(model, "molten_iron", false);
        setBoneVisible(model, "molten_steel", false);
        setBoneVisible(model, "molten_copper", false);
        setBoneVisible(model, "molten_dark_steel", false);
        // Hide big crucible molten bones
        setBoneVisible(model, "molten_big", false);
        setBoneVisible(model, "molten_tin_big", false);
        setBoneVisible(model, "molten_gold_big", false);
        setBoneVisible(model, "molten_iron_big", false);
        setBoneVisible(model, "molten_steel_big", false);
        setBoneVisible(model, "molten_copper_big", false);
        setBoneVisible(model, "molten_dark_steel_big", false);
    }

    private void setBoneVisible(BakedGeoModel model, String boneName, boolean visible) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            bone.setHidden(!visible);
        }
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
                                    SmithingFurnaceBlockItem animatable, BakedGeoModel model, boolean isReRender,
                                    float partialTick, int packedLight, int packedOverlay) {
        // Center the model
        poseStack.translate(0.5, 0.3, 0.3);
        // Isometric rotation (~40 degrees on X and Y axes)
        poseStack.mulPose(Axis.XP.rotationDegrees(40f));
        poseStack.mulPose(Axis.YP.rotationDegrees(40f));

        // Apply scale for GUI display
        super.scaleModelForRender(SCALE, SCALE, poseStack, animatable, model, isReRender,
                partialTick, packedLight, packedOverlay);
    }
}
