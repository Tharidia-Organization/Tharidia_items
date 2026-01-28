package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Smithing Furnace - A 5x2x3 multiblock GeckoLib animated block.
 *
 * Dimensions: 5 blocks wide (X) x 2 blocks tall (Y) x 3 blocks deep (Z)
 * The master block is at position (0,0,0) relative to the multiblock.
 *
 * Features:
 * - GeckoLib rendering with permanent "levitate2" animation
 * - Tier property (0-4) for future bone visibility upgrades
 * - Multiblock structure with dummy blocks for collision
 */
public class SmithingFurnaceBlock extends BaseEntityBlock {

    public static final MapCodec<SmithingFurnaceBlock> CODEC = simpleCodec(SmithingFurnaceBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 4);

    // Multiblock dimensions: 5 wide (X), 2 tall (Y), 3 deep (Z)
    // Master is at corner (0,0,0), extends to (4,1,2)
    private static final int WIDTH = 5;  // X dimension
    private static final int HEIGHT = 2; // Y dimension
    private static final int DEPTH = 3;  // Z dimension

    // Full block collision shape for the master block position
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public SmithingFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TIER, 0));
    }

    public SmithingFurnaceBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .noOcclusion()
                .lightLevel(state -> 7));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // GeckoLib handles rendering
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmithingFurnaceBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            if (!canFormMultiblock(level, pos, state.getValue(FACING))) {
                // Can't form multiblock, remove the block and drop item
                level.destroyBlock(pos, true);
                return;
            }
            formMultiblock(level, pos, state.getValue(FACING));
        }
    }

    /**
     * Checks if the multiblock can be formed at the given position
     */
    private boolean canFormMultiblock(Level level, BlockPos masterPos, Direction facing) {
        // Check all positions in the 5x2x3 area
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Skip master position

                    BlockPos checkPos = getOffsetPos(masterPos, x, y, z, facing);
                    BlockState existingState = level.getBlockState(checkPos);

                    // Check if space is clear or replaceable
                    if (!existingState.isAir() && !existingState.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Forms the multiblock by placing dummy blocks
     */
    private void formMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Skip master position

                    BlockPos dummyPos = getOffsetPos(masterPos, x, y, z, facing);

                    level.setBlock(dummyPos, TharidiaThings.SMITHING_FURNACE_DUMMY.get().defaultBlockState()
                            .setValue(SmithingFurnaceDummyBlock.OFFSET_X, x)
                            .setValue(SmithingFurnaceDummyBlock.OFFSET_Y, y)
                            .setValue(SmithingFurnaceDummyBlock.OFFSET_Z, z)
                            .setValue(SmithingFurnaceDummyBlock.FACING, facing), 3);
                }
            }
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
     * Destroys all dummy blocks belonging to this multiblock
     */
    private void destroyMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Skip master position

                    BlockPos dummyPos = getOffsetPos(masterPos, x, y, z, facing);
                    BlockState dummyState = level.getBlockState(dummyPos);

                    if (dummyState.getBlock() instanceof SmithingFurnaceDummyBlock) {
                        // Verify this dummy belongs to this master
                        int storedX = dummyState.getValue(SmithingFurnaceDummyBlock.OFFSET_X);
                        int storedY = dummyState.getValue(SmithingFurnaceDummyBlock.OFFSET_Y);
                        int storedZ = dummyState.getValue(SmithingFurnaceDummyBlock.OFFSET_Z);

                        if (storedX == x && storedY == y && storedZ == z) {
                            level.removeBlock(dummyPos, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculates world position from local offset based on facing direction
     */
    private BlockPos getOffsetPos(BlockPos masterPos, int localX, int localY, int localZ, Direction facing) {
        // Transform local coordinates based on facing direction
        // North: no rotation
        // South: rotate 180
        // East: rotate 90 CW
        // West: rotate 90 CCW
        int worldX, worldZ;

        switch (facing) {
            case SOUTH -> {
                worldX = -localX;
                worldZ = -localZ;
            }
            case EAST -> {
                worldX = -localZ;
                worldZ = localX;
            }
            case WEST -> {
                worldX = localZ;
                worldZ = -localX;
            }
            default -> { // NORTH
                worldX = localX;
                worldZ = localZ;
            }
        }

        return masterPos.offset(worldX, localY, worldZ);
    }

    /**
     * Calculates the master position from a dummy position and its offsets
     */
    public static BlockPos getMasterPosFromDummy(BlockPos dummyPos, int offsetX, int offsetY, int offsetZ, Direction facing) {
        int worldX, worldZ;

        switch (facing) {
            case SOUTH -> {
                worldX = offsetX;
                worldZ = offsetZ;
            }
            case EAST -> {
                worldX = offsetZ;
                worldZ = -offsetX;
            }
            case WEST -> {
                worldX = -offsetZ;
                worldZ = offsetX;
            }
            default -> { // NORTH
                worldX = -offsetX;
                worldZ = -offsetZ;
            }
        }

        return dummyPos.offset(worldX, -offsetY, worldZ);
    }
}
