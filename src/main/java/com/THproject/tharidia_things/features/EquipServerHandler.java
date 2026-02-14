package com.THproject.tharidia_things.features;

import com.THproject.tharidia_things.compoundTag.CustomArmorAttachments;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class EquipServerHandler {
    public static void apply(Player player, JsonObject json) {
        System.out.println("Applying equip to player " + player.getName().getString() + " [Server Thread]");

        try {
            // Restore Hands
            if (json.has("mainhand")) {
                ItemStack.CODEC.parse(JsonOps.INSTANCE, json.get("mainhand"))
                        .resultOrPartial(System.err::println)
                        .ifPresent(stack -> {
                            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                            System.out.println("Restored Main Hand: " + stack);
                        });
            }

            if (json.has("offhand")) {
                ItemStack.CODEC.parse(JsonOps.INSTANCE, json.get("offhand"))
                        .resultOrPartial(System.err::println)
                        .ifPresent(stack -> {
                            player.setItemInHand(InteractionHand.OFF_HAND, stack);
                            System.out.println("Restored Off Hand: " + stack);
                        });
            }

            // Restore Armor
            if (json.has("armor")) {
                JsonArray armor = json.getAsJsonArray("armor");
                EquipmentSlot[] slots = { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST,
                        EquipmentSlot.HEAD };

                for (int i = 0; i < armor.size() && i < slots.length; i++) {
                    ItemStack stack = ItemStack.EMPTY;
                    JsonElement el = armor.get(i);
                    if (el.isJsonObject() && el.getAsJsonObject().size() > 0) {
                        stack = ItemStack.CODEC.parse(JsonOps.INSTANCE, el)
                                .result().orElse(ItemStack.EMPTY);
                    }
                    player.setItemSlot(slots[i], stack);
                    System.out.println("Restored Armor " + slots[i] + ": " + stack);
                }
            }

            // Restore Under Armor
            if (json.has("under_armor")) {
                System.out.println("Clearing under armor");
                Container under_armor_container = player.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());
                for (int i = 0; i < 4; i++) {
                    under_armor_container.setItem(i, ItemStack.EMPTY);
                }
                player.getInventory().setChanged();

                JsonArray under_armor = json.getAsJsonArray("under_armor");
                System.out.println("Found " + under_armor.size() + "items to restore.");

                for (JsonElement element : under_armor) {
                    JsonObject itemJson = element.getAsJsonObject();
                    int slot = itemJson.get("slot").getAsInt();
                    if (itemJson.has("item") && slot >= 0 && slot < 4) {
                        ItemStack.CODEC.parse(JsonOps.INSTANCE, itemJson.get("item"))
                                .resultOrPartial(msg -> System.out.println("Item parse error:" + msg))
                                .ifPresent(stack -> {
                                    under_armor_container.setItem(slot, stack);
                                    System.out.println(
                                            "Set Under Armor" + slot + " -> " + stack + " (Count: " + stack.getCount() + ")");
                                });
                    }
                }
            }

            // Restore Main Inventory
            if (json.has("inventory")) {
                System.out.println("Clearing main inventory...");
                for (int i = 0; i < player.getInventory().items.size(); i++) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                // Determine if we cleared it
                player.getInventory().setChanged();

                JsonArray inventory = json.getAsJsonArray("inventory");
                System.out.println("Found " + inventory.size() + " items to restore.");

                for (JsonElement element : inventory) {
                    JsonObject itemJson = element.getAsJsonObject();
                    int slot = itemJson.get("slot").getAsInt();
                    if (itemJson.has("item") && slot >= 0 && slot < player.getInventory().items.size()) {
                        ItemStack.CODEC.parse(JsonOps.INSTANCE, itemJson.get("item"))
                                .resultOrPartial(msg -> System.err.println("Item parse error: " + msg))
                                .ifPresent(stack -> {
                                    player.getInventory().setItem(slot, stack);
                                    System.out.println(
                                            "Set Slot " + slot + " -> " + stack + " (Count: " + stack.getCount() + ")");
                                });
                    }
                }
            }

            // Force Sync
            System.out.println("Forcing inventory sync...");
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            player.inventoryMenu.sendAllDataToRemote();
            if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastChanges();
                player.containerMenu.sendAllDataToRemote();
            }

            System.out.println("Equip apply completed.");

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR applying equip: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
