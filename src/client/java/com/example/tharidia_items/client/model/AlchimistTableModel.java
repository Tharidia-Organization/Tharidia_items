package com.example.tharidia_items.client.model;

import com.example.tharidia_items.TharidiaItemsMod;
import com.example.tharidia_items.block.entity.AlchimistTableBlockEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class AlchimistTableModel extends DefaultedBlockGeoModel<AlchimistTableBlockEntity> {
    public AlchimistTableModel() {
        super(new Identifier(TharidiaItemsMod.MOD_ID, "alchimist_table"));
    }

    @Override
    public Identifier getModelResource(AlchimistTableBlockEntity animatable) {
        // Percorso reale del file geo già presente nel progetto
        return new Identifier(TharidiaItemsMod.MOD_ID, "geo/alchimist_table.geo.json");
    }

    @Override
    public Identifier getAnimationResource(AlchimistTableBlockEntity animatable) {
        // Percorso reale del file animazione già presente nel progetto
        return new Identifier(TharidiaItemsMod.MOD_ID, "animations/alchimist_table.animation.json");
    }

    @Override
    public Identifier getTextureResource(AlchimistTableBlockEntity animatable) {
        // Usa la texture esistente tavolo.png
        return new Identifier(TharidiaItemsMod.MOD_ID, "textures/block/tavolo.png");
    }
}