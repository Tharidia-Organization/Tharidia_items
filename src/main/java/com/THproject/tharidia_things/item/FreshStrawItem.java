package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import com.THproject.tharidia_things.stable.StableConfigLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Fresh straw item for placing bedding in stables.
 * Can only be placed on stables without existing bedding.
 * Sets bedding freshness to 100.
 */
public class FreshStrawItem extends Item {

    public FreshStrawItem(Properties properties) {
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

        // Can only place fresh straw if there's no existing bedding
        if (stable.hasBedding()) {
            return InteractionResult.FAIL;
        }

        // Place fresh bedding
        stable.setBeddingFreshness(StableConfigLoader.getConfig().beddingStartFreshness());

        // Consume item
        context.getItemInHand().shrink(1);

        // Play sound
        level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);

        return InteractionResult.SUCCESS;
    }
}
