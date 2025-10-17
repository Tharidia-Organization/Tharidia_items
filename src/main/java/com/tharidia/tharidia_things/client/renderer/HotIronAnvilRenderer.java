package com.tharidia.tharidia_things.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tharidia.tharidia_things.block.entity.HotIronAnvilEntity;
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
 * Renders the hot iron parallelepiped on top of the anvil
 * Progressive model changes based on hammer strikes
 */
public class HotIronAnvilRenderer implements BlockEntityRenderer<HotIronAnvilEntity> {
    
    private final BlockRenderDispatcher blockRenderer;
    private BakedModel[] hotIronModels;
    
    public HotIronAnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.hotIronModels = new BakedModel[5]; // 0-4 strikes
    }
    
    @Override
    public boolean shouldRenderOffScreen(HotIronAnvilEntity entity) {
        return true;
    }
    
    @Override
    public int getViewDistance() {
        return 256;
    }
    
    @Override
    public void render(HotIronAnvilEntity entity, float partialTick, PoseStack poseStack, 
                      MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        
        // Get the current hammer strikes (0-4)
        int strikes = Math.min(entity.getHammerStrikes(), 4);
        
        // Lazy load models as needed
        if (hotIronModels[strikes] == null) {
            var modelManager = Minecraft.getInstance().getModelManager();
            var modelLocation = ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/hot_iron_anvil_" + strikes)
            );
            hotIronModels[strikes] = modelManager.getModel(modelLocation);
        }
        
        poseStack.pushPose();
        
        // Render the appropriate progressive model
        var vertexConsumer = buffer.getBuffer(RenderType.cutout());
        
        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            vertexConsumer,
            null,
            hotIronModels[strikes],
            1.0f, 1.0f, 1.0f,
            combinedLight,
            combinedOverlay,
            ModelData.EMPTY,
            null
        );
        
        poseStack.popPose();
    }
}
