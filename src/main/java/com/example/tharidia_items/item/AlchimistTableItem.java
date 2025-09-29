package com.example.tharidia_items.item;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

public class AlchimistTableItem extends BlockItem {

    public AlchimistTableItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return super.getMaxUseTime(stack);
    }
}