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
import net.minecraft.util.RandomSource;
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
    private static final ModelResourceLocation MILK_MODEL = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/stall_milk"));

    // 10 manure levels (10%, 20%, ..., 100%)
    private static final ModelResourceLocation[] MANURE_MODELS = new ModelResourceLocation[10];
    static {
        for (int i = 0; i < 10; i++) {
            MANURE_MODELS[i] = ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/stall_shit_" + (i + 1)));
        }
    }

    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockRenderDispatcher blockRenderer;
    private final Map<EntityType<?>, EntityModel<?>> modelCache = new HashMap<>();
    private final Map<EntityType<?>, ResourceLocation> textureCache = new HashMap<>();

    // Cached overlay models (loaded lazily)
    private BakedModel hayModel = null;
    private BakedModel waterModel = null;
    private BakedModel milkModel = null;
    private BakedModel[] manureModels = new BakedModel[10];
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
            milkModel = modelManager.getModel(MILK_MODEL);
            for (int i = 0; i < 10; i++) {
                manureModels[i] = modelManager.getModel(MANURE_MODELS[i]);
            }
            modelsLoaded = true;
        }
    }

    private void renderBlockModel(StableBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // Center the scale transformation so model scales from center
        float offset = (1.0F - MODEL_SCALE) / 2.0F;
        poseStack.translate(offset, 0, offset);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        // Get the block state and render the model with proper shading
        var blockState = entity.getBlockState();
        var level = entity.getLevel();
        var pos = entity.getBlockPos();
        BakedModel model = blockRenderer.getBlockModel(blockState);

        // Use tesselateBlock for proper ambient occlusion and directional shading
        blockRenderer.getModelRenderer().tesselateBlock(
            level,
            model,
            blockState,
            pos,
            poseStack,
            buffer.getBuffer(RenderType.cutout()),
            false,  // checkSides - false to render all faces
            RandomSource.create(),
            blockState.getSeed(pos),
            packedOverlay
        );

        poseStack.popPose();
    }

    private void renderOverlayModel(BakedModel model, StableBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (model == null) return;

        poseStack.pushPose();

        // Apply same transformation as main model
        float offset = (1.0F - MODEL_SCALE) / 2.0F;
        poseStack.translate(offset, 0, offset);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        var blockState = entity.getBlockState();
        var level = entity.getLevel();
        var pos = entity.getBlockPos();

        // Use tesselateBlock for proper ambient occlusion and directional shading
        blockRenderer.getModelRenderer().tesselateBlock(
            level,
            model,
            blockState,
            pos,
            poseStack,
            buffer.getBuffer(RenderType.translucent()),
            false,  // checkSides - false to render all faces
            RandomSource.create(),
            blockState.getSeed(pos),
            packedOverlay
        );

        poseStack.popPose();
    }

    private void renderOverlayModelTinted(BakedModel model, StableBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, float r, float g, float b) {
        renderOverlayModelTintedScaled(model, entity, poseStack, buffer, packedLight, packedOverlay, r, g, b, 1.0F);
    }

    private void renderOverlayModelScaled(BakedModel model, StableBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, float yScale) {
        if (model == null || yScale <= 0) return;

        poseStack.pushPose();

        // Apply same transformation as main model, but with Y scaling for fill level
        float offset = (1.0F - MODEL_SCALE) / 2.0F;
        poseStack.translate(offset, 0, offset);
        // Scale X and Z normally, but Y is scaled by fill level
        poseStack.scale(MODEL_SCALE, MODEL_SCALE * yScale, MODEL_SCALE);

        var blockState = entity.getBlockState();
        var level = entity.getLevel();
        var pos = entity.getBlockPos();

        blockRenderer.getModelRenderer().tesselateBlock(
            level,
            model,
            blockState,
            pos,
            poseStack,
            buffer.getBuffer(RenderType.translucent()),
            false,
            RandomSource.create(),
            blockState.getSeed(pos),
            packedOverlay
        );

        poseStack.popPose();
    }

    private void renderOverlayModelTintedScaled(BakedModel model, StableBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, float r, float g, float b, float yScale) {
        if (model == null || yScale <= 0) return;

        poseStack.pushPose();

        // Apply same transformation as main model, but with Y scaling for fill level
        float offset = (1.0F - MODEL_SCALE) / 2.0F;
        poseStack.translate(offset, 0, offset);
        // Scale X and Z normally, but Y is scaled by fill level
        poseStack.scale(MODEL_SCALE, MODEL_SCALE * yScale, MODEL_SCALE);

        // For tinted overlays (like water), use renderModel with color multiplier
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
        // Scale Y based on remaining water level
        if (entity.hasWater()) {
            float waterLevel = entity.getWaterLevel();
            renderOverlayModelTintedScaled(waterModel, entity, poseStack, buffer, packedLight, packedOverlay, 0.247F, 0.463F, 0.894F, waterLevel);
        }

        // Render hay overlay if there is food
        // Scale Y based on remaining food level
        if (entity.getFoodAmount() > 0) {
            float foodLevel = entity.getFoodLevel();
            renderOverlayModelScaled(hayModel, entity, poseStack, buffer, packedLight, packedOverlay, foodLevel);
        }

        // Render manure overlays based on manure level (10%, 20%, ..., 100% thresholds)
        int manureAmount = entity.getManureAmount();
        for (int i = 0; i < 10; i++) {
            if (manureAmount >= (i + 1) * 10) {
                renderOverlayModel(manureModels[i], entity, poseStack, buffer, packedLight, packedOverlay);
            }
        }

        // Render milk in bucket if there's an adult milk-producing animal
        if (entity.hasMilkProducingAnimal()) {
            // Render milk with a creamy white tint
            renderOverlayModelTinted(milkModel, entity, poseStack, buffer, packedLight, packedOverlay, 0.98F, 0.98F, 0.95F);
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
            
            // Position animals: parents on hay patches (SW and NW), baby in center
            if (animals.size() == 1) {
                // Single animal - on hay_patch_sw (front-left)
                poseStack.translate(-1.0, 0.4, -0.3);
            } else if (animals.size() == 2) {
                // Two animals - one on hay_patch_sw, one on hay_patch_nw
                if (i == 0) {
                    // Parent 1 - hay_patch_sw (front-left)
                    poseStack.translate(-1.0, 0.4, -0.3);
                } else {
                    // Parent 2 - hay_patch_nw (back-left)
                    poseStack.translate(-1.0, 0.4, 1.2);
                }
            } else if (animals.size() == 3) {
                // Three animals - two parents on hay patches, baby in center-right
                if (i == 0) {
                    // Parent 1 - hay_patch_sw (front-left)
                    poseStack.translate(-1.0, 0.4, -0.3);
                } else if (i == 1) {
                    // Parent 2 - hay_patch_nw (back-left)
                    poseStack.translate(-1.0, 0.4, 1.2);
                } else {
                    // Baby - center-right area
                    poseStack.translate(1.8, 0.4, 0.5);
                }
            }
            
            // Render animal with animation offset per animal
            renderAnimal(animal.entityType, animal.isBaby, animationTime + (i * 1.5F), poseStack, buffer, packedLight);
            
            poseStack.popPose();
        }
        
        // Render eggs for all chickens that have produced them
        if (animalType == EntityType.CHICKEN) {
            int totalEggs = entity.getTotalEggCount();
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

        // Position eggs inside the chest/basket (front-left area of stall)
        // The chest in model coordinates is around x=-3 to 0, z=-11 to -8
        // With 2x scale and -0.5 offset, this translates to approximately:
        // x: -0.7 to -0.35, z: -1.6 to -1.0 in world coords relative to block pos

        // Calculate egg positions inside the basket
        float baseX = -0.55F;  // Center X of basket
        float baseY = 0.65F;   // Inside the basket (slightly above base)
        float baseZ = -1.35F;  // Center Z of basket

        for (int i = 0; i < Math.min(eggCount, 6); i++) {
            poseStack.pushPose();

            // Arrange eggs in a 2x3 grid inside the basket
            int row = i / 2;
            int col = i % 2;
            float xOffset = (col - 0.5F) * 0.15F;
            float zOffset = (row - 1.0F) * 0.15F;

            poseStack.translate(baseX + xOffset, baseY, baseZ + zOffset);
            poseStack.scale(0.25F, 0.25F, 0.25F);

            // Rotate eggs 45 degrees upward toward the player (south, negative Z)
            // X rotation tilts forward/backward, so negative tilts face upward toward south
            poseStack.mulPose(Axis.XP.rotationDegrees(-45));

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
