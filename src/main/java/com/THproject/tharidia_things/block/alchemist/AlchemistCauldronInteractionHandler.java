package com.THproject.tharidia_things.block.alchemist;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Interaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class AlchemistCauldronInteractionHandler {
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event){
        if(event.getHand() != InteractionHand.MAIN_HAND) return;
        if(!(event.getTarget() instanceof Interaction)) return;

        CompoundTag data = event.getTarget().getPersistentData();
        if(!data.contains("AlchemistMasterX") || !data.contains("AlchemistMasterY") || !data.contains("AlchemistMasterZ")) return;

        event.setCanceled(true);

        if(event.getLevel().isClientSide) return;

        BlockPos masterPos = new BlockPos(
                data.getInt("AlchemistMasterX"),
                data.getInt("AlchemistMasterY"),
                data.getInt("AlchemistMasterZ"));

        if(!(event.getLevel().getBlockEntity(masterPos) instanceof AlchemistTableBlockEntity table))
            return;

        table.stir(event.getEntity());
    }
}
