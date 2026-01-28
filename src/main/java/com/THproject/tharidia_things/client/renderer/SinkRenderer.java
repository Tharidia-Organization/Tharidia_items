package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sink.SinkBlockEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SinkRenderer extends GeoBlockRenderer<SinkBlockEntity> {
    public SinkRenderer() {
        super(new GeoModel<SinkBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(SinkBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/sink.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(SinkBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/sink.png");
            }

            @Override
            public ResourceLocation getAnimationResource(SinkBlockEntity animatable) {
                return null;
            }
        });
    }
}
