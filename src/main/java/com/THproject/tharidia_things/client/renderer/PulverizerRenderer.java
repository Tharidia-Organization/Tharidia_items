package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.pulverizer.PulverizerBlockEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class PulverizerRenderer extends GeoBlockRenderer<PulverizerBlockEntity> {
    public PulverizerRenderer() {
        super(new GeoModel<PulverizerBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(PulverizerBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/pulverizer.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(PulverizerBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/pulverizer.png");
            }

            @Override
            public ResourceLocation getAnimationResource(PulverizerBlockEntity animatable) {
                return null;
            }
        });
        addRenderLayer(new PulverizerGrinderRenderer(this));
    }
}
