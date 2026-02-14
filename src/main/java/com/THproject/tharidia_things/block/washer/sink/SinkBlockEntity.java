package com.THproject.tharidia_things.block.washer.sink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.MultiblockGetter;
import com.THproject.tharidia_things.block.washer.sieve.SieveBlockEntity;
import com.THproject.tharidia_things.block.washer.tank.TankBlockEntity;
import com.THproject.tharidia_things.recipe.WasherRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SinkBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final double PERCENTAGE_PER_TANK = 0.3;

    private int maxProgress = 1;
    private int progress;

    private ItemStack result = ItemStack.EMPTY;

    public SieveBlockEntity sieve;
    public TankBlockEntity tank1;
    public TankBlockEntity tank2;
    public TankBlockEntity tank3;

    public SinkBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.SINK_BLOCK_ENTITY.get(), pos, blockState);
    }

    public final ItemStackHandler sinkInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    };

    public static void tick(Level level, BlockPos pos, BlockState state, SinkBlockEntity sink) {
        if (level.isClientSide)
            return;

        MultiblockGetter.fromSink(level, sink, pos);

        List<TankBlockEntity> tanks = new ArrayList<>();

        if (sink.tank1 != null)
            tanks.add(sink.tank1);
        if (sink.tank2 != null)
            tanks.add(sink.tank2);
        if (sink.tank3 != null)
            tanks.add(sink.tank3);
        if (sink.sieve == null || tanks.size() == 0)
            return;

        int workingTanks = MultiblockGetter.getWorkingTanks(tanks);

        RecipeWrapper recipeWrapper = new RecipeWrapper(sink.sieve.inventory);
        Optional<RecipeHolder<WasherRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(TharidiaThings.WASHER_RECIPE_TYPE.get(), recipeWrapper, level);

        if (!recipe.isPresent()) {
            sink.resetProgress();
            return;
        }

        if (!sink.sieve.isActive() || !sink.sieve.hasMesh() || workingTanks == 0 || sink.hasInventoryFull(sink)
                || sink.sieve.inventory.getStackInSlot(1).getCount() == 64) {
            sink.resetProgress();
            return;
        }

        WasherRecipe washerRecipe = recipe.get().value();

        calcPercentage(sink, washerRecipe, workingTanks);

        if (sink.progress++ >= sink.maxProgress && sink.result.isEmpty()) {
            sink.result = washerRecipe.getResultItem(level.registryAccess());
        }

        if (!sink.result.isEmpty()) {
            if (sink.canInsertItem(sink.result)) {
                sink.craftItem(washerRecipe, sink.sieve.inventory);
                sink.resetProgress();
                sink.result = ItemStack.EMPTY;
            } else {
                sink.resetProgress();
            }
        }
    }

    private static void calcPercentage(SinkBlockEntity sink, WasherRecipe recipe, int workingTanks) {
        sink.maxProgress = recipe.getProcessingTime();
        sink.maxProgress -= (int) (recipe.getProcessingTime() *
                (sink.PERCENTAGE_PER_TANK * (workingTanks - 1)));
    }

    private void craftItem(WasherRecipe recipe, ItemStackHandler sieveInventory) {
        ItemStack result = recipe.getResultItem(this.level.registryAccess());
        sieveInventory.extractItem(0, 1, false);

        for (int i = 0; i < sinkInventory.getSlots(); i++) {
            ItemStack outputStack = sinkInventory.getStackInSlot(i);
            if (outputStack.isEmpty()) {
                sinkInventory.setStackInSlot(i, result.copy());
                break;
            } else if (!outputStack.isEmpty() && outputStack.getItem() == result.getItem()
                    && outputStack.getCount() + result.getCount() <= outputStack.getMaxStackSize()) {
                outputStack.grow(result.getCount());
                break;
            }
        }

        // Insert residue block
        ItemStack stack = sieveInventory.getStackInSlot(1);
        if (stack.isEmpty()) {
            sieveInventory.setStackInSlot(1, new ItemStack(Items.GRAVEL));
        } else {
            stack.grow(1);
        }
    }

    private boolean canInsertItem(ItemStack item) {
        for (int i = 0; i < sinkInventory.getSlots(); i++) {
            ItemStack outputStack = sinkInventory.getStackInSlot(i);
            if (outputStack.isEmpty()) {
                return true;
            } else if (!outputStack.isEmpty() && outputStack.getItem() == item.getItem()
                    && outputStack.getCount() + item.getCount() <= outputStack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasInventoryFull(SinkBlockEntity sink) {
        for (int i = 0; i < sink.sinkInventory.getSlots(); i++) {
            if (sink.sinkInventory.getStackInSlot(i).getCount() < 64) {
                return false;
            }
        }
        return true;
    }

    private void resetProgress() {
        this.progress = 0;
        this.setChanged();
    }

    public float getProcessPercentage() {
        return (float) progress / (float) maxProgress;
    }

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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", sinkInventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            sinkInventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
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
