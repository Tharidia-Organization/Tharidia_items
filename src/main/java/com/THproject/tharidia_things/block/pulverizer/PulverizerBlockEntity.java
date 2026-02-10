package com.THproject.tharidia_things.block.pulverizer;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PulverizerBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ItemStack grinder = ItemStack.EMPTY;
    private boolean active = false;

    // Animations
    private static final RawAnimation ACTIVE_ANIM = RawAnimation.begin().thenLoop("active");

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
        return grinder.copy();
    }

    public boolean hasGrinder() {
        return !grinder.isEmpty();
    }

    public void toogleActive() {
        active = !active;
        setChanged();
    }

    public boolean isActive() {
        return active;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PulverizerBlockEntity pulverizer) {

    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Active", this.active);
        if (!grinder.isEmpty()) {
            tag.put("Grinder", this.grinder.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Grinder")) {
            this.grinder = ItemStack.parse(registries, tag.getCompound("Grinder")).orElse(ItemStack.EMPTY);
        } else {
            this.grinder = ItemStack.EMPTY;
        }
        if (tag.contains("Active")) {
            this.active = tag.getBoolean("Active");
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
