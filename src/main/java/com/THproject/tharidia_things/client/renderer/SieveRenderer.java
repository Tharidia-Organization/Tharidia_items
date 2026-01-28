package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sieve.SieveBlockEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SieveRenderer extends GeoBlockRenderer<SieveBlockEntity> {
    public SieveRenderer() {
        super(new GeoModel<SieveBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(SieveBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/sieve.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(SieveBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/sieve.png");
            }

            @Override
            public ResourceLocation getAnimationResource(SieveBlockEntity animatable) {
                return null;
            }
        });
    }
}
