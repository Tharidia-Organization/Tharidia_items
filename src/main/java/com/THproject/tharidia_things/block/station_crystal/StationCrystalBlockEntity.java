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
    // 10 seconds in milliseconds
    private static final long MAX_TIME = 10L * 24 * 60 * 60 * 1000;
    private long placedTime;

    public StationCrystalBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.STATION_CRYSTAL_BLOCK_ENTITY.get(), pos, blockState);
        this.placedTime = System.currentTimeMillis();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StationCrystalBlockEntity blockEntity) {
        if (level.isClientSide)
            return;

        if (System.currentTimeMillis() >= blockEntity.placedTime + MAX_TIME) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.destroyBlock(new BlockPos(pos.getX(),pos.getY()+1,pos.getZ()), true);
        }
    }

    public boolean removeTimePercentage(double percentage) {
        if (getTimePercentage() <= 0.75) {
            long repairAmount = (long) (MAX_TIME * percentage);
            long currentTime = System.currentTimeMillis();

            this.placedTime += repairAmount;

            // Ensure placedTime doesn't exceed currentTime
            if (this.placedTime > currentTime) {
                this.placedTime = currentTime;
            }
            setChanged();
            return true;
        }
        return false;
    }

    public long getRemainingTime() {
        long elapsedTime = System.currentTimeMillis() - this.placedTime;
        long remainingTime = MAX_TIME - elapsedTime;
        return Math.max(remainingTime, 0);
    }

    public double getTimePercentage() {
        long elapsedTime = System.currentTimeMillis() - this.placedTime;
        return (double) (MAX_TIME - elapsedTime) / MAX_TIME;
    }

    public long getMaxTime() {
        return MAX_TIME;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("PlacedTime", this.placedTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("PlacedTime")) {
            this.placedTime = tag.getLong("PlacedTime");
        } else {
            // Default to current time if tag missing (new placement or migration)
            this.placedTime = System.currentTimeMillis();
        }
    }
}
