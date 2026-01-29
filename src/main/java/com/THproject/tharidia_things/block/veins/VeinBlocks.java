package com.THproject.tharidia_things.block.veins;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class VeinBlocks {
    public static final DeferredBlock<Block> VEIN_BLOCK_T0 = TharidiaThings.BLOCKS.register(
            "vein_block_t0",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(1.5F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> VEIN_BLOCK_T1 = TharidiaThings.BLOCKS.register(
            "vein_block_t1",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(1.5F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> VEIN_BLOCK_T2 = TharidiaThings.BLOCKS.register(
            "vein_block_t2",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(1.5F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> VEIN_BLOCK_T3 = TharidiaThings.BLOCKS.register(
            "vein_block_t3",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(1.5F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> VEIN_BLOCK_T4 = TharidiaThings.BLOCKS.register(
            "vein_block_t4",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(1.5F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> VEIN_BLOCK_T0_ITEM = TharidiaThings.ITEMS.register(
            "vein_block_t0",
            () -> new BlockItem(VEIN_BLOCK_T0.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> VEIN_BLOCK_T1_ITEM = TharidiaThings.ITEMS.register(
            "vein_block_t1",
            () -> new BlockItem(VEIN_BLOCK_T1.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> VEIN_BLOCK_T2_ITEM = TharidiaThings.ITEMS.register(
            "vein_block_t2",
            () -> new BlockItem(VEIN_BLOCK_T2.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> VEIN_BLOCK_T3_ITEM = TharidiaThings.ITEMS.register(
            "vein_block_t3",
            () -> new BlockItem(VEIN_BLOCK_T3.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> VEIN_BLOCK_T4_ITEM = TharidiaThings.ITEMS.register(
            "vein_block_t4",
            () -> new BlockItem(VEIN_BLOCK_T4.get(), new Item.Properties()));

    public static void init() {
    }
}
