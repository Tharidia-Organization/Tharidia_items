package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.Config;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.IHotMetalAnvilEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
 * Has 480 durability and can hold different items (hot iron, components).
 */
public class PinzaItem extends Item {

    private static final int MAX_DURABILITY = 480;
    private static final String TAG_HOLDING = "HoldingItem";
    private static final String TAG_HOLDING_TYPE = "HoldingType";
    private static final String TAG_MATERIAL = "Material";
    private static final String TAG_COMPONENT_TIME = "ComponentPickupTime";
    private static final String TAG_EXPIRED = "Expired";
    private static final String TAG_CRUCIBLE_PICKUP_TIME = "CruciblePickupTime";

    public enum HoldingType {
        NONE,
        HOT_IRON,
        HOT_GOLD,
        HOT_COPPER,
        COMPONENT,
        CRUCIBLE_EMPTY,
        CRUCIBLE_IRON,
        CRUCIBLE_GOLD,
        CRUCIBLE_COPPER
    }

    public PinzaItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY));
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return MAX_DURABILITY;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - (float)stack.getDamageValue() * 13.0F / (float)MAX_DURABILITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, ((float)MAX_DURABILITY - (float)stack.getDamageValue()) / (float)MAX_DURABILITY);
        return net.minecraft.util.Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        // Only check every 20 ticks (1 second) for server performance
        if (level.getGameTime() % 20 != 0) return;

        HoldingType type = getHoldingType(stack);

        // Component cooling timer
        if (type == HoldingType.COMPONENT) {
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            long pickupTime = tag.getLong(TAG_COMPONENT_TIME);
            if (pickupTime <= 0) return;

            long coolingTicks = Config.SMITHING_COOLING_TIME.get() * 20L;
            if (level.getGameTime() - pickupTime > coolingTicks) {
                clearHolding(stack);
                if (entity instanceof Player player) {
                    player.displayClientMessage(
                        Component.translatable("item.tharidiathings.pinza.component_cooled"),
                        true
                    );
                    level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 0.5F);
                }
            }
            return;
        }

        // Crucible expiration timer (1 minute = 1200 ticks)
        if (type == HoldingType.CRUCIBLE_IRON || type == HoldingType.CRUCIBLE_GOLD || type == HoldingType.CRUCIBLE_COPPER) {
            if (isExpired(stack)) return;

            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            long cruciblePickup = tag.getLong(TAG_CRUCIBLE_PICKUP_TIME);

            if (cruciblePickup <= 0) {
                // First tick: stamp pickup time
                CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                cd = cd.update(t -> t.putLong(TAG_CRUCIBLE_PICKUP_TIME, level.getGameTime()));
                stack.set(DataComponents.CUSTOM_DATA, cd);
                return;
            }

            if (level.getGameTime() - cruciblePickup > 1200L) {
                setExpired(stack, true);
                if (entity instanceof Player player) {
                    player.displayClientMessage(
                        Component.translatable("item.tharidiathings.pinza.crucible_expired"),
                        true
                    );
                    level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 0.5F);
                }
            }
        }
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

        // Handle expired crucible: right-click any block to dump solidified metal
        if ((holdingType == HoldingType.CRUCIBLE_IRON || holdingType == HoldingType.CRUCIBLE_GOLD
                || holdingType == HoldingType.CRUCIBLE_COPPER) && isExpired(stack)) {
            if (!level.isClientSide) {
                BlockPos dropPos = pos.relative(context.getClickedFace());
                level.addFreshEntity(new ItemEntity(level,
                        dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5,
                        new ItemStack(TharidiaThings.METAL_FRAGMENT.get())));
                setHoldingWithMaterial(stack, HoldingType.CRUCIBLE_EMPTY, "pinza_crucible", "");
                damagePinza(stack, player);
                level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.7f, 0.8f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Check if clicking on a slag casting table with hot iron
        if (holdingType == HoldingType.NONE) {
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (blockId.equals("slag:table")) {
                if (tryGrabHotIron(level, pos, player, stack)) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                return InteractionResult.PASS;
            }
        }

        // Check if clicking on top of any anvil variant with hot metal
        if ((holdingType == HoldingType.HOT_IRON || holdingType == HoldingType.HOT_GOLD || holdingType == HoldingType.HOT_COPPER)
                && state.getBlock() instanceof AnvilBlock && context.getClickedFace() == Direction.UP) {
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
        BlockPos checkPos = pos;
        if (state.getBlock() instanceof AnvilBlock) {
            checkPos = pos.above();
        }

        BlockEntity checkEntity = level.getBlockEntity(checkPos);
        if (holdingType == HoldingType.NONE && checkEntity instanceof IHotMetalAnvilEntity hotMetalEntity) {
            if (hotMetalEntity.isFinished()) {
                // Owner check for pickup
                if (hotMetalEntity.getOwnerUUID() != null && !hotMetalEntity.getOwnerUUID().equals(player.getUUID())) {
                    if (!level.isClientSide) {
                        player.displayClientMessage(
                            Component.translatable("item.tharidiathings.smithing.not_owner"), true
                        );
                    }
                    return InteractionResult.FAIL;
                }
                if (pickupComponent(level, checkPos, player, stack, hotMetalEntity)) {
                    return InteractionResult.SUCCESS;
                }
            } else {
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }

    private boolean tryGrabHotIron(Level level, BlockPos pos, Player player, ItemStack pinzaStack) {
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return false;

        try {
            if (blockEntity instanceof net.minecraft.world.Container container) {
                boolean foundHotIron = false;
                int hotIronSlot = -1;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack slotStack = container.getItem(i);
                    if (slotStack.is(TharidiaThings.HOT_IRON.get())) {
                        foundHotIron = true;
                        hotIronSlot = i;
                        break;
                    }
                }

                if (!foundHotIron) {
                    return false;
                }

                container.setItem(hotIronSlot, ItemStack.EMPTY);
                container.setChanged();

                if (!level.isClientSide) {
                    setHolding(pinzaStack, HoldingType.HOT_IRON, "hot_iron");
                    damagePinza(pinzaStack, player);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
                    blockEntity.setChanged();
                }

                return true;
            }
        } catch (Exception e) {
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
            clearHolding(pinzaStack);
            damagePinza(pinzaStack, player);

            if (holdingType == HoldingType.HOT_IRON) {
                level.setBlock(above, TharidiaThings.HOT_IRON_MARKER.get().defaultBlockState(), 3);
            } else if (holdingType == HoldingType.HOT_GOLD) {
                level.setBlock(above, TharidiaThings.HOT_GOLD_MARKER.get().defaultBlockState(), 3);
            } else if (holdingType == HoldingType.HOT_COPPER) {
                level.setBlock(above, TharidiaThings.HOT_COPPER_MARKER.get().defaultBlockState(), 3);
            }

            // Set owner and placement time on the new entity
            BlockEntity be = level.getBlockEntity(above);
            if (be instanceof IHotMetalAnvilEntity hotMetal) {
                hotMetal.setOwnerUUID(player.getUUID());
                hotMetal.setPlacementTime(level.getGameTime());
            }

            level.playSound(null, above, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        return true;
    }

    private boolean pickupComponent(Level level, BlockPos pos, Player player, ItemStack pinzaStack, IHotMetalAnvilEntity entity) {
        String componentType = entity.getSelectedComponent();
        String materialType = entity.getMaterialType();

        if (!level.isClientSide) {
            setHoldingWithMaterial(pinzaStack, HoldingType.COMPONENT, componentType, materialType);

            // Store pickup time and forge quality
            int forgeQuality = entity.getQualityScore() / 4;
            CustomData cd = pinzaStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            cd = cd.update(tag -> {
                tag.putLong(TAG_COMPONENT_TIME, level.getGameTime());
                tag.putInt("ForgeQuality", forgeQuality);
            });
            pinzaStack.set(DataComponents.CUSTOM_DATA, cd);

            damagePinza(pinzaStack, player);

            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        return true;
    }

    private boolean coolComponent(Level level, BlockPos pos, Player player, ItemStack pinzaStack) {
        String componentId = getHoldingItem(pinzaStack);
        String materialType = getMaterialType(pinzaStack);

        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            for (int i = 0; i < 20; i++) {
                double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5;
                serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, 0.1, 0, 0.05);
            }

            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);

            BlockState state = level.getBlockState(pos);
            int currentLevel = state.getValue(LayeredCauldronBlock.LEVEL);
            if (currentLevel > 1) {
                level.setBlock(pos, state.setValue(LayeredCauldronBlock.LEVEL, currentLevel - 1), 3);
            } else {
                level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            }

            // Read forge quality before clearing pinza data
            CustomData pinzaData = pinzaStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            int forgeQuality = pinzaData.copyTag().getInt("ForgeQuality");

            clearHolding(pinzaStack);
            damagePinza(pinzaStack, player);

            ItemStack componentStack = getComponentStack(componentId, materialType);
            if (!componentStack.isEmpty()) {
                // Transfer forge quality to the cooled component
                if (forgeQuality > 0) {
                    CustomData compData = componentStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                    compData = compData.update(tag -> tag.putInt("ForgeQuality", forgeQuality));
                    componentStack.set(DataComponents.CUSTOM_DATA, compData);
                }
                boolean added = player.getInventory().add(componentStack);
                if (!added) {
                    ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), componentStack);
                    level.addFreshEntity(itemEntity);
                }
            }
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

    public static void damagePinza(ItemStack stack, Player player) {
        int currentDamage = stack.getDamageValue();
        int newDamage = currentDamage + 1;

        if (newDamage >= MAX_DURABILITY) {
            stack.shrink(1);
        } else {
            stack.setDamageValue(newDamage);
        }
    }

    public static void setHolding(ItemStack stack, HoldingType type, String itemId) {
        setHoldingWithMaterial(stack, type, itemId, "iron");
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
        int modelData = switch (type) {
            case HOT_IRON -> 1;
            case HOT_GOLD -> 3;
            case HOT_COPPER -> 4;
            case COMPONENT -> switch (itemId) {
                case "lama_lunga" -> 5;
                case "lama_corta" -> 6;
                case "elsa" -> 7;
                default -> 2;
            };
            case CRUCIBLE_EMPTY -> 8;
            case CRUCIBLE_IRON -> 9;
            case CRUCIBLE_GOLD -> 10;
            case CRUCIBLE_COPPER -> 11;
            default -> 0;
        };
        if (modelData > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(modelData));
        }
    }

    public static void clearHolding(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        customData = customData.update(tag -> {
            tag.remove(TAG_HOLDING_TYPE);
            tag.remove(TAG_HOLDING);
            tag.remove(TAG_MATERIAL);
            tag.remove(TAG_COMPONENT_TIME);
            tag.remove(TAG_EXPIRED);
            tag.remove(TAG_CRUCIBLE_PICKUP_TIME);
        });
        stack.set(DataComponents.CUSTOM_DATA, customData);
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    public static void setExpired(ItemStack stack, boolean expired) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        customData = customData.update(tag -> tag.putBoolean(TAG_EXPIRED, expired));
        stack.set(DataComponents.CUSTOM_DATA, customData);
    }

    public static boolean isExpired(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return customData.copyTag().getBoolean(TAG_EXPIRED);
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
            return "iron";
        }
        return customData.copyTag().getString(TAG_MATERIAL);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        HoldingType type = getHoldingType(stack);
        if (type != HoldingType.NONE) {
            String item = getHoldingItem(stack);
            if (!item.isEmpty()) {
                String translationKey = "item.tharidiathings.pinza.holding." + item;
                tooltipComponents.add(Component.translatable(translationKey));
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
