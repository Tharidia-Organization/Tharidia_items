package com.THproject.tharidia_things.entity;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registers custom entities
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, TharidiaThings.MODID);
    
    public static final DeferredHolder<EntityType<?>, EntityType<RacePointEntity>> RACE_POINT = 
        ENTITIES.register("race_point", () -> EntityType.Builder.<RacePointEntity>of(RacePointEntity::new, MobCategory.MISC)
            .sized(0.5f, 1.0f)
            .clientTrackingRange(10)
            .updateInterval(1)
            .fireImmune()
            .build("race_point"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.THproject.tharidia_things.entity.DiceEntity>> DICE =
        ENTITIES.register("dice", () -> EntityType.Builder
            .<com.THproject.tharidia_things.entity.DiceEntity>of(com.THproject.tharidia_things.entity.DiceEntity::new, MobCategory.MISC)
            .sized(0.35f, 0.35f)
            .clientTrackingRange(8)
            .updateInterval(2)
            .build("dice"));
}
