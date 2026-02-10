package com.THproject.tharidia_things.block.crystals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class Crystal1BlockEntity extends BaseCrystalBlockEntity {

    public Crystal1BlockEntity(BlockPos pos, BlockState state) {
        super(CrystalsRegistry.CRYSTAL_1_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public ItemStack getDrop() {
        return CrystalsRegistry.PURE_CRYSTAL_1.get().getDefaultInstance();
    }
}
