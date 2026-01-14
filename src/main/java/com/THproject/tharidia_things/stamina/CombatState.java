package com.THproject.tharidia_things.stamina;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatState {
    private CombatState() {
    }

    private static final Map<UUID, Integer> LAST_ENTER_TICK = new HashMap<>();
    private static final Map<UUID, String> LAST_ENTER_REASON = new HashMap<>();

    public record EnterCombatResult(boolean applied, boolean wasInCombat, String previousReason) {
    }

    public static EnterCombatResult enterCombat(Player player, StaminaData data, int timeoutTicks, String reason) {
        if (player == null || data == null) {
            return new EnterCombatResult(false, false, null);
        }
        if (timeoutTicks <= 0) {
            return new EnterCombatResult(false, data.isInCombat(), null);
        }

        UUID id = player.getUUID();
        int tick = player.tickCount;
        Integer lastTick = LAST_ENTER_TICK.get(id);
        if (lastTick != null && lastTick == tick) {
            return new EnterCombatResult(false, data.isInCombat(), LAST_ENTER_REASON.get(id));
        }
        LAST_ENTER_TICK.put(id, tick);

        boolean wasInCombat = data.isInCombat();
        String previousReason = LAST_ENTER_REASON.put(id, reason);

        data.setCombatTicksRemaining(timeoutTicks);
        data.setInCombat(true);
        return new EnterCombatResult(true, wasInCombat, previousReason);
    }

    public static void clear(UUID playerId) {
        if (playerId == null) {
            return;
        }
        LAST_ENTER_TICK.remove(playerId);
        LAST_ENTER_REASON.remove(playerId);
    }

    public static void tickCombat(StaminaData data) {
        if (data == null) {
            return;
        }

        int remaining = data.getCombatTicksRemaining();
        if (remaining > 0) {
            data.setCombatTicksRemaining(remaining - 1);
            data.setInCombat(true);
            return;
        }

        if (data.isInCombat()) {
            data.setInCombat(false);
        }
    }
}
