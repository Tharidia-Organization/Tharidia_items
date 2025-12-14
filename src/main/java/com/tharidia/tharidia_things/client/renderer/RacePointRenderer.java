package com.tharidia.tharidia_things.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tharidia.tharidia_things.entity.RacePointEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;

/**
 * Renderer for the floating race point entity
 */
public class RacePointRenderer extends EntityRenderer<RacePointEntity> {
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/enchanting_table_book.png");
    
    public RacePointRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public void render(RacePointEntity entity, float entityYaw, float partialTick, PoseStack poseStack, 
                      MultiBufferSource bufferSource, int packedLight) {
        // Debug: Log that renderer is being called
        System.out.println("Rendering RacePointEntity at " + entity.getX() + ", " + entity.getY() + ", " + entity.getZ());
        
        poseStack.pushPose();
        
        // Position at entity location
        poseStack.translate(0, entity.getBbHeight() / 2, 0);
        
        // Bobbing animation
        float bobAmount = (float) Math.sin((entity.tickCount + partialTick) * 0.05) * 0.1f;
        poseStack.translate(0, bobAmount, 0);
        
        // Rotate with entity
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        
        // Render race-specific item
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        var itemStack = getRaceItem(entity.getRaceName());
        itemRenderer.renderStatic(itemStack, ItemDisplayContext.GROUND, 
            packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), entity.getId());
        
        poseStack.popPose();
        
        // Draw race name above entity with proper capitalization
        var raceInfo = com.tharidia.tharidia_things.character.RaceData.getRaceInfo(entity.getRaceName());
        String displayName = raceInfo != null ? raceInfo.name : entity.getRaceName();
        renderNameTag(entity, Component.literal(displayName), poseStack, bufferSource, packedLight, 0);
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
    
    private net.minecraft.world.item.ItemStack getRaceItem(String raceName) {
        return switch (raceName) {
            case "umano" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
            case "elfo" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW);
            case "nano" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
            case "dragonide" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_DRAGON_SPAWN_EGG);
            case "orcho" -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            default -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE);
        };
    }
    
    @Override
    public ResourceLocation getTextureLocation(RacePointEntity entity) {
        return TEXTURE;
    }
}
