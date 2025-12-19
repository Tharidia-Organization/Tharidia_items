package com.THproject.tharidia_things.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.THproject.tharidia_things.block.entity.PietroBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class PietroBlockRenderer implements BlockEntityRenderer<PietroBlockEntity> {

    public PietroBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PietroBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
    }
}
