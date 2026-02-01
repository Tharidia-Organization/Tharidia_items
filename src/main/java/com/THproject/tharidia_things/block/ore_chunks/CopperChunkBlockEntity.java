package com.THproject.tharidia_things.block.ore_chunks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class CopperChunkBlockEntity extends BaseChunkBlockEntity {
    private static final int MAX_HIT = 5;
    private static final ItemStack ITEM_DROP = Items.RAW_COPPER.getDefaultInstance();

    public CopperChunkBlockEntity(BlockPos pos, BlockState state) {
        super(ChunksRegistry.COPPER_CHUNK_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public int getMaxHit() {
        return MAX_HIT;
    }

    @Override
    public ItemStack getDrop() {
        return ITEM_DROP.copy();
    }
}
