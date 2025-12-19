package com.THproject.tharidia_things.character;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and stores race characteristics from CSV data
 */
public class RaceData {
    private static final Map<String, RaceInfo> RACES = new HashMap<>();
    private static boolean loaded = false;
    
    public static class RaceInfo {
        public String name;
        public String description;
        public Map<String, Integer> characteristics;
        
        public RaceInfo(String name, String description, Map<String, Integer> characteristics) {
            this.name = name;
            this.description = description;
            this.characteristics = characteristics;
        }
    }
    
    public static void loadRaceData() {
        if (loaded) return;
        
        // Initialize races with their characteristics
        Map<String, Map<String, Integer>> raceStats = new HashMap<>();
        
        // Load from hardcoded data (can be moved to JSON later)
        Map<String, Integer> umaniStats = new HashMap<>();
        umaniStats.put("vita", 100);
        umaniStats.put("velocità", 100);
        umaniStats.put("attack speed", 95);
        umaniStats.put("mana", 115);
        umaniStats.put("danno mag. avanzi", 105);
        umaniStats.put("res. magica avanzi", 100);
        umaniStats.put("danno mag. fuoco", 100);
        umaniStats.put("res. mag. fuoco", 100);
        umaniStats.put("danno mag. ghiaccio", 100);
        umaniStats.put("res. mag. ghiaccio", 100);
        umaniStats.put("danno mag. druido", 100);
        umaniStats.put("res. mag. druido", 100);
        umaniStats.put("danno mag. elettrico", 100);
        umaniStats.put("res. mag. elettrico", 100);
        umaniStats.put("danno mag. arcano", 100);
        umaniStats.put("res. mag. arcano", 100);
        umaniStats.put("danno mag. terra", 100);
        umaniStats.put("res. mag. terra", 100);
        umaniStats.put("danno mag. sangue", 100);
        umaniStats.put("res. mag. sangue", 100);
        umaniStats.put("danno fisico", 110);
        umaniStats.put("stamina", 100);
        umaniStats.put("res. fisica", 100);
        umaniStats.put("danno distanza", 105);
        umaniStats.put("res. distanza", 85);
        umaniStats.put("peso trasportato", 90);
        umaniStats.put("deb. caldo", 90);
        umaniStats.put("deb. freddo", 90);
        raceStats.put("Umani", umaniStats);
        
        Map<String, Integer> elfiStats = new HashMap<>();
        elfiStats.put("vita", 90);
        elfiStats.put("velocità", 110);
        elfiStats.put("attack speed", 105);
        elfiStats.put("mana", 95);
        elfiStats.put("danno mag. avanzi", 130);
        elfiStats.put("res. magica avanzi", 100);
        elfiStats.put("danno mag. fuoco", 100);
        elfiStats.put("res. mag. fuoco", 100);
        elfiStats.put("danno mag. ghiaccio", 100);
        elfiStats.put("res. mag. ghiaccio", 100);
        elfiStats.put("danno mag. druido", 100);
        elfiStats.put("res. mag. druido", 100);
        elfiStats.put("danno mag. elettrico", 100);
        elfiStats.put("res. mag. elettrico", 100);
        elfiStats.put("danno mag. arcano", 100);
        elfiStats.put("res. mag. arcano", 100);
        elfiStats.put("danno mag. terra", 100);
        elfiStats.put("res. mag. terra", 100);
        elfiStats.put("danno mag. sangue", 100);
        elfiStats.put("res. mag. sangue", 100);
        elfiStats.put("danno fisico", 95);
        elfiStats.put("stamina", 100);
        elfiStats.put("res. fisica", 75);
        elfiStats.put("danno distanza", 110);
        elfiStats.put("res. distanza", 80);
        elfiStats.put("peso trasportato", 80);
        elfiStats.put("deb. caldo", 150);
        elfiStats.put("deb. freddo", 70);
        raceStats.put("Elfi", elfiStats);
        
        Map<String, Integer> naniStats = new HashMap<>();
        naniStats.put("vita", 85);
        naniStats.put("velocità", 115);
        naniStats.put("attack speed", 105);
        naniStats.put("mana", 95);
        naniStats.put("danno mag. avanzi", 110);
        naniStats.put("res. magica avanzi", 80);
        naniStats.put("danno mag. fuoco", 100);
        naniStats.put("res. mag. fuoco", 100);
        naniStats.put("danno mag. ghiaccio", 100);
        naniStats.put("res. mag. ghiaccio", 100);
        naniStats.put("danno mag. druido", 100);
        naniStats.put("res. mag. druido", 100);
        naniStats.put("danno mag. elettrico", 100);
        naniStats.put("res. mag. elettrico", 100);
        naniStats.put("danno mag. arcano", 100);
        naniStats.put("res. mag. arcano", 100);
        naniStats.put("danno mag. terra", 100);
        naniStats.put("res. mag. terra", 100);
        naniStats.put("danno mag. sangue", 100);
        naniStats.put("res. mag. sangue", 100);
        naniStats.put("danno fisico", 90);
        naniStats.put("stamina", 70);
        naniStats.put("res. fisica", 90);
        naniStats.put("danno distanza", 140);
        naniStats.put("res. distanza", 120);
        naniStats.put("peso trasportato", 95);
        naniStats.put("deb. caldo", 75);
        naniStats.put("deb. freddo", 110);
        raceStats.put("Nani", naniStats);
        
        Map<String, Integer> dragonidiStats = new HashMap<>();
        dragonidiStats.put("vita", 125);
        dragonidiStats.put("velocità", 80);
        dragonidiStats.put("attack speed", 95);
        dragonidiStats.put("mana", 75);
        dragonidiStats.put("danno mag. avanzi", 85);
        dragonidiStats.put("res. magica avanzi", 150);
        dragonidiStats.put("danno mag. fuoco", 100);
        dragonidiStats.put("res. mag. fuoco", 100);
        dragonidiStats.put("danno mag. ghiaccio", 100);
        dragonidiStats.put("res. mag. ghiaccio", 100);
        dragonidiStats.put("danno mag. druido", 100);
        dragonidiStats.put("res. mag. druido", 100);
        dragonidiStats.put("danno mag. elettrico", 100);
        dragonidiStats.put("res. mag. elettrico", 100);
        dragonidiStats.put("danno mag. arcano", 100);
        dragonidiStats.put("res. mag. arcano", 100);
        dragonidiStats.put("danno mag. terra", 100);
        dragonidiStats.put("res. mag. terra", 100);
        dragonidiStats.put("danno mag. sangue", 100);
        dragonidiStats.put("res. mag. sangue", 100);
        dragonidiStats.put("danno fisico", 75);
        dragonidiStats.put("stamina", 110);
        dragonidiStats.put("res. fisica", 135);
        dragonidiStats.put("danno distanza", 80);
        dragonidiStats.put("res. distanza", 95);
        dragonidiStats.put("peso trasportato", 125);
        dragonidiStats.put("deb. caldo", 85);
        dragonidiStats.put("deb. freddo", 100);
        raceStats.put("Dragonidi", dragonidiStats);
        
        Map<String, Integer> orchiStats = new HashMap<>();
        orchiStats.put("vita", 100);
        orchiStats.put("velocità", 95);
        orchiStats.put("attack speed", 100);
        orchiStats.put("mana", 120);
        orchiStats.put("danno mag. avanzi", 70);
        orchiStats.put("res. magica avanzi", 70);
        orchiStats.put("danno mag. fuoco", 100);
        orchiStats.put("res. mag. fuoco", 100);
        orchiStats.put("danno mag. ghiaccio", 100);
        orchiStats.put("res. mag. ghiaccio", 100);
        orchiStats.put("danno mag. druido", 100);
        orchiStats.put("res. mag. druido", 100);
        orchiStats.put("danno mag. elettrico", 100);
        orchiStats.put("res. mag. elettrico", 100);
        orchiStats.put("danno mag. arcano", 100);
        orchiStats.put("res. mag. arcano", 100);
        orchiStats.put("danno mag. terra", 100);
        orchiStats.put("res. mag. terra", 100);
        orchiStats.put("danno mag. sangue", 100);
        orchiStats.put("res. mag. sangue", 100);
        orchiStats.put("danno fisico", 130);
        orchiStats.put("stamina", 120);
        orchiStats.put("res. fisica", 100);
        orchiStats.put("danno distanza", 65);
        orchiStats.put("res. distanza", 120);
        orchiStats.put("peso trasportato", 110);
        orchiStats.put("deb. caldo", 100);
        orchiStats.put("deb. freddo", 130);
        raceStats.put("Orchi", orchiStats);
        
        // Create RaceInfo objects with descriptions
        RACES.put("umano", new RaceInfo("Umano", 
            "Gli umani sono una razza versatile e bilanciata, adattabili a ogni situazione. " +
            "Con buone capacità sia magiche che fisiche, rappresentano il punto di riferimento per tutte le altre razze.",
            raceStats.get("Umani")));
            
        RACES.put("elfo", new RaceInfo("Elfo",
            "Gli elfi sono creature aggraziate e intelligenti, con una profonda connessione con la magia. " +
            "Eccellono nell'uso degli incantesimi e nel combattimento a distanza, ma sono più fragili nel corpo a corpo.",
            raceStats.get("Elfi")));
            
        RACES.put("nano", new RaceInfo("Nano",
            "I nani sono guerrieri resistenti e robusti, maestri nell'artiglieria e nel combattimento ravvicinato. " +
            "La loro bassa statura li rende bersagli difficili, e la loro forza è leggendaria.",
            raceStats.get("Nani")));
            
        RACES.put("dragonide", new RaceInfo("Dragonide",
            "I dragonidi sono discendenti dei draghi, creature imponenti con una resistenza magica senza pari. " +
            "Sebbene più lenti, possono sopportare danni che ucciderebbero altre razze e hanno una forza fisica formidabile.",
            raceStats.get("Dragonidi")));
            
        RACES.put("orcho", new RaceInfo("Orco",
            "Gli orchi sono guerrieri feroci e potenti, con una forza bruta che pochi possono eguagliare. " +
            "Sebbene meno raffinati, la loro resistenza e il loro potere fisico li rendono avversari temibili.",
            raceStats.get("Orchi")));
        
        loaded = true;
    }
    
    public static RaceInfo getRaceInfo(String raceName) {
        if (!loaded) loadRaceData();
        return RACES.get(raceName);
    }
    
    public static Map<String, RaceInfo> getAllRaces() {
        if (!loaded) loadRaceData();
        return new HashMap<>(RACES);
    }
}
