package com.THproject.tharidia_things.block.washer;

import com.THproject.tharidia_things.recipe.WasherRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import java.util.Optional;
import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WasherBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public final int FLUID_CONSUMPTION_TICK = 1;
    private boolean mesh = false;

    // GeckoLib Methods
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.progress > 0) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("levitate"));
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public final FluidTank tank = new FluidTank(8000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    };

    public final ItemStackHandler inventory = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                if (level == null)
                    return true;
                return level.getRecipeManager().getAllRecipesFor(TharidiaThings.WASHER_RECIPE_TYPE.get())
                        .stream()
                        .anyMatch(recipe -> recipe.value().getInput().test(stack));
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

        RecipeWrapper recipeWrapper = new RecipeWrapper(blockEntity.inventory);
        Optional<RecipeHolder<WasherRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(TharidiaThings.WASHER_RECIPE_TYPE.get(), recipeWrapper, level);

        blockEntity.tank.drain(blockEntity.FLUID_CONSUMPTION_TICK, IFluidHandler.FluidAction.EXECUTE);

        if (recipe.isPresent()) {
            WasherRecipe washerRecipe = recipe.get().value();
            blockEntity.maxProgress = washerRecipe.getProcessingTime();

            ItemStack result = washerRecipe.getResultItem(level.registryAccess());
            if (blockEntity.tank.getFluidAmount() > 0 && blockEntity.hasMesh() && blockEntity.canInsertItem(result)) {
                blockEntity.progress++;
                if (blockEntity.progress >= blockEntity.maxProgress) {
                    blockEntity.craftItem(washerRecipe);
                    blockEntity.progress = 0;
                }
                blockEntity.setChanged();
            } else {
                blockEntity.resetProgress();
                blockEntity.setChanged();
            }
        } else {
            blockEntity.resetProgress();
            blockEntity.setChanged();
        }
    }

    private void craftItem(WasherRecipe recipe) {
        ItemStack result = recipe.getResultItem(this.level.registryAccess());
        inventory.extractItem(0, 1, false);

        for (int i = 1; i < inventory.getSlots(); i++) {
            ItemStack outputStack = inventory.getStackInSlot(i);
            if (outputStack.isEmpty()) {
                inventory.setStackInSlot(i, result.copy());
                return;
            } else if (!outputStack.isEmpty() && outputStack.getItem() == result.getItem()
                    && outputStack.getCount() + result.getCount() <= outputStack.getMaxStackSize()) {
                outputStack.grow(result.getCount());
                return;
            }
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private boolean canInsertItem(ItemStack item) {
        for (int i = 1; i < inventory.getSlots(); i++) {
            ItemStack outputStack = inventory.getStackInSlot(i);
            if (outputStack.isEmpty()) {
                return true;
            } else if (!outputStack.isEmpty() && outputStack.getItem() == item.getItem()
                    && outputStack.getCount() + item.getCount() <= outputStack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("WasherProgress", progress);
        tag.putBoolean("mesh", mesh);
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
        if (tag.contains("mesh")) {
            mesh = tag.getBoolean("mesh");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt,
            HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, lookupProvider);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.loadAdditional(tag, lookupProvider);
        loadAdditional(tag, lookupProvider);
    }

    public void addWaterBucket() {
        tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
    }

    public void setMesh() {
        mesh = true;
    }

    public void removeMesh() {
        mesh = false;
    }

    public boolean hasMesh() {
        return mesh;
    }

    public boolean isTankFull() {
        return tank.getFluidAmount() >= tank.getCapacity() - 1000;
    }

    public boolean isTankEmpty() {
        return tank.isEmpty();
    }
}
