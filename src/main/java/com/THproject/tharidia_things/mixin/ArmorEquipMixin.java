package com.THproject.tharidia_things.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.THproject.tharidia_things.compoundTag.CustomArmorAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Mixin(InventoryMenu.class)
public abstract class ArmorEquipMixin extends AbstractContainerMenu {

    // Constructor required by Mixin (never called)
    protected ArmorEquipMixin() {
        super(null, 0);
    }

    @Redirect(method = "<init>(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;"))
    private Slot disableArmorSlots(InventoryMenu instance, Slot slot) {
        int containerSlotIndex = slot.getContainerSlot();

        if (containerSlotIndex >= 36 && containerSlotIndex <= 39) {
            int slotIndex = 39 - containerSlotIndex;

            EquipmentSlot[] equipmentSlots = new EquipmentSlot[] {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
            };

            ResourceLocation[] armorIcons = new ResourceLocation[] {
                InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
                InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
                InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
                InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
            };

            slot = new Slot(slot.container, containerSlotIndex, slot.x, slot.y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    Player player = Minecraft.getInstance().player;
                    Container armorContainer = player.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());
                    return stack.canEquip(equipmentSlots[slotIndex], player)
                        && !armorContainer.getItem(slotIndex).isEmpty();
                }
            };
            slot.setBackground(InventoryMenu.BLOCK_ATLAS, armorIcons[slotIndex]);
            return this.addSlot(slot);
        }

        return this.addSlot(slot);
    }
}
