package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.tank.TankBlockEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TankRenderer extends GeoBlockRenderer<TankBlockEntity> {
    public TankRenderer() {
        super(new GeoModel<TankBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(TankBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/tank.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(TankBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/tank.png");
            }

            @Override
            public ResourceLocation getAnimationResource(TankBlockEntity animatable) {
                return null;
            }
        });
    }
}
