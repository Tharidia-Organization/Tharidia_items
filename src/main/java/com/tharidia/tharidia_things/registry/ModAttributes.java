package com.tharidia.tharidia_things.registry;

import com.tharidia.tharidia_things.TharidiaThings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(BuiltInRegistries.ATTRIBUTE,
            TharidiaThings.MODID);

    public static final DeferredHolder<Attribute, Attribute> LAMA_CORTA_ATTACK_DAMAGE = ATTRIBUTES.register(
            "lama_corta_attack_damage",
            () -> new RangedAttribute("attribute.name.tharidia_features.lama_corta_attack_damage", 0.0D, 0.0D,
                    2048.0D)
                    .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> LANCIA_ATTACK_DAMAGE = ATTRIBUTES.register(
            "lancia_attack_damage",
            () -> new RangedAttribute("attribute.name.tharidia_features.lancia_attack_damage", 0.0D, 0.0D,
                    2048.0D)
                    .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> MARTELLI_ATTACK_DAMAGE = ATTRIBUTES.register(
            "martelli_attack_damage",
            () -> new RangedAttribute("attribute.name.tharidia_features.martelli_attack_damage", 0.0D, 0.0D,
                    2048.0D)
                    .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> MAZZE_ATTACK_DAMAGE = ATTRIBUTES.register(
            "mazze_attack_damage",
            () -> new RangedAttribute("attribute.name.tharidia_features.mazze_attack_damage", 0.0D, 0.0D,
                    2048.0D)
                    .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> SPADE_2_MANI_ATTACK_DAMAGE = ATTRIBUTES.register(
            "spade_2_mani_attack_damage",
            () -> new RangedAttribute("attribute.name.tharidia_features.spade_2_mani_attack_damage", 0.0D, 0.0D,
                    2048.0D)
                    .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> ASCE_ATTACK_DAMAGE = ATTRIBUTES.register(
            "asce_attack_damage",
            () -> new RangedAttribute("attribute.name.tharidia_features.asce_attack_damage", 0.0D, 0.0D,
                    2048.0D)
                    .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> SOCCHI_ATTACK_DAMAGE = ATTRIBUTES.register(
            "socchi_attack_damage",
            () -> new RangedAttribute("attribute.name.tharidia_features.socchi_attack_damage", 0.0D, 0.0D,
                    2048.0D)
                    .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> ARCHI_ATTACK_DAMAGE = ATTRIBUTES.register(
            "archi_attack_damage",
            () -> new RangedAttribute("attribute.name.tharidia_features.archi_attack_damage", 0.0D, 0.0D,
                    2048.0D)
                    .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> ARMI_DA_FUOCO_ATTACK_DAMAGE = ATTRIBUTES.register(
            "armi_da_fuoco_attack_damage",
            () -> new RangedAttribute("attribute.name.tharidia_features.armi_da_fuoco_attack_damage", 0.0D, 0.0D,
                    2048.0D)
                    .setSyncable(true));

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.PLAYER, ModAttributes.LAMA_CORTA_ATTACK_DAMAGE)) {
            event.add(EntityType.PLAYER, ModAttributes.LAMA_CORTA_ATTACK_DAMAGE);
        }

        if (!event.has(EntityType.PLAYER, ModAttributes.LANCIA_ATTACK_DAMAGE)) {
            event.add(EntityType.PLAYER, ModAttributes.LANCIA_ATTACK_DAMAGE);
        }

        if (!event.has(EntityType.PLAYER, ModAttributes.MARTELLI_ATTACK_DAMAGE)) {
            event.add(EntityType.PLAYER, ModAttributes.MARTELLI_ATTACK_DAMAGE);
        }

        if (!event.has(EntityType.PLAYER, ModAttributes.MAZZE_ATTACK_DAMAGE)) {
            event.add(EntityType.PLAYER, ModAttributes.MAZZE_ATTACK_DAMAGE);
        }

        if (!event.has(EntityType.PLAYER, ModAttributes.SPADE_2_MANI_ATTACK_DAMAGE)) {
            event.add(EntityType.PLAYER, ModAttributes.SPADE_2_MANI_ATTACK_DAMAGE);
        }

        if (!event.has(EntityType.PLAYER, ModAttributes.ASCE_ATTACK_DAMAGE)) {
            event.add(EntityType.PLAYER, ModAttributes.ASCE_ATTACK_DAMAGE);
        }

        if (!event.has(EntityType.PLAYER, ModAttributes.SOCCHI_ATTACK_DAMAGE)) {
            event.add(EntityType.PLAYER, ModAttributes.SOCCHI_ATTACK_DAMAGE);
        }

        if (!event.has(EntityType.PLAYER, ModAttributes.ARCHI_ATTACK_DAMAGE)) {
            event.add(EntityType.PLAYER, ModAttributes.ARCHI_ATTACK_DAMAGE);
        }

        if (!event.has(EntityType.PLAYER, ModAttributes.ARMI_DA_FUOCO_ATTACK_DAMAGE)) {
            event.add(EntityType.PLAYER, ModAttributes.ARMI_DA_FUOCO_ATTACK_DAMAGE);
        }
    }
}
