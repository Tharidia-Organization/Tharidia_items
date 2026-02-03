package com.THproject.tharidia_things.block.station_crystal;

import com.THproject.tharidia_things.TharidiaThings;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.jetbrains.annotations.Nullable;

public class StationCrystalBlock extends BaseEntityBlock {
    public static final MapCodec<StationCrystalBlock> CODEC = simpleCodec(StationCrystalBlock::new);

    public StationCrystalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        BlockPos posAbove = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
        if (level.getBlockEntity(pos) instanceof StationCrystalBlockEntity blockEntity) {
            blockEntity.removeBlockAbove(posAbove);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
            FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof StationCrystalBlockEntity blockEntity) {
            ItemStack itemDrop = new ItemStack(TharidiaThings.STATION_CRYSTAL_BLOCK_ITEM.asItem());
            itemDrop.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
                    data -> data.update(tag -> tag.putLong("durationTime", blockEntity.getDurationTime())));

            BaseEntityBlock.popResource(level, pos, itemDrop);
        }

        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StationCrystalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, TharidiaThings.STATION_CRYSTAL_BLOCK_ENTITY.get(),
                StationCrystalBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
