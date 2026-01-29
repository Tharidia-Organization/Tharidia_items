package com.THproject.tharidia_things.block.washer.sink;

import java.util.ArrayList;
import java.util.List;
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
    private final double PERCENTAGE_PER_TANK = 0.3;

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

        Direction sieveDirection = null;
        Direction tank1Direction = null;
        Direction tank2Direction = null;
        Direction tank3Direction = null;
        BlockEntity sieveBlockEntity = null;
        BlockEntity tank1BlockEntity = null;
        BlockEntity tank2BlockEntity = null;
        BlockEntity tank3BlockEntity = null;
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (facing == Direction.NORTH) {
            sieveBlockEntity = level.getBlockEntity(pos.offset(0, 0, 2));
            tank1BlockEntity = level.getBlockEntity(pos.offset(1, 0, 3));
            tank2BlockEntity = level.getBlockEntity(pos.offset(0, 0, 4));
            tank3BlockEntity = level.getBlockEntity(pos.offset(-1, 0, 3));
            sieveDirection = Direction.NORTH;
            tank1Direction = Direction.NORTH;
            tank2Direction = Direction.EAST;
            tank3Direction = Direction.SOUTH;
        } else if (facing == Direction.EAST) {
            sieveBlockEntity = level.getBlockEntity(pos.offset(-2, 0, 0));
            tank1BlockEntity = level.getBlockEntity(pos.offset(-3, 0, 1));
            tank2BlockEntity = level.getBlockEntity(pos.offset(-4, 0, 0));
            tank3BlockEntity = level.getBlockEntity(pos.offset(-3, 0, -1));
            sieveDirection = Direction.EAST;
            tank1Direction = Direction.EAST;
            tank2Direction = Direction.SOUTH;
            tank3Direction = Direction.WEST;
        } else if (facing == Direction.SOUTH) {
            sieveBlockEntity = level.getBlockEntity(pos.offset(0, 0, -2));
            tank1BlockEntity = level.getBlockEntity(pos.offset(-1, 0, -3));
            tank2BlockEntity = level.getBlockEntity(pos.offset(0, 0, -4));
            tank3BlockEntity = level.getBlockEntity(pos.offset(+1, 0, -3));
            sieveDirection = Direction.SOUTH;
            tank1Direction = Direction.SOUTH;
            tank2Direction = Direction.WEST;
            tank3Direction = Direction.NORTH;
        } else if (facing == Direction.WEST) {
            sieveBlockEntity = level.getBlockEntity(pos.offset(+2, 0, 0));
            tank1BlockEntity = level.getBlockEntity(pos.offset(+3, 0, -1));
            tank2BlockEntity = level.getBlockEntity(pos.offset(+4, 0, 0));
            tank3BlockEntity = level.getBlockEntity(pos.offset(+3, 0, +1));
            sieveDirection = Direction.WEST;
            tank1Direction = Direction.WEST;
            tank2Direction = Direction.NORTH;
            tank3Direction = Direction.EAST;
        }

        List<TankBlockEntity> tanks = new ArrayList<>();
        List<Direction> tanksDirections = new ArrayList<>();

        if (tank1BlockEntity instanceof TankBlockEntity tank) {
            tanks.add(tank);
            tanksDirections.add(tank1Direction);
        }
        if (tank2BlockEntity instanceof TankBlockEntity tank) {
            tanks.add(tank);
            tanksDirections.add(tank2Direction);
        }
        if (tank3BlockEntity instanceof TankBlockEntity tank) {
            tanks.add(tank);
            tanksDirections.add(tank3Direction);
        }

        if (sieveBlockEntity instanceof SieveBlockEntity sieve && tanks.size() > 0) {
            if (sieve.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) == sieveDirection) {
                int workingTanks = getWorkingTanks(tanks, tanksDirections);
                if (sieve.isActive() && workingTanks > 0) {
                    RecipeWrapper recipeWrapper = new RecipeWrapper(sieve.inventory);
                    Optional<RecipeHolder<WasherRecipe>> recipe = level.getRecipeManager()
                            .getRecipeFor(TharidiaThings.WASHER_RECIPE_TYPE.get(), recipeWrapper, level);
                    if (recipe.isPresent()) {
                        WasherRecipe sieveRecipe = recipe.get().value();

                        sink.maxProgress = sieveRecipe.getProcessingTime();
                        sink.maxProgress -= (int) Math
                                .floor(sieveRecipe.getProcessingTime() * (sink.PERCENTAGE_PER_TANK * (workingTanks - 1)));

                        ItemStack result = sieveRecipe.getResultItem(level.registryAccess());
                        if (workingTanks > 0 && sieve.hasMesh() && sink.canInsertItem(result)) {
                            sink.progress++;
                            if (sink.progress >= sink.maxProgress) {
                                sink.craftItem(sieveRecipe, sieve.inventory);
                                sink.resetProgress();
                            }
                            sink.setChanged();
                        } else {
                            sink.resetProgress();
                        }
                    } else {
                        sink.resetProgress();
                    }
                } else {
                    sink.resetProgress();
                }
            } else {
                sink.resetProgress();
            }
        } else {
            sink.resetProgress();
        }
    }

    private static int getWorkingTanks(List<TankBlockEntity> tanks, List<Direction> tanksDirections) {
        int result = 0;

        for (int i = 0; i < tanks.size(); i++) {
            TankBlockEntity tank = tanks.get(i);
            Direction direction = tanksDirections.get(i);

            if (tank.tank.getFluidAmount() > 0
                    && tank.isOpen()
                    && tank.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) == direction) {
                result++;
            }
        }

        return result;
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
        this.setChanged();
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
