package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.entity.IHotMetalAnvilEntity;
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
import net.minecraft.world.level.block.state.BlockState;
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
        
        
        // Check if clicking on marker block directly
        BlockPos checkPos = pos;
        boolean isIronMarker = level.getBlockState(pos).is(TharidiaThings.HOT_IRON_MARKER.get());
        boolean isGoldMarker = level.getBlockState(pos).is(TharidiaThings.HOT_GOLD_MARKER.get());
        boolean isCopperMarker = level.getBlockState(pos).is(TharidiaThings.HOT_COPPER_MARKER.get());
        boolean isMarkerBlock = isIronMarker || isGoldMarker || isCopperMarker;
        
        // If clicking anvil, check above for marker block
        if (!isMarkerBlock && level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.AnvilBlock) {
            checkPos = pos.above();
            isIronMarker = level.getBlockState(checkPos).is(TharidiaThings.HOT_IRON_MARKER.get());
            isGoldMarker = level.getBlockState(checkPos).is(TharidiaThings.HOT_GOLD_MARKER.get());
            isCopperMarker = level.getBlockState(checkPos).is(TharidiaThings.HOT_COPPER_MARKER.get());
            isMarkerBlock = isIronMarker || isGoldMarker || isCopperMarker;
        }
        
        if (!isMarkerBlock) {
            return;
        }
        
        BlockEntity blockEntity = level.getBlockEntity(checkPos);
        
        // Unified handling for all hot metal types (iron, gold, etc.)
        if (blockEntity instanceof IHotMetalAnvilEntity hotMetalEntity) {
            if (!hotMetalEntity.isFinished()) {
                // SERVER SIDE: Open GUI on first strike, but only once
                if (!level.isClientSide) {
                    if (hotMetalEntity.getHammerStrikes() == 0 && !hotMetalEntity.hasGuiBeenOpened()) {
                        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                            hotMetalEntity.setGuiOpened(true);
                            openComponentSelectionMenu(serverPlayer, checkPos);
                        }
                    }
                    
                    hotMetalEntity.onHammerStrike(event.getEntity());
                    
                    // Damage the hammer
                    if (event.getEntity() instanceof ServerPlayer player) {
                        heldItem.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                    }
                }
                
                // CLIENT AND SERVER: Show hammer swing animation
                event.getEntity().swing(InteractionHand.MAIN_HAND);
                
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
        
        // Check if the table contains hot iron or hot gold
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof net.minecraft.world.Container container) {
            boolean hasHotMetal = false;
            boolean isIron = false;
            boolean isGold = false;
            boolean isCopper = false;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slotItem = container.getItem(i);
                if (!slotItem.isEmpty()){
                    if (slotItem.is(TharidiaThings.HOT_IRON.get())) {
                        hasHotMetal = true;
                        isIron = true;
                        break;
                    } else if (slotItem.is(TharidiaThings.HOT_GOLD.get())) {
                        hasHotMetal = true;
                        isGold = true;
                        break;
                    } else if (slotItem.is(TharidiaThings.HOT_COPPER.get())) {
                        hasHotMetal = true;
                        isCopper = true;
                        break;
                    }
                }
            }
            
            if (hasHotMetal) {
                // ALWAYS cancel the event when hot iron is present to prevent table from giving item
                event.setCanceled(true);
                
                // If using Pinza with empty hands, manually grab the hot metal
                if (heldItem.getItem() instanceof PinzaItem pinza) {
                    PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(heldItem);
                    
                    if (holdingType == PinzaItem.HoldingType.NONE) {
                        // Manually grab hot metal from table
                        if (!level.isClientSide) {
                            if (isIron) {
                                grabHotIronWithPinza(container, pos, level, event.getEntity(), heldItem);
                            } else if (isGold) {
                                grabHotGoldWithPinza(container, pos, level, event.getEntity(), heldItem);
                            } else if (isCopper) {
                                grabHotCopperWithPinza(container, pos, level, event.getEntity(), heldItem);
                            }
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
        
        // Prevent hot gold from being obtained through any crafting/smelting
        if (event.getCrafting().is(TharidiaThings.HOT_GOLD.get())) {
            // Remove the hot gold from the result
            event.getCrafting().shrink(event.getCrafting().getCount());
            
            if (event.getEntity() != null && !event.getEntity().level().isClientSide) {
                event.getEntity().displayClientMessage(
                    Component.translatable("item.tharidiathings.hot_gold.too_hot"),
                    true
                );
            }
        }
        
        // Prevent hot copper from being obtained through any crafting/smelting
        if (event.getCrafting().is(TharidiaThings.HOT_COPPER.get())) {
            // Remove the hot copper from the result
            event.getCrafting().shrink(event.getCrafting().getCount());
            
            if (event.getEntity() != null && !event.getEntity().level().isClientSide) {
                event.getEntity().displayClientMessage(
                    Component.translatable("item.tharidiathings.hot_copper.too_hot"),
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
        damagePinza(pinzaStack, player);
        
        // Play sound
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
    
    private static void grabHotGoldWithPinza(net.minecraft.world.Container container, BlockPos pos, 
                                             Level level, Player player, ItemStack pinzaStack) {
        // Find and remove hot gold from container
        int hotGoldSlot = -1;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotStack = container.getItem(i);
            if (slotStack.is(TharidiaThings.HOT_GOLD.get())) {
                hotGoldSlot = i;
                break;
            }
        }
        
        if (hotGoldSlot == -1) {
            player.displayClientMessage(Component.translatable("item.tharidiathings.pinza.no_hot_gold"), true);
            return;
        }
        
        // Remove the hot gold from the table
        container.setItem(hotGoldSlot, ItemStack.EMPTY);
        container.setChanged();
        
        // Update the Pinza to hold hot gold
        PinzaItem.setHolding(pinzaStack, PinzaItem.HoldingType.HOT_GOLD, "hot_gold");
        
        // Damage the Pinza
        damagePinza(pinzaStack, player);
        
        // Play sound
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
    
    private static void grabHotCopperWithPinza(net.minecraft.world.Container container, BlockPos pos, 
                                               Level level, Player player, ItemStack pinzaStack) {
        // Find and remove hot copper from container
        int hotCopperSlot = -1;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotStack = container.getItem(i);
            if (slotStack.is(TharidiaThings.HOT_COPPER.get())) {
                hotCopperSlot = i;
                break;
            }
        }
        
        if (hotCopperSlot == -1) {
            player.displayClientMessage(Component.translatable("item.tharidiathings.pinza.no_hot_copper"), true);
            return;
        }
        
        // Remove the hot copper from the table
        container.setItem(hotCopperSlot, ItemStack.EMPTY);
        container.setChanged();
        
        // Update the Pinza to hold hot copper
        PinzaItem.setHolding(pinzaStack, PinzaItem.HoldingType.HOT_COPPER, "hot_copper");
        
        // Damage the Pinza
        damagePinza(pinzaStack, player);
        
        // Play sound
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAnvilInteraction(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ItemStack heldItem = event.getItemStack();
        
        // Prevent anvil GUI opening if player is holding a Pinza, but allow item use
        if (state.getBlock() instanceof net.minecraft.world.level.block.AnvilBlock) {
            if (heldItem.getItem() instanceof PinzaItem) {
                // Check if player is sneaking - if so, allow normal anvil interaction
                if (event.getEntity() != null && event.getEntity().isCrouching()) {
                    return;
                }
                
                // Deny block use (prevents GUI) but allow item use (allows Pinza actions)
                event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)  
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        
        // Check if player has hot iron, hot gold, or hot copper in inventory while holding Pinza
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            boolean isHotIron = stack.is(TharidiaThings.HOT_IRON.get());
            boolean isHotGold = stack.is(TharidiaThings.HOT_GOLD.get());
            boolean isHotCopper = stack.is(TharidiaThings.HOT_COPPER.get());
            
            if (isHotIron || isHotGold || isHotCopper) {
                // Check if player is holding Pinza
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();
                
                if (mainHand.getItem() instanceof PinzaItem) {
                    PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(mainHand);
                    
                    if (holdingType == PinzaItem.HoldingType.NONE) {
                        // Remove from inventory
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                        
                        // Set Pinza to hold it
                        if (isHotIron) {
                            PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                        } else if (isHotGold) {
                            PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_GOLD, "hot_gold");
                        } else if (isHotCopper) {
                            PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_COPPER, "hot_copper");
                        }
                        
                        // Damage Pinza
                        damagePinza(mainHand, player);
                        
                        return;
                    }
                } else if (offHand.getItem() instanceof PinzaItem) {
                    PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(offHand);
                    
                    if (holdingType == PinzaItem.HoldingType.NONE) {
                        // Remove from inventory
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                        
                        // Set Pinza to hold it
                        if (isHotIron) {
                            PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                        } else if (isHotGold) {
                            PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_GOLD, "hot_gold");
                        } else if (isHotCopper) {
                            PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_COPPER, "hot_copper");
                        }
                        
                        // Damage Pinza
                        damagePinza(offHand, player);
                        
                        return;
                    }
                }
                
                // If no Pinza or Pinza already holding something, the inventoryTick will remove it
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        // Prevent hot iron, hot gold, and hot copper item entities from spawning in the world
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            boolean isHotIron = stack.is(TharidiaThings.HOT_IRON.get());
            boolean isHotGold = stack.is(TharidiaThings.HOT_GOLD.get());
            boolean isHotCopper = stack.is(TharidiaThings.HOT_COPPER.get());
            
            if (isHotIron || isHotGold || isHotCopper) {
                
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
                                if (isHotIron) {
                                    PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                                } else if (isHotGold) {
                                    PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_GOLD, "hot_gold");
                                } else if (isHotCopper) {
                                    PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_COPPER, "hot_copper");
                                }
                                damagePinza(mainHand, nearestPlayer);
                                event.setCanceled(true);
                                return;
                            }
                        }
                        
                        // Check off hand
                        if (offHand.getItem() instanceof PinzaItem) {
                            PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(offHand);
                            if (holdingType == PinzaItem.HoldingType.NONE) {
                                if (isHotIron) {
                                    PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                                } else if (isHotGold) {
                                    PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_GOLD, "hot_gold");
                                } else if (isHotCopper) {
                                    PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_COPPER, "hot_copper");
                                }
                                damagePinza(offHand, nearestPlayer);
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
                
                // Cancel the spawn to prevent hot metal from being on ground
                event.setCanceled(true);
            }
        }
    }
    
    private static void damagePinza(ItemStack stack, Player player) {
        // Directly apply damage to the item
        int currentDamage = stack.getDamageValue();
        int newDamage = currentDamage + 1;
        int maxDurability = 480; // Match PinzaItem.MAX_DURABILITY
        
        if (newDamage >= maxDurability) {
            // Item is broken - remove it
            stack.shrink(1);
        } else {
            // Apply damage
            stack.setDamageValue(newDamage);
        }
    }
}
