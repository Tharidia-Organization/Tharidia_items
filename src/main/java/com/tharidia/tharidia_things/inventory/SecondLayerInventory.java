package com.tharidia.tharidia_things.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.network.PacketDistributor;
import com.tharidia.tharidia_things.network.SecondLayerSyncPacket;
import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class SecondLayerInventory extends ItemStackHandler implements INBTSerializable<CompoundTag> {
    public static final int SIZE = 4;
    private final IAttachmentHolder holder;

    private static final ResourceLocation[] ARMOR_MODIFIER_IDS = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "second_layer_helmet"),
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "second_layer_chestplate"),
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "second_layer_leggings"),
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "second_layer_boots")
    };

    public SecondLayerInventory(IAttachmentHolder holder) {
        super(SIZE);
        this.holder = holder;
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (holder instanceof ServerPlayer serverPlayer) {
            updateAttributes(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SecondLayerSyncPacket(this.serializeNBT(serverPlayer.registryAccess())));
        }
    }

    private void updateAttributes(ServerPlayer player) {
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = this.getStackInSlot(i);
            EquipmentSlot slotType = getSlotType(i);

            // Remove old modifiers
            if (player.getAttribute(Attributes.ARMOR) != null)
                player.getAttribute(Attributes.ARMOR).removeModifier(ARMOR_MODIFIER_IDS[i]);
            if (player.getAttribute(Attributes.ARMOR_TOUGHNESS) != null)
                player.getAttribute(Attributes.ARMOR_TOUGHNESS).removeModifier(ARMOR_MODIFIER_IDS[i]);

            if (!stack.isEmpty()) {
                var modifiers = stack.getAttributeModifiers();
                int finalI = i;
                modifiers.forEach(EquipmentSlotGroup.bySlot(slotType), (attribute, modifier) -> {
                    if (attribute == Attributes.ARMOR || attribute == Attributes.ARMOR_TOUGHNESS) {
                        AttributeModifier newModifier = new AttributeModifier(
                                ARMOR_MODIFIER_IDS[finalI],
                                modifier.amount(),
                                modifier.operation());
                        if (player.getAttribute(attribute) != null) {
                            player.getAttribute(attribute).addTransientModifier(newModifier);
                        }
                    }
                });
            }
        }
    }

    private EquipmentSlot getSlotType(int slot) {
        return switch (slot) {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.CHEST;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.FEET;
            default -> EquipmentSlot.HEAD;
        };
    }
}