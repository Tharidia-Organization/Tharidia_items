package com.THproject.tharidia_things.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import com.THproject.tharidia_things.TharidiaThings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.annotations.Expose;

import net.neoforged.fml.loading.FMLPaths;

public class ReviveConfig {
    public static ReviveConfig config;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();

    public static void reload() {
        config = load(FMLPaths.CONFIGDIR.get()
                .resolve(TharidiaThings.MODID + "-revive.json").toFile());
    }

    @Expose
    public Map<String, Object> REVIVE_ITEM = new HashMap<String, Object>() {
        {
            put("//", "Item to use to revive players");
            put("Value", new String());
        }
    };

    @Expose
    public Map<String, Object> TIME_TO_RES = new HashMap<String, Object>() {
        {
            put("//", "Time to revive a player");
            put("Value", 50);
        }
    };

    public static ReviveConfig load(File configFile) {
        ReviveConfig config = new ReviveConfig();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, ReviveConfig.class);
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
