package com.THproject.tharidia_things.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.SmithingFurnaceBlockEntity;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Dummy block for the Smithing Furnace multiblock structure.
 * Invisible (rendered by master's GeckoLib model), provides collision and interaction forwarding.
 */
public class SmithingFurnaceDummyBlock extends Block {

    public static final MapCodec<SmithingFurnaceDummyBlock> CODEC = simpleCodec(SmithingFurnaceDummyBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Offset from master block position (0-4 for X, 0-1 for Y, 0-1 for Z)
    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 4);
    public static final IntegerProperty OFFSET_Y = IntegerProperty.create("offset_y", 0, 1);
    public static final IntegerProperty OFFSET_Z = IntegerProperty.create("offset_z", 0, 1);

    // Full block collision shape
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public SmithingFurnaceDummyBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OFFSET_X, 0)
                .setValue(OFFSET_Y, 0)
                .setValue(OFFSET_Z, 0));
    }

    public SmithingFurnaceDummyBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .noOcclusion()
                .noLootTable());
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // This block should not be placed directly by players
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Invisible - master's GeckoLib model renders the entire structure
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Forward interaction to master block
        BlockPos masterPos = findMaster(level, pos, state);
        if (masterPos != null) {
            BlockState masterState = level.getBlockState(masterPos);
            if (masterState.getBlock() instanceof SmithingFurnaceBlock) {
                BlockHitResult newHit = new BlockHitResult(
                        hitResult.getLocation(), hitResult.getDirection(), masterPos, hitResult.isInside());
                // Use BlockState's public method which delegates to the block
                return masterState.useItemOn(stack, level, player, hand, newHit);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Find the master block
        BlockPos masterPos = findMaster(level, pos, state);
        if (masterPos != null) {
            if (level.getBlockEntity(masterPos) instanceof SmithingFurnaceBlockEntity furnace) {
                int offsetX = state.getValue(OFFSET_X);
                int offsetY = state.getValue(OFFSET_Y);
                int offsetZ = state.getValue(OFFSET_Z);

                if (!level.isClientSide) {
                    // Expired metal cleanup (priority over animation toggles)
                    if (offsetX == 2 && furnace.isBigCrucibleExpired()) {
                        if (furnace.cleanExpiredBigCrucible()) {
                            level.addFreshEntity(new ItemEntity(level,
                                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                                    new ItemStack(TharidiaThings.METAL_FRAGMENT.get())));
                            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.7f, 0.8f);
                        }
                    } else if (offsetX >= 3 && furnace.isCastExpired()) {
                        String castType = furnace.getCastMetalType();
                        if (furnace.cleanExpiredCast()) {
                            ItemStack ingotStack = switch (castType) {
                                case "iron" -> new ItemStack(Items.IRON_INGOT);
                                case "gold" -> new ItemStack(Items.GOLD_INGOT);
                                case "copper" -> new ItemStack(Items.COPPER_INGOT);
                                default -> new ItemStack(TharidiaThings.METAL_FRAGMENT.get());
                            };
                            level.addFreshEntity(new ItemEntity(level,
                                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ingotStack));
                            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.7f, 0.8f);
                        }
                    } else {
                        // Normal animation toggles
                        if (offsetX == 2 && offsetY == 0 && offsetZ == 1) {
                            if (furnace.hasDoor()) furnace.toggleDoor();
                        } else if (offsetX <= 1) {
                            furnace.toggleHoover();
                        } else if (offsetX >= 3) {
                            furnace.toggleCogiuolo();
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
            // Find and destroy master block when dummy is broken
            findAndDestroyMaster(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Finds the master block position from this dummy's offset
     */
    @Nullable
    private BlockPos findMaster(Level level, BlockPos dummyPos, BlockState dummyState) {
        int offsetX = dummyState.getValue(OFFSET_X);
        int offsetY = dummyState.getValue(OFFSET_Y);
        int offsetZ = dummyState.getValue(OFFSET_Z);
        Direction facing = dummyState.getValue(FACING);

        BlockPos masterPos = SmithingFurnaceBlock.getMasterPosFromDummy(dummyPos, offsetX, offsetY, offsetZ, facing);
        BlockState masterState = level.getBlockState(masterPos);

        if (masterState.getBlock() instanceof SmithingFurnaceBlock) {
            return masterPos;
        }
        return null;
    }

    /**
     * Destroys the master block (which will cascade to destroy all dummies)
     */
    private void findAndDestroyMaster(Level level, BlockPos dummyPos, BlockState dummyState) {
        BlockPos masterPos = findMaster(level, dummyPos, dummyState);
        if (masterPos != null) {
            // Destroy master which will drop the item and destroy all dummies
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
        if (!(state.getBlock() instanceof SmithingFurnaceDummyBlock)) {
            return null;
        }

        int offsetX = state.getValue(OFFSET_X);
        int offsetY = state.getValue(OFFSET_Y);
        int offsetZ = state.getValue(OFFSET_Z);
        Direction facing = state.getValue(FACING);

        BlockPos masterPos = SmithingFurnaceBlock.getMasterPosFromDummy(dummyPos, offsetX, offsetY, offsetZ, facing);
        BlockState masterState = level.getBlockState(masterPos);

        if (masterState.getBlock() instanceof SmithingFurnaceBlock) {
            return masterPos;
        }
        return null;
    }
}
