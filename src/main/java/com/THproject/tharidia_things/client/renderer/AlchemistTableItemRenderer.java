package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.client.model.AlchemistTableItemModel;
import com.THproject.tharidia_things.item.AlchemistTableBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib renderer for AlchemistTableBlockItem in inventory/hand.
 * Scales the large L-shaped model down to fit within a single GUI slot.
 */
public class AlchemistTableItemRenderer extends GeoItemRenderer<AlchemistTableBlockItem> {

    private static final float SCALE = 0.055f;

    public AlchemistTableItemRenderer() {
        super(new AlchemistTableItemModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
                                    AlchemistTableBlockItem animatable, BakedGeoModel model,
                                    boolean isReRender, float partialTick,
                                    int packedLight, int packedOverlay) {
        // Center and position the model within the GUI slot
        poseStack.translate(0.5, 0.5, 0.5);
        // Isometric-style rotation to show the L-shape clearly
        poseStack.mulPose(Axis.XP.rotationDegrees(30f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-45f));

        super.scaleModelForRender(SCALE, SCALE, poseStack, animatable, model,
                isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    public void preRender(PoseStack poseStack, AlchemistTableBlockItem animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
