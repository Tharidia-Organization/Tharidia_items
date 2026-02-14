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
import software.bernie.geckolib.cache.object.GeoBone;
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

        if (animatable.isOpen()) {
            GeoBone cascata = bakedModel.getBone("cascata").orElse(null);
            if (cascata == null)
                return;

            GeoBone tank = bakedModel.getBone("tank").orElse(null);
            GeoBone scala = bakedModel.getBone("scala").orElse(null);

            boolean tankHidden = tank != null && tank.isHidden();
            boolean scalaHidden = scala != null && scala.isHidden();
            boolean cascataHidden = cascata.isHidden();

            if (tank != null)
                tank.setHidden(true);
            if (scala != null)
                scala.setHidden(true);
            cascata.setHidden(false);

            getRenderer().reRender(
                    bakedModel,
                    poseStack,
                    bufferSource,
                    animatable,
                    fluidRenderType,
                    bufferSource.getBuffer(fluidRenderType),
                    partialTick,
                    highLight,
                    packedOverlay,
                    color);

            if (tank != null)
                tank.setHidden(tankHidden);
            if (scala != null)
                scala.setHidden(scalaHidden);
            cascata.setHidden(cascataHidden);
        }
    }
}