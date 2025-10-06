package com.tharidia.tharidia_things.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import com.tharidia.tharidia_things.client.model.PietroBlockModel;
import mod.azure.azurelib.common.api.client.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class PietroBlockRenderer extends GeoBlockRenderer<PietroBlockEntity> {

    public PietroBlockRenderer() {
        super(new PietroBlockModel());
    }

    @Override
    public RenderType getRenderType(PietroBlockEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
