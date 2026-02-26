package com.THproject.tharidia_things.block.herbalist.herbalist_tree;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HerbalistTreeBlock extends BaseEntityBlock {
    public static final MapCodec<HerbalistTreeBlock> CODEC = simpleCodec(HerbalistTreeBlock::new);

    public HerbalistTreeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HerbalistTreeBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return this.codec();
    }
    
}
