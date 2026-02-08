package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.DyeVatsBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DyeVatsBlock extends BaseEntityBlock {

    public static final MapCodec<DyeVatsBlock> CODEC = simpleCodec(DyeVatsBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");

    // Vat collision components
    private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 4, 16);
    private static final VoxelShape WALL_N = Block.box(0, 4, 0, 16, 16, 2);
    private static final VoxelShape WALL_S = Block.box(0, 4, 14, 16, 16, 16);
    private static final VoxelShape WALL_E = Block.box(14, 4, 0, 16, 16, 16);
    private static final VoxelShape WALL_W = Block.box(0, 4, 0, 2, 16, 16);

    // Per-facing shapes: 3 walls, open towards dummy side
    private static final VoxelShape SHAPE_NORTH = Shapes.or(BOTTOM, WALL_N, WALL_S, WALL_E); // open WEST
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(BOTTOM, WALL_N, WALL_S, WALL_W); // open EAST
    private static final VoxelShape SHAPE_EAST  = Shapes.or(BOTTOM, WALL_N, WALL_E, WALL_W); // open SOUTH
    private static final VoxelShape SHAPE_WEST  = Shapes.or(BOTTOM, WALL_S, WALL_E, WALL_W); // open NORTH

    public DyeVatsBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FILLED, false));
    }

    public DyeVatsBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .noOcclusion()
                .noLootTable());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos dummyPos = context.getClickedPos().relative(getDummyDirection(facing));
        // Check if the dummy position is free
        if (!context.getLevel().getBlockState(dummyPos).canBeReplaced(context)) {
            return null; // Prevent placement
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            Direction dummyDir = getDummyDirection(facing);
            BlockPos dummyPos = pos.relative(dummyDir);

            BlockState dummyState = level.getBlockState(dummyPos);
            if (dummyState.isAir() || dummyState.canBeReplaced()) {
                // The dummy's FACING points towards the master
                level.setBlock(dummyPos, TharidiaThings.DYE_VATS_DUMMY.get().defaultBlockState()
                        .setValue(DyeVatsDummyBlock.FACING, dummyDir.getOpposite()), 3);
            } else {
                // Can't place dummy, remove master
                level.destroyBlock(pos, true);
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Fill with water bucket
        if (stack.is(Items.WATER_BUCKET) && !state.getValue(FILLED)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(FILLED, true), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Empty with empty bucket
        if (stack.is(Items.BUCKET) && state.getValue(FILLED)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(FILLED, false), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            // Drop item
            Block.popResource(level, pos, new ItemStack(TharidiaThings.DYE_VATS.get()));

            // Remove dummy
            Direction dummyDir = getDummyDirection(state.getValue(FACING));
            BlockPos dummyPos = pos.relative(dummyDir);
            BlockState dummyState = level.getBlockState(dummyPos);
            if (dummyState.getBlock() instanceof DyeVatsDummyBlock) {
                level.removeBlock(dummyPos, false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DyeVatsBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Items get only the bottom plate so they fall inside freely
        if (context instanceof EntityCollisionContext ctx && ctx.getEntity() instanceof ItemEntity) {
            return BOTTOM;
        }
        // Players/mobs get directional walls (open towards dummy)
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !state.getValue(FILLED)) return;
        if (!(entity instanceof ItemEntity itemEntity)) return;

        if (level.getBlockEntity(pos) instanceof DyeVatsBlockEntity dyeVats) {
            dyeVats.processItemEntity(itemEntity);
        }
    }

    /**
     * Returns the direction where the dummy block should be placed,
     * based on the master block's facing.
     * The model extends to -X in Blockbench (default north facing),
     * so the dummy goes to the left of the player.
     */
    public static Direction getDummyDirection(Direction masterFacing) {
        // Model extends to -X in Blockbench. After blockstate Y rotation:
        // facing=north (y=0):   -X stays -X = West
        // facing=south (y=180): -X -> +X  = East
        // facing=east  (y=270): -X -> +Z  = South
        // facing=west  (y=90):  -X -> -Z  = North
        return switch (masterFacing) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            default -> Direction.WEST;
        };
    }
}
