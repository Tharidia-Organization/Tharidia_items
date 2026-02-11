package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.SmithingFurnaceBlock;
import com.THproject.tharidia_things.block.SmithingFurnaceDummyBlock;
import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import com.THproject.tharidia_things.client.model.SmithingFurnaceModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * GeckoLib renderer for the Smithing Furnace.
 *
 * IMPORTANT: The model faces SOUTH in Blockbench, but GeckoLib's default
 * rotation assumes models face NORTH. We override render to compensate.
 */
public class SmithingFurnaceRenderer extends GeoBlockRenderer<SmithingFurnaceBlockEntity> {

    // Fluid bone pivot from geo.json (Blockbench units)
    private static final float FLUID_PIVOT_X = 0.10625f;
    private static final float FLUID_PIVOT_Y = 14.56979f;
    private static final float FLUID_PIVOT_Z = 5.30833f;

    // Gray tint for expired molten metal (ARGB packed int)
    private static final int EXPIRED_COLOUR = 0xFF505050;

    private static final java.util.Set<String> TINY_CRUCIBLE_FLUID_BONES = java.util.Set.of(
            "fluid", "iron", "gold", "copper", "tin", "steel", "dark_steel"
    );
    private static final java.util.Set<String> CAST_MOLTEN_BONES = java.util.Set.of(
            "molten", "molten_iron", "molten_gold", "molten_copper", "molten_tin", "molten_steel", "molten_dark_steel"
    );
    private static final java.util.Set<String> BIG_CRUCIBLE_BONES = java.util.Set.of(
            "molten_big", "molten_iron_big", "molten_gold_big", "molten_copper_big", "molten_tin_big", "molten_steel_big", "molten_dark_steel_big"
    );

    // Ingot on embers color system
    private static final java.util.Set<String> INGOT_BONES = java.util.Set.of(
            "ing_1", "ing_2", "ing_3", "ing_4"
    );
    private static final int IRON_INGOT_COLOUR = 0xFFFFFFFF;   // no tint
    private static final int GOLD_INGOT_COLOUR = 0xFFFFE040;   // golden
    private static final int COPPER_INGOT_COLOUR = 0xFFD4875A; // copper
    private static final int HOT_RED_COLOUR = 0xFFCC1A00;      // red-hot

    private MultiBufferSource currentBufferSource;

    public SmithingFurnaceRenderer(BlockEntityRendererProvider.Context context) {
        super(new SmithingFurnaceModel());
    }

    @Override
    public void render(SmithingFurnaceBlockEntity animatable, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (animatable.getLevel() == null) {
            return;
        }

        this.currentBufferSource = bufferSource;

        poseStack.pushPose();

        // Get the facing direction for position adjustment
        Direction facing = animatable.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        // World-space translation to align model with dummy blocks
        // Applied FIRST so it's in world coordinates (not affected by rotations)
        switch (facing) {
            case NORTH -> poseStack.translate(0, 0, -0.5);
            case SOUTH -> poseStack.translate(0, 0, 0.5);
            case EAST -> poseStack.translate(0.5, 0, 0);
            case WEST -> poseStack.translate(-0.5, 0, 0);
        }

        // Apply rotation compensation for model facing SOUTH instead of NORTH
        // GeckoLib expects model to face NORTH, but ours faces SOUTH
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        poseStack.translate(-0.5, 0, -0.5);

        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();

        // Billboard label: show big crucible liquid amount when player looks at it
        if (animatable.hasCrucible() && animatable.getBigCrucibleCount() > 0) {
            HitResult hitResult = Minecraft.getInstance().hitResult;
            if (hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                if (isLookingAtBigCrucible(animatable, blockHit)) {
                    renderBigCrucibleLabel(animatable, facing, poseStack, bufferSource, packedLight);
                }
            }
        }
    }

    /**
     * Checks if the player's crosshair is targeting the big crucible area of this furnace.
     * The big crucible occupies dummy blocks at offsetX 0-1 (the side opposite the cast/hoover).
     */
    private boolean isLookingAtBigCrucible(SmithingFurnaceBlockEntity animatable, BlockHitResult blockHit) {
        if (animatable.getLevel() == null) return false;
        BlockPos hitPos = blockHit.getBlockPos();
        BlockState hitState = animatable.getLevel().getBlockState(hitPos);

        if (hitState.getBlock() instanceof SmithingFurnaceDummyBlock) {
            int offsetX = hitState.getValue(SmithingFurnaceDummyBlock.OFFSET_X);
            if (offsetX >= 3) {
                BlockPos masterPos = SmithingFurnaceDummyBlock.getMasterPos(animatable.getLevel(), hitPos);
                return masterPos != null && masterPos.equals(animatable.getBlockPos());
            }
        }
        return false;
    }

