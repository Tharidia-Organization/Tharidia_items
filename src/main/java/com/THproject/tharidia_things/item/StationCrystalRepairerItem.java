package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.block.station_crystal.StationCrystalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class StationCrystalRepairerItem extends Item {

    public StationCrystalRepairerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (level.getBlockEntity(pos) instanceof StationCrystalBlockEntity station) {
            if (station.removeTickPercentage(0.25)) {
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
