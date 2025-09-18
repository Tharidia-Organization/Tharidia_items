package com.example.tharidia_items.client;

import com.example.tharidia_items.client.renderer.AlchimistTableItemRenderer;
import java.util.function.Consumer;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import software.bernie.geckolib.animatable.client.RenderProvider;

public final class ClientRendererHelper {
    private ClientRendererHelper() {}

    public static void provideAlchimistTableItemRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private AlchimistTableItemRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null) this.renderer = new AlchimistTableItemRenderer();
                return this.renderer;
            }
        });
    }
}