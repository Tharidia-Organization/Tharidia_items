package com.THproject.tharidia_things.block.washer.sieve;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.MapColor;

public class SieveDummyBlock extends Block {
    public static final MapCodec<SieveDummyBlock> CODEC = simpleCodec(SieveDummyBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Offset to master block position (-2, -1, 0, 1, or 2)
    // We use 0-4 in blockstate and subtract 2 to get actual offset
    // Since sieve is smaller, maybe we don't need 5x5, but copying logic for now.
    // If it's 3x3 (1 radius), offset is -1 to 1. 0-2 range.
    // If it's same as stable (5x5, 2 radius), offset is -2 to 2. 0-4 range.
    // User said "copy the logic from stable", Stable has 5x5 logic.
    // I will use 5x5 logic to be consistent with request "copy the logic".

    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 4);
    public static final IntegerProperty OFFSET_Z = IntegerProperty.create("offset_z", 0, 4);

    public SieveDummyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(OFFSET_X, 2) // 2 - 2 = 0
                .setValue(OFFSET_Z, 2)); // 2 - 2 = 0
    }

    public SieveDummyBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.0F)
                .noOcclusion() // Matches SieveBlock properties somewhat (metal, noOcclusion)
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
            BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hitResult) {
        // Forward interaction to master block
        net.minecraft.core.BlockPos masterPos = findMaster(level, pos, state);
        if (masterPos != null) {
            BlockState masterState = level.getBlockState(masterPos);
            if (masterState.getBlock() instanceof SieveBlock sieveBlock) {
                net.minecraft.world.phys.BlockHitResult newHit = new net.minecraft.world.phys.BlockHitResult(
                        hitResult.getLocation(), hitResult.getDirection(), masterPos, hitResult.isInside());
                return sieveBlock.useItemOn(stack, masterState, level, masterPos, player, hand, newHit);
            }
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Find and destroy master block when dummy is broken
            if (!level.isClientSide) {
                findAndDestroyMaster(level, pos, state);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private net.minecraft.core.BlockPos findMaster(net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos dummyPos, BlockState dummyState) {
        int offsetX = dummyState.getValue(OFFSET_X) - 2;
        int offsetZ = dummyState.getValue(OFFSET_Z) - 2;
        net.minecraft.core.BlockPos masterPos = dummyPos.offset(offsetX, 0, offsetZ);
        if (level.getBlockState(masterPos).getBlock() instanceof SieveBlock) {
            return masterPos;
        }
        return null;
    }

    private void findAndDestroyMaster(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos dummyPos,
            BlockState dummyState) {
        net.minecraft.core.BlockPos masterPos = findMaster(level, dummyPos, dummyState);
        if (masterPos != null) {
            level.destroyBlock(masterPos, true); // True to drop loot/resources
        }
    }
}
