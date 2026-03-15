package com.THproject.tharidia_things.block.alchemist;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class AlchemistJarInteractionHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // Only process main hand to avoid double-firing
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof Interaction)) return;

        CompoundTag data = event.getTarget().getPersistentData();
        if (!data.contains("AlchemistJarIndex")) return;

        // Cancel vanilla Interaction entity behaviour
        event.setCanceled(true);

        if (event.getLevel().isClientSide) return;

        BlockPos masterPos = new BlockPos(
                data.getInt("AlchemistMasterX"),
                data.getInt("AlchemistMasterY"),
                data.getInt("AlchemistMasterZ"));

        if (!(event.getLevel().getBlockEntity(masterPos) instanceof AlchemistTableBlockEntity table))
            return;

        boolean isInput  = data.getBoolean("AlchemistIsInput");
        ItemStack held   = event.getEntity().getItemInHand(InteractionHand.MAIN_HAND);

        if (isInput) {
            if (held.isEmpty()) {
                table.tryPickJar(event.getEntity());
            } else {
                table.tryInsertIntoJar(held, event.getEntity());
            }
        } else {
            table.displayResultJars(event.getEntity());
        }
    }
}
