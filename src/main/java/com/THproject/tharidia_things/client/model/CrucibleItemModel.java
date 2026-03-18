package com.THproject.tharidia_things.client.model;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.CrucibleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for CrucibleItem.
 * Uses the smithing furnace model but will only show stage_2 bone.
 */
public class CrucibleItemModel extends GeoModel<CrucibleItem> {

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
    public ResourceLocation getModelResource(CrucibleItem item) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CrucibleItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CrucibleItem item) {
        return ANIMATIONS;
    }
}
