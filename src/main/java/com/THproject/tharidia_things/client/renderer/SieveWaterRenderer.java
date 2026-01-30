package com.THproject.tharidia_things.client.renderer;

import org.jetbrains.annotations.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sieve.SieveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SieveWaterRenderer extends GeoRenderLayer<SieveBlockEntity> {
    public SieveWaterRenderer(GeoRenderer<SieveBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, SieveBlockEntity animatable, BakedGeoModel bakedModel,
            @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
            float partialTick, int packedLight, int packedOverlay) {

        FluidStack fluidStack = new FluidStack(Fluids.WATER, 1000);

        // Ottieni la texture del fluido (es. acqua o lava)
        IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation stillTexture = props.getStillTexture(fluidStack);

        ResourceLocation texturePath = ResourceLocation.fromNamespaceAndPath(
                stillTexture.getNamespace(),
                "textures/" + stillTexture.getPath() + ".png");

        // Ottieni il colore (tint) del fluido
        int color = props.getTintColor(fluidStack);

        // Forza la luce al massimo per evitare che
        // sembri "scura" a causa delle ombre del modello
        int highLight = 0xF000F0;

        // Definisci il RenderType usando la texture del fluido
        RenderType fluidRenderType = RenderType.entityTranslucent(texturePath);

        if (animatable.canRenderWater())
            getRenderer().reRender(
                    this.getGeoModel().getBakedModel(ResourceLocation.fromNamespaceAndPath(
                            TharidiaThings.MODID, "geo/sieve_water.geo.json")),
                    poseStack,
                    bufferSource,
                    animatable,
                    fluidRenderType,
                    bufferSource.getBuffer(fluidRenderType),
                    partialTick,
                    highLight,
                    packedOverlay,
                    color);
    }
}
