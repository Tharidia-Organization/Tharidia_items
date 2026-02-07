package com.THproject.tharidia_things.block.crystals;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class Crystal5Block extends BaseCrystalBlock {
    public static final MapCodec<Crystal5Block> CODEC = simpleCodec(Crystal5Block::new);

    public Crystal5Block(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Crystal5BlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
