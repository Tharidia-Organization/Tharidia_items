package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.gui.ComponentSelectionMenu;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.IHotMetalAnvilEntity;
import com.THproject.tharidia_things.item.PinzaItem;
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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Handles smithing interactions, particularly hammer strikes on hot iron
 */
public class SmithingHandler {

    private static final String HAMMER_ID = "tharidiathings:iron_crusher_hammer";

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack heldItem = event.getItemStack();

        if (!isSmithingHammer(heldItem)) {
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

        if (blockEntity instanceof IHotMetalAnvilEntity hotMetalEntity) {
            if (!hotMetalEntity.isFinished()) {
                // Owner check: only the player who placed the metal can hammer it
                UUID ownerUUID = hotMetalEntity.getOwnerUUID();
                if (ownerUUID != null && !ownerUUID.equals(event.getEntity().getUUID())) {
                    if (!level.isClientSide) {
                        event.getEntity().displayClientMessage(
                            Component.translatable("item.tharidiathings.smithing.not_owner"),
                            true
                        );
                    }
                    event.setCanceled(true);
                    return;
                }

                if (!level.isClientSide) {
                    // First strike: only open GUI, do NOT start minigame yet
                    if (hotMetalEntity.getHammerStrikes() == 0 && !hotMetalEntity.hasGuiBeenOpened()) {
                        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                            hotMetalEntity.setGuiOpened(true);
                            openComponentSelectionMenu(serverPlayer, checkPos);
                        }
                        // Don't call onHammerStrike - wait for component selection
                    } else {
                        // Project player's look ray onto the anvil surface plane
                        // This gives accurate X,Z regardless of viewing angle
                        Player hitter = event.getEntity();
                        Vec3 eyePos = hitter.getEyePosition();
                        Vec3 lookVec = hitter.getLookAngle();
                        double surfaceY = checkPos.getY() + 0.14;

                        float hitX, hitZ;
                        if (Math.abs(lookVec.y) > 0.001) {
                            double t = (surfaceY - eyePos.y) / lookVec.y;
                            hitX = (float)(eyePos.x + lookVec.x * t - checkPos.getX());
                            hitZ = (float)(eyePos.z + lookVec.z * t - checkPos.getZ());
                        } else {
                            // Fallback for nearly horizontal view
                            var hitLoc = event.getHitVec().getLocation();
                            hitX = (float)(hitLoc.x - checkPos.getX());
                            hitZ = (float)(hitLoc.z - checkPos.getZ());
                        }
                        hotMetalEntity.onHammerStrike(hitter, hitX, hitZ);
                    }

                    // Damage the hammer
                    if (event.getEntity() instanceof ServerPlayer player) {
                        heldItem.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                    }
                }

                event.getEntity().swing(InteractionHand.MAIN_HAND);
                event.setCanceled(true);
            }
        }
    }

    private static boolean isSmithingHammer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return HAMMER_ID.equals(itemId);
    }

    private static void openComponentSelectionMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (containerId, playerInventory, p) -> new ComponentSelectionMenu(
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
                event.setCanceled(true);

                if (heldItem.getItem() instanceof PinzaItem pinza) {
                    PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(heldItem);

                    if (holdingType == PinzaItem.HoldingType.NONE) {
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
        if (event.getCrafting().is(TharidiaThings.HOT_IRON.get())) {
            event.getCrafting().shrink(event.getCrafting().getCount());
            if (event.getEntity() != null && !event.getEntity().level().isClientSide) {
                event.getEntity().displayClientMessage(
                    Component.translatable("item.tharidiathings.hot_iron.too_hot"), true
                );
            }
        }
        if (event.getCrafting().is(TharidiaThings.HOT_GOLD.get())) {
            event.getCrafting().shrink(event.getCrafting().getCount());
            if (event.getEntity() != null && !event.getEntity().level().isClientSide) {
                event.getEntity().displayClientMessage(
                    Component.translatable("item.tharidiathings.hot_gold.too_hot"), true
                );
            }
        }
        if (event.getCrafting().is(TharidiaThings.HOT_COPPER.get())) {
            event.getCrafting().shrink(event.getCrafting().getCount());
            if (event.getEntity() != null && !event.getEntity().level().isClientSide) {
                event.getEntity().displayClientMessage(
                    Component.translatable("item.tharidiathings.hot_copper.too_hot"), true
                );
            }
        }
    }

    private static void grabHotIronWithPinza(net.minecraft.world.Container container, BlockPos pos,
                                             Level level, Player player, ItemStack pinzaStack) {
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
        container.setItem(hotIronSlot, ItemStack.EMPTY);
        container.setChanged();
        PinzaItem.setHolding(pinzaStack, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
        PinzaItem.damagePinza(pinzaStack, player);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void grabHotGoldWithPinza(net.minecraft.world.Container container, BlockPos pos,
                                             Level level, Player player, ItemStack pinzaStack) {
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
        container.setItem(hotGoldSlot, ItemStack.EMPTY);
        container.setChanged();
        PinzaItem.setHolding(pinzaStack, PinzaItem.HoldingType.HOT_GOLD, "hot_gold");
        PinzaItem.damagePinza(pinzaStack, player);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void grabHotCopperWithPinza(net.minecraft.world.Container container, BlockPos pos,
                                               Level level, Player player, ItemStack pinzaStack) {
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
        container.setItem(hotCopperSlot, ItemStack.EMPTY);
        container.setChanged();
        PinzaItem.setHolding(pinzaStack, PinzaItem.HoldingType.HOT_COPPER, "hot_copper");
        PinzaItem.damagePinza(pinzaStack, player);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAnvilInteraction(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ItemStack heldItem = event.getItemStack();

        if (state.getBlock() instanceof net.minecraft.world.level.block.AnvilBlock) {
            if (heldItem.getItem() instanceof PinzaItem) {
                if (event.getEntity() != null && event.getEntity().isCrouching()) {
                    return;
                }
                event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            boolean isHotIron = stack.is(TharidiaThings.HOT_IRON.get());
            boolean isHotGold = stack.is(TharidiaThings.HOT_GOLD.get());
            boolean isHotCopper = stack.is(TharidiaThings.HOT_COPPER.get());

            if (isHotIron || isHotGold || isHotCopper) {
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();

                if (mainHand.getItem() instanceof PinzaItem) {
                    PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(mainHand);
                    if (holdingType == PinzaItem.HoldingType.NONE) {
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                        if (isHotIron) {
                            PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                        } else if (isHotGold) {
                            PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_GOLD, "hot_gold");
                        } else {
                            PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_COPPER, "hot_copper");
                        }
                        PinzaItem.damagePinza(mainHand, player);
                        return;
                    }
                } else if (offHand.getItem() instanceof PinzaItem) {
                    PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(offHand);
                    if (holdingType == PinzaItem.HoldingType.NONE) {
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                        if (isHotIron) {
                            PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                        } else if (isHotGold) {
                            PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_GOLD, "hot_gold");
                        } else {
                            PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_COPPER, "hot_copper");
                        }
                        PinzaItem.damagePinza(offHand, player);
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            boolean isHotIron = stack.is(TharidiaThings.HOT_IRON.get());
            boolean isHotGold = stack.is(TharidiaThings.HOT_GOLD.get());
            boolean isHotCopper = stack.is(TharidiaThings.HOT_COPPER.get());

            if (isHotIron || isHotGold || isHotCopper) {
                if (!event.getLevel().isClientSide()) {
                    Player nearestPlayer = event.getLevel().getNearestPlayer(itemEntity, 5.0);

                    if (nearestPlayer != null) {
                        ItemStack mainHand = nearestPlayer.getMainHandItem();
                        ItemStack offHand = nearestPlayer.getOffhandItem();

                        if (mainHand.getItem() instanceof PinzaItem) {
                            PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(mainHand);
                            if (holdingType == PinzaItem.HoldingType.NONE) {
                                if (isHotIron) {
                                    PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                                } else if (isHotGold) {
                                    PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_GOLD, "hot_gold");
                                } else {
                                    PinzaItem.setHolding(mainHand, PinzaItem.HoldingType.HOT_COPPER, "hot_copper");
                                }
                                PinzaItem.damagePinza(mainHand, nearestPlayer);
                                event.setCanceled(true);
                                return;
                            }
                        }

                        if (offHand.getItem() instanceof PinzaItem) {
                            PinzaItem.HoldingType holdingType = PinzaItem.getHoldingType(offHand);
                            if (holdingType == PinzaItem.HoldingType.NONE) {
                                if (isHotIron) {
                                    PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_IRON, "hot_iron");
                                } else if (isHotGold) {
                                    PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_GOLD, "hot_gold");
                                } else {
                                    PinzaItem.setHolding(offHand, PinzaItem.HoldingType.HOT_COPPER, "hot_copper");
                                }
                                PinzaItem.damagePinza(offHand, nearestPlayer);
                                event.setCanceled(true);
                                return;
                            }
                        }

                        nearestPlayer.displayClientMessage(
                            Component.translatable("item.tharidiathings.hot_iron.need_pinza"),
                            true
                        );
                    }
                }
                event.setCanceled(true);
            }
        }
    }
}
