package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sieve.SieveBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SieveRenderer extends GeoBlockRenderer<SieveBlockEntity> {
    public SieveRenderer() {
        super(new GeoModel<SieveBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(SieveBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/sieve.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(SieveBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/sieve.png");
            }

            @Override
            public ResourceLocation getAnimationResource(SieveBlockEntity animatable) {
                return null;
            }
        });
        addRenderLayer(new SieveLeverRenderer(this));
        addRenderLayer(new SieveWaterRenderer(this));
    }

    @Override
    public void render(SieveBlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        ItemStack stack = blockEntity.inventory.getStackInSlot(0);
        if (!stack.isEmpty()) {
            poseStack.pushPose();

            float processRatio = 1 - blockEntity.getProcessPercentage();
            // Position the item in the sieve
            poseStack.translate(0.5, 1.5, 0.5);
            switch (blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                case Direction.NORTH:
                    poseStack.translate(0, 0, 1);
                    break;
                case Direction.EAST:
                    poseStack.translate(-1, 0, 0);
                    break;
                case Direction.SOUTH:
                    poseStack.translate(0, 0, -1);
                    break;
                case Direction.WEST:
                    poseStack.translate(1, 0, 0);
                    break;
                default:
                    break;
            }
            poseStack.translate(0.0, -0.25, 0.0);
            poseStack.scale(1.0f, processRatio, 1.0f);
            poseStack.translate(0.0, 0.25, 0.0);

            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
                    packedOverlay, poseStack, bufferSource, blockEntity.getLevel(), 0);

            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(SieveBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 3, pos.getY(), pos.getZ() - 3,
                pos.getX() + 5, pos.getY() + 3, pos.getZ() + 5);
    }
}
