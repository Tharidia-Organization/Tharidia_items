package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.block.CookTableBlock;
import com.THproject.tharidia_things.block.entity.CookTableBlockEntity;
import com.THproject.tharidia_things.client.model.CookTableModel;
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

public class CookTableRenderer extends GeoBlockRenderer<CookTableBlockEntity> {

    public CookTableRenderer(BlockEntityRendererProvider.Context context) {
        super(new CookTableModel());
    }

    @Override
    public void render(CookTableBlockEntity animatable, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (animatable.getLevel() == null) return;

        poseStack.pushPose();

        // The model faces SOUTH in Blockbench; apply 180° so GeckoLib treats it as NORTH-facing.
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        poseStack.translate(-0.5, 0, -0.5);

        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    @Override
    public RenderType getRenderType(CookTableBlockEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }

    @Override
    public boolean shouldRenderOffScreen(CookTableBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(CookTableBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        // Large AABB to cover all 4 facing directions of the 5-block-wide model
        return new AABB(
                pos.getX() - 3, pos.getY(), pos.getZ() - 3,
                pos.getX() + 4, pos.getY() + 2, pos.getZ() + 4
        );
    }
}
