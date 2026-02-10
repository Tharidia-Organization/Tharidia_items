package com.THproject.tharidia_things.features;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import com.THproject.tharidia_things.network.EquipLoadPacket;
import com.THproject.tharidia_things.compoundTag.CustomArmorAttachments;
import com.THproject.tharidia_things.network.EquipActionPacket;
import com.THproject.tharidia_things.network.EquipSharePacket;
import com.THproject.tharidia_things.network.EquipListSyncPacket;
import java.nio.file.Files;

public class Equip {
    private static Path equipPath = FMLPaths.GAMEDIR.get().resolve("th_equips/");
    public static Runnable onListUpdate; // Callback for GUI refresh

    public static void save(Player player, String name) {
        File file = equipPath.resolve(name + ".json").toFile();
        if (file.getParentFile() != null)
            file.getParentFile().mkdirs();

        JsonObject json = new JsonObject();

        ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, player.getOffhandItem())
                .resultOrPartial(System.err::println)
                .ifPresent(element -> json.add("offhand", element));

        JsonArray armor = new JsonArray();
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isEmpty()) {
                armor.add(new JsonObject());
            } else {
                ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, stack)
                        .resultOrPartial(System.err::println)
                        .ifPresent(armor::add);
            }
        }
        json.add("armor", armor);

        JsonArray under_armor = new JsonArray();
        Container under_armor_container = player.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());
        for (int i = 0; i < 4; i++) {
            ItemStack stack = under_armor_container.getItem(i);
            if (!stack.isEmpty()) {
                JsonObject itemJson = new JsonObject();
                itemJson.addProperty("slot", i);
                ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, stack)
                        .resultOrPartial(System.err::println)
                        .ifPresent(element -> itemJson.add("item", element));
                under_armor.add(itemJson);
            }
        }
        json.add("under_armor", under_armor);

        JsonArray inventory = new JsonArray();
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                JsonObject itemJson = new JsonObject();
                itemJson.addProperty("slot", i);
                ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, stack)
                        .resultOrPartial(System.err::println)
                        .ifPresent(element -> itemJson.add("item", element));
                inventory.add(itemJson);
            }
        }
        json.add("inventory", inventory);

        try (FileWriter writer = new FileWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean loadAndSend(String name) {
        File file = equipPath.resolve(name + ".json").toFile();
        if (!file.exists()) {
            System.out.println("Equip file not found: " + file.getAbsolutePath());
            return false;
        }

        try {
            String content = Files.readString(file.toPath());
            System.out.println("Reading equip file: " + file.getAbsolutePath());
            System.out.println("Content length: " + content.length());

            if (content.length() > 30000) {
                System.out.println("WARNING: JSON content is very large (" + content.length()
                        + " chars). Packet might fail if > 32767.");
            }

            PacketDistributor.sendToServer(new EquipLoadPacket(content));
            System.out.println("EquipLoadPacket passed to distributor.");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean delete(String name) {
        File file = equipPath.resolve(name + ".json").toFile();
        if (file.exists()) {
            file.delete();
            return true;
        }
        return false;
    }

    public static List<String> getList() {
        File folder = equipPath.toFile();
        if (!folder.exists() || !folder.isDirectory())
            return List.of();

        String[] files = folder.list((dir, name) -> name.endsWith(".json"));
        if (files == null)
            return List.of();

        return List.of(files).stream()
                .map(name -> name.substring(0, name.length() - 5))
                .toList();
    }

    private static Map<String, String> pendingShares = new HashMap<>();

    public static List<String> getPendingList() {
        return List.copyOf(pendingShares.keySet());
    }

    public static void handleReceivedShare(String sender, String equipName, String jsonContent) {
        pendingShares.put(equipName, jsonContent);
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player
                    .sendSystemMessage(Component
                            .literal("Player " + sender + " wants to share equip '" + equipName + "' with you"));
            syncListToServer();
        }
    }

    public static boolean acceptShare(String name, String saveName) {
        if (!pendingShares.containsKey(name))
            return false;

        String targetName = (saveName != null && !saveName.isEmpty()) ? saveName : name;
        String jsonContent = pendingShares.get(name);

        File file = equipPath.resolve(targetName + ".json").toFile();
        if (file.getParentFile() != null)
            file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonContent);
            pendingShares.remove(name);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean declineShare(String name) {
        if (!pendingShares.containsKey(name))
            return false;

        pendingShares.remove(name);
        return true;
    }

    public static boolean rename(String oldName, String newName) {
        File oldFile = equipPath.resolve(oldName + ".json").toFile();
        File newFile = equipPath.resolve(newName + ".json").toFile();

        if (oldFile.exists() && !newFile.exists()) {
            return oldFile.renameTo(newFile);
        }
        return false;
    }

    // Client-side handler for server requests
    public static void handleServerRequest(byte action, String name, String extraData) {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;

        switch (action) {
            case EquipActionPacket.ACTION_SAVE:
                save(player, name);
                player.sendSystemMessage(Component.literal("Saved equip '" + name + "'"));
                syncListToServer();
                break;

            case EquipActionPacket.ACTION_LOAD:
                if (loadAndSend(name)) {
                    player.sendSystemMessage(Component.literal("Loaded equip '" + name + "'"));
                } else {
                    player.sendSystemMessage(Component.literal("Equip '" + name + "' not found"));
                }
                break;

            case EquipActionPacket.ACTION_DELETE:
                if (delete(name)) {
                    player.sendSystemMessage(Component.literal("Deleted equip '" + name + "'"));
                    syncListToServer();
                } else {
                    player.sendSystemMessage(Component.literal("Equip '" + name + "' not found"));
                }
                break;

            case EquipActionPacket.ACTION_SHARE_REQUEST:
                // Server wants us to share 'name' with target 'extraData'
                try {
                    Path path = equipPath.resolve(name + ".json");
                    if (Files.exists(path)) {
                        String content = Files.readString(path);
                        PacketDistributor.sendToServer(new EquipSharePacket(extraData, name, content));
                        player.sendSystemMessage(Component.literal("Shared equip '" + name + "' with " + extraData));
                    } else {
                        player.sendSystemMessage(Component.literal("Cannot share: Equip '" + name + "' not found"));
                    }
                } catch (Exception e) {
                    player.sendSystemMessage(Component.literal("Error sharing equip: " + e.getMessage()));
                }
                break;

            case EquipActionPacket.ACTION_ACCEPT:
                if (acceptShare(name, extraData)) {
                    String finalName = (extraData != null && !extraData.isEmpty()) ? extraData : name;
                    player.sendSystemMessage(Component.literal("Accepted share '" + name + "' as '" + finalName + "'"));
                    syncListToServer();
                } else {
                    player.sendSystemMessage(Component.literal("No pending share for '" + name + "'"));
                }
                break;

            case EquipActionPacket.ACTION_DECLINE:
                if (declineShare(name)) {
                    player.sendSystemMessage(Component.literal("Declined share '" + name + "'"));
                    syncListToServer();
                } else {
                    player.sendSystemMessage(Component.literal("No pending share for '" + name + "'"));
                }
                break;

            case EquipActionPacket.ACTION_RENAME:
                if (rename(name, extraData)) {
                    player.sendSystemMessage(Component.literal("Renamed equip '" + name + "' to '" + extraData + "'"));
                    syncListToServer();
                } else {
                    player.sendSystemMessage(Component
                            .literal("Cannot rename: '" + name + "' not found or '" + extraData + "' already exists"));
                }
                break;

            case EquipActionPacket.ACTION_SYNC_REQUEST:
                syncListToServer();
                break;
        }
    }

    public static void syncListToServer() {
        if (onListUpdate != null) {
            onListUpdate.run();
        }
        PacketDistributor.sendToServer(new EquipListSyncPacket(getList(), false));
        PacketDistributor.sendToServer(new EquipListSyncPacket(getPendingList(), true));
    }
}
