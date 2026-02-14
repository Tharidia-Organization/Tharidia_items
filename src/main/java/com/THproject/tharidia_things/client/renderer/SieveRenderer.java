package com.THproject.tharidia_things.client.renderer;

import org.jetbrains.annotations.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sieve.SieveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
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
        addRenderLayer(new SieveWaterRenderer(this));
    }

    @Override
    public void preRender(PoseStack poseStack, SieveBlockEntity animatable, BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        // Render lever
        setBoneVisible(model, "lever_deactive", !animatable.isActive());
        setBoneVisible(model, "lever_active", animatable.isActive());

        // Render mesh
        setBoneVisible(model, "grigliamateriali", animatable.hasMesh());

        // Render residue
        ItemStack residue_stack = animatable.inventory.getStackInSlot(1);
        float fillRatio = (float) (residue_stack.getCount()) / 64f;
        float fill = ((fillRatio / 12.5f) * 100);
        for (int i = 0; i < 8; i++) {
            setBoneVisible(model, "residue" + (i + 1), fill >= (i + 0.1));
        }
    }

    @Override
    public void render(SieveBlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // Render input
        ItemStack input_stack = blockEntity.inventory.getStackInSlot(0);
        if (!input_stack.isEmpty()) {
            poseStack.pushPose();

            float processRatio = 1 - blockEntity.getProcessPercentage();
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

            Minecraft.getInstance().getItemRenderer().renderStatic(input_stack, ItemDisplayContext.FIXED, packedLight,
                    packedOverlay, poseStack, bufferSource, blockEntity.getLevel(), 0);

            poseStack.popPose();
        }

        poseStack.clear();
    }

    private void setBoneVisible(BakedGeoModel model, String boneName, boolean visible) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            bone.setHidden(!visible);
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
