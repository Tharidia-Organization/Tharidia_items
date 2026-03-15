package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.block.alchemist.AlchemistTableBlockEntity;
import com.THproject.tharidia_things.client.model.AlchemistTableModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * GeckoLib renderer for the Alchemist Table multiblock.
 *
 * The model faces SOUTH in Blockbench, but GeckoLib's default
 * rotation assumes models face NORTH. We override render to compensate.
 */
public class AlchemistTableRenderer extends GeoBlockRenderer<AlchemistTableBlockEntity> {

    public AlchemistTableRenderer(BlockEntityRendererProvider.Context context) {
        super(new AlchemistTableModel());
    }

    /**
     * Called by GeckoLib for each bone before it is rendered.
     * We intercept the "Mestolone" bone to force its Y rotation to match
     * the stored craftingAngle — guaranteeing it stays in sync with the hotspot.
     */
    @Override
    public void renderRecursively(PoseStack poseStack, AlchemistTableBlockEntity animatable,
            GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
            VertexConsumer buffer, boolean isReRender, float partialTick,
            int packedLight, int packedOverlay, int colour) {

        if (bone.getName().equals("Mestolone")) {
            float angleDeg = animatable.getInterpolatedCraftingAngle(partialTick);
            // Negative to match the animation's original -360° direction.
            // If the ladle rotates the wrong way in-game, remove the minus sign.
            bone.setRotY((float) Math.toRadians(-angleDeg));
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource,
                buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public RenderType getRenderType(AlchemistTableBlockEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public AABB getRenderBoundingBox(AlchemistTableBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        // Generous bounding box for the L-shape multiblock
        return new AABB(
                pos.getX() - 5, pos.getY(), pos.getZ() - 5,
                pos.getX() + 8, pos.getY() + 3, pos.getZ() + 8);
    }
}
