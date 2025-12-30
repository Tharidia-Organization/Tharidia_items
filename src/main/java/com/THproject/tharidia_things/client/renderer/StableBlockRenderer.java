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
        
        var animals = entity.getAnimals();
        String animalType = entity.getAnimalType();
        
        // Calculate time-based animation for head movement
        long time = entity.getLevel() != null ? entity.getLevel().getGameTime() : 0;
        float animationTime = (time + partialTick) * 0.05F;
        
        // Render animals based on count
        for (int i = 0; i < animals.size(); i++) {
            var animal = animals.get(i);
            poseStack.pushPose();
            
            // Position animals: first two side by side with more space, third one far in back right corner
            if (animals.size() == 1) {
                // Single animal - center
                poseStack.translate(0.5, 0.0, 0.5);
            } else if (animals.size() == 2) {
                // Two animals - side by side with maximum space (especially for cows)
                if (i == 0) {
                    poseStack.translate(0.15, 0.0, 0.45);
                } else {
                    poseStack.translate(0.85, 0.0, 0.45);
                }
            } else if (animals.size() == 3) {
                // Three animals - two adults in front with maximum spacing, baby far in back right corner
                if (i == 0) {
                    poseStack.translate(0.15, 0.0, 0.3);
                } else if (i == 1) {
                    poseStack.translate(0.85, 0.0, 0.3);
                } else {
                    // Third animal (baby) very far in back right corner
                    poseStack.translate(1.5, 0.0, 1.5);
                }
            }
            
            // Render animal with animation offset per animal
            if (animalType.equals("cow")) {
                renderCow(animal.isBaby, animationTime + (i * 1.5F), poseStack, buffer, packedLight);
            } else if (animalType.equals("chicken")) {
                renderChicken(animal.isBaby, animationTime + (i * 1.5F), poseStack, buffer, packedLight);
            }
            
            poseStack.popPose();
        }
        
        // Render eggs for all chickens that have produced them
        if (animalType.equals("chicken")) {
            int totalEggs = 0;
            for (var animal : animals) {
                if (!animal.isBaby && animal.eggCount > 0) {
                    totalEggs += animal.eggCount;
                }
            }
            if (totalEggs > 0) {
                renderEggs(totalEggs, poseStack, buffer, packedLight);
            }
        }
    }
    
    private void renderCow(boolean isBaby, float animationTime, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Rotate 180 degrees
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        
        float scale = isBaby ? 0.5F : 0.8F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -1.5, 0);
        
        // Animate head with slight oscillation using setupAnim
        cowModel.young = isBaby;
        float headXRot = (float) Math.sin(animationTime) * 0.1F;
        float headYRot = (float) Math.cos(animationTime * 0.7F) * 0.15F;
        cowModel.setupAnim(null, 0, 0, 0, headYRot * 57.2958F, headXRot * 57.2958F);
        
        cowModel.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(COW_TEXTURE)), 
            packedLight, OverlayTexture.NO_OVERLAY, -1);
        
        poseStack.popPose();
    }
    
    private void renderChicken(boolean isBaby, float animationTime, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Rotate 180 degrees
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        
        float scale = isBaby ? 0.5F : 0.8F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -1.51, 0);
        
        // Animate head with slight oscillation using setupAnim
        chickenModel.young = isBaby;
        float headXRot = (float) Math.sin(animationTime * 1.2F) * 0.12F;
        float headYRot = (float) Math.cos(animationTime * 0.9F) * 0.2F;
        chickenModel.setupAnim(null, 0, 0, 0, headYRot * 57.2958F, headXRot * 57.2958F);
        
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
