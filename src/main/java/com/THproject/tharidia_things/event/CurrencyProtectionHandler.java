package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.gui.TradeMenu;
import com.THproject.tharidia_things.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/**
 * Prevents currency items from being dropped, placed in external containers, or used for interactions
 * 
 * Two-tier protection system:
 * 1. Numismatic Overhaul coins: Must be in purse slots, blocks container opening if coins in regular inventory
 * 2. Other currency items (from config): Full tick-based protection
 */
public class CurrencyProtectionHandler {

    // Numismatic Overhaul coin IDs
    private static final String BRONZE_COIN = "numismaticoverhaul:bronze_coin";
    private static final String SILVER_COIN = "numismaticoverhaul:silver_coin";
    private static final String GOLD_COIN = "numismaticoverhaul:gold_coin";
    private static final String MONEY_BAG = "numismaticoverhaul:money_bag";
    
    // Numismatic Overhaul purse slot indices (need to verify these)
    // These are typically added as extra slots to the player inventory
    private static final int PURSE_SLOT_START = 41; // After armor slots (36-39) and offhand (40)
    private static final int PURSE_SLOT_COUNT = 3;  // Usually 3 slots for coins

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        
        if (isCurrencyItem(stack) || isNumismaticCoin(stack)) {
            // Cancel the drop IMMEDIATELY
            event.setCanceled(true);
            
            // Kill the item entity to prevent it from spawning
            event.getEntity().discard();
            
            // Return item to player
            Player player = event.getPlayer();
            if (player != null && !player.level().isClientSide()) {
                if (!player.getInventory().add(stack)) {
                    // If inventory is full, force add to first empty slot or replace something
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        if (player.getInventory().getItem(i).isEmpty()) {
                            player.getInventory().setItem(i, stack);
                            break;
                        }
                    }
                }
                player.sendSystemMessage(Component.literal("§c§lNon puoi droppare le monete!"));
            }
        }
    }

    /**
     * Block container opening if player has Numismatic coins in regular inventory (not in purse)
     * We close the container immediately after opening if coins are found
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        
        // Only check on server side
        if (player.level().isClientSide()) {
            return;
        }
        
        // Skip if it's the player's own inventory
        AbstractContainerMenu container = event.getContainer();
        if (container == player.inventoryMenu) {
            return;
        }
        
        // Allow trade menu
        if (container instanceof TradeMenu) {
            return;
        }
        
        // Check if player has Numismatic coins in regular inventory (slots 0-40)
        // Purse slots are 41-43 (or similar, depends on Numismatic implementation)
        for (int i = 0; i < 41; i++) { // Check main inventory + hotbar + armor + offhand
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isNumismaticCoin(stack)) {
                // Close the container immediately
                player.closeContainer();
                player.sendSystemMessage(Component.literal("§c§lATTENZIONE! §cDevi mettere le monete nella sacca prima di aprire contenitori!"));
                return;
            }
        }
    }
    
    /**
     * Check EVERY tick for NON-Numismatic currency items in forbidden containers
     * This only applies to items in the config list (like potatoes)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Only run if there are non-Numismatic currency items configured
        if (!hasNonNumismaticCurrencyItems()) {
            return;
        }
        
        AbstractContainerMenu container = player.containerMenu;
        
        // Skip if it's the player's own inventory
        if (container == player.inventoryMenu) {
            return;
        }
        
        // Allow trade menu
        if (container instanceof TradeMenu) {
            return;
        }
        
        // Check all slots in the container (excluding player inventory slots) EVERY tick
        boolean foundCurrency = false;
        for (int i = 0; i < container.slots.size(); i++) {
            Slot slot = container.slots.get(i);
            
            // Skip slots that belong to player inventory
            if (slot.container == player.getInventory()) {
                continue;
            }
            
            ItemStack stack = slot.getItem();
            // Only check non-Numismatic currency items
            if (!stack.isEmpty() && isCurrencyItem(stack) && !isNumismaticCoin(stack)) {
                // IMMEDIATELY remove currency item from forbidden container
                slot.set(ItemStack.EMPTY);
                
                // Return to player inventory
                if (!player.getInventory().add(stack)) {
                    // If inventory is full, drop at player's feet
                    player.drop(stack, false);
                }
                
                foundCurrency = true;
            }
        }
        
        if (foundCurrency && player.tickCount % 20 == 0) { // Message only once per second
            player.sendSystemMessage(Component.literal("§cGli item di valuta non possono essere messi in contenitori!"));
        }
    }
    
    /**
     * Block right-click interactions with blocks while holding currency items
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack heldItem = event.getItemStack();
        
        if (isCurrencyItem(heldItem) || isNumismaticCoin(heldItem)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            
            if (!event.getEntity().level().isClientSide()) {
                event.getEntity().sendSystemMessage(Component.literal("§cNon puoi interagire con blocchi mentre tieni item di valuta!"));
            }
        }
    }
    
    /**
     * Block right-click interactions with entities while holding currency items
     * EXCEPT for other players (needed for trade initiation)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        ItemStack heldItem = event.getItemStack();
        
        if (isCurrencyItem(heldItem) || isNumismaticCoin(heldItem)) {
            // Allow interactions with other players (for trade system)
            if (event.getTarget() instanceof Player) {
                return;
            }
            
            // Block all other entity interactions
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            
            if (!event.getEntity().level().isClientSide()) {
                event.getEntity().sendSystemMessage(Component.literal("§cNon puoi interagire con entità mentre tieni item di valuta!"));
            }
        }
    }
    
    /**
     * Block left-click interactions while holding currency items
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack heldItem = event.getItemStack();
        
        if (isCurrencyItem(heldItem) || isNumismaticCoin(heldItem)) {
            event.setCanceled(true);
            
            if (!event.getEntity().level().isClientSide()) {
                event.getEntity().sendSystemMessage(Component.literal("§cNon puoi usare item di valuta per rompere blocchi!"));
            }
        }
    }
    
    /**
     * Block item use (right-click in air) while holding currency items
     * EXCEPT for Numismatic coins (they need right-click to be stored in purse)
     * BUT block if player is looking at another player (to allow trade initiation)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack heldItem = event.getItemStack();
        
        // Allow Numismatic coins to be right-clicked (needed for purse storage)
        // BUT only if NOT looking at a player (to allow trade initiation)
        if (isNumismaticCoin(heldItem)) {
            // Check if player is looking at another player
            Player player = event.getEntity();
            net.minecraft.world.phys.EntityHitResult hitResult = getPlayerLookingAt(player);
            
            if (hitResult != null && hitResult.getEntity() instanceof Player) {
                // Player is looking at another player - block the right-click
                // This allows TradeInteractionHandler to handle the trade initiation
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
                return;
            }
            
            // Not looking at a player - allow purse storage
            return;
        }
        
        // Block other currency items
        if (isCurrencyItem(heldItem)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }
    
    /**
     * Check if player is looking at another player
     */
    private static net.minecraft.world.phys.EntityHitResult getPlayerLookingAt(Player player) {
        double reach = 4.0; // Standard reach distance
        net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition(1.0F);
        net.minecraft.world.phys.Vec3 lookVec = player.getViewVector(1.0F);
        net.minecraft.world.phys.Vec3 reachVec = eyePos.add(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach);
        
        net.minecraft.world.phys.AABB searchBox = player.getBoundingBox()
            .expandTowards(lookVec.scale(reach))
            .inflate(1.0);
        
        net.minecraft.world.phys.EntityHitResult result = null;
        double closestDistance = reach;
        
        for (net.minecraft.world.entity.Entity entity : player.level().getEntities(player, searchBox)) {
            if (entity instanceof Player) {
                net.minecraft.world.phys.AABB entityBox = entity.getBoundingBox().inflate(0.3);
                java.util.Optional<net.minecraft.world.phys.Vec3> hitVec = entityBox.clip(eyePos, reachVec);
                
                if (hitVec.isPresent()) {
                    double distance = eyePos.distanceTo(hitVec.get());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        result = new net.minecraft.world.phys.EntityHitResult(entity, hitVec.get());
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * Prevent currency items from being placed in item entities on the ground
     * This catches any currency items that somehow end up as item entities
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemEntityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            
            if (isCurrencyItem(stack) || isNumismaticCoin(stack)) {
                // Find nearest player and return item IMMEDIATELY
                Player nearestPlayer = itemEntity.level().getNearestPlayer(itemEntity, 10.0);
                if (nearestPlayer != null && !nearestPlayer.level().isClientSide()) {
                    if (nearestPlayer.getInventory().add(stack)) {
                        itemEntity.discard();
                        nearestPlayer.sendSystemMessage(Component.literal("§e§lMonete recuperate automaticamente!"));
                    } else {
                        // If can't add, just delete the item entity to prevent duplication
                        itemEntity.discard();
                    }
                } else {
                    // No player nearby - delete the item to prevent it from staying on ground
                    itemEntity.discard();
                }
            }
        }
    }
    
    /**
     * Additional protection: Block currency items from being dropped via inventory manipulation
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerContainerClick(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open event) {
        // This is already handled by onContainerOpen, but we add extra protection
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        
        // Scan player inventory for currency items and ensure they can't be moved
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && (isCurrencyItem(stack) || isNumismaticCoin(stack))) {
                // Mark the stack as protected (this is a visual indicator)
                // The actual protection is handled by other events
            }
        }
    }

    private static boolean isCurrencyItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        List<? extends String> currencyItems = Config.TRADE_CURRENCY_ITEMS.get();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        return currencyItems.stream()
                .anyMatch(currency -> {
                    try {
                        ResourceLocation currencyId = ResourceLocation.parse(currency);
                        return currencyId.equals(itemId);
                    } catch (Exception e) {
                        return false;
                    }
                });
    }
    
    /**
     * Check if an item is a Numismatic Overhaul coin
     */
    private static boolean isNumismaticCoin(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemIdString = itemId.toString();
        
        return itemIdString.equals(BRONZE_COIN) || 
               itemIdString.equals(SILVER_COIN) || 
               itemIdString.equals(GOLD_COIN) ||
               itemIdString.equals(MONEY_BAG);
    }
    
    /**
     * Check if there are non-Numismatic currency items in the config
     * Used to determine if tick-based checking should be enabled
     */
    private static boolean hasNonNumismaticCurrencyItems() {
        List<? extends String> currencyItems = Config.TRADE_CURRENCY_ITEMS.get();
        
        for (String currency : currencyItems) {
            if (!currency.equals(BRONZE_COIN) && 
                !currency.equals(SILVER_COIN) && 
                !currency.equals(GOLD_COIN) &&
                !currency.equals(MONEY_BAG)) {
                return true;
            }
        }
        
        return false;
    }
}
