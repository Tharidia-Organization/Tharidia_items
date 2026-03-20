package com.THproject.tharidia_things.client.model;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.CookTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CookTableModel extends GeoModel<CookTableBlockEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "geo/cook_table.geo.json");

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "textures/block/cook_table.png");

    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "animations/cook_table.animation.json");

    @Override
    public ResourceLocation getModelResource(CookTableBlockEntity blockEntity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CookTableBlockEntity blockEntity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CookTableBlockEntity blockEntity) {
        return ANIMATIONS;
    }
}
