package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.block.entity.PulverizerBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PulverizerBlock extends BaseEntityBlock {
    public static final MapCodec<PulverizerBlock> CODEC = simpleCodec(PulverizerBlock::new);

    public PulverizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PulverizerBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

}
