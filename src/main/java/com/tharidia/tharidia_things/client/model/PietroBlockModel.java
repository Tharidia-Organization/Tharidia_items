package com.tharidia.tharidia_things.client.model;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

public class PietroBlockModel extends GeoModel<PietroBlockEntity> {

    @Override
    public ResourceLocation getModelResource(PietroBlockEntity animatable) {
        return TharidiaThings.modLoc("geo/animated_pietro.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PietroBlockEntity animatable) {
        return TharidiaThings.modLoc("textures/block/pietro.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PietroBlockEntity animatable) {
        return TharidiaThings.modLoc("animations/animated_pietro.animation.json");
    }
}
