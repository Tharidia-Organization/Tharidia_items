package com.THproject.tharidia_things.stamina;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TagModifierBridge {
    private static final Map<UUID, Set<String>> LAST_TAGS = new HashMap<>();

    private TagModifierBridge() {
    }

    public static void refresh(ServerPlayer player, StaminaData data) {
        UUID id = player.getUUID();
        Set<String> currentTags = new HashSet<>(player.getTags());
        Set<String> lastTags = LAST_TAGS.getOrDefault(id, Set.of());

        for (String tag : currentTags) {
            StaminaModifier modifier = StaminaTagIntegration.getModifierForTag(tag);
            if (modifier != null) {
                data.removeModifiersBySource(tag);
                data.addModifier(modifier);
            }
        }

        for (String tag : lastTags) {
            if (!currentTags.contains(tag)) {
                data.removeModifiersBySource(tag);
            }
        }

        LAST_TAGS.put(id, currentTags);
    }

    public static void clear(UUID playerId) {
        LAST_TAGS.remove(playerId);
    }
}
