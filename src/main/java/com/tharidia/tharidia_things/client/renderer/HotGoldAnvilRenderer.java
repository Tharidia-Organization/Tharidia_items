package com.tharidia.tharidia_things.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import org.joml.Matrix4f;

/**
 * Renders the hot gold parallelepiped on top of the anvil
 * Progressive model changes based on hammer strikes
 */
public class HotGoldAnvilRenderer implements BlockEntityRenderer<HotGoldAnvilEntity> {
    
    private final BlockRenderDispatcher blockRenderer;
    private BakedModel[] hotGoldModels;
    
    public HotGoldAnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.hotGoldModels = new BakedModel[5]; // 0-4 strikes
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
        
        // Get the current hammer strikes (0-4)
        int strikes = Math.min(entity.getHammerStrikes(), 4);
        
        // Lazy load models as needed
        if (hotGoldModels[strikes] == null) {
            var modelManager = Minecraft.getInstance().getModelManager();
            var modelLocation = ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/hot_gold_anvil_" + strikes)
            );
            hotGoldModels[strikes] = modelManager.getModel(modelLocation);
        }
        
        poseStack.pushPose();
        
        // Render the appropriate progressive model with full brightness (emissive/glowing effect)
        var vertexConsumer = buffer.getBuffer(RenderType.cutout());
        
        // Use maximum light level for glowing effect (15 for both block and sky light)
        int fullBright = 0xF000F0; // Max light level (240, 240)
        
        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            vertexConsumer,
            null,
            hotGoldModels[strikes],
            1.0f, 1.0f, 1.0f,
            fullBright, // Use full brightness instead of combinedLight
            combinedOverlay,
            ModelData.EMPTY,
            null
        );
        
        poseStack.popPose();
        
        // Render minigame circles if active
        if (entity.isMinigameActive()) {
            renderMinigameCircles(entity, partialTick, poseStack, buffer, combinedLight);
        }
    }
    
    private void renderMinigameCircles(HotGoldAnvilEntity entity, float partialTick, 
                                       PoseStack poseStack, MultiBufferSource buffer, int combinedLight) {
        poseStack.pushPose();
        
        // Position above the anvil
        float circleX = entity.getCircleX();
        float circleZ = entity.getCircleZ();
        float yOffset = 0.13f; // Height above anvil
        
        poseStack.translate(circleX, yOffset, circleZ);
        
        // Get current and target radii
        float currentRadius = entity.getCurrentRadius();
        float targetRadius = entity.getTargetRadius();
        
        // Calculate accuracy for color feedback
        float accuracy = Math.abs(currentRadius - targetRadius);
        float tolerance = com.tharidia.tharidia_things.Config.SMITHING_TOLERANCE.get().floatValue();
        
        // Determine color based on accuracy
        float r, g, b;
        if (accuracy <= tolerance * 0.1f) {
            // Perfect - Green
            r = 0.2f; g = 1.0f; b = 0.2f;
        } else if (accuracy <= tolerance * 0.2f) {
            // Good - Light green/yellow
            r = 0.6f; g = 1.0f; b = 0.2f;
        } else if (accuracy <= tolerance * 0.3f) {
            // OK - Yellow
            r = 1.0f; g = 1.0f; b = 0.2f;
        } else {
            // Bad - Red
            r = 1.0f; g = 0.2f; b = 0.2f;
        }
        
        // Render target circle (fixed, semi-transparent white)
        renderCircle(poseStack, buffer, targetRadius, 1.0f, 1.0f, 1.0f, 0.4f, combinedLight);
        
        // Render current circle (expanding/contracting, colored by accuracy)
        renderCircle(poseStack, buffer, currentRadius, r, g, b, 0.7f, combinedLight);
        
        poseStack.popPose();
    }
    
    private void renderCircle(PoseStack poseStack, MultiBufferSource buffer, float radius, 
                             float r, float g, float b, float alpha, int combinedLight) {
        // Use lines render type to draw circle outline
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        
        int segments = 64; // More segments for smoother circle
        float angleStep = (float) (2 * Math.PI / segments);
        float thickness = 0.008f; // Spessore della linea
        
        // Render 2 concentric circles for thickness
        for (int layer = 0; layer < 2; layer++) {
            float offset = (layer - 0.5f) * thickness;
            float layerRadius = radius + offset;
            
            // Render circle as connected line segments
            for (int i = 0; i < segments; i++) {
                float angle1 = i * angleStep;
                float angle2 = ((i + 1) % segments) * angleStep;
                
                float x1 = (float) Math.cos(angle1) * layerRadius;
                float z1 = (float) Math.sin(angle1) * layerRadius;
                float x2 = (float) Math.cos(angle2) * layerRadius;
                float z2 = (float) Math.sin(angle2) * layerRadius;
                
                // Line segment
                vertexConsumer.addVertex(matrix, x1, 0, z1)
                    .setColor(r, g, b, alpha)
                    .setNormal(0, 1, 0);
                vertexConsumer.addVertex(matrix, x2, 0, z2)
                    .setColor(r, g, b, alpha)
                    .setNormal(0, 1, 0);
            }
        }
    }
}
