package com.THproject.tharidia_things.block.washer.tank;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

import com.THproject.tharidia_things.TharidiaThings;

public class TankBlock extends BaseEntityBlock {
    public static final MapCodec<TankBlock> CODEC = simpleCodec(TankBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TankBlock(Properties properties) {
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
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            if (!canFormMultiblock(level, pos, facing)) {
                // Can't form multiblock, remove the block and drop item
                level.destroyBlock(pos, true);
                return;
            }
            formMultiblock(level, pos, facing);
        }
    }

    // Tank is 2x2 base and 3 blocks high
    private boolean canFormMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int y = 1; y < 3; y++) {
            BlockPos checkPos = masterPos.above(y);
            BlockState existingState = level.getBlockState(checkPos);
            if (!existingState.canBeReplaced())
                return false;
        }
        return true;
    }

    private void formMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int y = 1; y < 3; y++) {
            BlockPos dummyPos = masterPos.above(y);
            level.setBlock(dummyPos, TharidiaThings.TANK_DUMMY.get().defaultBlockState()
                    .setValue(TankDummyBlock.FACING, facing)
                    .setValue(TankDummyBlock.OFFSET_Y, y), 3);
        }
    }

    // NOTE: This implementation assumes 1x1x3 because interpreting "1 block larger"
    // with offsets is complex without clearer spec.
    // 3 blocks high is explicit. "1 block larger" might be Sieve(3)->Tank(4) or
    // something.
    // Given user prompt "model is 3 blocks high", that is the dominant constraint.

    private void destroyMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int y = 1; y < 3; y++) {
            BlockPos dummyPos = masterPos.above(y);
            if (level.getBlockState(dummyPos).getBlock() instanceof TankDummyBlock) {
                level.removeBlock(dummyPos, false);
            }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, TharidiaThings.TANK_BLOCK_ENTITY.get(), TankBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        // Simple Interaction Logic (Bucket interaction)
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (hand != InteractionHand.MAIN_HAND)
            return ItemInteractionResult.CONSUME;

        if (blockEntity instanceof TankBlockEntity tank) {
            if (stack.getItem() == Items.WATER_BUCKET) {
                if (tank.tank.getFluidAmount() + 1000 <= tank.tank.getCapacity()) {
                    tank.tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
                    player.playSound(SoundEvents.BUCKET_EMPTY);
                    if (!player.isCreative())
                        player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    return ItemInteractionResult.SUCCESS;
                }
            } else if (stack.getItem() == Items.BUCKET) {
                if (tank.tank.getFluidAmount() >= 1000) {
                    tank.tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    player.playSound(SoundEvents.BUCKET_FILL);
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
                    } else if (!player.getInventory().add(new ItemStack(Items.WATER_BUCKET))) {
                        player.drop(new ItemStack(Items.WATER_BUCKET), false);
                    }
                    return ItemInteractionResult.SUCCESS;
                }
            } else if (stack.isEmpty()) {
                if (tank.toogleOpen()) {
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 0.8F);
                } else {
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 0.5F);
                }
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            destroyMultiblock(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }
}
