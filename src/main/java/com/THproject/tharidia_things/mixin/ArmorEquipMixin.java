package com.THproject.tharidia_things.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
            return this.addSlot(new Slot(slot.container, containerSlotIndex, -999, -999) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public boolean isActive() {
                    return false;
                }
            });
        }

        return this.addSlot(slot);
    }
}
