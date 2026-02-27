package com.THproject.tharidia_things.client.renderer;

import org.jetbrains.annotations.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.herbalist_tree.HerbalistTreeBlockEntity;
import com.THproject.tharidia_things.block.herbalist.pot.PotBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
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
                return null;
            }
        });
    }

    @Override
    public void preRender(PoseStack poseStack, HerbalistTreeBlockEntity animatable, BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        Level level = animatable.getLevel();
        BlockPos pos = animatable.getBlockPos();
        Direction forwardDir = Direction.NORTH;
        Direction newDir;

        for (int i = 1; i <= 8; i++) {
            if (i % 2 != 0) {
                BlockPos newPos = pos.relative(forwardDir, 2);
                newDir = forwardDir.getCounterClockWise();
                BlockPos potPos = newPos.relative(newDir, 1);

                setBoneVisible(model, String.format("Radice%d", i),
                        level.getBlockState(potPos).getBlock() instanceof PotBlock);
            } else {
                BlockPos newPos = pos.relative(forwardDir, 2);
                newDir = forwardDir.getClockWise();
                BlockPos potPos = newPos.relative(newDir, 1);

                setBoneVisible(model, String.format("Radice%d", i),
                        level.getBlockState(potPos).getBlock() instanceof PotBlock);

                forwardDir = forwardDir.getClockWise();
            }
        }
    }

    @Override
    public void render(HerbalistTreeBlockEntity animatable, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private void setBoneVisible(BakedGeoModel model, String boneName, boolean visible) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            bone.setHidden(!visible);
        }
    }
}
