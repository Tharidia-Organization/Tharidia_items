package com.THproject.tharidia_things.block.alchemist;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the interactive crafting session on the Alchemist Table.
 *
 * <h3>Session flow</h3>
 * <pre>
 *   [all jars full] → activate()
 *
 *   pickNextJar()                    — player right-clicks D1 (empty hand)
 *     └→ marks next jar used, jarsPickedCount++, tokenOut = true
 *        block entity gives the player an {@link AlchemistTokenItem}
 *
 *   setDummyFirstOperand(D, value)   — player uses token on an empty operation dummy
 *     └→ operandByDummy[D] = value; tokenOut = false
 *        block entity consumes the token from the player's hand
 *
 *   executeOperation(D, op, second)  — player uses token on a primed operation dummy
 *     └→ result = operandByDummy[D] op second
 *        if jarsPickedCount < 4: block entity gives result token, tokenOut = true
 *        if jarsPickedCount == 4 (final): block entity stores in result table, tokenOut = false
 *
 *   reset() → back to inactive
 * </pre>
 */
public class AlchemistCraftingSession {

    private boolean active = false;

    /**
     * True when the block entity has given the player a token that has not yet been
     * consumed by an operation interaction. Prevents picking a second jar while the
     * first token is still outstanding.
     */
    private boolean tokenOut = false;

    /** Tracks which of the 4 jar slots have already been picked. */
    private final boolean[] jarUsed = new boolean[4];

    /** How many jars have been picked so far (0-4). */
    private int jarsPickedCount = 0;

    /**
     * First operand waiting per operation dummy.
     * Key = dummy block index (0, 2, 3, 4). Value = stored first operand.
     */
    private final Map<Integer, Integer> operandByDummy = new HashMap<>();

    // ==================== Lifecycle ====================

    public boolean isActive() { return active; }

    public void activate() {
        active = true;
        tokenOut = false;
        Arrays.fill(jarUsed, false);
        jarsPickedCount = 0;
        operandByDummy.clear();
    }

    public void reset() {
        active = false;
        tokenOut = false;
        Arrays.fill(jarUsed, false);
        jarsPickedCount = 0;
        operandByDummy.clear();
    }

    // ==================== Jar Picking ====================

    /** True when the player holds a token that has not yet been applied to an operation. */
    public boolean isTokenOut() { return tokenOut; }

    public void setTokenOut(boolean value) { tokenOut = value; }

    /**
     * Marks the next unused jar as used and increments the counter.
     *
     * @return the picked jar index (0-3), or -1 if all jars are already used
     */
    public int pickNextJar() {
        for (int i = 0; i < 4; i++) {
            if (!jarUsed[i]) {
                jarUsed[i] = true;
                jarsPickedCount++;
                tokenOut = true;
                return i;
            }
        }
        return -1;
    }

    // ==================== Operation Interactions ====================

    /** True if the given operation dummy already has a first operand waiting. */
    public boolean hasDummyOperand(int dummyIndex) {
        return operandByDummy.containsKey(dummyIndex);
    }

    /** Returns the first operand stored for {@code dummyIndex} without removing it, or null. */
    @Nullable
    public Integer getDummyOperand(int dummyIndex) {
        return operandByDummy.get(dummyIndex);
    }

    /**
     * Stores {@code value} as the first operand of {@code dummyIndex}.
     * Marks the token as consumed.
     */
    public void setDummyFirstOperand(int dummyIndex, int value) {
        operandByDummy.put(dummyIndex, value);
        tokenOut = false;
    }

    /**
     * Executes the operation on {@code dummyIndex} using {@code secondValue} as the second operand.
     * Removes the first operand from the dummy. The caller is responsible for giving/consuming tokens.
     *
     * @return the computed result
     */
    public int executeOperation(int dummyIndex, AlchemistOperation op, int secondValue) {
        int first  = operandByDummy.remove(dummyIndex);
        return op.apply(first, secondValue);
    }

    /**
     * True when all 4 jars have been picked — the next completed operation produces
     * the final result that goes to the result table instead of back to the player.
     */
    public boolean isFinalResult() { return jarsPickedCount >= 4; }

    public int getJarsPickedCount() { return jarsPickedCount; }

    // ==================== NBT ====================

    public void save(CompoundTag tag) {
        tag.putBoolean("SessionActive", active);
        tag.putBoolean("TokenOut", tokenOut);
        int[] usedArr = new int[4];
        for (int i = 0; i < 4; i++) usedArr[i] = jarUsed[i] ? 1 : 0;
        tag.putIntArray("JarUsed", usedArr);
        tag.putInt("JarsPickedCount", jarsPickedCount);
        CompoundTag operandsTag = new CompoundTag();
        operandByDummy.forEach((idx, val) -> operandsTag.putInt("D" + idx, val));
        tag.put("DummyOperands", operandsTag);
    }

    public void load(CompoundTag tag) {
        active = tag.getBoolean("SessionActive");
        tokenOut = tag.getBoolean("TokenOut");
        int[] usedArr = tag.getIntArray("JarUsed");
        for (int i = 0; i < 4 && i < usedArr.length; i++) jarUsed[i] = usedArr[i] == 1;
        jarsPickedCount = tag.getInt("JarsPickedCount");
        operandByDummy.clear();
        if (tag.contains("DummyOperands")) {
            CompoundTag op = tag.getCompound("DummyOperands");
            for (String key : op.getAllKeys()) {
                if (key.length() > 1 && key.charAt(0) == 'D') {
                    try { operandByDummy.put(Integer.parseInt(key.substring(1)), op.getInt(key)); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
    }
}
