package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Random;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class StableBlockRenderer implements BlockEntityRenderer<StableBlockEntity> {
    
    private static final ResourceLocation WATER_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/water_still.png");
    private static final ItemStack[] FOOD_ITEMS = new ItemStack[] {
        new ItemStack(Items.WHEAT),
        new ItemStack(Items.APPLE),
        new ItemStack(Items.POTATO),
        new ItemStack(Items.SWEET_BERRIES),
        new ItemStack(Items.CARROT)
    };
    
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final Map<EntityType<?>, EntityModel<?>> modelCache = new HashMap<>();
    private final Map<EntityType<?>, ResourceLocation> textureCache = new HashMap<>();
    private final Random random = new Random();
    
    public StableBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderDispatcher = context.getEntityRenderer();
    }
    
    @Override
    public void render(StableBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // Render water only if there is water
        if (entity.hasWater()) {
            renderTroughWater(poseStack, buffer, packedLight);
        }
        
        // Render food in feeder if there is food
        if (entity.getFoodAmount() > 0) {
            renderFeeder(entity, poseStack, buffer, packedLight);
        }
        
        if (!entity.hasAnimal()) {
            return;
        }
        
        var animals = entity.getAnimals();
        EntityType<?> animalType = entity.getAnimalType();
        
        if (animalType == null) {
            return;
        }
        
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
                poseStack.translate(0.8, 0.0, -0.35);
            } else if (animals.size() == 2) {
                // Two animals - side by side with maximum space (especially for cows)
                if (i == 0) {
                    poseStack.translate(0.5, 0.0, -0.4);
                } else {
                    poseStack.translate(1.2, 0.0, -0.4);
                }
            } else if (animals.size() == 3) {
                // Three animals - two adults in front with maximum spacing, baby far in back right corner
                if (i == 0) {
                    poseStack.translate(0.5, 0.0, -0.4);
                } else if (i == 1) {
                    poseStack.translate(1.2, 0.0, -0.4);
                } else {
                    // Third animal (baby) very far in back right corner
                    poseStack.translate(0.5, 0.0, 1);
                }
            }
            
            // Render animal with animation offset per animal
            renderAnimal(animal.entityType, animal.isBaby, animationTime + (i * 1.5F), poseStack, buffer, packedLight);
            
            poseStack.popPose();
        }
        
        // Render eggs for all chickens that have produced them
        if (animalType == EntityType.CHICKEN) {
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
    
    private void renderTroughWater(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        float px = 1F / 16F;
        float y = (5F * px) + 0.001F; // 3px above ground
        float x1 = 23F * px;
        float x2 = 31F * px;
        float z1 = 12.5F * px;
        float z2 = 31F * px;
        
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(WATER_TEXTURE));
        var pose = poseStack.last();
        var matrix = pose.pose();
        var normalMatrix = pose.normal();
        float r = 0.2F;
        float g = 0.4F;
        float b = 1.0F;
        float a = 0.75F;
        
        addWaterVertex(consumer, matrix, normalMatrix, x1, y, z1, 0F, 0F, r, g, b, a, packedLight);
        addWaterVertex(consumer, matrix, normalMatrix, x2, y, z1, 1F, 0F, r, g, b, a, packedLight);
        addWaterVertex(consumer, matrix, normalMatrix, x2, y, z2, 1F, 1F, r, g, b, a, packedLight);
        addWaterVertex(consumer, matrix, normalMatrix, x1, y, z2, 0F, 1F, r, g, b, a, packedLight);
        
        poseStack.popPose();
    }

    private void addWaterVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                float x, float y, float z, float u, float v,
                                float r, float g, float b, float a, int packedLight) {
        Vector4f position = new Vector4f(x, y, z, 1.0F);
        position.mul(matrix);
        
        Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);
        normal.mul(normalMatrix);
        
        consumer.addVertex(position.x(), position.y(), position.z())
            .setColor(r, g, b, a)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(normal.x(), normal.y(), normal.z());
    }
    
    @SuppressWarnings("unchecked")
    private void renderAnimal(EntityType<?> entityType, boolean isBaby, float animationTime, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Rotate 180 degrees
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        
        float scale = isBaby ? 0.5F : 0.8F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -1.5, 0);
        
        try {
            // Get cached model and texture, or create from ModelLayerLocation
            EntityModel<?> model = modelCache.get(entityType);
            ResourceLocation texture = textureCache.get(entityType);
            
            if (model == null) {
                // Create model for this entity type
                model = createModelForEntity(entityType);
                if (model != null) {
                    modelCache.put(entityType, model);
                }
            }
            
            if (texture == null) {
                // Build texture path from entity type
                texture = getTextureForEntity(entityType);
                if (texture != null) {
                    textureCache.put(entityType, texture);
                }
            }
            
            if (model == null) {
                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                TharidiaThings.LOGGER.warn("[STABLE RENDER] No model available for {}", entityId);
            }
            
            if (texture == null) {
                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                TharidiaThings.LOGGER.warn("[STABLE RENDER] No texture available for {}", entityId);
            }
            
            if (model != null && texture != null) {
                // Animate head with slight oscillation using setupAnim
                model.young = isBaby;
                
                // Try to animate, but skip if it fails (some models require non-null entity)
                try {
                    float headXRot = (float) Math.sin(animationTime) * 0.1F;
                    float headYRot = (float) Math.cos(animationTime * 0.7F) * 0.15F;
                    ((EntityModel<LivingEntity>) model).setupAnim(null, 0, 0, 0, headYRot * 57.2958F, headXRot * 57.2958F);
                } catch (NullPointerException e) {
                    // Some models (like LlamaModel) require a non-null entity for setupAnim
                }
                
                model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), 
                    packedLight, OverlayTexture.NO_OVERLAY, -1);
            }
        } catch (Exception e) {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            TharidiaThings.LOGGER.error("[STABLE RENDER] Exception rendering {}: {}", entityId, e.getMessage(), e);
        }
        
        poseStack.popPose();
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private EntityModel<?> createModelForEntity(EntityType<?> entityType) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                // Create a temporary entity to get its renderer
                net.minecraft.world.entity.Entity entity = entityType.create(mc.level);
                
                if (entity == null) {
                    ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                    TharidiaThings.LOGGER.warn("[STABLE] Failed to create entity for type: {}", entityId);
                    return null;
                }
                
                if (!(entity instanceof LivingEntity)) {
                    ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                    TharidiaThings.LOGGER.warn("[STABLE] Entity {} is not a LivingEntity, type: {}", entityId, entity.getClass().getSimpleName());
                    entity.discard();
                    return null;
                }
                
                LivingEntity livingEntity = (LivingEntity) entity;
                EntityRenderer renderer = entityRenderDispatcher.getRenderer(livingEntity);
                
                if (!(renderer instanceof LivingEntityRenderer)) {
                    ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                    TharidiaThings.LOGGER.warn("[STABLE] Renderer for {} is not LivingEntityRenderer, type: {}", entityId, renderer.getClass().getSimpleName());
                    entity.discard();
                    return null;
                }
                
                LivingEntityRenderer livingRenderer = (LivingEntityRenderer) renderer;
                EntityModel<?> model = livingRenderer.getModel();
                
                // Discard the temporary entity immediately
                entity.discard();
                
                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                TharidiaThings.LOGGER.info("[STABLE] Successfully loaded model for {}", entityId);
                
                return model;
            }
        } catch (Exception e) {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            TharidiaThings.LOGGER.error("[STABLE] Exception loading model for {}: {}", entityId, e.getMessage());
        }
        return null;
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResourceLocation getTextureForEntity(EntityType<?> entityType) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                // Create a temporary entity to get its texture
                net.minecraft.world.entity.Entity entity = entityType.create(mc.level);
                
                if (entity == null) {
                    ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                    TharidiaThings.LOGGER.warn("[STABLE TEXTURE] Failed to create entity for type: {}", entityId);
                    return null;
                }
                
                if (!(entity instanceof LivingEntity)) {
                    ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                    TharidiaThings.LOGGER.warn("[STABLE TEXTURE] Entity {} is not a LivingEntity", entityId);
                    entity.discard();
                    return null;
                }
                
                LivingEntity livingEntity = (LivingEntity) entity;
                EntityRenderer renderer = entityRenderDispatcher.getRenderer(livingEntity);
                
                if (!(renderer instanceof LivingEntityRenderer)) {
                    ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                    TharidiaThings.LOGGER.warn("[STABLE TEXTURE] Renderer for {} is not LivingEntityRenderer", entityId);
                    entity.discard();
                    return null;
                }
                
                LivingEntityRenderer livingRenderer = (LivingEntityRenderer) renderer;
                ResourceLocation texture = livingRenderer.getTextureLocation(livingEntity);
                
                // Discard the temporary entity immediately
                entity.discard();
                
                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                TharidiaThings.LOGGER.info("[STABLE TEXTURE] Successfully loaded texture for {}: {}", entityId, texture);
                
                return texture;
            }
        } catch (Exception e) {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            TharidiaThings.LOGGER.error("[STABLE TEXTURE] Exception loading texture for {}: {}", entityId, e.getMessage());
        }
        
        // Fallback: try standard path
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId != null) {
            return ResourceLocation.fromNamespaceAndPath(
                entityId.getNamespace(),
                "textures/entity/" + entityId.getPath() + ".png"
            );
        }
        
        return null;
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
    
    private void renderFeeder(StableBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        int foodAmount = entity.getFoodAmount();
        
        // Feeder coordinates: top-left corner of model, x0 z1 to x6 z18, from 1px to 5px above ground
        float px = 1F / 16F;
        float minX = -15F * px; // Top-left corner in model coordinates
        float maxX = -18F * px;
        float minZ = -15F * px;
        float maxZ = 10F * px;
        float minY = 0F * px; // 1 pixel from ground
        float maxY = 6F * px; // 5 pixels from ground
        
        // Use entity position as seed for consistent random placement
        long seed = entity.getBlockPos().asLong();
        random.setSeed(seed);
        
        // Render food items scattered in the feeder volume
        int itemsToRender = Math.min(foodAmount, 128); // Cap visual items at 96 for performance (tripled density)
        
        for (int i = 0; i < itemsToRender; i++) {
            poseStack.pushPose();
            
            // Use index-based seed for consistent positioning per item
            random.setSeed(seed + i);
            
            // Random position within feeder volume - ensure proper 3D distribution
            float x = minX + random.nextFloat() * Math.abs(maxX - minX);
            float y = minY + random.nextFloat() * Math.abs(maxY - minY);
            float z = minZ + random.nextFloat() * Math.abs(maxZ - minZ);
            
            poseStack.translate(x, y, z);
            
            // Random scale variation - increased to 1.5x base size
            float scale = 0.225F + random.nextFloat() * 0.15F; // 1.5x larger (0.15 * 1.5 = 0.225)
            poseStack.scale(scale, scale, scale);
            
            // Random rotation on all axes for scattered look
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360F));
            
            // Pick random food item
            ItemStack foodItem = FOOD_ITEMS[random.nextInt(FOOD_ITEMS.length)];
            
            mc.getItemRenderer().renderStatic(foodItem, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, mc.level, 0);
            
            poseStack.popPose();
        }
    }
}
