package com.THproject.tharidia_things.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.THproject.tharidia_things.block.PietroBlock;
import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import com.THproject.tharidia_things.client.model.PietroBlockModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Hybrid renderer for PietroBlock:
 * - Levels 0-1: Uses GeckoLib rendering (realm_stage_1 model)
 * - Levels 2-4: Uses standard JSON model rendering (handled by Minecraft)
 *
 * IMPORTANT: For levels 0-1, only the LOWER half renders the full GeckoLib model.
 * The UPPER half does nothing (the GeckoLib model includes both parts).
 */
public class PietroBlockRenderer extends GeoBlockRenderer<PietroBlockEntity> {

    public PietroBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new PietroBlockModel());
    }

    @Override
    public void render(PietroBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        // Safety check
        if (blockEntity.getLevel() == null) {
            return;
        }

        var blockState = blockEntity.getBlockState();

        // Get the realm level from block state
        int realmLevel = blockState.getValue(PietroBlock.REALM_LEVEL);

        // Only render with GeckoLib for levels 0-1
        if (realmLevel > 1) {
            // Levels 2-4: Standard rendering handled by Minecraft's block renderer
            // We don't render anything here - the JSON model is used automatically
            return;
        }

        // For levels 0-1 with GeckoLib:
        // Only render from the LOWER half (the model includes both halves)
        DoubleBlockHalf half = blockState.getValue(PietroBlock.HALF);
        if (half == DoubleBlockHalf.UPPER) {
            // UPPER half: don't render anything, the LOWER half's model covers this
            return;
        }

        // LOWER half, levels 0-1: Render the full GeckoLib model
        // Translate to compensate for model offset (8 pixels = 0.5 blocks to north-west)
        poseStack.pushPose();
        poseStack.translate(0.0, 0.0, 0.0);
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public RenderType getRenderType(PietroBlockEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        // Use cutout for transparency support (if texture has alpha)
        return RenderType.entityCutout(texture);
    }
}
