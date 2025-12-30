package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class StableBlockRenderer implements BlockEntityRenderer<StableBlockEntity> {
    
    private static final ResourceLocation COW_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/cow/cow.png");
    private static final ResourceLocation CHICKEN_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/chicken.png");
    
    private final CowModel<net.minecraft.world.entity.animal.Cow> cowModel;
    private final ChickenModel<net.minecraft.world.entity.animal.Chicken> chickenModel;
    
    public StableBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.cowModel = new CowModel<>(context.bakeLayer(ModelLayers.COW));
        this.chickenModel = new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN));
    }
    
    @Override
    public void render(StableBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!entity.hasAnimal()) {
            return;
        }
        
        poseStack.pushPose();
        
        // Center position in the stable
        poseStack.translate(0.5, 0.0, 0.5);
        
        // Render animal
        if (entity.getAnimalType().equals("cow")) {
            renderCow(entity, poseStack, buffer, packedLight);
        } else if (entity.getAnimalType().equals("chicken")) {
            renderChicken(entity, poseStack, buffer, packedLight);
        }
        
        poseStack.popPose();
        
        // Render eggs if chicken has produced them
        if (entity.getAnimalType().equals("chicken") && !entity.isBaby() && entity.getEggCount() > 0) {
            renderEggs(entity.getEggCount(), poseStack, buffer, packedLight);
        }
    }
    
    private void renderCow(StableBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Rotate 180 degrees
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        
        float scale = entity.isBaby() ? 0.5F : 0.8F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -1.5, 0);
        
        // Render cow model directly
        cowModel.young = entity.isBaby();
        cowModel.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(COW_TEXTURE)), 
            packedLight, OverlayTexture.NO_OVERLAY, -1);
        
        poseStack.popPose();
    }
    
    private void renderChicken(StableBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Rotate 180 degrees
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        
        float scale = entity.isBaby() ? 0.5F : 0.8F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -1.51, 0);
        
        // Render chicken model directly
        chickenModel.young = entity.isBaby();
        chickenModel.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(CHICKEN_TEXTURE)), 
            packedLight, OverlayTexture.NO_OVERLAY, -1);
        
        poseStack.popPose();
    }
    
    private void renderEggs(int eggCount, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack eggStack = new ItemStack(Items.EGG);
        
        for (int i = 0; i < eggCount; i++) {
            poseStack.pushPose();
            
            // Position eggs in a row
            float xOffset = -0.2F + (i * 0.2F);
            poseStack.translate(0.5 + xOffset, 0.1, 0.3);
            poseStack.scale(0.3F, 0.3F, 0.3F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            
            mc.getItemRenderer().renderStatic(eggStack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, mc.level, 0);
            
            poseStack.popPose();
        }
    }
}
