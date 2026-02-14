package com.THproject.tharidia_things.client.model;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model definition for SmithingFurnace.
 * Points to the geometry, texture, and animation files.
 */
public class SmithingFurnaceModel extends GeoModel<SmithingFurnaceBlockEntity> {

    // Path format: assets/tharidiathings/geo/smithing_furnace.geo.json
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "geo/smithing_furnace.geo.json"
    );

    // Path format: assets/tharidiathings/textures/block/smithing_furnace.png
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "textures/block/smithing_furnace.png"
    );

    // Animated texture for when furnace is active
    private static final ResourceLocation TEXTURE_ON = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "textures/block/smithing_furnace_on.png"
    );

    // Path format: assets/tharidiathings/animations/smithing_furnace.animation.json
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "animations/smithing_furnace.animation.json"
    );

    @Override
    public ResourceLocation getModelResource(SmithingFurnaceBlockEntity blockEntity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SmithingFurnaceBlockEntity blockEntity) {
        return blockEntity != null && blockEntity.isActive() ? TEXTURE_ON : TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SmithingFurnaceBlockEntity blockEntity) {
        return ANIMATIONS;
    }
}
