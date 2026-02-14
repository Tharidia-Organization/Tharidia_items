package com.THproject.tharidia_things.block.seed_extraction;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.ZoccolettaItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class SeedExtractionRegistry {

    // ── Blocks ──────────────────────────────────────────────────────────────

    public static final DeferredBlock<CompressedLeavesBlock> COMPRESSED_LEAVES = TharidiaThings.BLOCKS.register(
            "compressed_leaves",
            () -> new CompressedLeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(1.0F)
                    .sound(SoundType.GRASS)));

    public static final DeferredBlock<WetCompressedLeavesBlock> WET_COMPRESSED_LEAVES = TharidiaThings.BLOCKS.register(
            "wet_compressed_leaves",
            () -> new WetCompressedLeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(1.0F)
                    .sound(SoundType.GRASS)
                    .randomTicks()));

    public static final DeferredBlock<DriedCompressedLeavesBlock> DRIED_COMPRESSED_LEAVES = TharidiaThings.BLOCKS.register(
            "dried_compressed_leaves",
            () -> new DriedCompressedLeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .strength(1.0F)
                    .sound(SoundType.GRASS)));

    // ── Items ───────────────────────────────────────────────────────────────

    public static final DeferredItem<CompressedLeavesBlockItem> COMPRESSED_LEAVES_ITEM = TharidiaThings.ITEMS.register(
            "compressed_leaves",
            () -> new CompressedLeavesBlockItem(COMPRESSED_LEAVES.get(), new Item.Properties()));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> WET_COMPRESSED_LEAVES_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(WET_COMPRESSED_LEAVES);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> DRIED_COMPRESSED_LEAVES_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(DRIED_COMPRESSED_LEAVES);

    public static final DeferredItem<ZoccolettaItem> ZOCCOLETTA = TharidiaThings.ITEMS.register(
            "zoccoletta",
            () -> new ZoccolettaItem(new Item.Properties().durability(128)));

    // ── Init (forces class-load so DeferredRegister picks up everything) ───

    public static void init() {
    }
}
