package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sink.SinkBlockEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SinkRenderer extends GeoBlockRenderer<SinkBlockEntity> {
    public SinkRenderer() {
        super(new GeoModel<SinkBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(SinkBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/sink.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(SinkBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/sink.png");
            }

            @Override
            public ResourceLocation getAnimationResource(SinkBlockEntity animatable) {
                return null;
            }
        });
    }

    @Override
    public AABB getRenderBoundingBox(SinkBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 3, pos.getY(), pos.getZ() - 3,
                pos.getX() + 5, pos.getY() + 3, pos.getZ() + 5);
    }
}
