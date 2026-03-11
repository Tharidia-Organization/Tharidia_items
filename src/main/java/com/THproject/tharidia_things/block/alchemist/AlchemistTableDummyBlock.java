package com.THproject.tharidia_things.block.alchemist;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Dummy block for the Alchemist Table multiblock structure.
 * Invisible (master's GeckoLib model renders the entire structure), provides
 * collision and interaction forwarding.
 */
public class AlchemistTableDummyBlock extends Block {

    public static final MapCodec<AlchemistTableDummyBlock> CODEC = simpleCodec(AlchemistTableDummyBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART_INDEX = IntegerProperty.create("part_index", 0, 7);

    public AlchemistTableDummyBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART_INDEX, 0));
    }

    public AlchemistTableDummyBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(3.5F, 3.5F)
                .noOcclusion()
                .noLootTable());
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART_INDEX);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // This block should not be placed directly by players
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        BlockPos masterPos = findMaster(level, pos, state);
        if (masterPos != null) {
            if (level.getBlockEntity(masterPos) instanceof AlchemistTableBlockEntity table) {
                int partIndex = state.getValue(PART_INDEX);
                if (!level.isClientSide) {
                    switch (partIndex) {
                        case 0 -> { // Add
                            table.addInteraction(player);
                        }
                        case 2 -> { // Subtract
                            table.subtractInteraction(player);
                        }
                        case 3 -> { // Divide
                            table.divideInteraction(player);
                        }
                        case 4 -> { // Multiply
                            table.multiplyInteraction(player);
                        }
                        case 7 -> {
                            table.toggleMantice();
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            findAndDestroyMaster(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Finds the master block position from this dummy's part index and facing.
     */
    @Nullable
    private BlockPos findMaster(Level level, BlockPos dummyPos, BlockState dummyState) {
        int partIndex = dummyState.getValue(PART_INDEX);
        Direction facing = dummyState.getValue(FACING);

        BlockPos masterPos = AlchemistTableBlock.getMasterPosFromDummy(dummyPos, partIndex, facing);
        BlockState masterState = level.getBlockState(masterPos);

        if (masterState.getBlock() instanceof AlchemistTableBlock) {
            return masterPos;
        }
        return null;
    }

    /**
     * Destroys the master block (which will cascade to destroy all dummies).
     */
    private void findAndDestroyMaster(Level level, BlockPos dummyPos, BlockState dummyState) {
        BlockPos masterPos = findMaster(level, dummyPos, dummyState);
        if (masterPos != null) {
            level.destroyBlock(masterPos, true);
        }
    }

    /**
     * Gets the master block position from a dummy block position.
     * Can be called from client-side code.
     */
    @Nullable
    public static BlockPos getMasterPos(net.minecraft.world.level.LevelAccessor level, BlockPos dummyPos) {
        BlockState state = level.getBlockState(dummyPos);
        if (!(state.getBlock() instanceof AlchemistTableDummyBlock)) {
            return null;
        }

        int partIndex = state.getValue(PART_INDEX);
        Direction facing = state.getValue(FACING);

        BlockPos masterPos = AlchemistTableBlock.getMasterPosFromDummy(dummyPos, partIndex, facing);
        BlockState masterState = level.getBlockState(masterPos);

        if (masterState.getBlock() instanceof AlchemistTableBlock) {
            return masterPos;
        }
        return null;
    }
}
