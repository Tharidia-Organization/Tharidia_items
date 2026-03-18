package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.entity.ModEntities;
import com.THproject.tharidia_things.client.renderer.RacePointRenderer;
import com.THproject.tharidia_things.client.renderer.DiceEntityRenderer;
import com.THproject.tharidia_things.client.renderer.DyeVatsBlockEntityRenderer;
import com.THproject.tharidia_things.client.model.DiceModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side mod event handlers
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    
    public static final ModelLayerLocation DICE_LAYER = 
        new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "dice"), "main");
    
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DICE_LAYER, DiceModel::createBodyLayer);
    }
    
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register entity renderers
        event.registerEntityRenderer(ModEntities.RACE_POINT.get(), RacePointRenderer::new);
        event.registerEntityRenderer(ModEntities.DICE.get(), DiceEntityRenderer::new);
        // Register block entity renderers
        event.registerBlockEntityRenderer(TharidiaThings.DYE_VATS_BLOCK_ENTITY.get(), DyeVatsBlockEntityRenderer::new);
    }
}
