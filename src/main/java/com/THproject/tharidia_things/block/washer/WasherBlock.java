package com.THproject.tharidia_things.block.washer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.Containers;

import com.THproject.tharidia_things.TharidiaThings;

public class WasherBlock extends BaseEntityBlock {
    public static final MapCodec<WasherBlock> CODEC = simpleCodec(WasherBlock::new);

    public WasherBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, TharidiaThings.WASHER_BLOCK_ENTITY.get(), WasherBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof WasherBlockEntity washer) {
            if (stack.getItem() == Items.WATER_BUCKET) {
                if (!washer.isTankFull()) {
                    washer.addWaterBucket();
                    player.playSound(SoundEvents.BUCKET_EMPTY);
                    if (!player.isCreative())
                        player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    return ItemInteractionResult.SUCCESS;
                } else {
                    return ItemInteractionResult.CONSUME;
                }
            } else if (stack.getItem() == Items.BUCKET) {
                if (washer.tank.getFluidAmount() >= 1000) {
                    washer.tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    player.playSound(SoundEvents.BUCKET_FILL);
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
                    } else if (!player.getInventory().add(new ItemStack(Items.WATER_BUCKET))) {
                        player.drop(new ItemStack(Items.WATER_BUCKET), false);
                    }
                    return ItemInteractionResult.SUCCESS;
                }
            } else {
                if (stack.isEmpty() && !player.isShiftKeyDown()) {
                    boolean success = false;
                    for (int i = 1; i < washer.inventory.getSlots(); i++) {
                        ItemStack extracted = washer.inventory.extractItem(i, 64, false);
                        if (!extracted.isEmpty()) {
                            if (!player.getInventory().add(extracted.copy())) {
                                player.drop(extracted.copy(), false);
                            }
                            success = true;
                        }
                    }
                    return success ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
                } else if (stack.isEmpty() && player.isShiftKeyDown()) {
                    ItemStack extracted = washer.inventory.extractItem(0, 64, false);
                    if (!extracted.isEmpty()) {
                        if (!player.getInventory().add(extracted.copy())) {
                            player.drop(extracted.copy(), false);
                        }
                        return ItemInteractionResult.SUCCESS;
                    }
                    return ItemInteractionResult.CONSUME;
                } else {
                    ItemStack remainder = washer.inventory.insertItem(0, stack.copy(), false);
                    if (remainder.getCount() != stack.getCount()) {
                        player.setItemInHand(hand, remainder);
                        return ItemInteractionResult.SUCCESS;
                    } else {
                        return ItemInteractionResult.CONSUME;
                    }
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof WasherBlockEntity washer) {
                for (int i = 0; i < washer.inventory.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                            washer.inventory.getStackInSlot(i));
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WasherBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

}
