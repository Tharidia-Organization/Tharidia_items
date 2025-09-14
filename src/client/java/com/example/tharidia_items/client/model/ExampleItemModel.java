package com.example.tharidia_items.client.model;

import com.example.tharidia_items.TharidiaItemsMod;
import com.example.tharidia_items.item.ExampleGeoItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class ExampleItemModel extends DefaultedItemGeoModel<ExampleGeoItem> {
    public ExampleItemModel() {
        super(new Identifier(TharidiaItemsMod.MOD_ID, "example_item"));
    }

    @Override
    public Identifier getTextureResource(ExampleGeoItem animatable) {
        // Riusa la texture esistente del blocco come placeholder per evitare crash
        return new Identifier(TharidiaItemsMod.MOD_ID, "textures/block/tavolo.png");
    }
}