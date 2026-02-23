package com.THproject.tharidia_things.spice;

import net.minecraft.network.chat.TextColor;

public enum SpiceType {
    COCA("coca", TextColor.fromRgb(0x2E8B57), "spice.tharidiathings.coca"),
    SPIRU("spiru", TextColor.fromRgb(0x4169E1), "spice.tharidiathings.spiru");

    public static final SpiceType[] VALUES = values();

    private final String name;
    private final TextColor color;
    private final String translationKey;

    SpiceType(String name, TextColor color, String translationKey) {
        this.name = name;
        this.color = color;
        this.translationKey = translationKey;
    }

    public String getName() {
        return name;
    }

    public TextColor getColor() {
        return color;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public static SpiceType byName(String name) {
        for (SpiceType type : VALUES) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
