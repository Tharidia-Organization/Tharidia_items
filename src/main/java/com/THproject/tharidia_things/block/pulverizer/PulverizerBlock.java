package com.THproject.tharidia_things.block.pulverizer;

import javax.annotation.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class PulverizerBlock extends BaseEntityBlock {
    public static final MapCodec<PulverizerBlock> CODEC = simpleCodec(PulverizerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public PulverizerBlock() {
        this(BlockBehaviour.Properties.of().noOcclusion());
    }

    public PulverizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND)
            return ItemInteractionResult.CONSUME;

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof PulverizerBlockEntity pulverizer)) {
            return ItemInteractionResult.CONSUME;
        }

        if (stack.getItem() == TharidiaThings.GRINDER.asItem()) {
            pulverizer.setGrinder(stack);
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        } else if (stack.isEmpty() && player.isShiftKeyDown()) {
            ItemStack grinder = pulverizer.getGrinder();
            if (!grinder.isEmpty()) {
                pulverizer.removeGrinder();
                if (player.getInventory().add(grinder)) {
                    player.drop(grinder, false);
                }
            }
            return ItemInteractionResult.SUCCESS;
        } else if (stack.isEmpty()) {
            pulverizer.toogleActive();
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PulverizerBlockEntity pulverizer) {
                Block.popResource(level, pos, pulverizer.getGrinder());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, TharidiaThings.PULVERIZER_BLOCK_ENTITY.get(), PulverizerBlockEntity::tick);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PulverizerBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

}
