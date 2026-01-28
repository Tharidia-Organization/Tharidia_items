package com.THproject.tharidia_things.block.entity;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Block entity for the Smithing Furnace - a GeckoLib animated multiblock.
 * Features:
 * - Permanent "levitate2" animation always active
 * - Tier system (0-4) for future upgrades with bone visibility
 */
public class SmithingFurnaceBlockEntity extends BlockEntity implements GeoBlockEntity {

    // GeckoLib animation cache
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Animation that loops permanently
    private static final RawAnimation LEVITATE_ANIM = RawAnimation.begin().thenLoop("levitate2");

    // Tier system (0-4) for future bone visibility changes
    private int tier = 0;

    public SmithingFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.SMITHING_FURNACE_BLOCK_ENTITY.get(), pos, state);
    }

    // ==================== Tier System ====================

    /**
     * Gets the current tier of the smithing furnace (0-4)
     */
    public int getTier() {
        return tier;
    }

    /**
     * Sets the tier of the smithing furnace (0-4)
     */
    public void setTier(int tier) {
        this.tier = Math.max(0, Math.min(4, tier));
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Upgrades the tier by 1, up to maximum of 4
     * @return true if upgrade was successful, false if already at max tier
     */
    public boolean upgradeTier() {
        if (tier >= 4) {
            return false;
        }
        setTier(tier + 1);
        return true;
    }

    // ==================== NBT Persistence ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Tier", tier);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = tag.getInt("Tier");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ==================== GeckoLib Implementation ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Register the idle/levitate animation controller
        // This animation runs permanently and loops forever
        controllers.add(new AnimationController<>(this, "idle", 0, state -> {
            // Only set animation if not already playing to prevent restarts
            if (state.getController().getAnimationState() == AnimationController.State.STOPPED) {
                state.getController().setAnimation(LEVITATE_ANIM);
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
