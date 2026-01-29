package com.THproject.tharidia_things.block.washer.sink;

import java.util.Optional;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sieve.SieveBlockEntity;
import com.THproject.tharidia_things.block.washer.tank.TankBlockEntity;
import com.THproject.tharidia_things.recipe.WasherRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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

    private int maxProgress;
    private int progress;

    public SinkBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.SINK_BLOCK_ENTITY.get(), pos, blockState);
    }

    public final ItemStackHandler sinkInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public static void tick(Level level, BlockPos pos, BlockState state, SinkBlockEntity sink) {
        if (level.isClientSide)
            return;

        BlockEntity sieveBlockEntity = null;
        BlockEntity tankBlockEntity = null;
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (facing == Direction.NORTH) {
            sieveBlockEntity = level.getBlockEntity(pos.offset(0, 0, 2));
            tankBlockEntity = level.getBlockEntity(pos.offset(1, 0, 3));
        } else if (facing == Direction.EAST) {
            sieveBlockEntity = level.getBlockEntity(pos.offset(-2, 0, 0));
            tankBlockEntity = level.getBlockEntity(pos.offset(-3, 0, 1));
        } else if (facing == Direction.SOUTH) {
            sieveBlockEntity = level.getBlockEntity(pos.offset(0, 0, -2));
            tankBlockEntity = level.getBlockEntity(pos.offset(-1, 0, -3));
        } else if (facing == Direction.WEST) {
            sieveBlockEntity = level.getBlockEntity(pos.offset(+2, 0, 0));
            tankBlockEntity = level.getBlockEntity(pos.offset(+3, 0, -1));
        }

        if (sieveBlockEntity == null || tankBlockEntity == null)
            return;

        if (sieveBlockEntity instanceof SieveBlockEntity sieve && tankBlockEntity instanceof TankBlockEntity tank) {
            RecipeWrapper recipeWrapper = new RecipeWrapper(sieve.inventory);
            Optional<RecipeHolder<WasherRecipe>> recipe = level.getRecipeManager()
                    .getRecipeFor(TharidiaThings.WASHER_RECIPE_TYPE.get(), recipeWrapper, level);
            if (recipe.isPresent()) {
                WasherRecipe sieveRecipe = recipe.get().value();
                sink.maxProgress = sieveRecipe.getProcessingTime();

                ItemStack result = sieveRecipe.getResultItem(level.registryAccess());
                if (tank.tank.getFluidAmount() > 0 && sieve.hasMesh() && sink.canInsertItem(result)) {
                    sink.progress++;
                    if (sink.progress >= sink.maxProgress) {
                        sink.craftItem(sieveRecipe, sieve.inventory);
                        sink.resetProgress();
                    }
                    sink.setChanged();
                } else {
                    sink.resetProgress();
                    sink.setChanged();
                }
            } else {
                sink.resetProgress();
                sink.setChanged();
            }
        }
    }

    private void craftItem(WasherRecipe recipe, ItemStackHandler sieveInventory) {
        ItemStack result = recipe.getResultItem(this.level.registryAccess());
        sieveInventory.extractItem(0, 1, false);

        for (int i = 0; i < sinkInventory.getSlots(); i++) {
            ItemStack outputStack = sinkInventory.getStackInSlot(i);
            if (outputStack.isEmpty()) {
                sinkInventory.setStackInSlot(i, result.copy());
                return;
            } else if (!outputStack.isEmpty() && outputStack.getItem() == result.getItem()
                    && outputStack.getCount() + result.getCount() <= outputStack.getMaxStackSize()) {
                outputStack.grow(result.getCount());
                return;
            }
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

    private void resetProgress() {
        this.progress = 0;
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
