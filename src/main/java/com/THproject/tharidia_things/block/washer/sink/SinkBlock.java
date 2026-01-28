package com.THproject.tharidia_things.block.washer.sink;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

import com.THproject.tharidia_things.TharidiaThings;

public class SinkBlock extends BaseEntityBlock {
    public static final MapCodec<SinkBlock> CODEC = simpleCodec(SinkBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SinkBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            if (!canFormMultiblock(level, pos, facing)) {
                level.destroyBlock(pos, true);
                return;
            }
            formMultiblock(level, pos, facing);
        }
    }

    private boolean canFormMultiblock(Level level, BlockPos masterPos, Direction facing) {
        Direction right = facing.getClockWise();
        for (int w = 0; w < 2; w++) {
            for (int h = 0; h < 1; h++) {
                if (w == 0 && h == 0)
                    continue; // Skip master (d is 0)

                BlockPos checkPos = masterPos.relative(right, w).above(h);
                BlockState state = level.getBlockState(checkPos);
                if (!state.canBeReplaced()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void formMultiblock(Level level, BlockPos masterPos, Direction facing) {
        Direction right = facing.getClockWise();

        BlockState dummyState = TharidiaThings.SINK_DUMMY_BLOCK.get().defaultBlockState().setValue(FACING, facing);

        for (int w = 0; w < 2; w++) {
            for (int h = 0; h < 1; h++) {
                if (w == 0 && h == 0)
                    continue; // Skip master

                BlockPos pos = masterPos.relative(right, w).above(h);
                level.setBlock(pos, dummyState, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            Direction facing = state.getValue(FACING);
            Direction right = facing.getClockWise();

            for (int w = 0; w < 2; w++) {
                for (int h = 0; h < 1; h++) {
                    if (w == 0 && h == 0)
                        continue;

                    BlockPos partPos = pos.relative(right, w).above(h);
                    if (level.getBlockState(partPos).is(TharidiaThings.SINK_DUMMY_BLOCK.get())) {
                        level.destroyBlock(partPos, false);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SinkBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
