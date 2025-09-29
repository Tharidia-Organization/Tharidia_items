package com.example.tharidia_items;

import mod.azure.azurelib.AzureLib;
import com.example.tharidia_items.block.AlchimistTableBlock;
import com.example.tharidia_items.block.entity.AlchimistTableBlockEntity;
import com.example.tharidia_items.item.AlchimistTableItem;
import com.example.tharidia_items.screen.AlchimistTableScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.ScreenHandlerContext;

import net.minecraft.resource.featuretoggle.FeatureFlags;

public class TharidiaItemsMod implements ModInitializer {
    public static final String MOD_ID = "tharidia_items";

    // Blocco e BlockEntity Alchimist Table
    public static final Block ALCHIMIST_TABLE = new AlchimistTableBlock();
    public static BlockEntityType<AlchimistTableBlockEntity> ALCHIMIST_TABLE_BE;
    public static final ScreenHandlerType<AlchimistTableScreenHandler> ALCHIMIST_TABLE_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER, new Identifier(MOD_ID, "alchimist_table"), new ScreenHandlerType<AlchimistTableScreenHandler>((syncId, inventory) -> new AlchimistTableScreenHandler(syncId, inventory, ScreenHandlerContext.EMPTY), FeatureFlags.VANILLA_FEATURES));

    @Override
    public void onInitialize() {
        // Inizializza AzureLib
        AzureLib.initialize();

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
    }
}