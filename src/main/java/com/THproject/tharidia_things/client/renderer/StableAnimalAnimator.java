package com.THproject.tharidia_things.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Advanced animation system for stable animals.
 * Creates lifelike animations that work for ANY mob (vanilla or modded)
 * using very slow, smooth rotations only.
 */
public class StableAnimalAnimator {

    private static final Map<Long, AnimalAnimationState> animationStates = new HashMap<>();

    // VERY slow and subtle animation - all values reduced for smooth movement
    private static final float BREATH_INTENSITY = 0.25f;     // Very subtle breathing rotation
    private static final float SWAY_INTENSITY = 0.4f;        // Very gentle sway
    private static final float IDLE_ACTION_CHANCE = 0.0003f; // Rare actions

    // Interpolation speeds - VERY slow for smooth transitions
    private static final float LERP_SPEED_FAST = 0.015f;     // For body movements
    private static final float LERP_SPEED_SLOW = 0.008f;     // For head movements (slower = smoother)
    private static final float LERP_SPEED_RETURN = 0.003f;   // For returning to neutral (very slow)

    public static class AnimalAnimationState {
        public final long seed;
        public final Random random;

        // Current idle action
        public IdleAction currentAction = IdleAction.NONE;
        public float actionProgress = 0;
        public float actionDuration = 0;

        // Smoothly interpolated values (current state)
        public float currentHeadYaw = 0;
        public float currentHeadPitch = 0;
        public float currentBodyRoll = 0;
        public float currentBodyPitch = 0;

        // Target values (what we're interpolating towards)
        public float targetHeadYaw = 0;
        public float targetHeadPitch = 0;
        public float targetBodyRoll = 0;
        public float targetBodyPitch = 0;

        // Energy level affects animation speed
        public float energyLevel = 1.0f;

        // Continuous time for sine wave animations
        public float localTime = 0;
        public long lastUpdateTime = 0;

        // Breathing phase
        public float breathPhase = 0;

        public AnimalAnimationState(long seed) {
            this.seed = seed;
            this.random = new Random(seed);
            this.localTime = random.nextFloat() * 100f;
            this.breathPhase = random.nextFloat() * Mth.TWO_PI;
        }
    }

    public enum IdleAction {
        NONE(0),
        LOOK_LEFT(4.0f),       // Longer duration = slower movement
        LOOK_RIGHT(4.0f),
        LOOK_UP(3.5f),
        LOOK_DOWN(5.0f),
        WEIGHT_SHIFT(5.0f),
        SETTLE(4.0f);

        public final float baseDuration;

        IdleAction(float duration) {
            this.baseDuration = duration;
        }
    }

    public static AnimalAnimationState getAnimationState(long stableId, int animalIndex) {
        long key = stableId * 10 + animalIndex;
        return animationStates.computeIfAbsent(key, k -> new AnimalAnimationState(k));
    }

    public static void applyAnimations(
            PoseStack poseStack,
            AnimalAnimationState state,
            EntityType<?> entityType,
            boolean isBaby,
            boolean isResting,
            int wellness,
            long gameTime,
            float partialTick
    ) {
        // Very slow time progression
        float deltaTime = 0.05f;
        state.localTime += deltaTime;

        // Very slow breathing
        state.breathPhase += deltaTime * (isResting ? 0.04f : 0.06f);

        // Energy level
        float targetEnergy = isResting ? 0.3f : Mth.clamp(wellness / 100f, 0.5f, 1.0f);
        state.energyLevel = Mth.lerp(0.01f, state.energyLevel, targetEnergy);

        // Update idle action system
        updateIdleAction(state, isResting);

        // Update all interpolated values with VERY smooth lerp
        updateInterpolation(state, isResting);

        // ============ APPLY TRANSFORMATIONS ============
        poseStack.pushPose();

        if (isResting) {
            // ============ SLEEPING POSE ============
            // Rotate to make animal appear lying down on its side

            // Roll to the side (lying down) - 70 degrees
            poseStack.mulPose(Axis.ZP.rotationDegrees(70f + state.currentBodyRoll * 0.2f));

            // Compensate position after rotation: move right and down to ground level
            poseStack.translate(1.2f, -0.7f, 0);

            // Slight forward lean (head resting)
            poseStack.mulPose(Axis.XP.rotationDegrees(10f + state.currentBodyPitch * 0.2f));

            // Very subtle breathing movement while sleeping
            float sleepBreath = Mth.sin(state.breathPhase) * 0.4f;
            poseStack.mulPose(Axis.XP.rotationDegrees(sleepBreath));

            // Occasional tiny movement (dreaming/twitching)
            float twitch = Mth.sin(state.localTime * 0.03f) * 0.3f;
            poseStack.mulPose(Axis.ZP.rotationDegrees(twitch));

        } else {
            // ============ AWAKE POSE ============

            // 1. BREATHING - Very subtle, slow forward/back
            float breathAmount = Mth.sin(state.breathPhase) * BREATH_INTENSITY;
            poseStack.mulPose(Axis.XP.rotationDegrees(breathAmount));

            // 2. BODY SWAY - Very gentle, slow
            float swayPhase = state.localTime * 0.015f * state.energyLevel;
            float sway = Mth.sin(swayPhase) * SWAY_INTENSITY * 0.4f;
            poseStack.mulPose(Axis.ZP.rotationDegrees(sway + state.currentBodyRoll));

            // Very subtle forward/back
            float rockPhase = state.localTime * 0.012f * state.energyLevel;
            float rock = Mth.sin(rockPhase) * SWAY_INTENSITY * 0.2f;
            poseStack.mulPose(Axis.XP.rotationDegrees(rock + state.currentBodyPitch));

            // Baby animals - very subtle extra movement
            if (isBaby) {
                float babyEnergy = Mth.sin(state.localTime * 0.05f) * 0.3f;
                poseStack.mulPose(Axis.ZP.rotationDegrees(babyEnergy));
            }
        }
    }

