package com.THproject.tharidia_things.block.ore_chunks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public class IronChunkBlock extends BaseEntityBlock {
    public static final MapCodec<IronChunkBlock> CODEC = simpleCodec(IronChunkBlock::new);

    public IronChunkBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(Chunks.STAGE, 0));
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        Chunks.attackChunk(level, pos, player);
        super.attack(state, level, pos, player);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(Chunks.STAGE)) {
            case 0 -> Chunks.SHAPE_STAGE0;
            case 1 -> Chunks.SHAPE_STAGE1;
            case 2 -> Chunks.SHAPE_STAGE2;
            case 3 -> Chunks.SHAPE_STAGE3;
            case 4 -> Chunks.SHAPE_STAGE4;
            default -> Chunks.SHAPE_STAGE0;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronChunkBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Chunks.STAGE);
    }
}
