package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.block.entity.DyeVatsBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Dummy block for the 2-wide dye vats multiblock.
 * The FACING property points towards the master block.
 */
public class DyeVatsDummyBlock extends Block {

    public static final MapCodec<DyeVatsDummyBlock> CODEC = simpleCodec(DyeVatsDummyBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Vat collision components
    private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 4, 16);
    private static final VoxelShape WALL_N = Block.box(0, 4, 0, 16, 16, 2);
    private static final VoxelShape WALL_S = Block.box(0, 4, 14, 16, 16, 16);
    private static final VoxelShape WALL_E = Block.box(14, 4, 0, 16, 16, 16);
    private static final VoxelShape WALL_W = Block.box(0, 4, 0, 2, 16, 16);

    // Per-facing shapes: 3 walls, open towards master (FACING direction)
    private static final VoxelShape SHAPE_FACE_N = Shapes.or(BOTTOM, WALL_S, WALL_E, WALL_W); // open NORTH
    private static final VoxelShape SHAPE_FACE_S = Shapes.or(BOTTOM, WALL_N, WALL_E, WALL_W); // open SOUTH
    private static final VoxelShape SHAPE_FACE_E = Shapes.or(BOTTOM, WALL_N, WALL_S, WALL_W); // open EAST
    private static final VoxelShape SHAPE_FACE_W = Shapes.or(BOTTOM, WALL_N, WALL_S, WALL_E); // open WEST

    public DyeVatsDummyBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    public DyeVatsDummyBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .noOcclusion()
                .noLootTable());
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Items get only the bottom plate so they fall inside freely
        if (context instanceof EntityCollisionContext ctx && ctx.getEntity() instanceof ItemEntity) {
            return BOTTOM;
        }
        // Players/mobs get directional walls (open towards master)
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_FACE_N;
            case SOUTH -> SHAPE_FACE_S;
            case EAST  -> SHAPE_FACE_E;
            case WEST  -> SHAPE_FACE_W;
            default    -> SHAPE_FACE_N;
        };
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (!(entity instanceof ItemEntity itemEntity)) return;

        BlockPos masterPos = getMasterPos(level, pos, state);
        if (masterPos != null) {
            BlockState masterState = level.getBlockState(masterPos);
            if (masterState.getBlock() instanceof DyeVatsBlock && masterState.getValue(DyeVatsBlock.FILLED)) {
                if (level.getBlockEntity(masterPos) instanceof DyeVatsBlockEntity dyeVats) {
                    dyeVats.processItemEntity(itemEntity);
                }
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos masterPos = getMasterPos(level, pos, state);
        if (masterPos != null) {
            BlockState masterState = level.getBlockState(masterPos);
            if (masterState.getBlock() instanceof DyeVatsBlock) {
                BlockHitResult newHit = new BlockHitResult(
                        hitResult.getLocation(), hitResult.getDirection(), masterPos, hitResult.isInside());
                return masterState.useItemOn(stack, level, player, hand, newHit);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos masterPos = getMasterPos(level, pos, state);
        if (masterPos != null) {
            BlockState masterState = level.getBlockState(masterPos);
            if (masterState.getBlock() instanceof DyeVatsBlock) {
                BlockHitResult newHit = new BlockHitResult(
                        hitResult.getLocation(), hitResult.getDirection(), masterPos, hitResult.isInside());
                return masterState.useWithoutItem(level, player, newHit);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            // Find and destroy master when dummy is broken
            BlockPos masterPos = getMasterPos(level, pos, state);
            if (masterPos != null) {
                level.destroyBlock(masterPos, false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Gets the master block position from a dummy block.
     * The dummy's FACING points towards the master.
     */
    @Nullable
    public static BlockPos getMasterPos(LevelAccessor level, BlockPos dummyPos, BlockState dummyState) {
        Direction toMaster = dummyState.getValue(FACING);
        BlockPos masterPos = dummyPos.relative(toMaster);
        BlockState masterState = level.getBlockState(masterPos);
        if (masterState.getBlock() instanceof DyeVatsBlock) {
            return masterPos;
        }
        return null;
    }

    @Nullable
    public static BlockPos getMasterPos(LevelAccessor level, BlockPos dummyPos) {
        BlockState state = level.getBlockState(dummyPos);
        if (!(state.getBlock() instanceof DyeVatsDummyBlock)) {
            return null;
        }
        return getMasterPos(level, dummyPos, state);
    }
}