    /**
     * Renders a floating billboard label above the big crucible showing the liquid count.
     * The label always faces the camera (billboard effect).
     */
    private void renderBigCrucibleLabel(SmithingFurnaceBlockEntity animatable, Direction facing,
                                         PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Position above the big crucible (relative to master block entity pos)
        // Big crucible occupies offsetX 0-1. Using getOffsetPos mapping:
        // centeredX for offsetX 0-1 center = -1.5 (with X_OFFSET = -2)
        double bx, bz;
        double by = 2.5;
        switch (facing) {
            case SOUTH -> { bx = 2.0;  bz = 1.0; }
            case NORTH -> { bx = -1.0; bz = 0.0; }
            case EAST  -> { bx = 1.0;  bz = -1.0; }
            case WEST  -> { bx = 0.0;  bz = 2.0; }
            default    -> { bx = 0.5;  bz = 0.5; }
        }

        poseStack.pushPose();
        poseStack.translate(bx, by, bz);

        // Billboard rotation: always face the camera
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());

        // Scale (same as vanilla entity nametags: positive X, negative Y, positive Z)
        float scale = 0.025f;
        poseStack.scale(scale, -scale, scale);

        Font font = Minecraft.getInstance().font;
        String text = animatable.getBigCrucibleCount() + "/8";
        float textWidth = font.width(text);
        float x = -textWidth / 2f;
        float y = -font.lineHeight / 2f;

        Matrix4f matrix = poseStack.last().pose();

        // Text color: white normally, gray when expired
        int textColor = animatable.isBigCrucibleExpired() ? 0xFF808080 : 0xFFFFFFFF;
        int bgColor = 0x80000000; // semi-transparent black background
        int fullBright = 15728880; // LightTexture.FULL_BRIGHT - always readable

        // Background + text pass (SEE_THROUGH so it renders on top of world geometry)
        font.drawInBatch(text, x, y, textColor, false, matrix, bufferSource,
                Font.DisplayMode.SEE_THROUGH, bgColor, fullBright);

