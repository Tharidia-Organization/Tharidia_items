package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.entity.HotIronAnvilEntity;
import com.tharidia.tharidia_things.item.PinzaItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Handles smithing interactions, particularly hammer strikes on hot iron
 */
public class SmithingHandler {
    
    private static final String RUSTIC_HAMMER_ID = "rustic_engineer:rustic_hammer";
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack heldItem = event.getItemStack();
        
        // Check if player is holding rustic engineer hammer
        if (!isRusticHammer(heldItem)) {
            return;
        }
        
        TharidiaThings.LOGGER.debug("Hammer detected at: " + pos);
        
        // Check if clicking on marker block directly
        BlockPos checkPos = pos;
        boolean isMarkerBlock = level.getBlockState(pos).is(TharidiaThings.HOT_IRON_MARKER.get());
        
        // If clicking anvil, check above for marker block
        if (!isMarkerBlock && level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.AnvilBlock) {
            checkPos = pos.above();
            isMarkerBlock = level.getBlockState(checkPos).is(TharidiaThings.HOT_IRON_MARKER.get());
            TharidiaThings.LOGGER.debug("Clicked anvil, checking above: " + checkPos);
        }
        
        if (!isMarkerBlock) {
            TharidiaThings.LOGGER.debug("No hot iron marker block found");
            return;
        }
        
        BlockEntity blockEntity = level.getBlockEntity(checkPos);
        
