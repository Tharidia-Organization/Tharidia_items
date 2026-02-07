package com.THproject.tharidia_things.block.crystals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class Crystal3BlockEntity extends BaseCrystalBlockEntity {

    public Crystal3BlockEntity(BlockPos pos, BlockState state) {
        super(CrystalsRegistry.CRYSTAL_3_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public ItemStack getDrop() {
        return CrystalsRegistry.PURE_CRYSTAL_3.get().getDefaultInstance();
    }
}
