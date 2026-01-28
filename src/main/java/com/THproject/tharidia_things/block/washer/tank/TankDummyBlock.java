package com.THproject.tharidia_things.block.washer.tank;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;

public class TankDummyBlock extends Block {
    public static final MapCodec<TankDummyBlock> CODEC = simpleCodec(TankDummyBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty OFFSET_Y = IntegerProperty.create("offset_y", 1, 3); // 1 to 3 height offset

    public TankDummyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(OFFSET_Y, 1));
    }

    public TankDummyBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.0F)
                .noOcclusion()
                .noLootTable());
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OFFSET_Y);
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        int offsetY = state.getValue(OFFSET_Y);
        BlockPos masterPos = pos.below(offsetY);

        BlockState masterState = level.getBlockState(masterPos);
        if (masterState.getBlock() instanceof TankBlock tankBlock) {
            net.minecraft.world.phys.BlockHitResult newHit = new net.minecraft.world.phys.BlockHitResult(
                    hitResult.getLocation(), hitResult.getDirection(), masterPos, hitResult.isInside());
            return tankBlock.useItemOn(stack, masterState, level, masterPos, player, hand, newHit);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Destroy master if dummy broken
            if (!level.isClientSide) {
                int offsetY = state.getValue(OFFSET_Y);
                BlockPos masterPos = pos.below(offsetY);
                if (level.getBlockState(masterPos).getBlock() instanceof TankBlock) {
                    level.destroyBlock(masterPos, true);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
