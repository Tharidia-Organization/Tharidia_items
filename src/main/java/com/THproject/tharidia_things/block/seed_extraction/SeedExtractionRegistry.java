package com.THproject.tharidia_things.block.seed_extraction;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.ZoccolettaItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class SeedExtractionRegistry {

    // ── Leaves Blocks ───────────────────────────────────────────────────────

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

    // ── Grass Blocks ────────────────────────────────────────────────────────

    public static final DeferredBlock<Block> COMPRESSED_GRASS = TharidiaThings.BLOCKS.register(
            "compressed_grass",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GRASS)
                    .strength(1.0F)
                    .sound(SoundType.GRASS)));

    public static final DeferredBlock<WetCompressedGrassBlock> WET_COMPRESSED_GRASS = TharidiaThings.BLOCKS.register(
            "wet_compressed_grass",
            () -> new WetCompressedGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GRASS)
                    .strength(1.0F)
                    .sound(SoundType.GRASS)
                    .randomTicks()));

    public static final DeferredBlock<DriedCompressedGrassBlock> DRIED_COMPRESSED_GRASS = TharidiaThings.BLOCKS.register(
            "dried_compressed_grass",
            () -> new DriedCompressedGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(1.0F)
                    .sound(SoundType.GRASS)));

    // ── Fertilized Dirt & Abnormal Grass ───────────────────────────────────

    public static final DeferredBlock<FertilizedDirtBlock> FERTILIZED_DIRT = TharidiaThings.BLOCKS.register(
            "fertilized_dirt",
            () -> new FertilizedDirtBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .strength(0.5F)
                    .sound(SoundType.GRAVEL)
                    .randomTicks()));

    public static final DeferredBlock<AbnormalGrassBlock> ABNORMAL_GRASS = TharidiaThings.BLOCKS.register(
            "abnormal_grass",
            () -> new AbnormalGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY)));

    // ── Leaves Items ────────────────────────────────────────────────────────

    public static final DeferredItem<net.minecraft.world.item.BlockItem> WET_COMPRESSED_LEAVES_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(WET_COMPRESSED_LEAVES);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> DRIED_COMPRESSED_LEAVES_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(DRIED_COMPRESSED_LEAVES);

    public static final DeferredItem<CompressedLeavesBlockItem> COMPRESSED_LEAVES_ITEM = TharidiaThings.ITEMS.register(
            "compressed_leaves",
            () -> new CompressedLeavesBlockItem(COMPRESSED_LEAVES.get(), new Item.Properties(),
                    () -> WET_COMPRESSED_LEAVES_ITEM.get()));

    // ── Grass Items ─────────────────────────────────────────────────────────

    public static final DeferredItem<net.minecraft.world.item.BlockItem> WET_COMPRESSED_GRASS_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(WET_COMPRESSED_GRASS);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> DRIED_COMPRESSED_GRASS_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(DRIED_COMPRESSED_GRASS);

    public static final DeferredItem<CompressedLeavesBlockItem> COMPRESSED_GRASS_ITEM = TharidiaThings.ITEMS.register(
            "compressed_grass",
            () -> new CompressedLeavesBlockItem(COMPRESSED_GRASS.get(), new Item.Properties(),
                    () -> WET_COMPRESSED_GRASS_ITEM.get()));

    // ── Fertilized Dirt & Abnormal Grass Items ───────────────────────────────

    public static final DeferredItem<net.minecraft.world.item.BlockItem> FERTILIZED_DIRT_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(FERTILIZED_DIRT);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> ABNORMAL_GRASS_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(ABNORMAL_GRASS);

    // ── Tools ───────────────────────────────────────────────────────────────

    public static final DeferredItem<ZoccolettaItem> ZOCCOLETTA = TharidiaThings.ITEMS.register(
            "zoccoletta",
            () -> new ZoccolettaItem(new Item.Properties().durability(128)));

    // ── Init (forces class-load so DeferredRegister picks up everything) ───

    public static void init() {
    }
}
