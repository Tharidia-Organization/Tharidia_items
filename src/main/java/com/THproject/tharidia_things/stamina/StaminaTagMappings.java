package com.THproject.tharidia_things.stamina;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class StaminaTagMappings {
    private static final Map<String, StaminaModifier> TAG_TO_MODIFIER = new HashMap<>();

    private StaminaTagMappings() {
    }

    public static void clear() {
        TAG_TO_MODIFIER.clear();
    }

    public static void put(String tagId, StaminaModifier modifier) {
        if (tagId == null || tagId.isBlank() || modifier == null) {
            return;
        }
        TAG_TO_MODIFIER.put(tagId, modifier);
    }

    public static StaminaModifier get(String tagId) {
        if (tagId == null || tagId.isBlank()) {
            return null;
        }
        return TAG_TO_MODIFIER.get(tagId);
    }

    public static void remove(String tagId) {
        if (tagId == null || tagId.isBlank()) {
            return;
        }
        TAG_TO_MODIFIER.remove(tagId);
    }

    public static Map<String, StaminaModifier> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(TAG_TO_MODIFIER));
    }
}

