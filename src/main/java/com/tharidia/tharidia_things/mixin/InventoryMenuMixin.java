package com.tharidia.tharidia_things.mixin;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.inventory.SecondLayerInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {

    protected InventoryMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void tharidia$addSecondLayerSlots(Inventory playerInventory, boolean active, Player owner,
            CallbackInfo ci) {
        SecondLayerInventory inventory = owner.getData(TharidiaThings.SECOND_LAYER_INVENTORY.get());

        // Helmet 2
        this.addSlot(new SlotItemHandler(inventory, 0, 77, 8) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return stack.is(net.minecraft.tags.ItemTags.HEAD_ARMOR);
            }
        });
        // Chest 2
        this.addSlot(new SlotItemHandler(inventory, 1, 77, 26) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return stack.is(net.minecraft.tags.ItemTags.CHEST_ARMOR);
            }
        });
        // Legs 2
        this.addSlot(new SlotItemHandler(inventory, 2, 77, 44) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return stack.is(net.minecraft.tags.ItemTags.LEG_ARMOR);
            }
        });
        // Boots 2
        this.addSlot(new SlotItemHandler(inventory, 3, 98, 62) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return stack.is(net.minecraft.tags.ItemTags.FOOT_ARMOR);
            }
        });
    }
}