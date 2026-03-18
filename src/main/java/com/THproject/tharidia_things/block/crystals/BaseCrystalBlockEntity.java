package com.THproject.tharidia_things.block.crystals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseCrystalBlockEntity extends BlockEntity {
    protected int hit;

    public BaseCrystalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

    public void hit() {
        this.hit++;
        calcStage();
    }

    public void setHit(int hit) {
        this.hit = hit;
        calcStage();
    }

    public int getHit() {
        return hit;
    }

    /**
     * Returns the maximum number of hits needed to fully process the crystal.
     * For crystals, this is 4 (one hit per cluster).
     */
    public int getMaxHit() {
        return 4;
    }

    /**
     * Returns the item to drop when the crystal is fully processed.
     * Each crystal type should return its corresponding pure crystal.
     */
    public abstract ItemStack getDrop();

    /**
     * Calculates and updates the stage based on current hits.
     * Stage directly corresponds to hits: 0 hits = stage 0, 1 hit = stage 1, etc.
     */
    public void calcStage() {
        if (this.level != null && !this.level.isClientSide()) {
            // Stage directly corresponds to number of hits (0-4)
            int stage = Math.min(hit, 4);
            BlockState state = this.getBlockState();
            if (state.getValue(CrystalsRegistry.STAGE) != stage) {
                this.level.setBlock(this.worldPosition, state.setValue(CrystalsRegistry.STAGE, stage), 3);
            }
        }
    }
}
