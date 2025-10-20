package com.tharidia.tharidia_things.item;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.entity.IHotMetalAnvilEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Pinza (Tongs) - Used to handle hot iron and smithing components.
 * Has 300 durability and can hold different items (hot iron, components).
 */
public class PinzaItem extends Item {
    
    private static final int MAX_DURABILITY = 300;
    private static final String TAG_HOLDING = "HoldingItem";
    private static final String TAG_HOLDING_TYPE = "HoldingType";
    private static final String TAG_MATERIAL = "Material";
    
    public enum HoldingType {
        NONE,
        HOT_IRON,
        HOT_GOLD,
        HOT_COPPER,
        COMPONENT
    }
    
    public PinzaItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY));
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(pos);
        
        if (player == null) return InteractionResult.PASS;
        
        HoldingType holdingType = getHoldingType(stack);
        
        // Check if clicking on a slag casting table with hot iron
        // For now, we use a simplified check - the player manually places hot iron in the table
        // and uses the pinza to pick it up
        if (holdingType == HoldingType.NONE) {
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (blockId.equals("slag:table")) {
                // Try to grab hot iron from the table
                if (tryGrabHotIron(level, pos, player, stack)) {
                    // Return sidedSuccess to properly handle client/server and prevent block interaction
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                // If no hot iron found, allow normal table interaction
                return InteractionResult.PASS;
            }
        }
        
        // Check if clicking on top of anvil with hot iron, hot gold, or hot copper
        if ((holdingType == HoldingType.HOT_IRON || holdingType == HoldingType.HOT_GOLD || holdingType == HoldingType.HOT_COPPER) && state.is(Blocks.ANVIL) && context.getClickedFace() == Direction.UP) {
            if (placeHotMetalOnAnvil(level, pos, player, stack, holdingType)) {
                return InteractionResult.SUCCESS;
            }
        }
        
        // Check if clicking on water cauldron with component
        if (holdingType == HoldingType.COMPONENT && state.getBlock() instanceof LayeredCauldronBlock) {
            if (state.getValue(LayeredCauldronBlock.LEVEL) > 0) {
                if (coolComponent(level, pos, player, stack)) {
                    return InteractionResult.SUCCESS;
                }
            }
        }
        
        // Check if clicking on hot iron anvil entity to pick up finished component
        // Check both the clicked position and above (if clicking anvil)
        BlockPos checkPos = pos;
        
        // If clicking anvil, check the marker block above it
        if (state.getBlock() instanceof net.minecraft.world.level.block.AnvilBlock) {
            checkPos = pos.above();
        }
        
        BlockEntity checkEntity = level.getBlockEntity(checkPos);
        if (holdingType == HoldingType.NONE && checkEntity instanceof IHotMetalAnvilEntity hotMetalEntity) {
            if (hotMetalEntity.isFinished()) {
                if (pickupComponent(level, checkPos, player, stack, hotMetalEntity)) {
                    return InteractionResult.SUCCESS;
                }
            } else {
                // Hot metal not finished yet
                if (!level.isClientSide) {
                    player.displayClientMessage(
                        Component.translatable("item.tharidiathings.pinza.not_finished"), 
                        true
                    );
                }
                return InteractionResult.FAIL;
            }
        }
        
        return InteractionResult.PASS;
    }
    
    private boolean tryGrabHotIron(Level level, BlockPos pos, Player player, ItemStack pinzaStack) {
        // Check if the table actually has a hot iron result
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return false;
        
        // Try to access the table's inventory to check for hot iron
        // We need to check if there's actually a hot iron item in the result slot
        try {
            // Access the block entity's container if it has one
            if (blockEntity instanceof net.minecraft.world.Container container) {
                boolean foundHotIron = false;
                int hotIronSlot = -1;
                
                // Search for hot iron in the container
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack slotStack = container.getItem(i);
                    if (slotStack.is(TharidiaThings.HOT_IRON.get())) {
                        foundHotIron = true;
                        hotIronSlot = i;
                        break;
                    }
                }
                
                if (!foundHotIron) {
                    if (!level.isClientSide) {
                        player.displayClientMessage(Component.translatable("item.tharidiathings.pinza.no_hot_iron"), true);
                    }
                    return false;
                }
                
                // Remove the hot iron from BOTH client and server
                // This prevents the table from giving it to the player
                container.setItem(hotIronSlot, ItemStack.EMPTY);
                container.setChanged();
                
                if (!level.isClientSide) {
                    // Set the pinza to hold hot iron (server-side only)
                    setHolding(pinzaStack, HoldingType.HOT_IRON, "hot_iron");
                    damagePinza(pinzaStack, player);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.translatable("item.tharidiathings.pinza.grabbed_hot_iron"), true);
                    
                    // Mark the block entity as changed to sync to client
                    blockEntity.setChanged();
                }
                
                return true;
            }
        } catch (Exception e) {
            // If we can't access the inventory, fail silently
            return false;
        }
        
        return false;
    }
    
    private boolean placeHotMetalOnAnvil(Level level, BlockPos pos, Player player, ItemStack pinzaStack, HoldingType holdingType) {
        BlockPos above = pos.above();
        
        if (!level.getBlockState(above).isAir()) {
            return false;
        }
        
        if (!level.isClientSide) {
            // Clear the pinza's holding
            clearHolding(pinzaStack);
            damagePinza(pinzaStack, player);
            
            // Place the appropriate marker block entity at the position above the anvil
            if (holdingType == HoldingType.HOT_IRON) {
                level.setBlock(above, TharidiaThings.HOT_IRON_MARKER.get().defaultBlockState(), 3);
            } else if (holdingType == HoldingType.HOT_GOLD) {
                level.setBlock(above, TharidiaThings.HOT_GOLD_MARKER.get().defaultBlockState(), 3);
            } else if (holdingType == HoldingType.HOT_COPPER) {
                level.setBlock(above, TharidiaThings.HOT_COPPER_MARKER.get().defaultBlockState(), 3);
            }
            
            level.playSound(null, above, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("item.tharidiathings.pinza.placed_on_anvil"), true);
        }
        
        return true;
    }
    
    private boolean pickupComponent(Level level, BlockPos pos, Player player, ItemStack pinzaStack, IHotMetalAnvilEntity entity) {
        String componentType = entity.getSelectedComponent();
        String materialType = entity.getMaterialType();
        
        if (!level.isClientSide) {
            setHoldingWithMaterial(pinzaStack, HoldingType.COMPONENT, componentType, materialType);
            damagePinza(pinzaStack, player);
            
            // Remove the entity and block
            level.removeBlock(pos, false);
            
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("item.tharidiathings.pinza.picked_component"), true);
        }
        
        return true;
    }
    
    private boolean coolComponent(Level level, BlockPos pos, Player player, ItemStack pinzaStack) {
        String componentId = getHoldingItem(pinzaStack);
        String materialType = getMaterialType(pinzaStack);
        
        if (!level.isClientSide) {
            // Create particles
            ServerLevel serverLevel = (ServerLevel) level;
            for (int i = 0; i < 20; i++) {
                double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5;
                serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, 0.1, 0, 0.05);
            }
            
            // Play sound
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            
            // Lower water level
            BlockState state = level.getBlockState(pos);
            int currentLevel = state.getValue(LayeredCauldronBlock.LEVEL);
            if (currentLevel > 1) {
                level.setBlock(pos, state.setValue(LayeredCauldronBlock.LEVEL, currentLevel - 1), 3);
            } else {
                level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            }
            
            // Clear pinza and drop component
            clearHolding(pinzaStack);
            damagePinza(pinzaStack, player);
            
            // Try to add the component to player's inventory, if full drop it on the ground
            ItemStack componentStack = getComponentStack(componentId, materialType);
            if (!componentStack.isEmpty()) {
                boolean added = player.getInventory().add(componentStack);
                if (!added) {
                    // Inventory is full, drop on the ground
                    ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), componentStack);
                    level.addFreshEntity(itemEntity);
                }
            }
            
            player.displayClientMessage(Component.translatable("item.tharidiathings.pinza.cooled"), true);
        }
        
        return true;
    }
    
    private ItemStack getComponentStack(String componentId, String materialType) {
        return switch (materialType) {
            case "iron" -> switch (componentId) {
                case "lama_lunga" -> new ItemStack(TharidiaThings.LAMA_LUNGA.get());
                case "lama_corta" -> new ItemStack(TharidiaThings.LAMA_CORTA.get());
                case "elsa" -> new ItemStack(TharidiaThings.ELSA.get());
                default -> ItemStack.EMPTY;
            };
            case "gold" -> switch (componentId) {
                case "lama_lunga" -> new ItemStack(TharidiaThings.GOLD_LAMA_LUNGA.get());
                case "lama_corta" -> new ItemStack(TharidiaThings.GOLD_LAMA_CORTA.get());
                // No gold elsa
                default -> ItemStack.EMPTY;
            };
            case "copper" -> switch (componentId) {
                case "lama_lunga" -> new ItemStack(TharidiaThings.COPPER_LAMA_LUNGA.get());
                case "lama_corta" -> new ItemStack(TharidiaThings.COPPER_LAMA_CORTA.get());
                case "elsa" -> new ItemStack(TharidiaThings.COPPER_ELSA.get());
                default -> ItemStack.EMPTY;
            };
            default -> ItemStack.EMPTY;
        };
    }
    
    private void damagePinza(ItemStack stack, Player player) {
        stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
    }
    
    public static void setHolding(ItemStack stack, HoldingType type, String itemId) {
        setHoldingWithMaterial(stack, type, itemId, "iron"); // Default to iron for backward compatibility
    }
    
    public static void setHoldingWithMaterial(ItemStack stack, HoldingType type, String itemId, String materialType) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        customData = customData.update(tag -> {
            tag.putString(TAG_HOLDING_TYPE, type.name());
            tag.putString(TAG_HOLDING, itemId);
            tag.putString(TAG_MATERIAL, materialType);
        });
        stack.set(DataComponents.CUSTOM_DATA, customData);
        
        // Set custom model data for visual representation
        if (type == HoldingType.HOT_IRON) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(1));
        } else if (type == HoldingType.HOT_GOLD) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(3));
        } else if (type == HoldingType.HOT_COPPER) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(4));
        } else if (type == HoldingType.COMPONENT) {
            // Different model data based on component type
            int modelData = switch (itemId) {
                case "lama_lunga" -> 5;
                case "lama_corta" -> 6;
                case "elsa" -> 7;
                default -> 2; // Fallback to generic component model
            };
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(modelData));
        }
    }
    
    public static void clearHolding(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        customData = customData.update(tag -> {
            tag.remove(TAG_HOLDING_TYPE);
            tag.remove(TAG_HOLDING);
            tag.remove(TAG_MATERIAL);
        });
        stack.set(DataComponents.CUSTOM_DATA, customData);
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }
    
    public static HoldingType getHoldingType(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!customData.contains(TAG_HOLDING_TYPE)) {
            return HoldingType.NONE;
        }
        try {
            return HoldingType.valueOf(customData.copyTag().getString(TAG_HOLDING_TYPE));
        } catch (IllegalArgumentException e) {
            return HoldingType.NONE;
        }
    }
    
    public static String getHoldingItem(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!customData.contains(TAG_HOLDING)) {
            return "";
        }
        return customData.copyTag().getString(TAG_HOLDING);
    }
    
    public static String getMaterialType(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!customData.contains(TAG_MATERIAL)) {
            return "iron"; // Default to iron
        }
        return customData.copyTag().getString(TAG_MATERIAL);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        HoldingType type = getHoldingType(stack);
        if (type != HoldingType.NONE) {
            String item = getHoldingItem(stack);
            tooltipComponents.add(Component.translatable("item.tharidiathings.pinza.holding." + item));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
