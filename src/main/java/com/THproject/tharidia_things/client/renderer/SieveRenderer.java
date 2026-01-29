package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sieve.SieveBlockEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SieveRenderer extends GeoBlockRenderer<SieveBlockEntity> {
    public SieveRenderer() {
        super(new GeoModel<SieveBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(SieveBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/sieve.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(SieveBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/sieve.png");
            }

            @Override
            public ResourceLocation getAnimationResource(SieveBlockEntity animatable) {
                return null;
            }
        });
    }

    @Override
    public AABB getRenderBoundingBox(SieveBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 3, pos.getY(), pos.getZ() - 3,
                pos.getX() + 5, pos.getY() + 3, pos.getZ() + 5);
    }
}
