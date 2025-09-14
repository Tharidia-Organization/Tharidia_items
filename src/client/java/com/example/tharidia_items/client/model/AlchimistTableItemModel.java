package com.example.tharidia_items.client.model;

import com.example.tharidia_items.TharidiaItemsMod;
import com.example.tharidia_items.item.AlchimistTableItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class AlchimistTableItemModel extends DefaultedItemGeoModel<AlchimistTableItem> {
    public AlchimistTableItemModel() {
        // Usa un ID distinto per evitare collisioni con il modello del blocco
        super(new Identifier(TharidiaItemsMod.MOD_ID, "alchimist_table_item"));
    }

    @Override
    public Identifier getModelResource(AlchimistTableItem animatable) {
        // Usa il geo duplicato dedicato all'item
        return new Identifier(TharidiaItemsMod.MOD_ID, "geo/alchimist_table_item.geo.json");
    }

    @Override
    public Identifier getTextureResource(AlchimistTableItem animatable) {
        return new Identifier(TharidiaItemsMod.MOD_ID, "textures/block/tavolo.png");
    }

    @Override
    public Identifier getAnimationResource(AlchimistTableItem animatable) {
        return new Identifier(TharidiaItemsMod.MOD_ID, "animations/alchimist_table.animation.json");
    }
}