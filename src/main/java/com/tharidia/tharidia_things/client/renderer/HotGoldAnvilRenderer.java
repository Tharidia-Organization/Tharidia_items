package com.tharidia.tharidia_things.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tharidia.tharidia_things.block.entity.HotGoldAnvilEntity;
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
 * Renders the hot gold parallelepiped on top of the anvil
 */
public class HotGoldAnvilRenderer implements BlockEntityRenderer<HotGoldAnvilEntity> {
    
    private final BlockRenderDispatcher blockRenderer;
    private BakedModel hotGoldModel;
    
    public HotGoldAnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.hotGoldModel = null;
    }
    
    @Override
    public boolean shouldRenderOffScreen(HotGoldAnvilEntity entity) {
        return true;
    }
    
    @Override
    public int getViewDistance() {
        return 256;
    }
    
    @Override
    public void render(HotGoldAnvilEntity entity, float partialTick, PoseStack poseStack, 
                      MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        
        // Load 3D block model once (explicitly, not from item)
        if (hotGoldModel == null) {
            var modelManager = Minecraft.getInstance().getModelManager();
            var modelLocation = ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/hot_gold_anvil")
            );
            hotGoldModel = modelManager.getModel(modelLocation);
        }
        
        poseStack.pushPose();
        
        // Render the 3D model
        var vertexConsumer = buffer.getBuffer(RenderType.cutout());
        
        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            vertexConsumer,
            null,
            hotGoldModel,
            1.0f, 1.0f, 1.0f,
            combinedLight,
            combinedOverlay,
            ModelData.EMPTY,
            null
        );
        
        poseStack.popPose();
    }
}
