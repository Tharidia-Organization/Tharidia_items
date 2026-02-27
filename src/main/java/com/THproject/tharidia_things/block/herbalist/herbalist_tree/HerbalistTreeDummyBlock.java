package com.THproject.tharidia_things.block.herbalist.herbalist_tree;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class HerbalistTreeDummyBlock extends Block {
    public static final MapCodec<HerbalistTreeDummyBlock> CODEC = simpleCodec(HerbalistTreeDummyBlock::new);
    public static final IntegerProperty OFFSET_Y = IntegerProperty.create("offset_y", 1, 3); // 1 to 3 height offset

    public HerbalistTreeDummyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        int offsetY = state.getValue(OFFSET_Y);
        BlockPos masterPos = pos.below(offsetY);

        BlockState masterState = level.getBlockState(masterPos);
        if (masterState.getBlock() instanceof HerbalistTreeBlock herbalistTreeBlock) {
            BlockHitResult newHit = new net.minecraft.world.phys.BlockHitResult(
                    hitResult.getLocation(), hitResult.getDirection(), masterPos, hitResult.isInside());
            return herbalistTreeBlock.useItemOn(stack, masterState, level, masterPos, player, hand, newHit);
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
                if (level.getBlockState(masterPos).getBlock() instanceof HerbalistTreeBlock) {
                    level.destroyBlock(masterPos, true);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OFFSET_Y);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
