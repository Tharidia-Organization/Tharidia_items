package com.THproject.tharidia_things.block.washer;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

public class WasherBlockEntity extends BlockEntity {
    public final FluidTank tank = new FluidTank(8000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    };

    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return stack.getItem() == TharidiaThings.VEIN_SEDIMENT.asItem();
            }
            return false;
        }
    };

    private int progress = 0;
    private int maxProgress = 40;

    public WasherBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.WASHER_BLOCK_ENTITY.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WasherBlockEntity blockEntity) {
        if (level.isClientSide)
            return;

        if (blockEntity.hasRecipe()) {
            blockEntity.progress++;
            if (blockEntity.progress >= blockEntity.maxProgress) {
                blockEntity.craftItem();
                blockEntity.progress = 0;
            }
            blockEntity.setChanged(); // Mark as changed to save progress
        } else {
            blockEntity.resetProgress();
            blockEntity.setChanged();
        }
    }

    private boolean hasRecipe() {
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack result = new ItemStack(TharidiaThings.IRON_CHUNK.asItem());
        boolean hasWater = tank.getFluidAmount() >= 50;

        return input.getItem() == TharidiaThings.VEIN_SEDIMENT.asItem() && hasWater && canInsertItem(result);
    }

    private void craftItem() {
        ItemStack result = new ItemStack(TharidiaThings.IRON_CHUNK.asItem());
        inventory.extractItem(0, 1, false);

        ItemStack outputStack = inventory.getStackInSlot(1);
        if (outputStack.isEmpty()) {
            inventory.setStackInSlot(1, result);
        } else {
            outputStack.grow(result.getCount());
        }

        tank.drain(50, IFluidHandler.FluidAction.EXECUTE);
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private boolean canInsertItem(ItemStack item) {
        ItemStack outputStack = inventory.getStackInSlot(1);
        return outputStack.isEmpty() || (outputStack.getItem() == item.getItem()
                && outputStack.getCount() + item.getCount() <= outputStack.getMaxStackSize());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("WasherProgress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Tank")) {
            tank.readFromNBT(registries, tag.getCompound("Tank"));
        }
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("WasherProgress")) {
            progress = tag.getInt("WasherProgress");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void addWaterBucket() {
        tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
    }

    public boolean isTankFull() {
        return tank.getFluidAmount() >= tank.getCapacity();
    }

    public boolean isTankEmpty() {
        return tank.isEmpty();
    }
}
