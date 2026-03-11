package com.THproject.tharidia_things.block.alchemist;

import com.THproject.tharidia_things.TharidiaThings;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Alchemist Table - An L-shaped 8-block multiblock with GeckoLib rendering.
 *
 * Layout (facing NORTH):
 * D6
 * D5
 * D4
 * D3 D2 D1 D0 M <- arm along +X (east), model -X side (mantice/cauldron)
 * |
 * column along -Z (north), model +Z side (book)
 */
public class AlchemistTableBlock extends BaseEntityBlock {

    public static final MapCodec<AlchemistTableBlock> CODEC = simpleCodec(AlchemistTableBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;


    /**
     * Dummy block offsets from master position (local coordinates).
     * Index 0-2: arm along +localX (3 dummies + master = 4 blocks).
     * Index 3-6: column along +localZ (4 dummies + master = 5 blocks).
     */
    public static final int[][] DUMMY_OFFSETS = {
            { 1, 0, 0 },
            { 2, 0, 0 },
            { 0, 0, 1 },
            { 0, 0, 2 },
            { 0, 0, 3 },
            { 0, 0, 4 },
            { 0, 0, 5 },
            { 0, 0, 6 },
    };

    public AlchemistTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    public AlchemistTableBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(3.5F, 3.5F)
                .noOcclusion());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!canFormMultiblock(context.getLevel(), context.getClickedPos(), facing)) {
            return null; // prevent placement if multiblock can't form
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlchemistTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, TharidiaThings.ALCHEMIST_TABLE_BLOCK_ENTITY.get(),
                AlchemistTableBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        // Master block is the NW corner - no station animation here currently
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            formMultiblock(level, pos, state.getValue(FACING));
        }
    }

    /**
     * Checks if the multiblock can be formed at the given position.
     */
    private boolean canFormMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int i = 0; i < DUMMY_OFFSETS.length; i++) {
            BlockPos checkPos = getWorldPos(masterPos, DUMMY_OFFSETS[i][0], DUMMY_OFFSETS[i][2], facing);
            BlockState existingState = level.getBlockState(checkPos);
            if (!existingState.isAir() && !existingState.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Places all dummy blocks to form the multiblock.
     */
    private void formMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int i = 0; i < DUMMY_OFFSETS.length; i++) {
            BlockPos dummyPos = getWorldPos(masterPos, DUMMY_OFFSETS[i][0], DUMMY_OFFSETS[i][2], facing);
            level.setBlock(dummyPos, TharidiaThings.ALCHEMIST_TABLE_DUMMY.get().defaultBlockState()
                    .setValue(AlchemistTableDummyBlock.PART_INDEX, i)
                    .setValue(AlchemistTableDummyBlock.FACING, facing), 3);
            System.out.println("Placed dummy at " + dummyPos + " for part index " + i);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            destroyMultiblock(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Removes all dummy blocks belonging to this multiblock.
     */
    private void destroyMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int i = 0; i < DUMMY_OFFSETS.length; i++) {
            BlockPos dummyPos = getWorldPos(masterPos, DUMMY_OFFSETS[i][0], DUMMY_OFFSETS[i][2], facing);
            BlockState dummyState = level.getBlockState(dummyPos);

            if (dummyState.getBlock() instanceof AlchemistTableDummyBlock) {
                int storedIndex = dummyState.getValue(AlchemistTableDummyBlock.PART_INDEX);
                if (storedIndex == i) {
                    level.removeBlock(dummyPos, false);
                }
            }
        }
    }

    /**
     * Transforms local offset to world position based on facing direction.
     */
    public static BlockPos getWorldPos(BlockPos masterPos, int localX, int localZ, Direction facing) {
        // localX = arm direction (model -X: mantice side)
        // localZ = column direction (model +Z: book side)
        // After facing rotation, these map to world axes accordingly.
        int worldX, worldZ;
        switch (facing) {
            case NORTH -> {
                worldX = -localZ;
                worldZ = -localX;
            }
            case SOUTH -> {
                worldX = localZ;
                worldZ = localX;
            }
            case EAST -> {
                worldX = localX;
                worldZ = -localZ;
            }
            case WEST -> {
                worldX = -localX;
                worldZ = localZ;
            }
            default -> {
                worldX = -localZ;
                worldZ = -localX;
            }
        }
        return masterPos.offset(worldX, 0, worldZ);
    }

    /**
     * Calculates the master position from a dummy position and its part index.
     * Reverse transformation of getWorldPos.
     */
    public static BlockPos getMasterPosFromDummy(BlockPos dummyPos, int partIndex, Direction facing) {
        int localX = DUMMY_OFFSETS[partIndex][0];
        int localZ = DUMMY_OFFSETS[partIndex][2];

        int worldX, worldZ;
        switch (facing) {
            case NORTH -> {
                worldX = localZ;
                worldZ = localX;
            }
            case SOUTH -> {
                worldX = -localZ;
                worldZ = -localX;
            }
            case EAST -> {
                worldX = -localX;
                worldZ = localZ;
            }
            case WEST -> {
                worldX = localX;
                worldZ = -localZ;
            }
            default -> {
                worldX = localZ;
                worldZ = localX;
            }
        }
        return dummyPos.offset(worldX, 0, worldZ);
    }
}
