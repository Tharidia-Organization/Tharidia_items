package com.example.tharidia_items.client.renderer;

import com.example.tharidia_items.block.entity.AlchimistTableBlockEntity;
import com.example.tharidia_items.client.model.AlchimistTableModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class AlchimistTableRenderer extends GeoBlockRenderer<AlchimistTableBlockEntity> {
    public AlchimistTableRenderer() {
        super(new AlchimistTableModel());
        // Disabilitato glow per prevenire possibili side-effect sullo stato di rendering
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderLayer getRenderType(AlchimistTableBlockEntity animatable, Identifier texture, VertexConsumerProvider bufferSource, float partialTick) {
        // Usa un layer opaco (cutout senza cull) per stabilizzare lo stato grafico
        return RenderLayer.getEntityCutoutNoCull(texture);
    }
}