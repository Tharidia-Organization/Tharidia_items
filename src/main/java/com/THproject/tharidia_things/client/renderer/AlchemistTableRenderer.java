package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.block.entity.AlchemistTableBlockEntity;
import com.THproject.tharidia_things.client.model.AlchemistTableModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
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

    @Override
    public void render(AlchemistTableBlockEntity animatable, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (animatable.getLevel() == null) {
            return;
        }

        poseStack.pushPose();

        Direction facing = animatable.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        // World-space translation to align model L-corner (pixel 24,0,-24) with block entity pos
        switch (facing) {
            case NORTH -> poseStack.translate(-2.0, 0, -1.0);
            case SOUTH -> poseStack.translate(2.0, 0, 1.0);
            case EAST  -> poseStack.translate(1.0, 0, -2.0);
            case WEST  -> poseStack.translate(-1.0, 0, 2.0);
        }

        // Rotate model to match block facing (model faces WEST in Blockbench)
        float yRot = switch (facing) {
            case NORTH -> 270f;
            case SOUTH -> 90f;
            case EAST  -> 180f;
            case WEST  -> 0f;
            default -> 270f;
        };
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(-0.5, 0, -0.5);

        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    @Override
    public RenderType getRenderType(AlchemistTableBlockEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }

    @Override
    public boolean shouldRenderOffScreen(AlchemistTableBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(AlchemistTableBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        // Generous bounding box for the L-shape multiblock
        return new AABB(
                pos.getX() - 5, pos.getY(), pos.getZ() - 5,
                pos.getX() + 8, pos.getY() + 3, pos.getZ() + 8
        );
    }
}
