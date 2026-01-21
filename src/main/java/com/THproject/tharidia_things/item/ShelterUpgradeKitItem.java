package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shelter upgrade kit for installing permanent weather protection on stables.
 * One-time use, permanently protects animals from rain/thunder comfort penalties.
 */
public class ShelterUpgradeKitItem extends Item {

    public ShelterUpgradeKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (level.isClientSide) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StableBlockEntity stable)) {
            return InteractionResult.PASS;
        }

        // Check if already has upgrade
        if (stable.hasShelterUpgrade()) {
            return InteractionResult.FAIL;
        }

        // Install upgrade (plays sound internally)
        if (stable.installShelterUpgrade()) {
            // Consume item
            context.getItemInHand().shrink(1);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }
}
