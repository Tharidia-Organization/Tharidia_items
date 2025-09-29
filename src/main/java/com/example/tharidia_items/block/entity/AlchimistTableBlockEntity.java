package com.example.tharidia_items.block.entity;

import com.example.tharidia_items.TharidiaItemsMod;
import mod.azure.azurelib.animatable.GeoBlockEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.util.AzureLibUtil;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AlchimistTableBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);
    public final AlchimistTableDispatcher dispatcher;

    public AlchimistTableBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaItemsMod.ALCHIMIST_TABLE_BE, pos, state);
        this.dispatcher = new AlchimistTableDispatcher();
    }

    public static void tick(World level, BlockPos pos, BlockState state, AlchimistTableBlockEntity blockEntity) {
        if (level.isClient()) {
            blockEntity.dispatcher.playSlideshow(blockEntity);
        }
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 0, state -> PlayState.CONTINUE)
            .triggerableAnim("slideshow", RawAnimation.begin().thenLoop("slideshow"))
        );
    }
}