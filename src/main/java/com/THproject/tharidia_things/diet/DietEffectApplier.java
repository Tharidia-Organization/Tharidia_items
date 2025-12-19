package com.THproject.tharidia_things.diet;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Applies diet-based buffs and debuffs depending on a player's nutrition balance.
 */
public final class DietEffectApplier {
    private static final float LOW_THRESHOLD = 0.20f;
    private static final float HIGH_THRESHOLD = 0.80f;
    private static final int EFFECT_DURATION = 220;

    private static final ResourceLocation PROTEIN_DEBUFF_HEALTH = id("protein_debuff_health");
    private static final ResourceLocation PROTEIN_BUFF_HEALTH = id("protein_buff_health");
    private static final ResourceLocation FRUIT_DEBUFF_SPEED = id("fruit_debuff_speed");
    private static final ResourceLocation FRUIT_BUFF_SPEED = id("fruit_buff_speed");
    private static final ResourceLocation SUGAR_DEBUFF_ATTACK = id("sugar_debuff_attack");
    private static final ResourceLocation WATER_DEBUFF_SPEED = id("water_debuff_speed");

    private DietEffectApplier() {}

    public static void apply(ServerPlayer player, DietData data) {
        DietProfile maxValues = DietRegistry.getMaxValues();
        if (maxValues == null) {
            return;
        }

        applyGrainEffects(player, data, maxValues);
        applyProteinEffects(player, data, maxValues);
        applyVegetableEffects(player, data, maxValues);
        applyFruitEffects(player, data, maxValues);
        applySugarEffects(player, data, maxValues);
        applyWaterEffects(player, data, maxValues);
    }

    private static void applyGrainEffects(ServerPlayer player, DietData data, DietProfile maxValues) {
        float ratio = ratio(data, maxValues, DietCategory.GRAIN);
        boolean low = ratio < LOW_THRESHOLD;
        boolean high = ratio > HIGH_THRESHOLD;

        setMobEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 0, low);
        setMobEffect(player, MobEffects.DIG_SLOWDOWN, 0, low);
        setMobEffect(player, MobEffects.MOVEMENT_SPEED, 0, high);
    }

    private static void applyProteinEffects(ServerPlayer player, DietData data, DietProfile maxValues) {
        float ratio = ratio(data, maxValues, DietCategory.PROTEIN);
        boolean low = ratio < LOW_THRESHOLD;
        boolean high = ratio > HIGH_THRESHOLD;

        setMobEffect(player, MobEffects.WEAKNESS, 0, low);
        setAttributeModifier(player, Attributes.MAX_HEALTH, PROTEIN_DEBUFF_HEALTH,
                "diet_protein_deficit", -4.0d, AttributeModifier.Operation.ADD_VALUE, low);
        setAttributeModifier(player, Attributes.MAX_HEALTH, PROTEIN_BUFF_HEALTH,
                "diet_protein_optimal", 2.0d, AttributeModifier.Operation.ADD_VALUE, high && !low);
    }

    private static void applyVegetableEffects(ServerPlayer player, DietData data, DietProfile maxValues) {
        float ratio = ratio(data, maxValues, DietCategory.VEGETABLE);
        boolean low = ratio < LOW_THRESHOLD;
        boolean high = ratio > HIGH_THRESHOLD;

        setMobEffect(player, MobEffects.DAMAGE_RESISTANCE, 0, high);
        if (low && player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        }
        if (high && player.hasEffect(MobEffects.POISON)) {
            player.removeEffect(MobEffects.POISON);
        }
    }

    private static void applyFruitEffects(ServerPlayer player, DietData data, DietProfile maxValues) {
        float ratio = ratio(data, maxValues, DietCategory.FRUIT);
        boolean low = ratio < LOW_THRESHOLD;
        boolean high = ratio > HIGH_THRESHOLD;

        setAttributeModifier(player, Attributes.MOVEMENT_SPEED, FRUIT_DEBUFF_SPEED,
                "diet_fruit_deficit", -0.10d, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, low);
        setAttributeModifier(player, Attributes.MOVEMENT_SPEED, FRUIT_BUFF_SPEED,
                "diet_fruit_optimal", 0.10d, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, high && !low);
    }

    private static void applySugarEffects(ServerPlayer player, DietData data, DietProfile maxValues) {
        float ratio = ratio(data, maxValues, DietCategory.SUGAR);
        boolean low = ratio < LOW_THRESHOLD;
        boolean high = ratio > HIGH_THRESHOLD;

        setAttributeModifier(player, Attributes.ATTACK_SPEED, SUGAR_DEBUFF_ATTACK,
                "diet_sugar_deficit", -0.15d, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, low);
        setMobEffect(player, MobEffects.DIG_SPEED, 0, high && !low);
    }

    private static void applyWaterEffects(ServerPlayer player, DietData data, DietProfile maxValues) {
        float ratio = ratio(data, maxValues, DietCategory.WATER);
        boolean low = ratio < LOW_THRESHOLD;
        boolean high = ratio > HIGH_THRESHOLD;

        setMobEffect(player, MobEffects.CONFUSION, 0, low);
        setAttributeModifier(player, Attributes.MOVEMENT_SPEED, WATER_DEBUFF_SPEED,
                "diet_water_deficit", -0.10d, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, low);

        setMobEffect(player, MobEffects.WATER_BREATHING, 0, high && !low);
        setMobEffect(player, MobEffects.DOLPHINS_GRACE, 0, high && !low);
    }

    private static float ratio(DietData data, DietProfile maxValues, DietCategory category) {
        float max = maxValues.get(category);
        if (max <= 0.0f) {
            return 0.0f;
        }
        float value = data.get(category);
        return Math.max(0.0f, Math.min(1.0f, value / max));
    }

    private static void setMobEffect(Player player, Holder<MobEffect> effect, int amplifier, boolean enable) {
        if (enable) {
            player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, amplifier, true, false, true));
        } else if (player.hasEffect(effect)) {
            player.removeEffect(effect);
        }
    }

    private static void setAttributeModifier(Player player, Holder<Attribute> attribute, ResourceLocation id,
                                             String name, double amount,
                                             AttributeModifier.Operation operation, boolean enable) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (!enable) {
            if (existing != null) {
                instance.removeModifier(existing);
            }
            return;
        }
        if (existing != null && (existing.amount() != amount || existing.operation() != operation)) {
            instance.removeModifier(existing);
            existing = null;
        }
        if (existing == null) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "diet/" + path);
    }
}
