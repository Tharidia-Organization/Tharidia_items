package com.THproject.tharidia_things.block.washer.sink;

import com.THproject.tharidia_things.TharidiaThings;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.MapColor;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

public class SinkDummyBlock extends Block {
    public static final MapCodec<SinkDummyBlock> CODEC = simpleCodec(SinkDummyBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SinkDummyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    public SinkDummyBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.0F)
                .noOcclusion());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Forward to neighbor master if possible, or just ignore
        // Since we don't store offsets, we need to search for master
        // Search 2x1x2 area around this block to find the master SinkBlock
        BlockPos masterPos = findMaster(state, level, pos);
        if (masterPos != null) {
            BlockState masterState = level.getBlockState(masterPos);
            if (masterState.getBlock() instanceof SinkBlock sinkBlock) {
                BlockHitResult newHit = new BlockHitResult(
                        hitResult.getLocation(), hitResult.getDirection(), masterPos, hitResult.isInside());
                return sinkBlock.useItemOn(stack, masterState, level, masterPos, player, hand, newHit);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                BlockPos masterPos = findMaster(state, level, pos);
                if (masterPos != null) {
                    level.destroyBlock(masterPos, true);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private BlockPos findMaster(BlockState state, Level level, BlockPos dummyPos) {
        Direction facing = state.getValue(FACING);
        Direction left = facing.getClockWise();

        BlockPos partPos = dummyPos.relative(left, 1);
        if (level.getBlockState(partPos).is(TharidiaThings.SINK_BLOCK.get())) {
            return partPos;
        }
        return null;
    }
}
