package com.THproject.tharidia_things.stamina;

public final class StaminaTagIntegration {
    private StaminaTagIntegration() {
    }

    public static void registerTagMapping(String tagId, StaminaModifierType type, float value, boolean isPercentage, int priority) {
        if (tagId == null || tagId.isBlank() || type == null) {
            return;
        }
        StaminaTagMappings.put(tagId, new StaminaModifier(type, value, isPercentage, tagId, priority));
    }

    public static void unregisterTag(String tagId) {
        StaminaTagMappings.remove(tagId);
    }

    public static StaminaModifier getModifierForTag(String tagId) {
        return StaminaTagMappings.get(tagId);
    }
}

