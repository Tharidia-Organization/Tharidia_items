package com.THproject.tharidia_things.block.ore_chunks.tin;

import com.THproject.tharidia_things.block.ore_chunks.BaseChunkBlockEntity;
import com.THproject.tharidia_things.block.ore_chunks.ChunksRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class TinChunkBlockEntity extends BaseChunkBlockEntity {
    private static final int MAX_HIT = 5;
    private static final ItemStack ITEM_DROP = Items.DIAMOND.getDefaultInstance();

    public TinChunkBlockEntity(BlockPos pos, BlockState state) {
        super(ChunksRegistry.TIN_CHUNK_BLOCK_ENTITY.get(), pos, state);
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
