package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.WasherBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class WasherRenderer extends GeoBlockRenderer<WasherBlockEntity> {
    public WasherRenderer() {
        super(new GeoModel<WasherBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(WasherBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/washer.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(WasherBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/washer.png");
            }

            @Override
            public ResourceLocation getAnimationResource(WasherBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "animations/washer.animation.json");
            }
        });
    }
}
