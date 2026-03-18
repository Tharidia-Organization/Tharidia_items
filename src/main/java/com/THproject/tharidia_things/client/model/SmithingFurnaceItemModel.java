package com.THproject.tharidia_things.client.model;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.SmithingFurnaceBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model definition for SmithingFurnaceBlockItem.
 * Uses the same model as the block.
 */
public class SmithingFurnaceItemModel extends GeoModel<SmithingFurnaceBlockItem> {

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
    public ResourceLocation getModelResource(SmithingFurnaceBlockItem item) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SmithingFurnaceBlockItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SmithingFurnaceBlockItem item) {
        return ANIMATIONS;
    }
}
