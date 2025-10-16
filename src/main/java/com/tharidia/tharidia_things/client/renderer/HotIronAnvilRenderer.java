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

/**
 * Renders the hot iron parallelepiped on top of the anvil
 */
public class HotIronAnvilRenderer implements BlockEntityRenderer<HotIronAnvilEntity> {
    
    // Don't use ModelResourceLocation for block entity rendering - just use direct item rendering
    private static final ResourceLocation HOT_IRON_ITEM = ResourceLocation.fromNamespaceAndPath(
        "tharidiathings", "hot_iron"
    );
    
    private final BlockRenderDispatcher blockRenderer;
    
    public HotIronAnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
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
        
        poseStack.pushPose();
        
        // Position above the anvil (entity is at marker block position)
        poseStack.translate(0.5, 0, 0.5);
        
        // Render the hot iron item as a 3D model
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        var hotIronStack = new net.minecraft.world.item.ItemStack(com.tharidia.tharidia_things.TharidiaThings.HOT_IRON.get());
        
        itemRenderer.renderStatic(
            hotIronStack,
            net.minecraft.world.item.ItemDisplayContext.GROUND,
            combinedLight,
            combinedOverlay,
            poseStack,
            buffer,
            entity.getLevel(),
            0
        );
        
        poseStack.popPose();
    }
}
