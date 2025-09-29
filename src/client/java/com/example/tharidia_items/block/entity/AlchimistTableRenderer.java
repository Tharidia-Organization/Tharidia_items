package com.example.tharidia_items.block.entity;

import mod.azure.azurelib.renderer.GeoBlockRenderer;
import mod.azure.azurelib.model.DefaultedBlockGeoModel;
import net.minecraft.util.Identifier;

public class AlchimistTableRenderer extends GeoBlockRenderer<AlchimistTableBlockEntity> {
    public AlchimistTableRenderer() {
        super(new DefaultedBlockGeoModel<>(new Identifier("tharidia_items", "alchimist_table")));
    }
}