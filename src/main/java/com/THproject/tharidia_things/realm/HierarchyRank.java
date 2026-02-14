package com.THproject.tharidia_things.realm;

import org.joml.Vector3f;

/**
 * Represents the hierarchy rank of a player within a realm
 */
public enum HierarchyRank {
    COLONO(0, "Colono", new Vector3f(0.55f, 0.55f, 0.55f)),       // Gray stone
    MILIZIANO(1, "Miliziano", new Vector3f(0.35f, 0.70f, 0.35f)), // Forest green
    GUARDIA(2, "Guardia", new Vector3f(0.90f, 0.85f, 0.20f)),     // Pale yellow
    CONSIGLIERE(3, "Consigliere", new Vector3f(0.85f, 0.65f, 0.10f)), // Antique gold
    LORD(4, "Lord", new Vector3f(0.85f, 0.12f, 0.12f));           // Deep crimson

    private final int level;
    private final String displayName;
    private final Vector3f color;

    HierarchyRank(int level, String displayName, Vector3f color) {
        this.level = level;
        this.displayName = displayName;
        this.color = color;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the RGB color for this rank (for particles)
     */
    public Vector3f getColor() {
        return color;
    }

    /**
     * Gets the Minecraft color code for chat formatting
     */
    public String getChatColor() {
        return switch (this) {
            case LORD -> "§c";        // Red
            case CONSIGLIERE -> "§6"; // Gold
            case GUARDIA -> "§e";     // Yellow
            case MILIZIANO -> "§a";   // Green
            case COLONO -> "§7";      // Gray
        };
    }

    public static HierarchyRank fromLevel(int level) {
        for (HierarchyRank rank : values()) {
            if (rank.level == level) {
                return rank;
            }
        }
        return COLONO; // Default
    }

    public static HierarchyRank fromName(String name) {
        for (HierarchyRank rank : values()) {
            if (rank.displayName.equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return COLONO; // Default
    }
}
