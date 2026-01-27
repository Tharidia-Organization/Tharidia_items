package com.THproject.tharidia_things.block.station_crystal;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StationCrystalBlockEntity extends BlockEntity {
    private static final int MAX_TICK = 20 * 60 * 60 * 24 * 10; // 10 days
    private int tick;

    public StationCrystalBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.STATION_CRYSTAL_BLOCK_ENTITY.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StationCrystalBlockEntity blockEntity) {
        if (level.isClientSide)
            return;

        if (level.getBlockEntity(pos) instanceof StationCrystalBlockEntity be) {
            be.addTick();
            if (be.getTick() >= be.getMaxTick()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 1 | 2);
            }
        }
    }

    public void addTick() {
        tick++;
    }

    public boolean removeTickPercentage(double percentage) {
        if (getTickPercentage() <= 0.75) {
            tick -= (int) (MAX_TICK * percentage);
            return true;
        }
        return false;
    }

    public double getTickPercentage() {
        return (double) (MAX_TICK - tick) / MAX_TICK;
    }

    public void setTick(int duration) {
        this.tick = duration;
    }

    public int getTick() {
        return tick;
    }

    public int getMaxTick() {
        return MAX_TICK;
    }

    public int getRemainingTick() {
        return getMaxTick() - getTick();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Tick", this.tick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tick = tag.getInt("Tick");
    }
}
