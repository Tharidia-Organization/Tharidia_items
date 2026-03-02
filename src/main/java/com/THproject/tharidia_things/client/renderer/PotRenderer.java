package com.THproject.tharidia_things.client.renderer;

import org.jetbrains.annotations.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.pot.PotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class PotRenderer extends GeoBlockRenderer<PotBlockEntity> {
    public PotRenderer() {
        super(new GeoModel<PotBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(PotBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/pot.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(PotBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/pot.png");
            }

            @Override
            public ResourceLocation getAnimationResource(PotBlockEntity animatable) {
                return null;
            }
        });
    }

    @Override
    public void preRender(PoseStack poseStack, PotBlockEntity animatable, BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        setBoneVisible(model, "dirt", (animatable.hasDirt() && !animatable.isFarmed()));
        setBoneVisible(model, "farmland", (animatable.hasDirt() && animatable.isFarmed()));
    }

    @Override
    public void render(PotBlockEntity animatable, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        ItemStack plantStack = animatable.getPlant();
        if (!plantStack.isEmpty() &&
                plantStack.getItem() instanceof BlockItem blockItem && animatable.hasPlant()) {
            Block plantBlock = blockItem.getBlock();
            BlockState plantState = plantBlock.defaultBlockState();
            BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

            poseStack.pushPose();
            poseStack.translate(0, 0.1, 0);

            blockRenderer.renderSingleBlock(
                    plantState,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    ModelData.EMPTY,
                    null);
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
}
