package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.station_crystal.StationCrystalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class StationCrystalTool extends Item {
    public StationCrystalTool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            BlockEntity beBelow = level.getBlockEntity(new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ()));
            return ((be instanceof StationCrystalBlockEntity) || (beBelow instanceof StationCrystalBlockEntity))
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        if (level.getBlockEntity(pos) instanceof StationCrystalBlockEntity blockEntity) {
            long tick = blockEntity.getRemainingTime();
            int totalSeconds = (int) (tick / 1000);
            int days = totalSeconds / 86400;
            int hours = (totalSeconds % 86400) / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            int seconds = totalSeconds % 60;

            player.displayClientMessage(Component.literal(
                    String.format("Remaining %02d:%02d:%02d:%02d (%d%%)", days, hours, minutes, seconds,
                            (int) (blockEntity.getTimePercentage() * 100)))
                    .withColor(0x00FF00),
                    true);
        } else if (level.getBlockEntity(new BlockPos(pos.getX(), pos.getY() - 1,
                pos.getZ())) instanceof StationCrystalBlockEntity stationBelow) {
            long tick = stationBelow.getRemainingTime();
            int totalSeconds = (int) (tick / 1000);
            int days = totalSeconds / 86400;
            int hours = (totalSeconds % 86400) / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            int seconds = totalSeconds % 60;

            player.displayClientMessage(Component.literal(
                    String.format("Remaining %02d:%02d:%02d:%02d (%d%%)", days, hours, minutes, seconds,
                            (int) (stationBelow.getTimePercentage() * 100)))
                    .withColor(0x00FF00),
                    true);
        }
        return super.useOn(context);
    }
}
