package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.tank.TankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class TankFluidLayer extends GeoRenderLayer<TankBlockEntity> {
    public TankFluidLayer(GeoRenderer<TankBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, TankBlockEntity animatable, BakedGeoModel bakedModel,
            RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
            float partialTick, int packedLight, int packedOverlay) {

        FluidStack fluidStack = animatable.tank.getFluid();
        if (fluidStack.isEmpty())
            return;

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

        getRenderer().reRender(
                this.getGeoModel().getBakedModel(ResourceLocation.fromNamespaceAndPath(
                        TharidiaThings.MODID, "geo/tank_water.geo.json")),
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