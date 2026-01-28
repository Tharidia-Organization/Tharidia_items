package com.THproject.tharidia_things.block.washer.sieve;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SieveBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean Mesh = false;

    public SieveBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.SIEVE_BLOCK_ENTITY.get(), pos, blockState);
    }

    // GeckoLib Methods
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                if (level == null)
                    return true;
                return level.getRecipeManager().getAllRecipesFor(TharidiaThings.SIEVE_RECIPE_TYPE.get())
                        .stream()
                        .anyMatch(recipe -> recipe.value().getInput().test(stack));
            }
            return false;
        }
    };

    public boolean hasMesh() {
        return Mesh;
    }

    public void setMesh() {
        this.Mesh = true;
        setChanged();
    }

    public void removeMesh() {
        this.Mesh = false;
        setChanged();
    }

    // public static void tick(Level level, BlockPos pos, BlockState state,
    // SieveBlockEntity blockEntity) {
    // if (level.isClientSide)
    // return;

    // RecipeWrapper recipeWrapper = new RecipeWrapper(blockEntity.inventory);
    // Optional<RecipeHolder<SieveRecipe>> recipe = level.getRecipeManager()
    // .getRecipeFor(TharidiaThings.SIEVE_RECIPE_TYPE.get(), recipeWrapper, level);

    // blockEntity.tank.drain(blockEntity.FLUID_CONSUMPTION_TICK,
    // IFluidHandler.FluidAction.EXECUTE);

    // if (recipe.isPresent()) {
    // SieveRecipe sieveRecipe = recipe.get().value();
    // blockEntity.maxProgress = sieveRecipe.getProcessingTime();

    // ItemStack result = sieveRecipe.getResultItem(level.registryAccess());
    // if (blockEntity.tank.getFluidAmount() > 0 && blockEntity.hasMesh() &&
    // blockEntity.canInsertItem(result)) {
    // blockEntity.progress++;
    // if (blockEntity.progress >= blockEntity.maxProgress) {
    // blockEntity.craftItem(sieveRecipe);
    // blockEntity.progress = 0;
    // }
    // blockEntity.setChanged();
    // } else {
    // blockEntity.resetProgress();
    // blockEntity.setChanged();
    // }
    // } else {
    // blockEntity.resetProgress();
    // blockEntity.setChanged();
    // }
    // }

    // private void craftItem(SieveRecipe recipe) {
    // ItemStack result = recipe.getResultItem(this.level.registryAccess());
    // inventory.extractItem(0, 1, false);

    // for (int i = 1; i < inventory.getSlots(); i++) {
    // ItemStack outputStack = inventory.getStackInSlot(i);
    // if (outputStack.isEmpty()) {
    // inventory.setStackInSlot(i, result.copy());
    // return;
    // } else if (!outputStack.isEmpty() && outputStack.getItem() ==
    // result.getItem()
    // && outputStack.getCount() + result.getCount() <=
    // outputStack.getMaxStackSize()) {
    // outputStack.grow(result.getCount());
    // return;
    // }
    // }
    // }

    // private void resetProgress() {
    // this.progress = 0;
    // }

    // private boolean canInsertItem(ItemStack item) {
    // for (int i = 1; i < inventory.getSlots(); i++) {
    // ItemStack outputStack = inventory.getStackInSlot(i);
    // if (outputStack.isEmpty()) {
    // return true;
    // } else if (!outputStack.isEmpty() && outputStack.getItem() == item.getItem()
    // && outputStack.getCount() + item.getCount() <= outputStack.getMaxStackSize())
    // {
    // return true;
    // }
    // }
    // return false;
    // }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putBoolean("Mesh", Mesh);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Mesh")) {
            Mesh = tag.getBoolean("Mesh");
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
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,
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
}
