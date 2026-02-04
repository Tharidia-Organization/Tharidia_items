package com.THproject.tharidia_things.client.renderer;

import java.util.ArrayList;
import java.util.List;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sink.SinkBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SinkRenderer extends GeoBlockRenderer<SinkBlockEntity> {

    // Adjust these slightly if items clip into the sides
    private static final float ITEM_SCALE = 0.25f;
    private static final float GRID_SPACING = 0.7f;

    public SinkRenderer() {
        super(new GeoModel<SinkBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(SinkBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/sink.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(SinkBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/sink.png");
            }

            @Override
            public ResourceLocation getAnimationResource(SinkBlockEntity animatable) {
                return null;
            }
        });
    }

    @Override
    public void render(SinkBlockEntity animatable, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        List<ItemStack> items = new ArrayList<>();

        // Collect valid items
        for (int i = 0; i < 9; i++) {
            ItemStack stack = animatable.sinkInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        if (!items.isEmpty()) {
            poseStack.pushPose();

            Direction facing = animatable.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

            // 1. Center in the block
            poseStack.translate(0.5, 0.51, 0.5);

            // 2. Rotate to match block facing
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

            // 3. APPLY OFFSET (FIXED)
            poseStack.translate(-0.8, 0, 0.0);

            // 4. Scale down
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

            // 5. Render items in a 3x3 grid
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);

                // Calculate grid position
                // i % 3 gives column (0, 1, 2) -> -1, 0, 1
                // i / 3 gives row (0, 1, 2) -> -1, 0, 1
                float offsetX = (i % 3 - 1) * GRID_SPACING;
                float offsetZ = (i / 3 - 1) * GRID_SPACING;

                poseStack.pushPose();

                // Move to grid slot
                poseStack.translate(offsetX, 0, offsetZ);

                itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED,
                        packedLight, packedOverlay, poseStack, bufferSource, animatable.getLevel(), 0);

                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(SinkBlockEntity blockEntity) {
        // Reduced bounding box size slightly to be more reasonable, but large enough
        // for animations
        var pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
    }
}