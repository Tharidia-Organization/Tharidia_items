package com.THproject.tharidia_things.client.model;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.DoorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for DoorItem.
 * Uses the smithing furnace model but will only show stage_5 bone.
 */
public class DoorItemModel extends GeoModel<DoorItem> {

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
    public ResourceLocation getModelResource(DoorItem item) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DoorItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DoorItem item) {
        return ANIMATIONS;
    }
}
