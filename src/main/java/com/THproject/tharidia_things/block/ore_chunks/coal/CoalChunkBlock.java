package com.THproject.tharidia_things.block.ore_chunks.coal;


import javax.annotation.Nullable;

import com.THproject.tharidia_things.block.ore_chunks.BaseChunkBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CoalChunkBlock extends BaseChunkBlock {
    public static final MapCodec<CoalChunkBlock> CODEC = simpleCodec(CoalChunkBlock::new);

    public CoalChunkBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoalChunkBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
