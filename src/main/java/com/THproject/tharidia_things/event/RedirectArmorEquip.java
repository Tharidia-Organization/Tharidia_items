package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.CustomArmorAttachments;
import com.THproject.tharidia_things.features.Revive;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class RedirectArmorEquip {
    @SubscribeEvent
    public static void onRightClickArmorItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();

        if (stack.getItem() instanceof ArmorItem || stack.getItem() instanceof Equipable) {
            Container armorAttachments = player.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());

            EquipmentSlot slot = null;
            if (stack.getItem() instanceof ArmorItem armorItem) {
                slot = armorItem.getEquipmentSlot();
            } else if (stack.getItem() instanceof Equipable equipable) {
                slot = equipable.getEquipmentSlot();
            }

            if (slot != null) {
                int index = -1;
                if (slot == EquipmentSlot.HEAD)
                    index = 0;
                else if (slot == EquipmentSlot.CHEST)
                    index = 1;
                else if (slot == EquipmentSlot.LEGS)
                    index = 2;
                else if (slot == EquipmentSlot.FEET)
                    index = 3;

                if (index != -1) {
                    if (armorAttachments.getItem(index).isEmpty()) {
                        player.displayClientMessage(
                                Component.translatable("message.tharidiathings.equip.no_under_armor")
                                        .withColor(0xFF0000),
                                true);
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.FAIL);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onArmorEquiped(LivingEquipmentChangeEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack stack = event.getTo();
        if (stack.isEmpty()) {
            return;
        }

        EquipmentSlot slot = event.getSlot();
        int index = -1;

        if (slot == EquipmentSlot.HEAD) {
            index = 0;
        } else if (slot == EquipmentSlot.CHEST) {
            index = 1;
        } else if (slot == EquipmentSlot.LEGS) {
            index = 2;
        } else if (slot == EquipmentSlot.FEET) {
            index = 3;
        }

        if (index != -1) {
            Container armorAttachments = player.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());
            if (armorAttachments.getItem(index).isEmpty()) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickUnderArmorItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();

        ResourceLocation tagLocation = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "under_armor/");
        TagKey<Item> helmetTag = TagKey.create(Registries.ITEM, tagLocation.withSuffix("helmet"));
        TagKey<Item> chestplateTag = TagKey.create(Registries.ITEM, tagLocation.withSuffix("chestplate"));
        TagKey<Item> leggingsTag = TagKey.create(Registries.ITEM, tagLocation.withSuffix("leggings"));
        TagKey<Item> bootsTag = TagKey.create(Registries.ITEM, tagLocation.withSuffix("boots"));

        int targetSlot = -1;
        if (stack.is(helmetTag)) {
            targetSlot = 0;
        } else if (stack.is(chestplateTag)) {
            targetSlot = 1;
        } else if (stack.is(leggingsTag)) {
            targetSlot = 2;
        } else if (stack.is(bootsTag)) {
            targetSlot = 3;
        }

        if (targetSlot != -1) {
            Container armorAttachments = player.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());
            if (armorAttachments.getItem(targetSlot).isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                armorAttachments.setItem(targetSlot, copy);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        if (Revive.isPlayerFallen(player)) {
            Container container = player.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());
            for (int i = 0; i < 4; i++) {
                ItemStack item = container.getItem(i).copy();
                container.removeItem(i, 1);
                player.drop(item, false);
            }
        }
    }
}