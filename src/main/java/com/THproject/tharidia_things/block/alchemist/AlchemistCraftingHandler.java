package com.THproject.tharidia_things.block.alchemist;

import net.minecraft.nbt.CompoundTag;

/**
 * Manages the crafting state machine for the Alchemist Table.
 *
 * <p>On each phase transition, {@link AlchemistTableBlockEntity#onCraftingPhaseChanged} is called
 * so the block entity can trigger animations and sync state to the client.
 */
public class AlchemistCraftingHandler {

    private final AlchemistTableBlockEntity owner;

    private AlchemistCraftingPhase phase = AlchemistCraftingPhase.IDLE;

    public AlchemistCraftingHandler(AlchemistTableBlockEntity owner) {
        this.owner = owner;
    }

    // ==================== Control ====================

    /**
     * Initiates the crafting sequence. Called when dummy index 1 is right-clicked.
     *
     * @return {@code true} if the sequence started successfully.
     */
    public boolean startSequence() {
        if (phase != AlchemistCraftingPhase.IDLE) return false;
        // TODO: validate that the required ingredients are present
        enterPhase(AlchemistCraftingPhase.PROCESSING);
        return true;
    }

    /** Aborts any in-progress sequence and resets to IDLE. */
    public void abort() {
        enterPhase(AlchemistCraftingPhase.IDLE);
    }

    // ==================== Tick ====================

    /** Must be called once per server tick from {@link AlchemistTableBlockEntity#serverTick}. */
    public void serverTick() {}

    // ==================== Phase Transitions ====================

    private void enterPhase(AlchemistCraftingPhase newPhase) {
        phase = newPhase;
        owner.onCraftingPhaseChanged(newPhase);
    }

    // ==================== Accessors ====================

    public AlchemistCraftingPhase getPhase() {
        return phase;
    }

    public boolean isIdle() {
        return phase == AlchemistCraftingPhase.IDLE;
    }


    // ==================== NBT ====================

    public void save(CompoundTag tag) {
        tag.putString("CraftingPhase", phase.name());
    }

    public void load(CompoundTag tag) {
        String phaseName = tag.getString("CraftingPhase");
        try {
            phase = phaseName.isEmpty()
                    ? AlchemistCraftingPhase.IDLE
                    : AlchemistCraftingPhase.valueOf(phaseName);
        } catch (IllegalArgumentException e) {
            phase = AlchemistCraftingPhase.IDLE;
        }
    }
}
