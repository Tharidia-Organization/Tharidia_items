package com.THproject.tharidia_things.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.annotations.Expose;
import com.THproject.tharidia_things.TharidiaThings;

import net.neoforged.fml.loading.FMLPaths;

public class ItemCatalogueConfig {
    public static ItemCatalogueConfig config;

    public static void reload() {
        config = load(FMLPaths.CONFIGDIR.get()
                .resolve(TharidiaThings.MODID + "-item_catalogue" + ".json").toFile());
    }

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();

    @Expose
    public Map<String, Object> LAMA_CORTA_ITEMS = new HashMap<String, Object>() {
        {
            put("//", "List of item IDs that have the Lama Corta attribute");
            put("Value", new java.util.ArrayList<String>());
        }
    };

    @Expose
    public Map<String, Object> LANCIA_ITEMS = new HashMap<String, Object>() {
        {
            put("//", "List of item IDs that have the Lancia attribute");
            put("Value", new java.util.ArrayList<String>());
        }
    };

    @Expose
    public Map<String, Object> MARTELLI_ITEMS = new HashMap<String, Object>() {
        {
            put("//", "List of item IDs that have the Martelli attribute");
            put("Value", new java.util.ArrayList<String>());
        }
    };

    @Expose
    public Map<String, Object> MAZZE_ITEMS = new HashMap<String, Object>() {
        {
            put("//", "List of item IDs that have the Mazze attribute");
            put("Value", new java.util.ArrayList<String>());
        }
    };

    @Expose
    public Map<String, Object> SPADE_2_MANI_ITEMS = new HashMap<String, Object>() {
        {
            put("//", "List of item IDs that have the Spade 2 Mani attribute");
            put("Value", new java.util.ArrayList<String>());
        }
    };

    @Expose
    public Map<String, Object> ASCE_ITEMS = new HashMap<String, Object>() {
        {
            put("//", "List of item IDs that have the Asce attribute");
            put("Value", new java.util.ArrayList<String>());
        }
    };

    @Expose
    public Map<String, Object> SOCCHI_ITEMS = new HashMap<String, Object>() {
        {
            put("//", "List of item IDs that have the Socchi attribute");
            put("Value", new java.util.ArrayList<String>());
        }
    };

    @Expose
    public Map<String, Object> ARCHI_ITEMS = new HashMap<String, Object>() {
        {
            put("//", "List of item IDs that have the Archi attribute");
            put("Value", new java.util.ArrayList<String>());
        }
    };

    @Expose
    public Map<String, Object> ARMI_DA_FUOCO_ITEMS = new HashMap<String, Object>() {
        {
            put("//", "List of item IDs that have the Armi da Fuoco attribute");
            put("Value", new java.util.ArrayList<String>());
        }
    };

    public static ItemCatalogueConfig load(File configFile) {
        ItemCatalogueConfig config = new ItemCatalogueConfig();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, ItemCatalogueConfig.class);
            } catch (Exception e) {
            }
        } else {
            configFile.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
        }

        return config;
    }
}
