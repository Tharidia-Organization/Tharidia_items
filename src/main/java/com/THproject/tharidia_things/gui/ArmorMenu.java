package com.THproject.tharidia_things.gui;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArmorMenu extends AbstractContainerMenu {
    // Defines the size of the custom inventory
    private static final int CONTAINER_SIZE = 4;
    private final Container armorContainer;

    // Client-side constructor
    public ArmorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE));
    }

    // Server-side constructor
    public ArmorMenu(int containerId, Inventory playerInventory, Container container) {
        super(TharidiaThings.ARMOR_MENU.get(), containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        this.armorContainer = container;
        container.startOpen(playerInventory.player);

        // Add the armor slots and custom slots (2 columns, 4 rows)
        int armor_startX = 24;
        int armor_startY = 12;
        int armor_column_spacing = 36;

        // Column 0: Vanilla Armor Slots (from Player Inventory)
        // Order in UI: top to bottom (Helmet -> Boots) matches row 0->3
        // Slots in Inventory: 39 (Head), 38 (Chest), 37 (Legs), 36 (Feet)
        EquipmentSlot[] equipmentSlots = new EquipmentSlot[] {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };
        for (int row = 0; row < 4; ++row) {
            final EquipmentSlot slotType = equipmentSlots[row];
            final int slotIndex = row;
            this.addSlot(new Slot(playerInventory, 39 - row, armor_startX, armor_startY + row * 18) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.canEquip(slotType, playerInventory.player)
                            && !armorContainer.getItem(slotIndex).isEmpty();
                }
            });
        }

        // Column 1: Custom Slots (from custom Container)
        // Indices 0 to 3
        for (int row = 0; row < 4; ++row) {
            final int slotIndex = 39 - row;
            this.addSlot(new Slot(container, row, armor_startX + armor_column_spacing,
                    armor_startY + row * 18) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(TagKey.create(Registries.ITEM,
                            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "under_armor")));
                }

                @Override
                public boolean mayPickup(Player player) {
                    return player.getInventory().getItem(slotIndex).isEmpty();
                }
            });
        }

        int inventory_startX = 6;
        int inventory_startY = 97;
        int hotbar_startY = 155;

        // Add player inventory (3 rows of 9 slots)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, inventory_startX + col * 18,
                        inventory_startY + row * 18));

            }
        }

        // Add player hotbar (9 slots)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, inventory_startX + col * 18, hotbar_startY));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // Slots 0-3: Armor
            // Slots 4-7: Custom
            // Slots 8-43: Player Inventory + Hotbar

            if (index < 8) { // From Armor (0-3) or Custom (4-7) to Player Inventory
                if (!this.moveItemStackTo(itemstack1, 8, 44, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From Player Inventory
                // Try to equip armor first
                if (itemstack1.canEquip(EquipmentSlot.HEAD, player) && !this.slots.get(0).hasItem()) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false))
                        return ItemStack.EMPTY;
                } else if (itemstack1.canEquip(EquipmentSlot.CHEST, player) && !this.slots.get(1).hasItem()) {
                    if (!this.moveItemStackTo(itemstack1, 1, 2, false))
                        return ItemStack.EMPTY;
                } else if (itemstack1.canEquip(EquipmentSlot.LEGS, player) && !this.slots.get(2).hasItem()) {
                    if (!this.moveItemStackTo(itemstack1, 2, 3, false))
                        return ItemStack.EMPTY;
                } else if (itemstack1.canEquip(EquipmentSlot.FEET, player) && !this.slots.get(3).hasItem()) {
                    if (!this.moveItemStackTo(itemstack1, 3, 4, false))
                        return ItemStack.EMPTY;
                }
                // Then try custom buffer (4-7)
                else if (!this.moveItemStackTo(itemstack1, 4, 8, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.armorContainer.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.armorContainer.stopOpen(player);
    }
}
