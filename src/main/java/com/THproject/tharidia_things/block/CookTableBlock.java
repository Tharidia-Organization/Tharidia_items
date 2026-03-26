package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.CookTableBlockEntity;
import com.THproject.tharidia_things.cook.CookRecipe;
import com.THproject.tharidia_things.cook.CookRecipeRegistry;
import com.THproject.tharidia_things.network.OpenCookRecipePacket;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * Cook Table – a 5×1×1 GeckoLib multiblock.
 * The master block is placed at the center of the 5-block span.
 * Four dummy blocks cover the remaining positions (2 left, 2 right relative to facing).
 */
public class CookTableBlock extends BaseEntityBlock {

    public static final MapCodec<CookTableBlock> CODEC = simpleCodec(CookTableBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // 5 wide (X), 1 tall (Y), 1 deep (Z)
    private static final int WIDTH = 5;
    private static final int HEIGHT = 1;
    private static final int DEPTH = 1;

    // Offset so that localX=2 maps to the master (centeredX=0)
    private static final int X_OFFSET = -2;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public CookTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    public CookTableBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F, 3.0F)
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
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
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
        return new CookTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return createTickerHelper(blockEntityType, TharidiaThings.COOK_TABLE_BLOCK_ENTITY.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        // Master block interaction is intentionally passive.
        // The recipe book opens from the dummy at offset_x=1 (second from left).
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            if (!canFormMultiblock(level, pos, state.getValue(FACING))) {
                level.destroyBlock(pos, true);
                return;
            }
            formMultiblock(level, pos, state.getValue(FACING));
        }
    }

    private boolean canFormMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    if (isMasterPosition(x, y, z)) continue;
                    BlockPos checkPos = getOffsetPos(masterPos, x, y, z, facing);
                    BlockState existing = level.getBlockState(checkPos);
                    if (!existing.isAir() && !existing.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void formMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    if (isMasterPosition(x, y, z)) continue;
                    BlockPos dummyPos = getOffsetPos(masterPos, x, y, z, facing);
                    level.setBlock(dummyPos, TharidiaThings.COOK_TABLE_DUMMY.get().defaultBlockState()
                            .setValue(CookTableDummyBlock.OFFSET_X, x)
                            .setValue(CookTableDummyBlock.FACING, facing), 3);
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

    private void destroyMultiblock(Level level, BlockPos masterPos, Direction facing) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    if (isMasterPosition(x, y, z)) continue;
                    BlockPos dummyPos = getOffsetPos(masterPos, x, y, z, facing);
                    BlockState dummyState = level.getBlockState(dummyPos);
                    if (dummyState.getBlock() instanceof CookTableDummyBlock) {
                        if (dummyState.getValue(CookTableDummyBlock.OFFSET_X) == x) {
                            level.removeBlock(dummyPos, false);
                        }
                    }
                }
            }
        }
    }

    private boolean isMasterPosition(int localX, int localY, int localZ) {
        return (localX + X_OFFSET) == 0 && localY == 0 && localZ == 0;
    }

    private BlockPos getOffsetPos(BlockPos masterPos, int localX, int localY, int localZ, Direction facing) {
        int centeredX = localX + X_OFFSET;
        int worldX, worldZ;

        switch (facing) {
            case NORTH -> { worldX = -centeredX; worldZ = -localZ; }
            case SOUTH -> { worldX =  centeredX; worldZ =  localZ; }
            case EAST  -> { worldX =  localZ;    worldZ = -centeredX; }
            case WEST  -> { worldX = -localZ;    worldZ =  centeredX; }
            default    -> { worldX =  centeredX; worldZ =  localZ; }
        }

        return masterPos.offset(worldX, localY, worldZ);
    }

    /**
     * Reverse of getOffsetPos: computes master position from a dummy's world position and stored offset.
     */
    public static BlockPos getMasterPosFromDummy(BlockPos dummyPos, int offsetX, Direction facing) {
        int centeredX = offsetX + X_OFFSET;
        int worldX, worldZ;

        switch (facing) {
            case NORTH -> { worldX =  centeredX; worldZ = 0; }
            case SOUTH -> { worldX = -centeredX; worldZ = 0; }
            case EAST  -> { worldX = 0;           worldZ =  centeredX; }
            case WEST  -> { worldX = 0;           worldZ = -centeredX; }
            default    -> { worldX = -centeredX; worldZ = 0; }
        }

        return dummyPos.offset(worldX, 0, worldZ);
    }
}
