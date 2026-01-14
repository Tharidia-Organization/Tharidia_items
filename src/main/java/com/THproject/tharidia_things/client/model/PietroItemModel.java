package com.THproject.tharidia_things.client.model;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.PietroBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model definition for PietroBlockItem.
 * Uses the same model as the block (realm_stage_1).
 */
public class PietroItemModel extends GeoModel<PietroBlockItem> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
        TharidiaThings.MODID, "geo/realm_stage_1.geo.json"
    );

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        TharidiaThings.MODID, "textures/block/realm_stage_1.png"
    );

    @Override
    public ResourceLocation getModelResource(PietroBlockItem item) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PietroBlockItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(PietroBlockItem item) {
        return null;
    }
}
