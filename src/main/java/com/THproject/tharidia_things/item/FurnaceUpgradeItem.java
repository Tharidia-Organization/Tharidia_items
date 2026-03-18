package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.SmithingFurnaceBlock;
import com.THproject.tharidia_things.block.SmithingFurnaceDummyBlock;
import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Base class for furnace upgrade items (Mantice, Crucible, Hoover, Chimney).
 * These items can be used on the smithing furnace to install upgrades.
 */
public abstract class FurnaceUpgradeItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public FurnaceUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // Find the master block entity
        SmithingFurnaceBlockEntity furnace = null;

        if (state.getBlock() instanceof SmithingFurnaceBlock) {
            furnace = (SmithingFurnaceBlockEntity) level.getBlockEntity(pos);
        } else if (state.getBlock() instanceof SmithingFurnaceDummyBlock) {
            BlockPos masterPos = SmithingFurnaceDummyBlock.getMasterPos(level, pos);
            if (masterPos != null) {
                furnace = (SmithingFurnaceBlockEntity) level.getBlockEntity(masterPos);
            }
        }

        if (furnace != null && !level.isClientSide) {
            if (tryInstall(furnace)) {
                // Consume the item
                context.getItemInHand().shrink(1);
                return InteractionResult.SUCCESS;
            }
        }

        return furnace != null ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    /**
     * Attempts to install this upgrade on the furnace.
     * @return true if installation was successful, false if already installed
     */
    protected abstract boolean tryInstall(SmithingFurnaceBlockEntity furnace);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Static model - no animations needed
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
