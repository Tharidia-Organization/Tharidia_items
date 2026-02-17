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
 * Block entity for the Alchemist Table multiblock.
 * Handles GeckoLib animations and future crafting state.
 */
public class AlchemistTableBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Looping animation for mantice (chimney flap)
    private static final RawAnimation MANTICE_ANIM = RawAnimation.begin().thenLoop("mantice");
    // One-shot triggered animations
    private static final RawAnimation BOOK_ANIM = RawAnimation.begin().thenPlay("book");
    private static final RawAnimation PESTEL_ANIM = RawAnimation.begin().thenPlay("pestel");

    // State
    private boolean manticeActive = false;

    public AlchemistTableBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.ALCHEMIST_TABLE_BLOCK_ENTITY.get(), pos, state);
    }

    // ==================== Animation Triggers ====================

    public void toggleMantice() {
        this.manticeActive = !this.manticeActive;
        syncToClient();
    }

    public void triggerBookAnimation() {
        triggerAnim("book_controller", "flip");
        syncToClient();
    }

    public void triggerPestelAnimation() {
        triggerAnim("pestel_controller", "grind");
        syncToClient();
    }

    // ==================== GeckoLib ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Mantice (looping, state-driven)
        controllers.add(new AnimationController<>(this, "mantice_controller", 5, state -> {
            if (this.manticeActive) {
                state.getController().setAnimation(MANTICE_ANIM);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));

        // Book (one-shot, triggered)
        controllers.add(new AnimationController<>(this, "book_controller", 5,
                state -> PlayState.STOP)
                .triggerableAnim("flip", BOOK_ANIM));

        // Pestel (one-shot, triggered)
        controllers.add(new AnimationController<>(this, "pestel_controller", 5,
                state -> PlayState.STOP)
                .triggerableAnim("grind", PESTEL_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("ManticeActive", manticeActive);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        manticeActive = tag.getBoolean("ManticeActive");
    }

    // ==================== Network Sync ====================

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

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
