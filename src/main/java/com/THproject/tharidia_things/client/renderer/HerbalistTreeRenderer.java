package com.THproject.tharidia_things.client.renderer;

import org.jetbrains.annotations.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.herbalist_tree.HerbalistTreeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HerbalistTreeRenderer extends GeoBlockRenderer<HerbalistTreeBlockEntity> {
    public HerbalistTreeRenderer() {
        super(new GeoModel<HerbalistTreeBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(HerbalistTreeBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/herbalist_tree.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(HerbalistTreeBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/herbalist_tree.png");
            }

            @Override
            public ResourceLocation getAnimationResource(HerbalistTreeBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
                        "animations/herbalist_tree.animation.json");
            }
        });
    }

    @Override
    public void preRender(PoseStack poseStack, HerbalistTreeBlockEntity animatable, BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        for (int i = 1; i <= 8; i++) {
            setBoneVisible(model, "Radice" + i, animatable.hasPotAtRoot(i));
        }

        float scale = animatable.getPetalScale();
        for (int i = 0; i < HerbalistTreeBlockEntity.getPetalCount(); i++) {
            String boneName = (i == 0) ? "Petali" : "Petali" + i;
            GeoBone bone = model.getBone(boneName).orElse(null);
            if (bone != null) {
                bone.updateScale(scale, scale, scale);
            }
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, HerbalistTreeBlockEntity animatable, GeoBone bone,
            RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int colour) {
        if (bone.getName().startsWith("Petali")) {
            colour = animatable.getPetalColor();
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }

    private void setBoneVisible(BakedGeoModel model, String boneName, boolean visible) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            bone.setHidden(!visible);
        }
    }

    @Override
    public AABB getRenderBoundingBox(HerbalistTreeBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 3, pos.getY(), pos.getZ() - 3,
                pos.getX() + 5, pos.getY() + 3, pos.getZ() + 5);
    }
}
