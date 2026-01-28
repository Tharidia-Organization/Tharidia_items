package com.THproject.tharidia_things.block.washer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.Containers;
import javax.annotation.Nullable;

import com.THproject.tharidia_things.TharidiaThings;

public class WasherBlock extends BaseEntityBlock {
    public static final MapCodec<WasherBlock> CODEC = simpleCodec(WasherBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public WasherBlock(Properties properties) {
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
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

    private boolean canFormMultiblock(Level level, BlockPos masterPos, Direction facing) {
        // Determine check area based on facing
        int xMin, xMax, zMin, zMax;

        if (facing.getAxis() == Direction.Axis.Z) {
            // North/South: Facing Parallel to Z axis (Deep structure)
            // Width X is minimal (1 block), Depth Z is 3 blocks.
            // Check 3x1 area: Z from -1 to 1. X is 0.
            // Buffer: +1 all around.
            xMin = -1;
            xMax = 1; // Width Buffer
            zMin = -2;
            zMax = 2; // Length (-1 to 1) + Buffer
        } else {
            // East/West: Facing Parallel to X axis (Long structure)
            // Width Z is minimal (1 block), Depth X is 3 blocks.
            // Check 3x1 area: X from -1 to 1. Z is 0.
            // Buffer: +1 all around.
            xMin = -2;
            xMax = 2; // Length buffer
            zMin = -1;
            zMax = 1; // Width Buffer
        }

        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                if (x == 0 && z == 0)
                    continue;

                BlockPos checkPos = masterPos.offset(x, 0, z);
                BlockState existingState = level.getBlockState(checkPos);

                // If there's already a washer master or dummy here, can't form
                if (existingState.getBlock() instanceof WasherBlock ||
                        existingState.getBlock() instanceof WasherDummyBlock) {
                    return false;
                }
            }
        }
        return true;
    }

