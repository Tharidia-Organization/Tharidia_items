package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.block.SmithingFurnaceBlock;
import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import com.THproject.tharidia_things.client.model.SmithingFurnaceModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * GeckoLib renderer for the Smithing Furnace.
 * Handles:
 * - Model scaling (Blockbench coordinates to Minecraft blocks)
 * - Rotation based on FACING direction
 * - Extended render bounds for the large 5x2x3 model
 */
public class SmithingFurnaceRenderer extends GeoBlockRenderer<SmithingFurnaceBlockEntity> {

    public SmithingFurnaceRenderer(BlockEntityRendererProvider.Context context) {
        super(new SmithingFurnaceModel());
    }

    @Override
    public void render(SmithingFurnaceBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        if (blockEntity.getLevel() == null) {
            return;
        }

        var blockState = blockEntity.getBlockState();
        Direction facing = blockState.getValue(SmithingFurnaceBlock.FACING);

        poseStack.pushPose();

        // Move to center of block for rotation
        poseStack.translate(0.5, 0, 0.5);

        // Rotate based on facing direction
        // North = 0, South = 180, West = 90, East = 270
        float rotation = switch (facing) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f; // NORTH
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Move back from center
        poseStack.translate(-0.5, 0, -0.5);

        // GeckoLib handles the model scale automatically
        // No manual scaling needed - Blockbench exports at correct scale

        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    @Override
    public RenderType getRenderType(SmithingFurnaceBlockEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        // Use entityCutoutNoCull to render both sides of faces (no backface culling)
        // This allows internal meshes to be visible
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public boolean shouldRenderOffScreen(SmithingFurnaceBlockEntity blockEntity) {
        // Always render - the model is larger than a single block
        return true;
    }

    @Override
    public int getViewDistance() {
        // Increase view distance for large structure
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(SmithingFurnaceBlockEntity blockEntity) {
        // The model is 5x2x3 blocks, extend bounding box accordingly
        var pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                pos.getX() + 6, pos.getY() + 3, pos.getZ() + 4
        );
    }
}
