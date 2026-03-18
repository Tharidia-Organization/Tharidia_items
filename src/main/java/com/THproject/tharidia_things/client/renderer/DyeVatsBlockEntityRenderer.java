package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.block.DyeVatsBlock;
import com.THproject.tharidia_things.block.entity.DyeVatsBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class DyeVatsBlockEntityRenderer implements BlockEntityRenderer<DyeVatsBlockEntity> {

    private static final ResourceLocation WATER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/block/dye_vats.png");

    public DyeVatsBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DyeVatsBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!state.getValue(DyeVatsBlock.FILLED)) return;

        int color = be.getCurrentColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 153; // ~60% opacity

        Direction facing = state.getValue(DyeVatsBlock.FACING);

        poseStack.pushPose();

        // Rotate to match blockstate Y rotation
        poseStack.translate(0.5, 0, 0.5);
        float rotY = switch (facing) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.translate(-0.5, 0, -0.5);

        // Water surface quad at Y=13/16
        // Base orientation (north): X from -14/16 to 14/16, Z from 2/16 to 14/16
        float y = 13f / 16f;
        float x1 = -14f / 16f;
        float x2 = 14f / 16f;
        float z1 = 2f / 16f;
        float z2 = 14f / 16f;

        // UV for paint region in the 128x128 texture (pixels 0-28 x, 48-60 y)
        float u1 = 0f;
        float u2 = 28f / 128f;
        float v1 = 48f / 128f;
        float v2 = 60f / 128f;

        // entityTranslucent uses NO_CULL, so quad is visible from both sides
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucent(WATER_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();

        consumer.addVertex(matrix, x1, y, z1).setColor(r, g, b, a)
                .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y, z2).setColor(r, g, b, a)
                .setUv(u1, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y, z2).setColor(r, g, b, a)
                .setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y, z1).setColor(r, g, b, a)
                .setUv(u2, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                .setNormal(0, 1, 0);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(DyeVatsBlockEntity be) {
        return true; // Water quad extends into the dummy block
    }
}
