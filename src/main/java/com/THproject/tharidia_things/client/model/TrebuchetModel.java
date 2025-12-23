package com.THproject.tharidia_things.client.model;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.resources.ResourceLocation;

/**
 * Centralized resource locations for Trebuchet assets.
 * AzureLib 3.x consumes these directly via renderer config, no Geo classes needed.
 */
public final class TrebuchetModel {

    public static final ResourceLocation MODEL = TharidiaThings.modLoc("geo/trebuchet.json");
    public static final ResourceLocation TEXTURE = TharidiaThings.modLoc("textures/entity/trebuchet.png");
    public static final ResourceLocation ANIMATION = TharidiaThings.modLoc("animations/trebuchet.animation.json");

    private TrebuchetModel() {
    }
}