    public static void finishAnimations(PoseStack poseStack) {
        poseStack.popPose();
    }

    /**
     * Returns VERY smooth head yaw
     */
    public static float getHeadYaw(AnimalAnimationState state, boolean isResting) {
        if (isResting) {
            // When sleeping, head is mostly still with very subtle movement
            float drift = Mth.sin(state.localTime * 0.004f) * 1f;
            return drift;
        }
        // Very slow, subtle drift when awake
        float drift = Mth.sin(state.localTime * 0.008f) * 2f;
        return state.currentHeadYaw * 12f + drift;
    }

    /**
     * Returns VERY smooth head pitch
     */
    public static float getHeadPitch(AnimalAnimationState state, boolean isResting) {
        if (isResting) {
            // When sleeping, head is resting down with subtle breathing movement
            float drift = Mth.sin(state.localTime * 0.003f) * 0.8f;
            return 15f + drift; // Head down, resting
        }
        float drift = Mth.sin(state.localTime * 0.006f) * 1.5f;
        return state.currentHeadPitch * 10f + drift;
    }

    private static void updateIdleAction(AnimalAnimationState state, boolean isResting) {
        // Progress current action SLOWLY
        if (state.currentAction != IdleAction.NONE) {
            state.actionProgress += 0.015f * state.energyLevel;

            if (state.actionProgress >= state.actionDuration) {
                state.currentAction = IdleAction.NONE;
                state.actionProgress = 0;
                // Don't reset targets immediately - let them drift back slowly
            }
        }

        // Maybe start a new action (very rarely)
        if (state.currentAction == IdleAction.NONE) {
            float chance = isResting ? IDLE_ACTION_CHANCE * 0.1f : IDLE_ACTION_CHANCE;
            if (state.random.nextFloat() < chance) {
                startRandomIdleAction(state, isResting);
            }
        }
    }

    private static void startRandomIdleAction(AnimalAnimationState state, boolean isResting) {
        float roll = state.random.nextFloat();

        if (isResting) {
            state.currentAction = IdleAction.SETTLE;
            state.targetBodyRoll = (state.random.nextFloat() - 0.5f) * 1f;
        } else {
            if (roll < 0.2f) {
                state.currentAction = IdleAction.LOOK_LEFT;
                state.targetHeadYaw = -1f - state.random.nextFloat() * 0.5f;
            } else if (roll < 0.4f) {
                state.currentAction = IdleAction.LOOK_RIGHT;
                state.targetHeadYaw = 1f + state.random.nextFloat() * 0.5f;
            } else if (roll < 0.5f) {
                state.currentAction = IdleAction.LOOK_UP;
                state.targetHeadPitch = -1f;
            } else if (roll < 0.7f) {
                state.currentAction = IdleAction.LOOK_DOWN;
                state.targetHeadPitch = 1.5f;
                state.targetBodyPitch = 0.8f;
            } else if (roll < 0.85f) {
                state.currentAction = IdleAction.WEIGHT_SHIFT;
                state.targetBodyRoll = (state.random.nextFloat() - 0.5f) * 2f;
            } else {
                state.currentAction = IdleAction.SETTLE;
                state.targetBodyRoll = (state.random.nextFloat() - 0.5f) * 1.5f;
                state.targetBodyPitch = (state.random.nextFloat() - 0.5f) * 1f;
            }
        }

        // Randomize duration
        state.actionDuration = state.currentAction.baseDuration * (0.9f + state.random.nextFloat() * 0.2f);
        state.actionProgress = 0;
    }

    private static void updateInterpolation(AnimalAnimationState state, boolean isResting) {
        // VERY slow interpolation for smooth movement
        // Head moves slower than body for more natural look
        state.currentHeadYaw = Mth.lerp(LERP_SPEED_SLOW, state.currentHeadYaw, state.targetHeadYaw);
        state.currentHeadPitch = Mth.lerp(LERP_SPEED_SLOW, state.currentHeadPitch, state.targetHeadPitch);
        state.currentBodyRoll = Mth.lerp(LERP_SPEED_FAST, state.currentBodyRoll, state.targetBodyRoll);
        state.currentBodyPitch = Mth.lerp(LERP_SPEED_FAST, state.currentBodyPitch, state.targetBodyPitch);

        // When no action, VERY slowly drift back to neutral
        if (state.currentAction == IdleAction.NONE) {
            state.targetHeadYaw = Mth.lerp(LERP_SPEED_RETURN, state.targetHeadYaw, 0);
            state.targetHeadPitch = Mth.lerp(LERP_SPEED_RETURN, state.targetHeadPitch, 0);
            state.targetBodyRoll = Mth.lerp(LERP_SPEED_RETURN, state.targetBodyRoll, 0);
            state.targetBodyPitch = Mth.lerp(LERP_SPEED_RETURN, state.targetBodyPitch, 0);
        }
    }

    public static void cleanupOldStates(long currentTime) {
        animationStates.entrySet().removeIf(entry ->
            currentTime - entry.getValue().lastUpdateTime > 12000);
    }

    public static float getTailWagRotation(AnimalAnimationState state) {
        return Mth.sin(state.localTime * 0.08f) * state.energyLevel * 10f;
    }
}
