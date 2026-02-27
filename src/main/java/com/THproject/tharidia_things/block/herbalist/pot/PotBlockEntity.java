package com.THproject.tharidia_things.block.herbalist.pot;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PotBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean hasDirt = false;
    private boolean isFarmed = false;

    public PotBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.POT_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean setDirt() {
        if (!this.hasDirt()) {
            this.hasDirt = true;
            setChanged();
            return true;
        }
        return false;
    }

    public boolean removeDirt() {
        if (this.hasDirt()) {
            this.removeFarmed();
            this.hasDirt = false;
            setChanged();
            return true;
        }
        return false;
    }

    public boolean hasDirt() {
        return this.hasDirt;
    }

    public boolean setFarmed() {
        if (this.hasDirt() && !this.isFarmed()) {
            this.isFarmed = true;
            setChanged();
            return true;
        }
        return false;
    }

    public boolean removeFarmed() {
        if (this.hasDirt) {
            this.isFarmed = false;
            setChanged();
            return true;
        }
        return false;
    }

    public boolean isFarmed() {
        return this.isFarmed;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("hasDirt", this.hasDirt);
        tag.putBoolean("isFarmed", this.isFarmed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.hasDirt = tag.getBoolean("hasDirt");
        this.isFarmed = tag.getBoolean("isFarmed");
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        return;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
