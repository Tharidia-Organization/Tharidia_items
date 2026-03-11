package com.THproject.tharidia_things.block.alchemist;

import org.jetbrains.annotations.Nullable;

/**
 * The four arithmetic operations available on the Alchemist Table.
 * Each is permanently bound to a specific dummy block index.
 */
public enum AlchemistOperation {

    ADD     (0, "+"),
    SUBTRACT(2, "-"),
    DIVIDE  (3, "÷"),
    MULTIPLY(4, "×");

    /** Dummy block index that triggers this operation on right-click. */
    public final int dummyIndex;
    /** Display symbol for player feedback messages. */
    public final String symbol;

    AlchemistOperation(int dummyIndex, String symbol) {
        this.dummyIndex = dummyIndex;
        this.symbol = symbol;
    }

    /**
     * Applies this operation to two integer operands.
     * Division by zero returns 0 to avoid crashes.
     */
    public int apply(int a, int b) {
        return switch (this) {
            case ADD      -> a + b;
            case SUBTRACT -> a - b;
            case MULTIPLY -> a * b;
            case DIVIDE   -> b != 0 ? a / b : 0;
        };
    }

    /** Returns the operation bound to {@code dummyIndex}, or {@code null} if none. */
    @Nullable
    public static AlchemistOperation fromDummyIndex(int dummyIndex) {
        for (AlchemistOperation op : values()) {
            if (op.dummyIndex == dummyIndex) return op;
        }
        return null;
    }
}
