package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.registry.BabyMobRegistry;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Map;

/**
 * Handles dynamic model loading for BabyMobItems.
 * All baby mob items share the same template model with dynamic tinting.
 *
 * Dynamically assigns the template model to baby mob items that don't have explicit JSON files.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BabyMobModelHandler {

    private static final ResourceLocation TEMPLATE_MODEL =
        ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "item/baby_mob_template");

    /**
     * Register the template model so it's available for baking
     */
    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // Register the template model
        event.register(ModelResourceLocation.standalone(TEMPLATE_MODEL));
        TharidiaThings.LOGGER.info("[BABY MOBS] Registered baby_mob_template model");
    }

    /**
     * After models are baked, redirect missing baby mob item models to use the template.
     * This handles mobs from other mods that don't have explicit model JSON files.
     */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();

        TharidiaThings.LOGGER.info("[BABY MOBS MODEL] ModifyBakingResult called, models map size: {}", models.size());
        TharidiaThings.LOGGER.info("[BABY MOBS MODEL] Looking for template: {}", ModelResourceLocation.standalone(TEMPLATE_MODEL));

        // Get the baked template model
        BakedModel templateModel = models.get(ModelResourceLocation.standalone(TEMPLATE_MODEL));

        if (templateModel == null) {
            TharidiaThings.LOGGER.error("[BABY MOBS MODEL] Template model not found! Baby mobs will have missing textures.");
            return;
        }

        TharidiaThings.LOGGER.info("[BABY MOBS MODEL] Template model found: {}", templateModel.getClass().getSimpleName());

        int assigned = 0;
        for (Map.Entry<EntityType<?>, DeferredItem<Item>> entry : BabyMobRegistry.getAllBabyMobs().entrySet()) {
            ResourceLocation itemId = entry.getValue().getId();

            // Create the model location for this item
            ModelResourceLocation itemModelLocation = new ModelResourceLocation(itemId, "inventory");

            // FORCE replace ALL baby mob models with our template
            models.put(itemModelLocation, templateModel);
            assigned++;
        }

        TharidiaThings.LOGGER.info("[BABY MOBS MODEL] Forced template assignment to {} baby mob items", assigned);
    }
}
