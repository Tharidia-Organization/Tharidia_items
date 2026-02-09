package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.CustomArmorAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.THproject.tharidia_things.network.SyncCustomArmorPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.*;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class CustomArmorAttributeHandler {

    // Cache to store the processed stacks to detect changes locally
    // Note: In production code, attaching this state to the player
    // (Capability/Attachment) is better
    // to avoid map leaks on player disconnect, but for simplicity here we use a
    // WeakHashMap.
    private static final WeakHashMap<Player, List<ItemStack>> LAST_STACKS = new WeakHashMap<>();

    // Mapping showing which custom slot index corresponds to which EquipmentSlot
    // ArmorMenu: 0 -> PRE HEAD (Helmet), 1 -> CHEST, 2 -> LEGS, 3 -> FEET
    // But ArmorMenu defines:
    // Slot 0 (Custom) accepts tag under_armor/helmet -> corresponds to HEAD
    // Slot 1 (Custom) accepts tag under_armor/chestplate -> corresponds to CHEST
    // Slot 2 (Custom) accepts tag under_armor/leggings -> corresponds to LEGS
    // Slot 3 (Custom) accepts tag under_armor/boots -> corresponds to FEET
    private static final EquipmentSlot[] SLOT_MAPPING = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncToPlayer(sp, sp);
            syncToTracking(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncToPlayer(sp, sp);
            syncToTracking(sp);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player targetPlayer && event.getEntity() instanceof ServerPlayer tracker) {
            syncToPlayer(targetPlayer, tracker);
        }
    }

    private static void syncToPlayer(Player target, ServerPlayer receiver) {
        if (target.level().isClientSide)
            return;
        List<ItemStack> items = new ArrayList<>();
        Container container = target.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());
        for (int i = 0; i < container.getContainerSize(); i++)
            items.add(container.getItem(i));

        PacketDistributor.sendToPlayer(receiver, new SyncCustomArmorPacket(target.getId(), items));
    }

    private static void syncToTracking(Player player) {
        if (player.level().isClientSide)
            return;
        List<ItemStack> items = new ArrayList<>();
        Container container = player.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());
        for (int i = 0; i < container.getContainerSize(); i++)
            items.add(container.getItem(i));

        PacketDistributor.sendToPlayersTrackingEntity(player, new SyncCustomArmorPacket(player.getId(), items));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide)
            return; // Server side only for attributes

        Container container = player.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());

        List<ItemStack> currentStacks = new ArrayList<>();
        boolean changed = false;

        List<ItemStack> lastKnown = LAST_STACKS.computeIfAbsent(player, k -> {
            List<ItemStack> list = new ArrayList<>();
            for (int i = 0; i < 4; i++)
                list.add(ItemStack.EMPTY);
            return list;
        });

        // 1. Check for changes
        for (int i = 0; i < 4; i++) {
            ItemStack current = container.getItem(i);
            currentStacks.add(current.copy()); // Store copy to detect changes

            if (!ItemStack.matches(current, lastKnown.get(i))) {
                changed = true;
            }
        }

        // 2. If changed, re-apply attributes
        if (changed) {
            // Remove attributes from OLD stacks
            for (int i = 0; i < 4; i++) {
                ItemStack oldStack = lastKnown.get(i);
                if (!oldStack.isEmpty()) {
                    removeAttributes(player, oldStack, i);
                }
            }

            // Add attributes from NEW stacks
            for (int i = 0; i < 4; i++) {
                ItemStack newStack = currentStacks.get(i);
                if (!newStack.isEmpty()) {
                    addAttributes(player, newStack, i);
                }
            }

            // Update cache
            LAST_STACKS.put(player, currentStacks);

            // SYNC TO CLIENTS
            if (player instanceof ServerPlayer sp) {
                syncToPlayer(player, sp);
                syncToTracking(player);
            }
        }
    }

    private static void removeAttributes(Player player, ItemStack stack, int slotIndex) {
        EquipmentSlot targetSlot = SLOT_MAPPING[slotIndex];
        ItemAttributeModifiers modifiers = stack.getAttributeModifiers();

        modifiers.modifiers().forEach(entry -> {
            if (entry.slot() != EquipmentSlotGroup.bySlot(targetSlot))
                return;

            AttributeModifier originalModifier = entry.modifier();
            ResourceLocation uniqueId = createUniqueId(originalModifier.id(), slotIndex);

            AttributeInstance instance = player.getAttribute(entry.attribute());
            if (instance != null) {
                instance.removeModifier(uniqueId);
            }
        });
    }

    private static void addAttributes(Player player, ItemStack stack, int slotIndex) {
        EquipmentSlot targetSlot = SLOT_MAPPING[slotIndex];
        // Use EquipmentSlotGroup to get attributes that apply to this slot
        ItemAttributeModifiers modifiers = stack.getAttributeModifiers();

        modifiers.modifiers().forEach(entry -> {
            if (entry.slot() != EquipmentSlotGroup.bySlot(targetSlot))
                return;

            AttributeModifier originalModifier = entry.modifier();

            // Create a UNIQUE ID for this slot so it doesn't conflict with main armor
            ResourceLocation uniqueId = createUniqueId(originalModifier.id(), slotIndex);

            AttributeModifier neomodifier = new AttributeModifier(
                    uniqueId,
                    originalModifier.amount(),
                    originalModifier.operation());

            AttributeInstance instance = player.getAttribute(entry.attribute());
            if (instance != null) {
                // Remove existing if present to avoid dupes (safety)
                if (instance.getModifier(uniqueId) != null) {
                    instance.removeModifier(uniqueId);
                }
                instance.addTransientModifier(neomodifier);
            }
        });
    }

    private static ResourceLocation createUniqueId(ResourceLocation originalId, int slotIndex) {
        // e.g. tharidia_things:custom_slot_0_minecraft_armor_helmet
        return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
                "custom_slot_" + slotIndex + "_" + originalId.getNamespace() + "_"
                        + originalId.getPath().replace('/', '_'));
    }
}
