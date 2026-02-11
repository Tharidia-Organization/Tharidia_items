package com.THproject.tharidia_things.client.renderer;

import org.jetbrains.annotations.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.pulverizer.PulverizerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class PulverizerRenderer extends GeoBlockRenderer<PulverizerBlockEntity> {
    public PulverizerRenderer(BlockEntityRendererProvider.Context context) {
        super(new GeoModel<PulverizerBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(PulverizerBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(
                        TharidiaThings.MODID, "geo/pulverizer.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(PulverizerBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(
                        TharidiaThings.MODID, "textures/block/pulverizer.png");
            }

            @Override
            public ResourceLocation getAnimationResource(PulverizerBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(
                        TharidiaThings.MODID, "animations/pulverizer.animation.json");
            }
        });
    }

    @Override
    public void render(PulverizerBlockEntity animatable, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // Render input item
        ItemStack input_item = animatable.inventory.getStackInSlot(0);
        if (!input_item.isEmpty()) {
            float processRatio = 1 - animatable.getProcessPercentage();
            poseStack.pushPose();
            poseStack.translate(0.5, 1, 0.5);
            poseStack.scale(1.2f, processRatio, 1.2f);

            Minecraft.getInstance().getItemRenderer().renderStatic(input_item, ItemDisplayContext.FIXED, packedLight,
                    packedOverlay, poseStack, bufferSource, animatable.getLevel(), 0);

            poseStack.popPose();
        }

        // Render output items (WIP)
        ItemStack output_item = ItemStack.EMPTY;
        for (int i = 1; i < animatable.inventory.getSlots(); i++) {
            ItemStack item = animatable.inventory.getStackInSlot(i);
            if (!item.isEmpty()) {
                output_item = item.copy();
                break;
            }
        }

        if (!output_item.isEmpty()) {
            poseStack.pushPose();

            poseStack.translate(2.5, 1.5, 0.5);
            poseStack.scale(0.5f, 0.5f, 0.5f);

            Minecraft.getInstance().getItemRenderer().renderStatic(output_item, ItemDisplayContext.FIXED, packedLight,
                    packedOverlay, poseStack, bufferSource, animatable.getLevel(), 0);

            poseStack.popPose();
        }
    }

    @Override
    public void preRender(PoseStack poseStack, PulverizerBlockEntity animatable, BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        setBoneVisible(model, "grinder_left", (animatable.getGrindersCount() >= 1));
        setBoneVisible(model, "grinder_right", (animatable.getGrindersCount() >= 2));
    }

    private void setBoneVisible(BakedGeoModel model, String boneName, boolean visible) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            bone.setHidden(!visible);
        }
    }

    @Override
    public AABB getRenderBoundingBox(PulverizerBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
    }
}
