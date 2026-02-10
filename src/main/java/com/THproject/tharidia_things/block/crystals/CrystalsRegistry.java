package com.THproject.tharidia_things.block.crystals;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public class CrystalsRegistry {
    // Stage property: 0 = all 4 clusters, 1 = 3 clusters, 2 = 2 clusters, 3 = 1 cluster, 4 = 0 clusters
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);

    // Crystal 1
    public static final DeferredBlock<Crystal1Block> CRYSTAL_1 = TharidiaThings.BLOCKS.register(
            "crystal_1",
            () -> new Crystal1Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .destroyTime(4.0f)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Crystal1BlockEntity>> CRYSTAL_1_BLOCK_ENTITY = TharidiaThings.BLOCK_ENTITIES
            .register("crystal_1",
                    () -> BlockEntityType.Builder.of(Crystal1BlockEntity::new, CRYSTAL_1.get()).build(null));

    public static final DeferredItem<BlockItem> CRYSTAL_1_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(CRYSTAL_1);

    // Crystal 2
    public static final DeferredBlock<Crystal2Block> CRYSTAL_2 = TharidiaThings.BLOCKS.register(
            "crystal_2",
            () -> new Crystal2Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .destroyTime(4.0f)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Crystal2BlockEntity>> CRYSTAL_2_BLOCK_ENTITY = TharidiaThings.BLOCK_ENTITIES
            .register("crystal_2",
                    () -> BlockEntityType.Builder.of(Crystal2BlockEntity::new, CRYSTAL_2.get()).build(null));

    public static final DeferredItem<BlockItem> CRYSTAL_2_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(CRYSTAL_2);

    // Crystal 3
    public static final DeferredBlock<Crystal3Block> CRYSTAL_3 = TharidiaThings.BLOCKS.register(
            "crystal_3",
            () -> new Crystal3Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .destroyTime(4.0f)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Crystal3BlockEntity>> CRYSTAL_3_BLOCK_ENTITY = TharidiaThings.BLOCK_ENTITIES
            .register("crystal_3",
                    () -> BlockEntityType.Builder.of(Crystal3BlockEntity::new, CRYSTAL_3.get()).build(null));

    public static final DeferredItem<BlockItem> CRYSTAL_3_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(CRYSTAL_3);

    // Crystal 4
    public static final DeferredBlock<Crystal4Block> CRYSTAL_4 = TharidiaThings.BLOCKS.register(
            "crystal_4",
            () -> new Crystal4Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .destroyTime(4.0f)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Crystal4BlockEntity>> CRYSTAL_4_BLOCK_ENTITY = TharidiaThings.BLOCK_ENTITIES
            .register("crystal_4",
                    () -> BlockEntityType.Builder.of(Crystal4BlockEntity::new, CRYSTAL_4.get()).build(null));

    public static final DeferredItem<BlockItem> CRYSTAL_4_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(CRYSTAL_4);

    // Crystal 5
    public static final DeferredBlock<Crystal5Block> CRYSTAL_5 = TharidiaThings.BLOCKS.register(
            "crystal_5",
            () -> new Crystal5Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .destroyTime(4.0f)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Crystal5BlockEntity>> CRYSTAL_5_BLOCK_ENTITY = TharidiaThings.BLOCK_ENTITIES
            .register("crystal_5",
                    () -> BlockEntityType.Builder.of(Crystal5BlockEntity::new, CRYSTAL_5.get()).build(null));

    public static final DeferredItem<BlockItem> CRYSTAL_5_ITEM = TharidiaThings.ITEMS
            .registerSimpleBlockItem(CRYSTAL_5);

    // Pure crystal items (dropped when all clusters are removed)
    public static final DeferredItem<Item> PURE_CRYSTAL_1 = TharidiaThings.ITEMS.register(
            "pure_crystal_1",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PURE_CRYSTAL_2 = TharidiaThings.ITEMS.register(
            "pure_crystal_2",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PURE_CRYSTAL_3 = TharidiaThings.ITEMS.register(
            "pure_crystal_3",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PURE_CRYSTAL_4 = TharidiaThings.ITEMS.register(
            "pure_crystal_4",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PURE_CRYSTAL_5 = TharidiaThings.ITEMS.register(
            "pure_crystal_5",
            () -> new Item(new Item.Properties()));

    public static void init() {
    }
}
