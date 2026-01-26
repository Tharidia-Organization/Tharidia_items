package com.THproject.tharidia_things.block.ore_chunks;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CopperChunkBlockEntity extends BlockEntity {
    private static final int MAX_HIT = 5;
    private static final ItemStack ITEM_DROP = Items.RAW_COPPER.getDefaultInstance();
    private int hit;

    public CopperChunkBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.COPPER_CHUNK_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("hit", this.hit);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.hit = tag.getInt("hit");
    }

    public void setHit(int hit) {
        this.hit = hit;
        calcStage();
    }

    public int getHit() {
        return hit;
    }

    public int getMaxHit() {
        return MAX_HIT;
    }

    public ItemStack getDrop() {
        return ITEM_DROP.copy();
    }

    public void hit() {
        this.hit++;
        calcStage();
    }

    public void calcStage() {
        if (this.level != null && !this.level.isClientSide()) {
            int stage = (int) Math.floor(((float) hit / MAX_HIT * 100) / 20);
            stage = stage <= 4 ? stage : 4;
            BlockState state = this.getBlockState();
            if (state.getValue(Chunks.STAGE) != stage) {
                this.level.setBlock(this.worldPosition, state.setValue(Chunks.STAGE, stage), 3);
            }
        }
    }
}
