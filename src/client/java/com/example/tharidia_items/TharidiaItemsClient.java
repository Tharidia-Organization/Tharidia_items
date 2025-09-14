package com.example.tharidia_items;

import com.example.tharidia_items.client.renderer.AlchimistTableRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public class TharidiaItemsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Renderer registration is handled via the item's createRenderer/getRenderProvider in GeckoLib 4.
        BlockEntityRendererRegistry.register(TharidiaItemsMod.ALCHIMIST_TABLE_BE, ctx -> new AlchimistTableRenderer());
    }
}