    private void formMultiblock(Level level, BlockPos masterPos, Direction facing) {
        int xMin, xMax, zMin, zMax;

        if (facing.getAxis() == Direction.Axis.Z) {
            // North/South: Length along Z axis (Parallel)
            xMin = 0;
            xMax = 0;
            zMin = -1;
            zMax = 1;
        } else {
            // East/West: Length along X axis (Parallel)
            xMin = -1;
            xMax = 1;
            zMin = 0;
            zMax = 0;
        }

        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                // Skip center block (master)
                if (x == 0 && z == 0)
                    continue;

                BlockPos dummyPos = masterPos.offset(x, 0, z);
                BlockState existingState = level.getBlockState(dummyPos);

                // Only place dummy if space is empty or replaceable
                if (existingState.isAir() || existingState.canBeReplaced()) {
                    // Offset to master is (-x, -z) logically
                    // But WasherDummyBlock uses positive 0-4 indices, where 2 is center
                    level.setBlock(dummyPos, TharidiaThings.WASHER_DUMMY.get().defaultBlockState()
                            .setValue(WasherDummyBlock.FACING, facing)
                            .setValue(WasherDummyBlock.OFFSET_X, -x + 2)
                            .setValue(WasherDummyBlock.OFFSET_Z, -z + 2), 3);
                }
            }
        }
    }

    private void destroyMultiblock(Level level, BlockPos masterPos, Direction facing) {
        int xMin, xMax, zMin, zMax;

        if (facing.getAxis() == Direction.Axis.Z) {
            // North/South: Length along Z axis
            xMin = 0;
            xMax = 0;
            zMin = -1;
            zMax = 1;
        } else {
            // East/West: Length along X axis
            xMin = -1;
            xMax = 1;
            zMin = 0;
            zMax = 0;
        }

        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                if (x == 0 && z == 0)
                    continue;

                BlockPos dummyPos = masterPos.offset(x, 0, z);
                BlockState dummyState = level.getBlockState(dummyPos);
                if (dummyState.getBlock() instanceof WasherDummyBlock) {
                    // Verify this dummy belongs to this master by checking offsets
                    int expectedOffsetX = -x + 2;
                    int expectedOffsetZ = -z + 2;
                    if (dummyState.getValue(WasherDummyBlock.OFFSET_X) == expectedOffsetX &&
                            dummyState.getValue(WasherDummyBlock.OFFSET_Z) == expectedOffsetZ) {
                        level.removeBlock(dummyPos, false);
                    }
                }
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
        return createTickerHelper(type, TharidiaThings.WASHER_BLOCK_ENTITY.get(), WasherBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (hand != InteractionHand.MAIN_HAND)
            return ItemInteractionResult.CONSUME;

        if (blockEntity instanceof WasherBlockEntity washer) {
            if (stack.getItem() == Items.WATER_BUCKET) {
                if (!washer.isTankFull()) {
                    washer.addWaterBucket();
                    player.playSound(SoundEvents.BUCKET_EMPTY);
                    if (!player.isCreative())
                        player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    return ItemInteractionResult.SUCCESS;
                } else {
                    return ItemInteractionResult.CONSUME;
                }
            } else if (stack.getItem() == Items.BUCKET) {
                if (washer.tank.getFluidAmount() >= 1000) {
                    washer.tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    player.playSound(SoundEvents.BUCKET_FILL);
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
                    } else if (!player.getInventory().add(new ItemStack(Items.WATER_BUCKET))) {
                        player.drop(new ItemStack(Items.WATER_BUCKET), false);
                    }
                    return ItemInteractionResult.SUCCESS;
                }
            } else if (stack.getItem() == TharidiaThings.MESH.get()) {
                if (!washer.hasMesh()) {
                    washer.setMesh();
                    stack.shrink(1);
                    return ItemInteractionResult.SUCCESS;
                }
                return ItemInteractionResult.CONSUME;
            } else {
                if (stack.isEmpty() && !player.isShiftKeyDown()) {
                    boolean success = false;
                    for (int i = 1; i < washer.inventory.getSlots(); i++) {
                        ItemStack extracted = washer.inventory.extractItem(i, 64, false);
                        if (!extracted.isEmpty()) {
                            if (!player.getInventory().add(extracted.copy())) {
                                player.drop(extracted.copy(), false);
                            }
                            success = true;
                        }
                    }
                    return success ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
                } else if (stack.isEmpty() && player.isShiftKeyDown()) {
                    ItemStack extracted = washer.inventory.extractItem(0, 64, false);
                    if (!extracted.isEmpty()) {
                        if (!player.getInventory().add(extracted.copy()))
                            player.drop(extracted.copy(), false);
                        return ItemInteractionResult.SUCCESS;
                    } else {
                        if (washer.hasMesh()) {
                            washer.removeMesh();
                            if (!player.getInventory().add(new ItemStack(TharidiaThings.MESH.get())))
                                player.drop(new ItemStack(TharidiaThings.MESH.get()), false);
                            return ItemInteractionResult.SUCCESS;
                        }
                        return ItemInteractionResult.CONSUME;
                    }
                } else {
                    ItemStack remainder = washer.inventory.insertItem(0, stack.copy(), false);
                    if (remainder.getCount() != stack.getCount()) {
                        player.setItemInHand(hand, remainder);
                        return ItemInteractionResult.SUCCESS;
                    } else {
                        return ItemInteractionResult.CONSUME;
                    }
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            // Drop the washer item manually if it's the master block being removed
            // Note: WasherBlock has dropResource handled by standard loot tables if not for
            // this special logic
            // But StableBlock pops it manually.
            // If this block is broken, we should destroy multiblock.

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof WasherBlockEntity washer) {
                for (int i = 0; i < washer.inventory.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                            washer.inventory.getStackInSlot(i));
                }
                if (washer.hasMesh()) {
                    BaseEntityBlock.popResource(level, pos, new ItemStack(TharidiaThings.MESH.get()));
                }
            }

            destroyMultiblock(level, pos, state.getValue(FACING));
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WasherBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

}
