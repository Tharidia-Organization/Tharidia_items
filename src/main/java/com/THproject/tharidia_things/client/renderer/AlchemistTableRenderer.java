package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.block.alchemist.AlchemistTableBlockEntity;
import com.THproject.tharidia_things.client.model.AlchemistTableModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
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
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }

    @Override
    public RenderType getRenderType(AlchemistTableBlockEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
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
                pos.getX() + 8, pos.getY() + 3, pos.getZ() + 8);
    }
}
