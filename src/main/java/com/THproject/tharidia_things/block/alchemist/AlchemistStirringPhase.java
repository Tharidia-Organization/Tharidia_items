package com.THproject.tharidia_things.block.alchemist;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * State machine for the final stirring phase of the Alchemist Table.
 *
 * Flow:
 *   IDLE
 *     → (resultJarCount == 3 + first stir) → STIRRING
 *   STIRRING  (timer counts only while player stirs each tick)
 *     → (timer reaches 0) → pick random unfinished jar → JAR_SHAKING
 *   JAR_SHAKING  (the jar shakes to signal the player must touch it)
 *     → (player touches the correct output-jar Interaction entity) → JAR_POURING
 *   JAR_POURING  (jar_f_X_drop plays: the jar is poured into the cauldron)
 *     → (animation timer done + jars remain) → STIRRING
 *     → (animation timer done + all jars done)  → COMPLETE
 *   COMPLETE
 *     → AlchemistTableBlockEntity.onStirringComplete() is called
 */
public class AlchemistStirringPhase {

    /** Minimum ticks the player must stir before a jar starts shaking (0.75 s). */
    private static final int MIN_STIR_TICKS = 15;
    /** Maximum ticks the player must stir before a jar starts shaking (2 s). */
    private static final int MAX_STIR_TICKS = 40;
    /**
     * Ticks to wait while the pouring animation plays before moving to the next jar
     * or completing (≈ 2 s; adjust to match the animation length).
     */
    private static final int POUR_ANIMATION_TICKS = 40;

    /** Ticks of final stirring after the last jar is poured before completion (1 s). */
    private static final int COMPLETING_TICKS = 20;

    /** GeckoLib animation controller for all output-jar animations. */
    private static final String JAR_CONTROLLER = "jar_f_controller";

    public enum State { IDLE, STIRRING, JAR_SHAKING, JAR_POURING, COMPLETING, COMPLETE }

    // ── fields ───────────────────────────────────────────────────────────────

    private final AlchemistTableBlockEntity be;
    private final Random random = new Random();

    private State state = State.IDLE;
    /** Remaining jar indices (0-2) not yet poured, in shuffled order. */
    private final List<Integer> jarQueue = new ArrayList<>();
    /** Index of the jar currently shaking or being poured (-1 if none). */
    private int activeJar = -1;
    /** Countdown ticker (stir ticks or pour ticks). */
    private int timer = 0;

    // ── constructor ──────────────────────────────────────────────────────────

    public AlchemistStirringPhase(AlchemistTableBlockEntity be) {
        this.be = be;
    }

    // ── public API ───────────────────────────────────────────────────────────

    /**
     * Called from {@link AlchemistTableBlockEntity#serverTick} every server tick.
     * {@code isBeingStirred} reflects whether the player stirred the cauldron this tick.
     */
    public void tick(boolean isBeingStirred) {
        switch (state) {
            case STIRRING -> {
                if (isBeingStirred && --timer <= 0) {
                    pickAndShakeNextJar();
                }
            }
            case JAR_POURING -> {
                if (--timer <= 0) {
                    onPourComplete();
                }
            }
            case COMPLETING -> {
                if (--timer <= 0) {
                    state = State.COMPLETE;
                    be.spawnCompletionParticles();
                    be.onStirringComplete();
                    be.syncToClient();
                }
            }
            default -> { /* IDLE / JAR_SHAKING / COMPLETE: nothing to count */ }
        }
    }

    /**
     * Called when the player interacts with the cauldron Interaction entity.
     * Activates stirring if all 3 result jars are full.
     */
    public void onStir(Player player) {
        if (state == State.IDLE) {
            if (be.getResultJarCount() < 3) {
                player.displayClientMessage(
                        Component.literal("Fill all 3 result jars first!"), true);
                return;
            }
            if (!be.hasCauldronWater()) return; // guard: message already shown in stir()
            activate();
            player.displayClientMessage(
                    Component.literal("The cauldron awakens — keep stirring!").withColor(0x00FFCC), true);
            return;
        }
        if (state == State.JAR_SHAKING) {
            player.displayClientMessage(
                    Component.literal("→ Touch the shaking jar!").withColor(0xFFAA00), true);
        }
    }

    /**
     * Called when the player touches an output-jar Interaction entity.
     *
     * @param jarIndex 0-based index of the touched output jar (matches Jar_final order)
     * @param player   the interacting player
     */
    public void onJarClicked(int jarIndex, Player player) {
        if (state != State.JAR_SHAKING) {
            be.displayResultJars(player);
            return;
        }
        if (jarIndex != activeJar) {
            player.displayClientMessage(
                    Component.literal("That's not the shaking jar!").withColor(0xFF4444), true);
            return;
        }
        // Correct jar touched — stop shake, pour into cauldron
        int humanIndex = activeJar + 1;
        be.triggerAnim(JAR_CONTROLLER, "jar_f_" + humanIndex + "_drop");
        state = State.JAR_POURING;
        timer = POUR_ANIMATION_TICKS;
        player.displayClientMessage(
                Component.literal("✓ Jar " + humanIndex + " poured!").withColor(0x00FF88), true);
        be.syncToClient();
    }

    /** @return whether the stirring phase is currently active (not IDLE or COMPLETE). */
    public boolean isActive() {
        return state != State.IDLE && state != State.COMPLETE;
    }

    /** @return whether the phase is in its final countdown (particles about to spawn). */
    public boolean isCompleting() { return state == State.COMPLETING; }

    public State getState()     { return state; }
    public int   getActiveJar() { return activeJar; }

    // ── private helpers ──────────────────────────────────────────────────────

    private void activate() {
        jarQueue.clear();
        jarQueue.add(0); jarQueue.add(1); jarQueue.add(2);
        Collections.shuffle(jarQueue, random);
        state = State.STIRRING;
        timer = randomStirTicks();
        be.syncToClient();
    }

    private void pickAndShakeNextJar() {
        if (jarQueue.isEmpty()) {
            state = State.COMPLETE;
            be.onStirringComplete();
            return;
        }
        activeJar = jarQueue.remove(0);
        int humanIndex = activeJar + 1;
        be.triggerAnim(JAR_CONTROLLER, "jar_f_" + humanIndex);
        state = State.JAR_SHAKING;
        be.syncToClient();
    }

    private void onPourComplete() {
        activeJar = -1;
        if (jarQueue.isEmpty()) {
            // All jars poured — short countdown before the magic finale
            state = State.COMPLETING;
            timer = COMPLETING_TICKS;
        } else {
            state = State.STIRRING;
            timer = randomStirTicks();
        }
        be.syncToClient();
    }

    private int randomStirTicks() {
        return MIN_STIR_TICKS + random.nextInt(MAX_STIR_TICKS - MIN_STIR_TICKS + 1);
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    public void save(CompoundTag tag) {
        tag.putString("StirState", state.name());
        tag.putInt("StirActiveJar", activeJar);
        tag.putInt("StirTimer", timer);
        tag.putIntArray("StirJarQueue",
                jarQueue.stream().mapToInt(Integer::intValue).toArray());
    }

    public void load(CompoundTag tag) {
        try { state = State.valueOf(tag.getString("StirState")); }
        catch (Exception e) { state = State.IDLE; }
        activeJar = tag.getInt("StirActiveJar");
        timer     = tag.getInt("StirTimer");
        jarQueue.clear();
        for (int v : tag.getIntArray("StirJarQueue")) jarQueue.add(v);
    }
}
