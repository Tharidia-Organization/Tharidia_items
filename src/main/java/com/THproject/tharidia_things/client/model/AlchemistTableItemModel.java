package com.THproject.tharidia_things.client.model;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.AlchemistTableBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model definition for AlchemistTableBlockItem (inventory/hand rendering).
 * Reuses the same geo and texture as the block entity.
 */
public class AlchemistTableItemModel extends GeoModel<AlchemistTableBlockItem> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "geo/alchemist_table.geo.json");

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "textures/block/alchemist_table.png");

    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "animations/alchemist_table.animation.json");

    @Override
    public ResourceLocation getModelResource(AlchemistTableBlockItem item) { return MODEL; }

    @Override
    public ResourceLocation getTextureResource(AlchemistTableBlockItem item) { return TEXTURE; }

    @Override
    public ResourceLocation getAnimationResource(AlchemistTableBlockItem item) { return ANIMATIONS; }
}
