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

        event.getEntity().swing(InteractionHand.MAIN_HAND, true);

        CompoundTag data = event.getTarget().getPersistentData();

        // Must be one of our entities (jar or cauldron)
        boolean isJar     = data.contains("AlchemistJarIndex");
        boolean isCauldron = data.getBoolean("AlchemistIsCauldron");
        if (!isJar && !isCauldron) return;

        // Cancel vanilla Interaction entity behaviour
        event.setCanceled(true);

        if (event.getLevel().isClientSide) return;

        BlockPos masterPos = new BlockPos(
                data.getInt("AlchemistMasterX"),
                data.getInt("AlchemistMasterY"),
                data.getInt("AlchemistMasterZ"));

        if (!(event.getLevel().getBlockEntity(masterPos) instanceof AlchemistTableBlockEntity table))
            return;

        // ── Cauldron (Mestolone) ──────────────────────────────────────────────
        if (isCauldron) {
            table.stir(event.getEntity());
            return;
        }

        // ── Jar entity ────────────────────────────────────────────────────────
        boolean isInput = data.getBoolean("AlchemistIsInput");
        int jarIndex    = data.getInt("AlchemistJarIndex");
        ItemStack held  = event.getEntity().getItemInHand(InteractionHand.MAIN_HAND);

        if (isInput) {
            if (held.isEmpty()) {
                table.tryPickJar(event.getEntity());
            } else {
                table.tryInsertIntoJar(held, event.getEntity());
            }
        } else {
            table.onOutputJarClicked(jarIndex, event.getEntity());
        }
    }
}
