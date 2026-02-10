package com.THproject.tharidia_things.block.pulverizer;

import java.util.List;
import java.util.Optional;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.recipe.PulverizerRecipe;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PulverizerBlockEntity extends BlockEntity implements GeoBlockEntity {
    private Object workingSoundInstance;

    private static int MAX_ACTIVE_PER_CLICK = 1000;

    private ItemStack grinder = ItemStack.EMPTY;
    private long active_timestamp = -1;

    private int progress;
    private int maxProgress;

    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        public boolean isItemValid(int slot, ItemStack stack) {
            // Slot 0 -> input
            if (slot == 0) {
                if (level == null)
                    return true;
                return level.getRecipeManager().getAllRecipesFor(TharidiaThings.PULVERIZER_RECIPE_TYPE.get())
                        .stream()
                        .anyMatch(recipe -> recipe.value().getInput().test(stack));
            }
            return false;
        }
    };

    // Animations
    private static final RawAnimation ACTIVE_ANIM = RawAnimation.begin().thenLoop("active");

    public PulverizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(TharidiaThings.PULVERIZER_BLOCK_ENTITY.get(), pos, blockState);
    }

    public void setWorkingSound(Object sound) {
        this.workingSoundInstance = sound;
    }

    public SoundInstance getWorkingSound() {
        return (SoundInstance) workingSoundInstance;
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
        return grinder.copy();
    }

    public void damageGrinder() {
        grinder.setDamageValue(grinder.getDamageValue() + 1);
    }

    public boolean hasGrinder() {
        return !grinder.isEmpty();
    }

    public void setActive() {
        if (hasGrinder())
            active_timestamp = System.currentTimeMillis();
    }

    public boolean isActive() {
        return ((System.currentTimeMillis() - active_timestamp) <= MAX_ACTIVE_PER_CLICK);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PulverizerBlockEntity pulverizer) {
        RecipeWrapper recipeWrapper = new RecipeWrapper(pulverizer.inventory);
        Optional<RecipeHolder<PulverizerRecipe>> recipeHolder = level.getRecipeManager()
                .getRecipeFor(TharidiaThings.PULVERIZER_RECIPE_TYPE.get(), recipeWrapper, level);

        List<Entity> above_entities = level.getEntities(null, new AABB(pos.above(1)));
        above_entities.forEach(entity -> {
            if (entity instanceof ItemEntity item) {
                ItemStack item_return = pulverizer.inventory.insertItem(0, item.getItem().copy(), false);
                if (!item_return.isEmpty())
                    item.setItem(item_return);
                else
                    item.remove(RemovalReason.DISCARDED);
            }
        });

        if (!recipeHolder.isPresent()) {
            pulverizer.resetProgress();
            return;
        }

        PulverizerRecipe recipe = recipeHolder.get().value();
        if (pulverizer.isActive() && pulverizer.hasGrinder()) {
            pulverizer.maxProgress = recipe.getProcessingTime();
            pulverizer.processParticle(pulverizer.inventory.getStackInSlot(0));
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if ((pulverizer.progress++ >= pulverizer.maxProgress) && pulverizer.canInsertItem(result)) {
                pulverizer.damageGrinder();
                pulverizer.craftItem(recipe);
                pulverizer.resetProgress();
            }
        } else {
            pulverizer.resetProgress();
        }
    }

    private void processParticle(ItemStack input) {
        if (level instanceof ServerLevel serverLevel) {
            if (!input.isEmpty()) {
                ParticleOptions particleOptions;
                if (input.getItem() instanceof BlockItem blockItem) {
                    BlockState blockState = blockItem.getBlock().defaultBlockState();
                    particleOptions = new BlockParticleOption(ParticleTypes.BLOCK, blockState);
                } else {
                    particleOptions = new ItemParticleOption(ParticleTypes.ITEM, input);
                }
                serverLevel.sendParticles(
                        particleOptions,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        2,
                        0.0, 0.0, 0.0,
                        0.05);
            }
        }
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

    private void craftItem(PulverizerRecipe recipe) {
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
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putLong("ActiveTimestamp", this.active_timestamp);
        if (!grinder.isEmpty()) {
            tag.put("Grinder", this.grinder.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("ActiveTimestamp")) {
            this.active_timestamp = tag.getLong("ActiveTimestamp");
        }
        if (tag.contains("Grinder")) {
            this.grinder = ItemStack.parse(registries, tag.getCompound("Grinder")).orElse(ItemStack.EMPTY);
        } else {
            this.grinder = ItemStack.EMPTY;
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
        loadAdditional(tag, lookupProvider);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "active", 5, state -> {
            if (this.isActive()) {
                state.getController().setAnimation(ACTIVE_ANIM);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
