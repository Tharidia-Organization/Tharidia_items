package com.THproject.tharidia_things.block.ore_chunks;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.ore_chunks.copper.CopperChunkBlock;
import com.THproject.tharidia_things.block.ore_chunks.copper.CopperChunkBlockEntity;
import com.THproject.tharidia_things.block.ore_chunks.coal.CoalChunkBlock;
import com.THproject.tharidia_things.block.ore_chunks.coal.CoalChunkBlockEntity;
import com.THproject.tharidia_things.block.ore_chunks.tin.TinChunkBlock;
import com.THproject.tharidia_things.block.ore_chunks.tin.TinChunkBlockEntity;
import com.THproject.tharidia_things.block.ore_chunks.iron.IronChunkBlock;
import com.THproject.tharidia_things.block.ore_chunks.iron.IronChunkBlockEntity;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public class ChunksRegistry {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);

    // Coal chunk
    public static final DeferredBlock<CoalChunkBlock> COAL_CHUNK = TharidiaThings.BLOCKS.register(
            "coal_chunk",
            () -> new CoalChunkBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(4.0f)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoalChunkBlockEntity>> COAL_CHUNK_BLOCK_ENTITY = TharidiaThings.BLOCK_ENTITIES
            .register("coal_chunk",
                    () -> BlockEntityType.Builder.of(CoalChunkBlockEntity::new, COAL_CHUNK.get()).build(null));

    public static final DeferredItem<BlockItem> COAL_CHUNK_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(COAL_CHUNK);

    // Copper chunk
    public static final DeferredBlock<CopperChunkBlock> COPPER_CHUNK = TharidiaThings.BLOCKS.register(
            "copper_chunk",
            () -> new CopperChunkBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(4.0f)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CopperChunkBlockEntity>> COPPER_CHUNK_BLOCK_ENTITY = TharidiaThings.BLOCK_ENTITIES
            .register("copper_chunk",
                    () -> BlockEntityType.Builder.of(CopperChunkBlockEntity::new, COPPER_CHUNK.get()).build(null));

    public static final DeferredItem<BlockItem> COPPER_CHUNK_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(COPPER_CHUNK);

    // Tin chunk
    public static final DeferredBlock<TinChunkBlock> TIN_CHUNK = TharidiaThings.BLOCKS.register(
            "tin_chunk",
            () -> new TinChunkBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(4.0f)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TinChunkBlockEntity>> TIN_CHUNK_BLOCK_ENTITY = TharidiaThings.BLOCK_ENTITIES
            .register("tin_chunk",
                    () -> BlockEntityType.Builder.of(TinChunkBlockEntity::new, TIN_CHUNK.get()).build(null));

    public static final DeferredItem<BlockItem> TIN_CHUNK_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(TIN_CHUNK);

    // Iron chunk
    public static final DeferredBlock<IronChunkBlock> IRON_CHUNK = TharidiaThings.BLOCKS.register(
            "iron_chunk",
            () -> new IronChunkBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(4.0f)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IronChunkBlockEntity>> IRON_CHUNK_BLOCK_ENTITY = TharidiaThings.BLOCK_ENTITIES
            .register("iron_chunk",
                    () -> BlockEntityType.Builder.of(IronChunkBlockEntity::new, IRON_CHUNK.get()).build(null));

    public static final DeferredItem<BlockItem> IRON_CHUNK_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(IRON_CHUNK);

    public static void init() {
    }
}
