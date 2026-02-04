package com.THproject.tharidia_things.client.renderer;

import java.util.ArrayList;
import java.util.List;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sink.SinkBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SinkRenderer extends GeoBlockRenderer<SinkBlockEntity> {
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

        // 9 slots
        for (int i = 0; i < 9; i++) {
            ItemStack stack = animatable.sinkInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        if (!items.isEmpty()) {
            poseStack.pushPose();

            // Position the items in the sink
            switch (animatable.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                case NORTH:
                    poseStack.translate(1.3, 0.45, 0.55);
                    break;
                case EAST:
                    poseStack.translate(0.46, 0.45, 1.3);
                    break;
                case SOUTH:
                    poseStack.translate(-0.41, 0.45, 0.46);
                    break;
                case WEST:
                    poseStack.translate(0.55, 0.45, -0.41);
                    break;
                default:
                    break;
            }

            poseStack.scale(0.3f, 0.3f, 0.3f);
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                float offsetX = (i % 3 - 1) * 0.55f; // -0.55, 0, 0.55
                float offsetZ = (i / 3 - 1) * 0.55f; // -0.55, 0, 0.55
                poseStack.pushPose();
                poseStack.translate(offsetX, 0, offsetZ);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND,
                        packedLight, packedOverlay, poseStack, bufferSource, animatable.getLevel(), 0);
                poseStack.popPose();
            }

            poseStack.popPose();
        }

    }

    @Override
    public AABB getRenderBoundingBox(SinkBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 3, pos.getY(), pos.getZ() - 3,
                pos.getX() + 5, pos.getY() + 3, pos.getZ() + 5);
    }

}
