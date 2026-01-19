package com.THproject.tharidia_things.block;

import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
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
 * Dummy block for multiblock stable structure.
 * Points to master block and has collision but no logic.
 */
public class StableDummyBlock extends Block {

    public static final MapCodec<StableDummyBlock> CODEC = simpleCodec(StableDummyBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Offset to master block position (-2, -1, 0, 1, or 2)
    // We use 0-4 in blockstate and subtract 2 to get actual offset
    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 4);
    public static final IntegerProperty OFFSET_Z = IntegerProperty.create("offset_z", 0, 4);

    // Floor collision shape - matches the master block floor height (base at Y 2.8)
    private static final VoxelShape FLOOR_SHAPE = Shapes.box(0, 0, 0, 1, 0.4, 1);

    public StableDummyBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(OFFSET_X, 2)
            .setValue(OFFSET_Z, 2));
    }
    
    public StableDummyBlock() {
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
        builder.add(FACING, OFFSET_X, OFFSET_Z);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLOOR_SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLOOR_SHAPE;
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        // Get offset from blockstate (stored as 0-4, actual offset is -2 to 2)
        int offsetX = state.getValue(OFFSET_X) - 2;
        int offsetZ = state.getValue(OFFSET_Z) - 2;

        // Calculate master position using stored offset
        BlockPos masterPos = pos.offset(offsetX, 0, offsetZ);

        if (level.getBlockEntity(masterPos) instanceof StableBlockEntity stable && stable.getManureAmount() > 0) {
            return SoundType.MUD;
        }
        return super.getSoundType(state, level, pos, entity);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
    
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        // Forward interaction to master block
        BlockPos masterPos = findMaster(level, pos, state);
        if (masterPos != null) {
            BlockState masterState = level.getBlockState(masterPos);
            if (masterState.getBlock() instanceof StableBlock stableBlock) {
                net.minecraft.world.phys.BlockHitResult newHit = new net.minecraft.world.phys.BlockHitResult(
                    hitResult.getLocation(), hitResult.getDirection(), masterPos, hitResult.isInside());
                return stableBlock.useItemOn(stack, masterState, level, masterPos, player, hand, newHit);
            }
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hitResult) {
        // Forward interaction to master block
        BlockPos masterPos = findMaster(level, pos, state);
        if (masterPos != null) {
            BlockState masterState = level.getBlockState(masterPos);
            if (masterState.getBlock() instanceof StableBlock stableBlock) {
                net.minecraft.world.phys.BlockHitResult newHit = new net.minecraft.world.phys.BlockHitResult(
                    hitResult.getLocation(), hitResult.getDirection(), masterPos, hitResult.isInside());
                return stableBlock.useWithoutItem(masterState, level, masterPos, player, newHit);
            }
        }
        return net.minecraft.world.InteractionResult.PASS;
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Find and destroy master block when dummy is broken
            if (!level.isClientSide) {
                // Use the state parameter directly since the block is already being removed
                findAndDestroyMaster(level, pos, state);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    private BlockPos findMaster(Level level, BlockPos dummyPos, BlockState dummyState) {
        // Get offset from blockstate (stored as 0-4, actual offset is -2 to 2)
        int offsetX = dummyState.getValue(OFFSET_X) - 2;
        int offsetZ = dummyState.getValue(OFFSET_Z) - 2;

        // Calculate master position using stored offset
        BlockPos masterPos = dummyPos.offset(offsetX, 0, offsetZ);
        BlockState masterState = level.getBlockState(masterPos);

        if (masterState.getBlock() instanceof StableBlock) {
            return masterPos;
        }

        return null;
    }

    private void findAndDestroyMaster(Level level, BlockPos dummyPos, BlockState dummyState) {
        BlockPos masterPos = findMaster(level, dummyPos, dummyState);
        if (masterPos != null) {
            level.destroyBlock(masterPos, true);
        }
    }
}
