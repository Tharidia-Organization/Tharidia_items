package com.THproject.tharidia_things.block.herbalist.pot;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.herbalist_tree.HerbalistTreeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class HandlePotBreak {
    @SubscribeEvent
    public static void onPotBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();

        if (level.getBlockEntity(event.getPos()) instanceof PotBlockEntity pot) {
            BlockPos treePos = pot.getTreePos();
            if (treePos == null)
                return;
            if (level.getBlockEntity(pot.getTreePos()) instanceof HerbalistTreeBlockEntity tree
                    && tree.isCrafting())
                event.setCanceled(true);
            level.sendBlockUpdated(event.getPos(), event.getState(), event.getState(), 3);
        }
    }
}
