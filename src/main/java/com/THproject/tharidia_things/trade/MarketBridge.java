package com.THproject.tharidia_things.trade;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Bridge to communicate transaction data to Tharidia Features mod
 * Uses a shared file system approach for cross-mod communication
 */
public class MarketBridge {
    private static final String TRANSACTION_QUEUE_FILE = "tharidia_market_transactions.dat";

    /**
     * Send a transaction to Tharidia Features for market processing
     */
    public static void sendTransaction(MinecraftServer server, 
                                      UUID seller, UUID buyer,
                                      String sellerName, String buyerName,
                                      List<ItemStack> sellerItems, List<ItemStack> buyerItems,
                                      boolean isFictional) {
        try {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            File transactionFile = new File(worldDir, TRANSACTION_QUEUE_FILE);

            // Load existing queue or create new
            CompoundTag queueTag;
            if (transactionFile.exists()) {
                queueTag = NbtIo.readCompressed(transactionFile.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            } else {
                queueTag = new CompoundTag();
                queueTag.put("Transactions", new ListTag());
            }

            // Create transaction data
            CompoundTag transactionTag = new CompoundTag();
            transactionTag.putUUID("Seller", seller);
            transactionTag.putUUID("Buyer", buyer);
            transactionTag.putString("SellerName", sellerName);
            transactionTag.putString("BuyerName", buyerName);
            transactionTag.putLong("Timestamp", System.currentTimeMillis());
            transactionTag.putBoolean("IsFictional", isFictional);

            // Serialize seller items
            ListTag sellerList = new ListTag();
            for (ItemStack item : sellerItems) {
                if (!item.isEmpty()) {
                    sellerList.add(item.save(server.registryAccess()));
                }
            }
            transactionTag.put("SellerItems", sellerList);

            // Serialize buyer items
            ListTag buyerList = new ListTag();
            for (ItemStack item : buyerItems) {
                if (!item.isEmpty()) {
                    buyerList.add(item.save(server.registryAccess()));
                }
            }
            transactionTag.put("BuyerItems", buyerList);

            // Add to queue
            ListTag transactions = queueTag.getList("Transactions", Tag.TAG_COMPOUND);
            transactions.add(transactionTag);
            queueTag.put("Transactions", transactions);

            // Save queue
            NbtIo.writeCompressed(queueTag, transactionFile.toPath());

            TharidiaThings.LOGGER.info("Transaction queued for market processing: {} <-> {}", sellerName, buyerName);
        } catch (IOException e) {
            TharidiaThings.LOGGER.error("Failed to queue transaction for market", e);
        }
    }
}
