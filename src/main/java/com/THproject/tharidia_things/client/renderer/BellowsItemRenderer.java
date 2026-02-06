package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.client.model.BellowsItemModel;
import com.THproject.tharidia_things.item.BellowsItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib renderer for BellowsItem.
 * Shows only the stage_1 bone in the GUI.
 */
public class BellowsItemRenderer extends GeoItemRenderer<BellowsItem> {

    private static final float SCALE = 0.3f;

    public BellowsItemRenderer() {
        super(new BellowsItemModel());
    }

    @Override
    public void preRender(PoseStack poseStack, BellowsItem animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        // Hide all bones except stage_1 (bellows top part)
        hideAllExcept(model, "stage_1");
    }

    private void hideAllExcept(BakedGeoModel model, String visibleBone) {
        setBoneHidden(model, "base", true);
        setBoneHidden(model, "tiny_crucible", true);
        setBoneHidden(model, "stage_1", !visibleBone.equals("stage_1"));
        setBoneHidden(model, "stage_2", !visibleBone.equals("stage_2"));
        setBoneHidden(model, "stage_3", !visibleBone.equals("stage_3"));
        setBoneHidden(model, "stage_4", !visibleBone.equals("stage_4"));
        // Hide coal bones
        setBoneHidden(model, "coal_1", true);
        setBoneHidden(model, "coal_2", true);
        setBoneHidden(model, "coal_3", true);
        setBoneHidden(model, "coal_4", true);
        // Hide ash bones
        setBoneHidden(model, "st_1", true);
        setBoneHidden(model, "st_2", true);
        setBoneHidden(model, "st_3", true);
        setBoneHidden(model, "st_4", true);
        setBoneHidden(model, "st_5", true);
        setBoneHidden(model, "st_6", true);
        // Hide fluid bones
        setBoneHidden(model, "fluid", true);
        setBoneHidden(model, "iron", true);
        setBoneHidden(model, "gold", true);
        setBoneHidden(model, "copper", true);
        setBoneHidden(model, "tin", true);
        setBoneHidden(model, "steel", true);
        setBoneHidden(model, "dark_steel", true);
        // Hide cast molten bones
        setBoneHidden(model, "molten", true);
        setBoneHidden(model, "molten_tin", true);
        setBoneHidden(model, "molten_gold", true);
        setBoneHidden(model, "molten_iron", true);
        setBoneHidden(model, "molten_steel", true);
        setBoneHidden(model, "molten_copper", true);
        setBoneHidden(model, "molten_dark_steel", true);
        // Hide big crucible molten bones
        setBoneHidden(model, "molten_big", true);
        setBoneHidden(model, "molten_tin_big", true);
        setBoneHidden(model, "molten_gold_big", true);
        setBoneHidden(model, "molten_iron_big", true);
        setBoneHidden(model, "molten_steel_big", true);
        setBoneHidden(model, "molten_copper_big", true);
        setBoneHidden(model, "molten_dark_steel_big", true);
    }

    private void setBoneHidden(BakedGeoModel model, String boneName, boolean hidden) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            bone.setHidden(hidden);
        }
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
                                    BellowsItem animatable, BakedGeoModel model, boolean isReRender,
                                    float partialTick, int packedLight, int packedOverlay) {
        // Position model
        poseStack.translate(0.3, 0.2, 0.0);
        // Isometric rotation (~40 degrees on X and Y axes)
        poseStack.mulPose(Axis.XP.rotationDegrees(40f));
        poseStack.mulPose(Axis.YP.rotationDegrees(40f));
        super.scaleModelForRender(SCALE, SCALE, poseStack, animatable, model, isReRender,
                partialTick, packedLight, packedOverlay);
    }
}
