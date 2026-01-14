package com.THproject.tharidia_things.client.model;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model definition for PietroBlock (realm_stage_1).
 * Used for rendering at realm levels 0-1.
 */
public class PietroBlockModel extends GeoModel<PietroBlockEntity> {

    // Path format: assets/tharidiathings/geo/realm_stage_1.geo.json
    // GeckoLib 4 requires the full path including .geo.json extension
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
        TharidiaThings.MODID, "geo/realm_stage_1.geo.json"
    );

    // Path format: assets/tharidiathings/textures/block/realm_stage_1.png
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        TharidiaThings.MODID, "textures/block/realm_stage_1.png"
    );

    @Override
    public ResourceLocation getModelResource(PietroBlockEntity blockEntity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PietroBlockEntity blockEntity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(PietroBlockEntity blockEntity) {
        // Static model - no animations
        return null;
    }
}
