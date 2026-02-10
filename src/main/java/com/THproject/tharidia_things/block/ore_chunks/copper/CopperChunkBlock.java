package com.THproject.tharidia_things.block.ore_chunks.copper;

import com.THproject.tharidia_things.block.ore_chunks.BaseChunkBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CopperChunkBlock extends BaseChunkBlock {
    public static final MapCodec<CopperChunkBlock> CODEC = simpleCodec(CopperChunkBlock::new);

    public CopperChunkBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperChunkBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
