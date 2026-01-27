package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.block.station_crystal.StationCrystalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber
public class StationCrystalLogic {
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide())
            return;

        BlockEntity be_bottom = event.getLevel().getBlockEntity(
                new BlockPos(event.getPos().getX(), event.getPos().getY() - 1, event.getPos().getZ()));
        if (event.getEntity() instanceof Player player) {
            if (event.getPlacedBlock().is(TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath("tharidiathings", "placeable_station_crystal")))) {
                if (!(be_bottom instanceof StationCrystalBlockEntity)) {
                    player.displayClientMessage(Component.literal("You have to place stations on Station Crystal"),
                            true);
                    event.setCanceled(true);
                }
            }
        }
    }
}
