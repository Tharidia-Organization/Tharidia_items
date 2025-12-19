package com.THproject.tharidia_things.event;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.THproject.tharidia_things.config.ItemCatalogueConfig;
import com.THproject.tharidia_things.registry.ModAttributes;

public class ItemAttributeHandler {

    private static final Map<Object, DeferredHolder<Attribute, Attribute>> WEAPON_ATTRIBUTES = new HashMap<>();

    public static void reload() {
        WEAPON_ATTRIBUTES.clear();
        WEAPON_ATTRIBUTES.put(ItemCatalogueConfig.config.LAMA_CORTA_ITEMS.get("Value"),
                ModAttributes.LAMA_CORTA_ATTACK_DAMAGE);
        WEAPON_ATTRIBUTES.put(ItemCatalogueConfig.config.LANCIA_ITEMS.get("Value"),
                ModAttributes.LANCIA_ATTACK_DAMAGE);
        WEAPON_ATTRIBUTES.put(ItemCatalogueConfig.config.MARTELLI_ITEMS.get("Value"),
                ModAttributes.MARTELLI_ATTACK_DAMAGE);
        WEAPON_ATTRIBUTES.put(ItemCatalogueConfig.config.MAZZE_ITEMS.get("Value"),
                ModAttributes.MAZZE_ATTACK_DAMAGE);
        WEAPON_ATTRIBUTES.put(ItemCatalogueConfig.config.SPADE_2_MANI_ITEMS.get("Value"),
                ModAttributes.SPADE_2_MANI_ATTACK_DAMAGE);
        WEAPON_ATTRIBUTES.put(ItemCatalogueConfig.config.ASCE_ITEMS.get("Value"),
                ModAttributes.ASCE_ATTACK_DAMAGE);
        WEAPON_ATTRIBUTES.put(ItemCatalogueConfig.config.SOCCHI_ITEMS.get("Value"),
                ModAttributes.SOCCHI_ATTACK_DAMAGE);
        WEAPON_ATTRIBUTES.put(ItemCatalogueConfig.config.ARCHI_ITEMS.get("Value"),
                ModAttributes.ARCHI_ATTACK_DAMAGE);
        WEAPON_ATTRIBUTES.put(ItemCatalogueConfig.config.ARMI_DA_FUOCO_ITEMS.get("Value"),
                ModAttributes.ARMI_DA_FUOCO_ATTACK_DAMAGE);
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide())
            return; // Only run on server side

        Player player = event.getEntity();
        ItemStack mainHandItem = player.getMainHandItem();

        WEAPON_ATTRIBUTES.forEach((list, attributeHolder) -> {

            if (listContains(list, mainHandItem)) {
                // Get the multiplier value from the player's attribute
                double multiplier = player.getAttributeBaseValue(attributeHolder);
                ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath("tharidia",
                        attributeHolder.getId().getPath() + "_multiplier"); // Unique ID per attribute type

                // We need to preserve existing modifiers (like base damage) and add our
                // multiplier
                ItemAttributeModifiers currentModifiers = mainHandItem.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                        ItemAttributeModifiers.EMPTY);

                boolean exists = false;
                double existingValue = 0;

                for (var entry : currentModifiers.modifiers()) {
                    if (entry.modifier().id().equals(modifierId)) {
                        exists = true;
                        existingValue = entry.modifier().amount();
                        break;
                    }
                }

                // Update if value changed or if it doesn't exist (and we have a value to set)
                if ((exists && existingValue != multiplier) || (!exists && multiplier != 0)) {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

                    // Add existing modifiers EXCEPT our custom one
                    for (var entry : currentModifiers.modifiers()) {
                        if (!entry.modifier().id().equals(modifierId)) {
                            builder.add(entry.attribute(), entry.modifier(), entry.slot());
                        }
                    }

                    // Add the multiplier to ATTACK_DAMAGE if not zero
                    if (multiplier != 0) {
                        builder.add(
                                Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(
                                        modifierId,
                                        multiplier,
                                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                                EquipmentSlotGroup.MAINHAND);
                    }

                    mainHandItem.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
                }
            }
        });
    }

    private static boolean listContains(Object list, ItemStack item) {
        for (Object obj : (ArrayList<?>) list) {
            if (obj instanceof String) {
                if (String.valueOf(obj).equals(item.getItem().toString())) {
                    return true;
                }
            }
        }
        return false;
    }
}