        poseStack.popPose();
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                   int packedLight, int packedOverlay, int colour) {
        // Render raw ore item overlay at the fluid bone's position during smelting
        if (bone.getName().equals("fluid") && getAnimatable() != null && this.currentBufferSource != null) {
            SmithingFurnaceBlockEntity entity = getAnimatable();
            String smeltingType = entity.getSmeltingRawType();
            if (!smeltingType.isEmpty()) {
                ItemStack rawStack = switch (smeltingType) {
                    case "iron" -> new ItemStack(Items.RAW_IRON);
                    case "gold" -> new ItemStack(Items.RAW_GOLD);
                    case "copper" -> new ItemStack(Items.RAW_COPPER);
                    default -> ItemStack.EMPTY;
                };
                if (!rawStack.isEmpty()) {
                    poseStack.pushPose();
                    // Translate to fluid bone pivot (Blockbench units / 16 = block units)
                    poseStack.translate(FLUID_PIVOT_X / 16f, FLUID_PIVOT_Y / 16f, FLUID_PIVOT_Z / 16f);
                    // Scale down to fit inside the crucible
                    poseStack.scale(0.175f, 0.175f, 0.175f);
                    // Lay flat inside the crucible
                    poseStack.mulPose(Axis.XP.rotationDegrees(90f));
                    Minecraft.getInstance().getItemRenderer().renderStatic(
                            rawStack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                            poseStack, this.currentBufferSource, entity.getLevel(), 0);
                    poseStack.popPose();
                }
            }
        }

        // Render solidifying/solidified hot ingot item on the cast_ingot bone
        // cast_ingot bone has pivot [-19.675, 12, 4.375] and rotation [90, 0, 0]
        // GeckoLib negates X when converting from Bedrock/Blockbench to Java coords.
        // Molten cubes center in Blockbench: X=-24.436, Y=16.4, Z=4.933
        // In GeckoLib space: negate X → 24.436
        if (bone.getName().equals("cast_ingot") && getAnimatable() != null && this.currentBufferSource != null) {
            SmithingFurnaceBlockEntity castEntity = getAnimatable();
            if (castEntity.hasCastMetal() && !castEntity.isCastExpired()) {
                float progress = castEntity.getCastSolidifyProgress();
                if (progress > 0) {
                    String castType = castEntity.getCastMetalType();
                    ItemStack hotIngotStack = switch (castType) {
                        case "iron" -> new ItemStack(TharidiaThings.HOT_IRON.get());
                        case "gold" -> new ItemStack(TharidiaThings.HOT_GOLD.get());
                        case "copper" -> new ItemStack(TharidiaThings.HOT_COPPER.get());
                        default -> ItemStack.EMPTY;
                    };
                    if (!hotIngotStack.isEmpty()) {
                        poseStack.pushPose();
                        // Negate X for Bedrock→Java conversion, Y and Z stay as Blockbench values
                        poseStack.translate(24.436f / 16f, 16.4f / 16f, 4.933f / 16f);
                        float scale = progress * 0.55f;
                        poseStack.scale(scale, scale, scale);
                        // Rotate counterclockwise (bone already has 90° X rotation making item flat)
                        poseStack.mulPose(Axis.ZP.rotationDegrees(75f));
                        Minecraft.getInstance().getItemRenderer().renderStatic(
                                hotIngotStack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                                poseStack, this.currentBufferSource, castEntity.getLevel(), 0);
                        poseStack.popPose();
                    }
                }
            }
        }

        // Apply color tinting
        int effectiveColour = colour;
        if (getAnimatable() != null) {
            SmithingFurnaceBlockEntity entity = getAnimatable();
            String boneName = bone.getName();

            // Ingot on embers: metal tint + heat gradient
            if (INGOT_BONES.contains(boneName) && entity.getIngotCount() > 0) {
                int baseColour = switch (entity.getIngotMetalType()) {
                    case "gold" -> GOLD_INGOT_COLOUR;
                    case "copper" -> COPPER_INGOT_COLOUR;
                    default -> IRON_INGOT_COLOUR;
                };
                float heat = entity.getIngotHeatProgress();
                effectiveColour = heat > 0 ? lerpColour(baseColour, HOT_RED_COLOUR, heat) : baseColour;
            }
            // Expired molten metal: gray tint
            else if (entity.isTinyCrucibleExpired() && TINY_CRUCIBLE_FLUID_BONES.contains(boneName)) {
                effectiveColour = EXPIRED_COLOUR;
            } else if (entity.isCastExpired() && CAST_MOLTEN_BONES.contains(boneName)) {
                effectiveColour = EXPIRED_COLOUR;
            } else if (entity.isBigCrucibleExpired() && BIG_CRUCIBLE_BONES.contains(boneName)) {
                effectiveColour = EXPIRED_COLOUR;
            }
        }

        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, effectiveColour);
    }

    @Override
    public void preRender(PoseStack poseStack, SmithingFurnaceBlockEntity animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        // Reset all bone visibility first (important because model might be shared with item renderers)
        // Base and tiny_crucible are always visible
        setBoneVisible(model, "base", true);
        setBoneVisible(model, "tiny_crucible", animatable.hasTinyCrucible());

        // Stage bones depend on installed components
        setBoneVisible(model, "stage_1", animatable.hasBellows());
        setBoneVisible(model, "stage_2", animatable.hasCrucible());
        setBoneVisible(model, "stage_3", animatable.hasHoover());
        setBoneVisible(model, "stage_4", animatable.hasChimney());
        setBoneVisible(model, "stage_5", animatable.hasDoor());

        // Coal bones visibility based on coal count
        // coal_1 visible when coalCount >= 2, coal_2 >= 4, coal_3 >= 6, coal_4 >= 8
        int coalCount = animatable.getCoalCount();
        setBoneVisible(model, "coal_1", coalCount >= 2);
        setBoneVisible(model, "coal_2", coalCount >= 4);
        setBoneVisible(model, "coal_3", coalCount >= 6);
        setBoneVisible(model, "coal_4", coalCount >= 8);

        // Ash bones visibility based on ash count (st_1 to st_6)
        int ashCount = animatable.getAshCount();
        setBoneVisible(model, "st_1", ashCount >= 1);
        setBoneVisible(model, "st_2", ashCount >= 2);
        setBoneVisible(model, "st_3", ashCount >= 3);
        setBoneVisible(model, "st_4", ashCount >= 4);
        setBoneVisible(model, "st_5", ashCount >= 5);
        setBoneVisible(model, "st_6", ashCount >= 6);

        // Fluid bones: show only the active molten metal type
        boolean hasMolten = animatable.hasMoltenMetal();
        String moltenType = animatable.getMoltenMetalType();
        setBoneVisible(model, "fluid", hasMolten);
        setBoneVisible(model, "iron", hasMolten && "iron".equals(moltenType));
        setBoneVisible(model, "gold", hasMolten && "gold".equals(moltenType));
        setBoneVisible(model, "copper", hasMolten && "copper".equals(moltenType));
        setBoneVisible(model, "tin", hasMolten && "tin".equals(moltenType));
        setBoneVisible(model, "steel", hasMolten && "steel".equals(moltenType));
        setBoneVisible(model, "dark_steel", hasMolten && "dark_steel".equals(moltenType));

        // Cast ingot molten bones - hide when solidified, re-show when expired (gray mass)
        boolean hasCast = animatable.hasCastMetal();
        String castType = animatable.getCastMetalType();
        boolean castSolidified = animatable.isCastSolidified();
        boolean castExpired = animatable.isCastExpired();
        boolean showMolten = hasCast && (!castSolidified || castExpired);
        setBoneVisible(model, "molten", showMolten);
        setBoneVisible(model, "molten_tin", showMolten && "tin".equals(castType));
        setBoneVisible(model, "molten_gold", showMolten && "gold".equals(castType));
        setBoneVisible(model, "molten_iron", showMolten && "iron".equals(castType));
        setBoneVisible(model, "molten_steel", showMolten && "steel".equals(castType));
        setBoneVisible(model, "molten_copper", showMolten && "copper".equals(castType));
        setBoneVisible(model, "molten_dark_steel", showMolten && "dark_steel".equals(castType));

        // During solidification, shrink molten bones scaleY for "liquid settling" effect
        if (hasCast && !castSolidified) {
            float progress = animatable.getCastSolidifyProgress();
            float moltenScale = 1.0f - progress; // 1.0 → 0.0
            setBoneScaleY(model, "molten", moltenScale);
        } else if (castExpired) {
            // Reset scaleY for expired gray mass display
            setBoneScaleY(model, "molten", 1.0f);
        }

        // Big crucible molten bones
        boolean hasBigMetal = animatable.hasCrucible() && animatable.getBigCrucibleCount() > 0;
        String bigType = animatable.getBigCrucibleMetalType();
        setBoneVisible(model, "molten_big", hasBigMetal);
        setBoneVisible(model, "molten_tin_big", hasBigMetal && "tin".equals(bigType));
        setBoneVisible(model, "molten_gold_big", hasBigMetal && "gold".equals(bigType));
        setBoneVisible(model, "molten_iron_big", hasBigMetal && "iron".equals(bigType));
        setBoneVisible(model, "molten_steel_big", hasBigMetal && "steel".equals(bigType));
        setBoneVisible(model, "molten_copper_big", hasBigMetal && "copper".equals(bigType));
        setBoneVisible(model, "molten_dark_steel_big", hasBigMetal && "dark_steel".equals(bigType));

        // Crystal bones visibility based on crystal count
        int crystalCount = animatable.getCrystalCount();
        setBoneVisible(model, "crystal", crystalCount > 0);
        setBoneVisible(model, "cry_1", crystalCount >= 1);
        setBoneVisible(model, "cry_2", crystalCount >= 2);
        setBoneVisible(model, "cry_3", crystalCount >= 3);
        setBoneVisible(model, "cry_4", crystalCount >= 4);

        // Ingots on embers bones
        int ingotCount = animatable.getIngotCount();
        setBoneVisible(model, "ing_1", ingotCount >= 1);
        setBoneVisible(model, "ing_2", ingotCount >= 2);
        setBoneVisible(model, "ing_3", ingotCount >= 3);
        setBoneVisible(model, "ing_4", ingotCount >= 4);
    }

    private void setBoneVisible(BakedGeoModel model, String boneName, boolean visible) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            bone.setHidden(!visible);
        }
    }

    private int lerpColour(int from, int to, float t) {
        int a = (int) (((from >> 24) & 0xFF) + (((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * t);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void setBoneScaleY(BakedGeoModel model, String boneName, float scaleY) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            bone.setScaleY(scaleY);
        }
    }

    @Override
    public RenderType getRenderType(SmithingFurnaceBlockEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }

    @Override
    public boolean shouldRenderOffScreen(SmithingFurnaceBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(SmithingFurnaceBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        // Bounding box for 5x2x2 multiblock structure
        return new AABB(
                pos.getX() - 3, pos.getY(), pos.getZ() - 2,
                pos.getX() + 6, pos.getY() + 3, pos.getZ() + 4
        );
    }
}
