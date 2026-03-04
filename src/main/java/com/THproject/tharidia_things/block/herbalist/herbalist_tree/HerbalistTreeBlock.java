package com.THproject.tharidia_things.block.herbalist.herbalist_tree;

import javax.annotation.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class HerbalistTreeBlock extends BaseEntityBlock {
    public static final MapCodec<HerbalistTreeBlock> CODEC = simpleCodec(HerbalistTreeBlock::new);

    public HerbalistTreeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            if (!canFormMultiblock(level, pos)) {
                level.destroyBlock(pos, true);
                return;
            }
            formMultiblock(level, pos);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HerbalistTreeBlockEntity treeBE) {
            // Dead tree: no interaction
            if (treeBE.isTreeDead()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // Watering: water bucket
            if (stack.is(Items.WATER_BUCKET)) {
                treeBE.waterTree();
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.SUCCESS;
            }

            // Fertilizing: items in the herbalist_tree_food tag (concime)
            if (stack.is(ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    TharidiaThings.MODID, "herbalist_tree_food")))) {
                treeBE.fertilizeTree();
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.SUCCESS;
            }

            // Minigame interactions
            if (!treeBE.hasAllPots()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (treeBE.isMinigameComplete() && stack.isEmpty()) {
                treeBE.collectPetals(player);
                return ItemInteractionResult.SUCCESS;
            }
            if (!treeBE.isCrafting() && !treeBE.isMinigameComplete()) {
                treeBE.startCrafting();
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, TharidiaThings.HERBALIST_TREE_BLOCK_ENTITY.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            destroyMultiblock(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private boolean canFormMultiblock(Level level, BlockPos masterPos) {
        for (int y = 1; y < 3; y++) {
            BlockPos checkPos = masterPos.above(y);
            BlockState existingState = level.getBlockState(checkPos);
            if (!existingState.canBeReplaced())
                return false;
        }
        return true;
    }

    private void formMultiblock(Level level, BlockPos masterPos) {
        for (int y = 1; y < 3; y++) {
            BlockPos dummyPos = masterPos.above(y);
            level.setBlock(dummyPos, TharidiaThings.HERBALIST_TREE_DUMMY_BLOCK.get().defaultBlockState()
                    .setValue(HerbalistTreeDummyBlock.OFFSET_Y, y), 3);
        }
    }

    private void destroyMultiblock(Level level, BlockPos masterPos) {
        for (int y = 1; y < 3; y++) {
            BlockPos dummyPos = masterPos.above(y);
            if (level.getBlockState(dummyPos).getBlock() instanceof HerbalistTreeDummyBlock) {
                level.removeBlock(dummyPos, false);
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HerbalistTreeBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return this.codec();
    }
}
