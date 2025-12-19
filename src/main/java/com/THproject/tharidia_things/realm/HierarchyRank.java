package com.THproject.tharidia_things.realm;

/**
 * Represents the hierarchy rank of a player within a realm
 */
public enum HierarchyRank {
    COLONO(0, "Colono"),
    MILIZIANO(1, "Miliziano"),
    GUARDIA(2, "Guardia"),
    CONSIGLIERE(3, "Consigliere"),
    LORD(4, "Lord");
    
    private final int level;
    private final String displayName;
    
    HierarchyRank(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDisplayName() {
        return displayName;
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
