package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.block.entity.CookTableBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Invisible dummy block for the Cook Table multiblock.
 * Provides collision and forwards destruction to the master block.
 */
public class CookTableDummyBlock extends Block {

    public static final MapCodec<CookTableDummyBlock> CODEC = simpleCodec(CookTableDummyBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // localX index (0-4); master is at localX=2
    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 4);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public CookTableDummyBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OFFSET_X, 0));
    }

    public CookTableDummyBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F, 3.0F)
                .noOcclusion()
                .noLootTable());
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OFFSET_X);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(OFFSET_X) == 3 && !level.isClientSide) {
            BlockPos masterPos = findMaster(level, pos, state);
            if (masterPos != null && level.getBlockEntity(masterPos) instanceof CookTableBlockEntity be) {
                be.playRegistryAnimation();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos masterPos = findMaster(level, pos, state);
            if (masterPos != null) {
                level.destroyBlock(masterPos, true);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    private BlockPos findMaster(Level level, BlockPos dummyPos, BlockState dummyState) {
        int offsetX = dummyState.getValue(OFFSET_X);
        Direction facing = dummyState.getValue(FACING);
        BlockPos masterPos = CookTableBlock.getMasterPosFromDummy(dummyPos, offsetX, facing);
        BlockState masterState = level.getBlockState(masterPos);
        return masterState.getBlock() instanceof CookTableBlock ? masterPos : null;
    }

    @Nullable
    public static BlockPos getMasterPos(net.minecraft.world.level.LevelAccessor level, BlockPos dummyPos) {
        BlockState state = level.getBlockState(dummyPos);
        if (!(state.getBlock() instanceof CookTableDummyBlock)) return null;
        int offsetX = state.getValue(OFFSET_X);
        Direction facing = state.getValue(FACING);
        BlockPos masterPos = CookTableBlock.getMasterPosFromDummy(dummyPos, offsetX, facing);
        BlockState masterState = level.getBlockState(masterPos);
        return masterState.getBlock() instanceof CookTableBlock ? masterPos : null;
    }
}
