package com.example.tharidia_items;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.DefaultAccessoryRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import com.example.tharidia_items.block.entity.AlchimistTableRenderer;
import com.example.tharidia_items.screen.AlchimistTableScreen;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import com.example.tharidia_items.block.entity.AlchimistTableBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TharidiaItemsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(TharidiaItemsMod.ALCHIMIST_TABLE_SCREEN_HANDLER, AlchimistTableScreen::new);
        BlockEntityRendererRegistry.register(TharidiaItemsMod.ALCHIMIST_TABLE_BE, ctx -> new AlchimistTableRenderer());

        // Register Accessories renderers for armor items listed in the provided JSON
        Map<String, List<String>> armorIdsByCategory = readArmorIdsFromResource();
        if (armorIdsByCategory != null) {
            registerArmorCategory(armorIdsByCategory.get("head"));
            registerArmorCategory(armorIdsByCategory.get("chestplate"));
            registerArmorCategory(armorIdsByCategory.get("leggings"));
            registerArmorCategory(armorIdsByCategory.get("boots"));
        }
        // Inside onInitializeClient, after existing registrations:
        BuiltinItemRendererRegistry.INSTANCE.register(TharidiaItemsMod.ALCHIMIST_TABLE.asItem(), (stack, mode, matrices, vertexConsumers, light, overlay) -> {
            AlchimistTableBlockEntity entity = new AlchimistTableBlockEntity(BlockPos.ORIGIN, TharidiaItemsMod.ALCHIMIST_TABLE.getDefaultState());
            MinecraftClient.getInstance().getBlockEntityRenderDispatcher().renderEntity(entity, matrices, vertexConsumers, light, overlay);
        });
    }

    private static Map<String, List<String>> readArmorIds(Path path) {
        if (!Files.exists(path)) return null;
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            return new Gson().fromJson(json, type);
        } catch (IOException e) {
            System.err.println("[TharidiaItemsClient] Failed to read armor ids json: " + e.getMessage());
            return null;
        }
    }

    private static Map<String, List<String>> readArmorIdsFromResource() {
        // Loads from classpath: assets/tharidia_items/antique_legacy_armor_ids.json
        String resourcePath = "/assets/tharidia_items/antique_legacy_armor_ids.json";
        try (InputStream in = TharidiaItemsClient.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("[TharidiaItemsClient] Resource not found: " + resourcePath);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[4096];
                int n;
                while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);
                Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
                return new Gson().fromJson(sb.toString(), type);
            }
        } catch (IOException e) {
            System.err.println("[TharidiaItemsClient] Failed to read armor ids resource: " + e.getMessage());
            return null;
        }
    }

    private static void registerArmorCategory(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        Set<String> seen = new HashSet<>();
        for (String idStr : ids) {
            if (idStr == null || idStr.isBlank()) continue;
            if (!seen.add(idStr)) continue; // skip duplicates
            Identifier id = Identifier.tryParse(idStr);
            if (id == null) continue;
            Item item = Registries.ITEM.get(id);
            if (item == null || Registries.ITEM.getId(item) == Registries.ITEM.getDefaultId()) {
                // Not found in registry
                continue;
            }
            // Register default renderer supplier for the item per Accessories 1.0.0-beta.40 API
            AccessoriesRendererRegistry.registerRenderer(item, () -> DefaultAccessoryRenderer.INSTANCE);
        }
    }
}