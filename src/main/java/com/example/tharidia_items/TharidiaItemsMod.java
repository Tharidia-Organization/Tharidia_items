package com.example.tharidia_items;

import com.example.tharidia_items.item.ExampleGeoItem;
import com.example.tharidia_items.block.AlchimistTableBlock;
import com.example.tharidia_items.block.entity.AlchimistTableBlockEntity;
import com.example.tharidia_items.item.AlchimistTableItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.block.entity.BlockEntityType;
import software.bernie.geckolib.GeckoLib;

public class TharidiaItemsMod implements ModInitializer {
    public static final String MOD_ID = "tharidia_items";

    public static final ExampleGeoItem EXAMPLE_ITEM = new ExampleGeoItem(new net.minecraft.item.Item.Settings().maxCount(1));

    // Blocco e BlockEntity Alchimist Table
    public static final Block ALCHIMIST_TABLE = new AlchimistTableBlock();
    public static BlockEntityType<AlchimistTableBlockEntity> ALCHIMIST_TABLE_BE;

    @Override
    public void onInitialize() {
        // Inizializza GeckoLib
        GeckoLib.initialize();

        // Registra un semplice oggetto che usa un modello GeckoLib
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "example_item"), EXAMPLE_ITEM);

        // Registra il blocco e la relativa BlockEntity
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "alchimist_table"), ALCHIMIST_TABLE);
        ALCHIMIST_TABLE_BE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MOD_ID, "alchimist_table"),
                FabricBlockEntityTypeBuilder.create(AlchimistTableBlockEntity::new, ALCHIMIST_TABLE).build()
        );
        // Registra il BlockItem personalizzato (GeoItem) per poter posizionare il blocco e renderizzarlo custom come item
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "alchimist_table"), new AlchimistTableItem(ALCHIMIST_TABLE, new net.minecraft.item.Item.Settings()));

        // Aggiungi il blocco a un gruppo creativo per ottenerlo facilmente
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(ALCHIMIST_TABLE));

        // Aggiungi l'oggetto di esempio a un gruppo creativo per poterlo ottenere facilmente in gioco
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(EXAMPLE_ITEM));
    }
}