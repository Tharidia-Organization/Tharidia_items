package com.THproject.tharidia_things.trade;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
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

    // ==================== Dynamic Tax Calculation via Tharidia Features ====================

    private static Class<?> integrationClass = null;
    private static Method calculateTaxRateMethod = null;
    private static Method calculateTaxAmountMethod = null;
    private static Method recordCompletedTradeMethod = null;
    private static Method getPlayerTotalTradesMethod = null;
    private static Method getPlayerDiscountTierMethod = null;
    private static boolean featuresAvailable = false;
    private static boolean initialized = false;

    /**
     * Initialize reflection references to Tharidia Features
     */
    private static void initializeFeatures() {
        if (initialized) return;
        initialized = true;

        try {
            if (!ModList.get().isLoaded("tharidiafeatures")) {
                TharidiaThings.LOGGER.info("Tharidia Features not loaded, using default tax rates");
                return;
            }

            integrationClass = Class.forName("com.THproject.tharidia_features.integration.TharidiaThingsIntegration");
            calculateTaxRateMethod = integrationClass.getMethod("calculateTaxRate", UUID.class, int.class, List.class);
            calculateTaxAmountMethod = integrationClass.getMethod("calculateTaxAmount", int.class, double.class);
            recordCompletedTradeMethod = integrationClass.getMethod("recordCompletedTrade",
                UUID.class, UUID.class, UUID.class, String.class, String.class,
                int.class, int.class, double.class, double.class, int.class);
            getPlayerTotalTradesMethod = integrationClass.getMethod("getPlayerTotalTrades", UUID.class);
            getPlayerDiscountTierMethod = integrationClass.getMethod("getPlayerDiscountTier", UUID.class);

            featuresAvailable = true;
            TharidiaThings.LOGGER.info("Tharidia Features integration initialized - Dynamic tax rates enabled");
        } catch (ClassNotFoundException e) {
            TharidiaThings.LOGGER.info("Tharidia Features integration class not found, using default tax rates");
        } catch (NoSuchMethodException e) {
            TharidiaThings.LOGGER.warn("Tharidia Features integration methods not found: {}", e.getMessage());
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to initialize Tharidia Features integration", e);
        }
    }

    /**
     * Calculate dynamic tax rate from Tharidia Features.
     * Falls back to default rate if Features mod is not available.
     *
     * @param receiverUUID Player receiving currency
     * @param currencyAmount Total currency amount
     * @param tradedItems Items being traded for the currency
     * @param defaultRate Default tax rate to use if Features unavailable
     * @return Tax rate (0.0 to 1.0)
     */
    public static double getDynamicTaxRate(UUID receiverUUID, int currencyAmount, List<ItemStack> tradedItems, double defaultRate) {
        initializeFeatures();

        if (!featuresAvailable || calculateTaxRateMethod == null) {
            return defaultRate;
        }

        try {
            double rate = (double) calculateTaxRateMethod.invoke(null, receiverUUID, currencyAmount, tradedItems);
            if (rate < 0) {
                return defaultRate; // -1 signals to use default
            }
            return rate;
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to get dynamic tax rate, using default", e);
            return defaultRate;
        }
    }

    /**
     * Calculate tax amount using Tharidia Features.
     *
     * @param currencyAmount Total currency
     * @param taxRate Tax rate
     * @return Tax amount
     */
    public static int getDynamicTaxAmount(int currencyAmount, double taxRate) {
        initializeFeatures();

        if (!featuresAvailable || calculateTaxAmountMethod == null) {
            return (int) Math.floor(currencyAmount * taxRate);
        }

        try {
            return (int) calculateTaxAmountMethod.invoke(null, currencyAmount, taxRate);
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to calculate tax amount", e);
            return (int) Math.floor(currencyAmount * taxRate);
        }
    }

    /**
     * Record completed trade with Tharidia Features for statistics.
     */
    public static void recordCompletedTrade(
            UUID transactionId,
            UUID player1UUID, UUID player2UUID,
            String player1Name, String player2Name,
            int player1CurrencyReceived, int player2CurrencyReceived,
            double player1TaxRate, double player2TaxRate,
            int totalItemsTraded) {
        initializeFeatures();

        if (!featuresAvailable || recordCompletedTradeMethod == null) {
            return;
        }

        try {
            recordCompletedTradeMethod.invoke(null,
                transactionId, player1UUID, player2UUID,
                player1Name, player2Name,
                player1CurrencyReceived, player2CurrencyReceived,
                player1TaxRate, player2TaxRate,
                totalItemsTraded);
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to record completed trade", e);
        }
    }

    /**
     * Get player's total trades count.
     */
    public static int getPlayerTotalTrades(UUID playerUUID) {
        initializeFeatures();

        if (!featuresAvailable || getPlayerTotalTradesMethod == null) {
            return 0;
        }

        try {
            return (int) getPlayerTotalTradesMethod.invoke(null, playerUUID);
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to get player total trades", e);
            return 0;
        }
    }

    /**
     * Get player's discount tier (0-5).
     */
    public static int getPlayerDiscountTier(UUID playerUUID) {
        initializeFeatures();

        if (!featuresAvailable || getPlayerDiscountTierMethod == null) {
            return 0;
        }

        try {
            return (int) getPlayerDiscountTierMethod.invoke(null, playerUUID);
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to get player discount tier", e);
            return 0;
        }
    }

    /**
     * Check if Tharidia Features dynamic tax is available.
     */
    public static boolean isDynamicTaxAvailable() {
        initializeFeatures();
        return featuresAvailable;
    }
}
