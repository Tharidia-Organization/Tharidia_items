package com.THproject.tharidia_things.block.station_crystal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class StationCrystalBlockItem extends BlockItem {

    public StationCrystalBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        long durationTime = 0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("durationTime")) {
            durationTime = customData.copyTag().getLong("durationTime");
        }

        long remainingTime = StationCrystalBlockEntity.MAX_TIME - durationTime;
        if (remainingTime < 0)
            remainingTime = 0;

        long seconds = remainingTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        String timeString;
        if (days > 0) {
            timeString = String.format("%02d:%02d:%02d:%02d",
                    days, hours % 24, minutes % 60, seconds % 60);
        } else if (hours > 0) {
            timeString = String.format("%02d:%02d:%02d",
                    hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            timeString = String.format("%02d:%02d",
                    minutes, seconds % 60);
        } else {
            timeString = String.format("%02d", seconds);
        }

        tooltipComponents.add(Component.literal("Remaining Time: " + timeString).withStyle(ChatFormatting.BLUE));
    }
}
