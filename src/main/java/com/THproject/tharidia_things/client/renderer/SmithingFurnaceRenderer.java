package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import com.THproject.tharidia_things.client.model.SmithingFurnaceModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
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

        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
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
        setBoneVisible(model, "tiny_crucible", true);

        // Stage bones depend on installed components
        setBoneVisible(model, "stage_1", animatable.hasBellows());
        setBoneVisible(model, "stage_2", animatable.hasCrucible());
        setBoneVisible(model, "stage_3", animatable.hasHoover());
        setBoneVisible(model, "stage_4", animatable.hasChimney());

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

        // Cast ingot molten bones
        boolean hasCast = animatable.hasCastMetal();
        String castType = animatable.getCastMetalType();
        setBoneVisible(model, "molten", hasCast);
        setBoneVisible(model, "molten_tin", hasCast && "tin".equals(castType));
        setBoneVisible(model, "molten_gold", hasCast && "gold".equals(castType));
        setBoneVisible(model, "molten_iron", hasCast && "iron".equals(castType));
        setBoneVisible(model, "molten_steel", hasCast && "steel".equals(castType));
        setBoneVisible(model, "molten_copper", hasCast && "copper".equals(castType));
        setBoneVisible(model, "molten_dark_steel", hasCast && "dark_steel".equals(castType));

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
    }

    private void setBoneVisible(BakedGeoModel model, String boneName, boolean visible) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            bone.setHidden(!visible);
        }
    }

    @Override
    public RenderType getRenderType(SmithingFurnaceBlockEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
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
