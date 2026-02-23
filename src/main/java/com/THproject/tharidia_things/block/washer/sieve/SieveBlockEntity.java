package com.THproject.tharidia_things.block.washer.sieve;

import java.util.ArrayList;
import java.util.List;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.MultiblockGetter;
import com.THproject.tharidia_things.block.washer.sink.SinkBlockEntity;
import com.THproject.tharidia_things.block.washer.tank.TankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SieveBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float processPercentage = 0.0F;
    private boolean Active = false;
    private boolean wasActive = false;
    private boolean Mesh = false;
    private boolean CanRenderWater = false;

    public SinkBlockEntity sink;
    public TankBlockEntity tank1;
    public TankBlockEntity tank2;
    public TankBlockEntity tank3;

    public SieveBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.SIEVE_BLOCK_ENTITY.get(), pos, blockState);
    }

    // GeckoLib Methods
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<SieveBlockEntity> controller = new AnimationController<>(this, "controller", 0, state -> {
            if (this.isActive()) {
                this.wasActive = true;
                return state.setAndContinue(RawAnimation.begin().thenPlayAndHold("active_lever"));
            } else {
                if (this.wasActive) {
                    return state.setAndContinue(RawAnimation.begin().thenPlayAndHold("deactive_lever"));
                }
                return PlayState.STOP;
            }
        });
        controller.setAnimationSpeed(5.0d);
        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Input slot (0)
            if (slot == 0) {
                if (level == null)
                    return true;
                return level.getRecipeManager().getAllRecipesFor(TharidiaThings.WASHER_RECIPE_TYPE.get())
                        .stream()
                        .anyMatch(recipe -> recipe.value().getInput().test(stack));
            }

            // Residue Slot (1)
            if (slot == 1) {
                if (level == null)
                    return true;
                if (stack.getItem() instanceof BlockItem blockItem)
                    return blockItem.getBlock().equals(Blocks.GRAVEL);
                return false;
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

    public boolean toogleActive() {
        this.Active = !Active;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return Active;
    }

    public boolean isActive() {
        return this.Active;
    }

    public void setCanRenderwater(boolean val) {
        this.CanRenderWater = val;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean canRenderWater() {
        return this.CanRenderWater;
    }

    public void setProcessPercentage(float percentage) {
        this.processPercentage = percentage;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public float getProcessPercentage() {
        return this.processPercentage;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SieveBlockEntity sieve) {
        if (level.isClientSide)
            return;

        MultiblockGetter.fromSieve(level, sieve, pos);

        List<TankBlockEntity> tanks = new ArrayList<>();
        if (sieve.tank1 != null)
            tanks.add(sieve.tank1);
        if (sieve.tank2 != null)
            tanks.add(sieve.tank2);
        if (sieve.tank3 != null)
            tanks.add(sieve.tank3);

        int workingTanks = MultiblockGetter.getWorkingTanks(tanks);

        // Render water
        sieve.setCanRenderwater(workingTanks > 0);

        // Render Input
        if (sieve.sink != null)
            sieve.setProcessPercentage(sieve.sink.getProcessPercentage());
        else
            sieve.setProcessPercentage(0.0f);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putBoolean("Mesh", Mesh);
        tag.putBoolean("Active", Active);
        tag.putBoolean("CanRenderWater", CanRenderWater);
        tag.putFloat("ProcessPercentage", processPercentage);
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
        if (tag.contains("CanRenderWater")) {
            CanRenderWater = tag.getBoolean("CanRenderWater");
        }
        if (tag.contains("Active")) {
            Active = tag.getBoolean("Active");
        }
        if (tag.contains("ProcessPercentage")) {
            processPercentage = tag.getFloat("ProcessPercentage");
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