        if (blockEntity instanceof HotIronAnvilEntity hotIronEntity) {
            if (!hotIronEntity.isFinished()) {
                // SERVER SIDE: Open GUI on first strike, but only once
                if (!level.isClientSide) {
                    if (hotIronEntity.getHammerStrikes() == 0 && !hotIronEntity.hasGuiBeenOpened()) {
                        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                            hotIronEntity.setGuiOpened(true);
                            openComponentSelectionMenu(serverPlayer, checkPos);
                        }
                    }
                    
                    hotIronEntity.onHammerStrike(event.getEntity());
                    
                    // Damage the hammer
                    if (event.getEntity() instanceof ServerPlayer player) {
                        heldItem.hurtAndBreak(1, player, player.getEquipmentSlotForItem(heldItem));
                    }
                }
                
                // Cancel event on BOTH sides to prevent other interactions
                event.setCanceled(true);
            }
        }
    }
    
    private static boolean isRusticHammer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return RUSTIC_HAMMER_ID.equals(itemId);
    }
    
    private static void openComponentSelectionMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (containerId, playerInventory, p) -> new com.tharidia.tharidia_things.gui.ComponentSelectionMenu(
                containerId, playerInventory, pos
            ),
            net.minecraft.network.chat.Component.translatable("gui.tharidiathings.component_selection")
        ), pos);
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerInteractBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack heldItem = event.getItemStack();
        var blockState = level.getBlockState(pos);
        
        // Check if interacting with slag:table
        String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        if (!blockId.equals("slag:table")) {
            return;
        }
        
        // Check if the table contains hot iron
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof net.minecraft.world.Container container) {
            boolean hasHotIron = false;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slotItem = container.getItem(i);
                if (!slotItem.isEmpty() && slotItem.is(TharidiaThings.HOT_IRON.get())) {
                    hasHotIron = true;
                    break;
                }
            }
            
            if (hasHotIron) {
                TharidiaThings.LOGGER.info("HOT IRON DETECTED IN TABLE!");
                
                // ALWAYS cancel the event when hot iron is present to prevent table from giving item
                event.setCanceled(true);
                TharidiaThings.LOGGER.info("EVENT CANCELED");
                
                // If using Pinza with empty hands, manually grab the hot iron
                if (heldItem.getItem() instanceof PinzaItem pinza) {
                    PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(heldItem);
                    TharidiaThings.LOGGER.info("PINZA DETECTED! Holding type: " + holdingType);
                    
                    if (holdingType == PinzaItem.HoldingType.NONE) {
                        // Manually grab hot iron from table
                        if (!level.isClientSide && event.getEntity() != null) {
                            TharidiaThings.LOGGER.info("CALLING grabHotIronWithPinza");
                            grabHotIronWithPinza(container, pos, level, event.getEntity(), heldItem);
                        }
                        return;
                    }
                }
                
                // Show error for all other cases
                if (!level.isClientSide && event.getEntity() != null) {
                    event.getEntity().displayClientMessage(
                        Component.translatable("item.tharidiathings.hot_iron.need_pinza"), 
                        true
                    );
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        // Prevent hot iron from being obtained through any crafting/smelting
        if (event.getCrafting().is(TharidiaThings.HOT_IRON.get())) {
            // Remove the hot iron from the result
            event.getCrafting().shrink(event.getCrafting().getCount());
            
            if (event.getEntity() != null && !event.getEntity().level().isClientSide) {
                event.getEntity().displayClientMessage(
                    Component.translatable("item.tharidiathings.hot_iron.too_hot"),
                    true
                );
            }
        }
    }
    
    private static void grabHotIronWithPinza(net.minecraft.world.Container container, BlockPos pos, 
                                             Level level, Player player, ItemStack pinzaStack) {
        // Find and remove hot iron from container
        int hotIronSlot = -1;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotStack = container.getItem(i);
            if (slotStack.is(TharidiaThings.HOT_IRON.get())) {
                hotIronSlot = i;
                break;
            }
        }
        
        if (hotIronSlot == -1) {
            player.displayClientMessage(Component.translatable("item.tharidiathings.pinza.no_hot_iron"), true);
            return;
        }
        
        // Remove the hot iron from the table
        container.setItem(hotIronSlot, ItemStack.EMPTY);
        container.setChanged();
        
        // Update the Pinza to hold hot iron
        PinzaItem.setHolding(pinzaStack, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
        
        // Damage the Pinza
        pinzaStack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        
        // Play sound and show message
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("item.tharidiathings.pinza.grabbed_hot_iron"), true);
    }
    
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)  
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        
        // Check if player has hot iron in inventory while holding Pinza
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(TharidiaThings.HOT_IRON.get())) {
                // Check if player is holding Pinza
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();
                
                if (mainHand.getItem() instanceof PinzaItem) {
                    PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(mainHand);
                    
                    if (holdingType == PinzaItem.HoldingType.NONE) {
                        // Remove from inventory
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                        
                        // Set Pinza to hold it
                        PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                        
                        // Damage Pinza
                        mainHand.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                        
                        player.displayClientMessage(Component.translatable("item.tharidiathings.pinza.grabbed_hot_iron"), true);
                        return;
                    }
                } else if (offHand.getItem() instanceof PinzaItem) {
                    PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(offHand);
                    
                    if (holdingType == PinzaItem.HoldingType.NONE) {
                        // Remove from inventory
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                        
                        // Set Pinza to hold it
                        PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                        
                        // Damage Pinza
                        offHand.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                        
                        player.displayClientMessage(Component.translatable("item.tharidiathings.pinza.grabbed_hot_iron"), true);
                        return;
                    }
                }
                
                // If no Pinza or Pinza already holding something, the inventoryTick will remove it
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        // Prevent hot iron item entities from spawning in the world
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (stack.is(TharidiaThings.HOT_IRON.get())) {
                TharidiaThings.LOGGER.warn("HOT IRON ITEM ENTITY SPAWNING! Canceling.");
                
                // Try to find nearest player with empty Pinza
                if (!event.getLevel().isClientSide()) {
                    Player nearestPlayer = event.getLevel().getNearestPlayer(
                        itemEntity, 5.0 // Within 5 blocks
                    );
                    
                    if (nearestPlayer != null) {
                        ItemStack mainHand = nearestPlayer.getMainHandItem();
                        ItemStack offHand = nearestPlayer.getOffhandItem();
                        
                        // Check main hand
                        if (mainHand.getItem() instanceof PinzaItem) {
                            PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(mainHand);
                            if (holdingType == PinzaItem.HoldingType.NONE) {
                                TharidiaThings.LOGGER.info("Found player with empty Pinza! Transferring...");
                                PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                                mainHand.hurtAndBreak(1, nearestPlayer, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                                nearestPlayer.displayClientMessage(
                                    Component.translatable("item.tharidiathings.pinza.grabbed_hot_iron"), 
                                    true
                                );
                                event.setCanceled(true);
                                return;
                            }
                        }
                        
                        // Check off hand
                        if (offHand.getItem() instanceof PinzaItem) {
                            PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(offHand);
                            if (holdingType == PinzaItem.HoldingType.NONE) {
                                TharidiaThings.LOGGER.info("Found player with empty Pinza in off hand! Transferring...");
                                PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                                offHand.hurtAndBreak(1, nearestPlayer, net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                                nearestPlayer.displayClientMessage(
                                    Component.translatable("item.tharidiathings.pinza.grabbed_hot_iron"), 
                                    true
                                );
                                event.setCanceled(true);
                                return;
                            }
                        }
                        
                        // If player doesn't have empty Pinza, show message
                        nearestPlayer.displayClientMessage(
                            Component.translatable("item.tharidiathings.hot_iron.need_pinza"), 
                            true
                        );
                    }
                }
                
                // Cancel the spawn to prevent hot iron from being on ground
                event.setCanceled(true);
                TharidiaThings.LOGGER.info("Hot iron item entity spawn canceled.");
            }
        }
    }
}
