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

public class StationCrystalRepairerItem extends Item {

    public StationCrystalRepairerItem(Properties properties) {
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

        if (level.getBlockEntity(pos) instanceof StationCrystalBlockEntity station) {
            if (station.removeTimePercentage(0.25)) {
                context.getItemInHand().shrink(1);
                player.displayClientMessage(
                        Component.literal("Repaired 25% of the Station Crystal's durability!").withColor(0x00FF00),
                        true);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(
                        Component.literal("Cannot repair the Station Crystal at this time").withColor(0xFF0000),
                        true);
                return InteractionResult.SUCCESS_NO_ITEM_USED;
            }
        } else if (level.getBlockEntity(new BlockPos(pos.getX(), pos.getY() - 1,
                pos.getZ())) instanceof StationCrystalBlockEntity stationBelow) {
            if (stationBelow.removeTimePercentage(0.25)) {
                context.getItemInHand().shrink(1);
                player.displayClientMessage(
                        Component.literal("Repaired 25% of the Station Crystal's durability!").withColor(0x00FF00),
                        true);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(
                        Component.literal("Cannot repair the Station Crystal at this time").withColor(0xFF0000),
                        true);
                return InteractionResult.SUCCESS_NO_ITEM_USED;
            }
        }

        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }
}
