package com.THproject.tharidia_things.block.ore_chunks.tin;

import com.THproject.tharidia_things.block.ore_chunks.BaseChunkBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TinChunkBlock extends BaseChunkBlock {
    public static final MapCodec<TinChunkBlock> CODEC = simpleCodec(TinChunkBlock::new);

    public TinChunkBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TinChunkBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
