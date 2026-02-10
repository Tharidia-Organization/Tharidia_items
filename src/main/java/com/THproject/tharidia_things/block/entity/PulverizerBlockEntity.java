package com.THproject.tharidia_things.block.entity;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PulverizerBlockEntity extends BlockEntity {
    private ItemStack grinder = ItemStack.EMPTY;

    public PulverizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.PULVERIZER_BLOCK_ENTITY.get(), pos, blockState);
    }

    public void setGrinder(ItemStack stack) {
        grinder = stack.copy();
        setChanged();
    }

    public void removeGrinder() {
        grinder = ItemStack.EMPTY;
        setChanged();
    }

    public ItemStack getGrinder() {
        return grinder;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!grinder.isEmpty()) {
            tag.put("Grinder", grinder.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Grinder")) {
            grinder = ItemStack.parse(registries, tag.getCompound("Grinder")).orElse(ItemStack.EMPTY);
        }
    }

    @Override
    public void setChanged() {
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
        super.setChanged();
    }
}
