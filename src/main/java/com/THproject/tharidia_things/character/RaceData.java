package com.THproject.tharidia_things.character;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads and stores race characteristics
 */
public class RaceData {
    private static final Map<String, RaceInfo> RACES = new HashMap<>();
    private static boolean loaded = false;

    public static class RaceInfo {
        public final String name;
        public final String description;
        public final Map<String, Integer> characteristics;

        public RaceInfo(String name, String description, Map<String, Integer> characteristics) {
            this.name = name;
            this.description = description;
            this.characteristics = characteristics;
        }
    }

    /**
     * Creates a stats map with consistent Italian keys and maintains insertion order
     */
    private static Map<String, Integer> createStatsMap() {
        return new LinkedHashMap<>();
    }

    public static void loadRaceData() {
        if (loaded) return;

        // Initialize races with their characteristics
        Map<String, Map<String, Integer>> raceStats = new HashMap<>();

        // Umani - razza bilanciata
        Map<String, Integer> umaniStats = createStatsMap();
        umaniStats.put("Vita", 100);
        umaniStats.put("Velocità", 100);
        umaniStats.put("Velocità Attacco", 95);
        umaniStats.put("Mana", 115);
        umaniStats.put("Danno Mag. Avanzi", 105);
        umaniStats.put("Res. Mag. Avanzi", 100);
        umaniStats.put("Danno Mag. Fuoco", 100);
        umaniStats.put("Res. Mag. Fuoco", 100);
        umaniStats.put("Danno Mag. Ghiaccio", 100);
        umaniStats.put("Res. Mag. Ghiaccio", 100);
        umaniStats.put("Danno Mag. Druido", 100);
        umaniStats.put("Res. Mag. Druido", 100);
        umaniStats.put("Danno Mag. Elettrico", 100);
        umaniStats.put("Res. Mag. Elettrico", 100);
        umaniStats.put("Danno Mag. Arcano", 100);
        umaniStats.put("Res. Mag. Arcano", 100);
        umaniStats.put("Danno Mag. Terra", 100);
        umaniStats.put("Res. Mag. Terra", 100);
        umaniStats.put("Danno Mag. Sangue", 100);
        umaniStats.put("Res. Mag. Sangue", 100);
        umaniStats.put("Danno Fisico", 110);
        umaniStats.put("Stamina", 100);
        umaniStats.put("Res. Fisica", 100);
        umaniStats.put("Danno Distanza", 105);
        umaniStats.put("Res. Distanza", 85);
        umaniStats.put("Peso Trasportato", 90);
        umaniStats.put("Deb. Caldo", 90);
        umaniStats.put("Deb. Freddo", 90);
        raceStats.put("Umani", umaniStats);

        // Elfi - agili e magici
        Map<String, Integer> elfiStats = createStatsMap();
        elfiStats.put("Vita", 90);
        elfiStats.put("Velocità", 110);
        elfiStats.put("Velocità Attacco", 105);
        elfiStats.put("Mana", 95);
        elfiStats.put("Danno Mag. Avanzi", 130);
        elfiStats.put("Res. Mag. Avanzi", 100);
        elfiStats.put("Danno Mag. Fuoco", 100);
        elfiStats.put("Res. Mag. Fuoco", 100);
        elfiStats.put("Danno Mag. Ghiaccio", 100);
        elfiStats.put("Res. Mag. Ghiaccio", 100);
        elfiStats.put("Danno Mag. Druido", 100);
        elfiStats.put("Res. Mag. Druido", 100);
        elfiStats.put("Danno Mag. Elettrico", 100);
        elfiStats.put("Res. Mag. Elettrico", 100);
        elfiStats.put("Danno Mag. Arcano", 100);
        elfiStats.put("Res. Mag. Arcano", 100);
        elfiStats.put("Danno Mag. Terra", 100);
        elfiStats.put("Res. Mag. Terra", 100);
        elfiStats.put("Danno Mag. Sangue", 100);
        elfiStats.put("Res. Mag. Sangue", 100);
        elfiStats.put("Danno Fisico", 95);
        elfiStats.put("Stamina", 100);
        elfiStats.put("Res. Fisica", 75);
        elfiStats.put("Danno Distanza", 110);
        elfiStats.put("Res. Distanza", 80);
        elfiStats.put("Peso Trasportato", 80);
        elfiStats.put("Deb. Caldo", 150);
        elfiStats.put("Deb. Freddo", 70);
        raceStats.put("Elfi", elfiStats);

        // Nani - resistenti e forti
        Map<String, Integer> naniStats = createStatsMap();
        naniStats.put("Vita", 85);
        naniStats.put("Velocità", 115);
        naniStats.put("Velocità Attacco", 105);
        naniStats.put("Mana", 95);
        naniStats.put("Danno Mag. Avanzi", 110);
        naniStats.put("Res. Mag. Avanzi", 80);
        naniStats.put("Danno Mag. Fuoco", 100);
        naniStats.put("Res. Mag. Fuoco", 100);
        naniStats.put("Danno Mag. Ghiaccio", 100);
        naniStats.put("Res. Mag. Ghiaccio", 100);
        naniStats.put("Danno Mag. Druido", 100);
        naniStats.put("Res. Mag. Druido", 100);
        naniStats.put("Danno Mag. Elettrico", 100);
        naniStats.put("Res. Mag. Elettrico", 100);
        naniStats.put("Danno Mag. Arcano", 100);
        naniStats.put("Res. Mag. Arcano", 100);
        naniStats.put("Danno Mag. Terra", 100);
        naniStats.put("Res. Mag. Terra", 100);
        naniStats.put("Danno Mag. Sangue", 100);
        naniStats.put("Res. Mag. Sangue", 100);
        naniStats.put("Danno Fisico", 90);
        naniStats.put("Stamina", 70);
        naniStats.put("Res. Fisica", 90);
        naniStats.put("Danno Distanza", 140);
        naniStats.put("Res. Distanza", 120);
        naniStats.put("Peso Trasportato", 95);
        naniStats.put("Deb. Caldo", 75);
        naniStats.put("Deb. Freddo", 110);
        raceStats.put("Nani", naniStats);

        // Dragonidi - potenti e resistenti
        Map<String, Integer> dragonidiStats = createStatsMap();
        dragonidiStats.put("Vita", 125);
        dragonidiStats.put("Velocità", 80);
        dragonidiStats.put("Velocità Attacco", 95);
        dragonidiStats.put("Mana", 75);
        dragonidiStats.put("Danno Mag. Avanzi", 85);
        dragonidiStats.put("Res. Mag. Avanzi", 150);
        dragonidiStats.put("Danno Mag. Fuoco", 100);
        dragonidiStats.put("Res. Mag. Fuoco", 100);
        dragonidiStats.put("Danno Mag. Ghiaccio", 100);
        dragonidiStats.put("Res. Mag. Ghiaccio", 100);
        dragonidiStats.put("Danno Mag. Druido", 100);
        dragonidiStats.put("Res. Mag. Druido", 100);
        dragonidiStats.put("Danno Mag. Elettrico", 100);
        dragonidiStats.put("Res. Mag. Elettrico", 100);
        dragonidiStats.put("Danno Mag. Arcano", 100);
        dragonidiStats.put("Res. Mag. Arcano", 100);
        dragonidiStats.put("Danno Mag. Terra", 100);
        dragonidiStats.put("Res. Mag. Terra", 100);
        dragonidiStats.put("Danno Mag. Sangue", 100);
        dragonidiStats.put("Res. Mag. Sangue", 100);
        dragonidiStats.put("Danno Fisico", 75);
        dragonidiStats.put("Stamina", 110);
        dragonidiStats.put("Res. Fisica", 135);
        dragonidiStats.put("Danno Distanza", 80);
        dragonidiStats.put("Res. Distanza", 95);
        dragonidiStats.put("Peso Trasportato", 125);
        dragonidiStats.put("Deb. Caldo", 85);
        dragonidiStats.put("Deb. Freddo", 100);
        raceStats.put("Dragonidi", dragonidiStats);

        // Orchi - guerrieri feroci
        Map<String, Integer> orchiStats = createStatsMap();
        orchiStats.put("Vita", 100);
        orchiStats.put("Velocità", 95);
        orchiStats.put("Velocità Attacco", 100);
        orchiStats.put("Mana", 120);
        orchiStats.put("Danno Mag. Avanzi", 70);
        orchiStats.put("Res. Mag. Avanzi", 70);
        orchiStats.put("Danno Mag. Fuoco", 100);
        orchiStats.put("Res. Mag. Fuoco", 100);
        orchiStats.put("Danno Mag. Ghiaccio", 100);
        orchiStats.put("Res. Mag. Ghiaccio", 100);
        orchiStats.put("Danno Mag. Druido", 100);
        orchiStats.put("Res. Mag. Druido", 100);
        orchiStats.put("Danno Mag. Elettrico", 100);
        orchiStats.put("Res. Mag. Elettrico", 100);
        orchiStats.put("Danno Mag. Arcano", 100);
        orchiStats.put("Res. Mag. Arcano", 100);
        orchiStats.put("Danno Mag. Terra", 100);
        orchiStats.put("Res. Mag. Terra", 100);
        orchiStats.put("Danno Mag. Sangue", 100);
        orchiStats.put("Res. Mag. Sangue", 100);
        orchiStats.put("Danno Fisico", 130);
        orchiStats.put("Stamina", 120);
        orchiStats.put("Res. Fisica", 100);
        orchiStats.put("Danno Distanza", 65);
        orchiStats.put("Res. Distanza", 120);
        orchiStats.put("Peso Trasportato", 110);
        orchiStats.put("Deb. Caldo", 100);
        orchiStats.put("Deb. Freddo", 130);
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

        // FIXED: "orcho" -> "orco"
        RACES.put("orco", new RaceInfo("Orco",
            "Gli orchi sono guerrieri feroci e potenti, con una forza bruta che pochi possono eguagliare. " +
            "Sebbene meno raffinati, la loro resistenza e il loro potere fisico li rendono avversari temibili.",
            raceStats.get("Orchi")));

        loaded = true;
    }

    public static RaceInfo getRaceInfo(String raceName) {
        if (!loaded) loadRaceData();
        if (raceName == null) return null;
        // Normalize race name to lowercase for lookup
        return RACES.get(raceName.toLowerCase());
    }

    public static Map<String, RaceInfo> getAllRaces() {
        if (!loaded) loadRaceData();
        return new HashMap<>(RACES);
    }

    /**
     * Check if a race name is valid
     */
    public static boolean isValidRace(String raceName) {
        if (!loaded) loadRaceData();
        return raceName != null && RACES.containsKey(raceName.toLowerCase());
    }

    /**
     * Get all valid race names
     */
    public static String[] getValidRaceNames() {
        if (!loaded) loadRaceData();
        return RACES.keySet().toArray(new String[0]);
    }
}
