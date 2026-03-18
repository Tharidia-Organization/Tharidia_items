package com.THproject.tharidia_things.client.model;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.ChimneyItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for ChimneyItem.
 * Uses the smithing furnace model but will only show stage_4 bone.
 */
public class ChimneyItemModel extends GeoModel<ChimneyItem> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "geo/smithing_furnace.geo.json"
    );

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "textures/block/smithing_furnace.png"
    );

    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "animations/smithing_furnace.animation.json"
    );

    @Override
    public ResourceLocation getModelResource(ChimneyItem item) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ChimneyItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ChimneyItem item) {
        return ANIMATIONS;
    }
}
