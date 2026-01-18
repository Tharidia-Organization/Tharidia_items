package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.phys.AABB;

public class StableBlockRenderer implements BlockEntityRenderer<StableBlockEntity> {

    private static final float MODEL_SCALE = 2.0F;

    // Overlay model locations for conditional rendering
    private static final ModelResourceLocation HAY_MODEL = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/stall_hay"));
    private static final ModelResourceLocation WATER_MODEL = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/stall_water"));

    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockRenderDispatcher blockRenderer;
    private final Map<EntityType<?>, EntityModel<?>> modelCache = new HashMap<>();
    private final Map<EntityType<?>, ResourceLocation> textureCache = new HashMap<>();

    // Cached overlay models (loaded lazily)
    private BakedModel hayModel = null;
    private BakedModel waterModel = null;
    private boolean modelsLoaded = false;

    public StableBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderDispatcher = context.getEntityRenderer();
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    private void loadOverlayModels() {
        if (!modelsLoaded) {
            var modelManager = Minecraft.getInstance().getModelManager();
            hayModel = modelManager.getModel(HAY_MODEL);
            waterModel = modelManager.getModel(WATER_MODEL);
            modelsLoaded = true;
        }
    }

    private void renderBlockModel(StableBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // Center the scale transformation so model scales from center
        float offset = (1.0F - MODEL_SCALE) / 2.0F;
        poseStack.translate(offset, 0, offset);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        // Get the block state and render the model
        var blockState = entity.getBlockState();
        BakedModel model = blockRenderer.getBlockModel(blockState);

        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            buffer.getBuffer(RenderType.cutout()),
            blockState,
            model,
            1.0F, 1.0F, 1.0F,
            packedLight,
            packedOverlay
        );

        poseStack.popPose();
    }

    private void renderOverlayModel(BakedModel model, StableBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        renderOverlayModel(model, entity, poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F);
    }

    private void renderOverlayModel(BakedModel model, StableBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, float r, float g, float b) {
        if (model == null) return;

        poseStack.pushPose();

        // Apply same transformation as main model
        float offset = (1.0F - MODEL_SCALE) / 2.0F;
        poseStack.translate(offset, 0, offset);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            buffer.getBuffer(RenderType.translucent()),
            entity.getBlockState(),
            model,
            r, g, b,
            packedLight,
            packedOverlay
        );

        poseStack.popPose();
    }

    @Override
    public void render(StableBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // Load overlay models if not yet loaded
        loadOverlayModels();

        // Render the block model with 1.5x scale
        renderBlockModel(entity, poseStack, buffer, packedLight, packedOverlay);

        // Render water overlay if there is water (with vanilla water blue tint #3F76E4)
        if (entity.hasWater()) {
            renderOverlayModel(waterModel, entity, poseStack, buffer, packedLight, packedOverlay, 0.247F, 0.463F, 0.894F);
        }

        // Render hay overlay if there is food
        if (entity.getFoodAmount() > 0) {
            renderOverlayModel(hayModel, entity, poseStack, buffer, packedLight, packedOverlay);
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

    @Override
    public boolean shouldRenderOffScreen(StableBlockEntity blockEntity) {
        // Always render because the scaled model extends beyond the block bounds
        return true;
    }

    @Override
    public int getViewDistance() {
        // Increase view distance since the model is large and extends beyond normal block bounds
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(StableBlockEntity blockEntity) {
        // The model is scaled 2.0x and extends beyond the block bounds
        var pos = blockEntity.getBlockPos();
        return new AABB(
            pos.getX() - 3, pos.getY(), pos.getZ() - 3,
            pos.getX() + 5, pos.getY() + 3, pos.getZ() + 5
        );
    }
}
