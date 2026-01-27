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
            return (be instanceof StationCrystalBlockEntity) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (level.getBlockEntity(
                pos) instanceof StationCrystalBlockEntity blockEntity) {
            int tick = blockEntity.getRemainingTick();
            int totalSeconds = tick / 20;
            int days = totalSeconds / 86400;
            int hours = (totalSeconds % 86400) / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            int seconds = totalSeconds % 60;

            player.displayClientMessage(Component.literal(
                    String.format("Remaining %02d:%02d:%02d:%02d", days, hours, minutes, seconds))
                    .withColor(0x00FF00),
                    true);

        }
        return super.useOn(context);
    }
}
