package com.example.tharidia_items.client.renderer;

import com.example.tharidia_items.client.model.ExampleItemModel;
import com.example.tharidia_items.item.ExampleGeoItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ExampleItemRenderer extends GeoItemRenderer<ExampleGeoItem> {
    public ExampleItemRenderer() {
        super(new ExampleItemModel());
    }
}