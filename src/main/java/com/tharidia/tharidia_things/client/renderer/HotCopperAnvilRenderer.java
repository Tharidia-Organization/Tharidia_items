package com.tharidia.tharidia_things.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tharidia.tharidia_things.block.entity.HotCopperAnvilEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer for hot copper on the anvil
 */
public class HotCopperAnvilRenderer implements BlockEntityRenderer<HotCopperAnvilEntity> {
    
    private final BlockRenderDispatcher blockRenderer;
    private BakedModel hotCopperModel;
    
    public HotCopperAnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.hotCopperModel = null;
    }
    
    @Override
    public boolean shouldRenderOffScreen(HotCopperAnvilEntity entity) {
        return true;
    }
    
    @Override
    public int getViewDistance() {
        return 256;
    }
    
    @Override
    public void render(HotCopperAnvilEntity entity, float partialTick, PoseStack poseStack, 
                      MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        
        // Lazy load the model on first render
        if (hotCopperModel == null) {
            var modelManager = Minecraft.getInstance().getModelManager();
            var modelLocation = ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/hot_copper_anvil")
            );
            hotCopperModel = modelManager.getModel(modelLocation);
        }
        
        poseStack.pushPose();
        
        // Render the hot copper model
        var vertexConsumer = buffer.getBuffer(RenderType.cutout());
        
        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            vertexConsumer,
            null,
            hotCopperModel,
            1.0f, 1.0f, 1.0f,
            combinedLight,
            combinedOverlay,
            ModelData.EMPTY,
            null
        );
        
        poseStack.popPose();
    }
}
