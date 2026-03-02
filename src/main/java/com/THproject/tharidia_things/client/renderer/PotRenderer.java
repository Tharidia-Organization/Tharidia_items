package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.pot.PotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
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
            poseStack.translate(0.5, 0.2, 0.5); // Center and raise the plant
            poseStack.scale(0.6f, 0.6f, 0.6f); // Scale down for pot

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
}
