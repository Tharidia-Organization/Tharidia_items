package com.THproject.tharidia_things.block.herbalist.herbalist_tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.Plants;
import com.THproject.tharidia_things.block.herbalist.pot.PotBlock;
import com.THproject.tharidia_things.block.herbalist.pot.PotBlockEntity;
import com.THproject.tharidia_things.client.gui.medieval.MedievalGuiRenderer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HerbalistTreeBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation GROW_ANIM = RawAnimation.begin().thenPlayAndHold("grow");
    private static final RawAnimation ROOT1_ANIM = RawAnimation.begin().thenPlayAndHold("root");
    private static final RawAnimation ROOT2_ANIM = RawAnimation.begin().thenPlayAndHold("root2");
    private static final RawAnimation ROOT3_ANIM = RawAnimation.begin().thenPlayAndHold("root3");
    private static final RawAnimation ROOT4_ANIM = RawAnimation.begin().thenPlayAndHold("root4");

    private static final float PETAL_SCALE_MIN = 1.0f;
    private static final float PETAL_SCALE_MAX = 4.0f;

    private float petalScale = PETAL_SCALE_MIN;
    private int petalColor = 0xFFFFFFFF;

    // HP / Fame / Sete system
    private static final int MAX_HP = 100;
    private static final int MAX_FAME = 6;
    private static final int MAX_SETE = 4;
    private static final int FAME_DECAY = 4;
    private static final int SETE_DECAY = 3;
    private static final long DAY_EVALUATION_TICK = 18000; // Tramonto

    private int treeHp = MAX_HP;
    private int fame = 0; // hunger counter 0-6
    private int sete = 0; // thirst counter 0-4
    private long lastDayChecked = -1;
    private boolean treeDead = false;

    private static final SoundEvent[] NOTEBLOCK_INSTRUMENTS = {
            SoundEvents.NOTE_BLOCK_HARP.value(),
            SoundEvents.NOTE_BLOCK_BASS.value(),
            SoundEvents.NOTE_BLOCK_BELL.value(),
            SoundEvents.NOTE_BLOCK_CHIME.value(),
            SoundEvents.NOTE_BLOCK_FLUTE.value(),
            SoundEvents.NOTE_BLOCK_GUITAR.value(),
            SoundEvents.NOTE_BLOCK_XYLOPHONE.value(),
            SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(),
            SoundEvents.NOTE_BLOCK_COW_BELL.value(),
            SoundEvents.NOTE_BLOCK_BIT.value(),
            SoundEvents.NOTE_BLOCK_BANJO.value(),
            SoundEvents.NOTE_BLOCK_PLING.value()
    };
    private static final int NOTE_INTERVAL = 10;
    private static final int SYMPHONY_LENGTH = 8;
    private static final int WRONG_FLOWER_TIMEOUT = 40;
    private static final int STEP3_TIME_LIMIT = 200; // 10 seconds (200 ticks)
    private static final int MAX_ERRORS = 6;
    private static final int PRESTART_DELAY = 40; // 2 seconds before music starts
    private static final int SYNC_INTERVAL = 20; // sync to client every second

    private boolean isCrafting = false;
    private int[] symphonyNotes = new int[SYMPHONY_LENGTH];
    private float[] symphonyPitches = new float[SYMPHONY_LENGTH];
    private SoundEvent symphonyInstrument = null;
    private int currentNoteIndex = 0;
    private int tickCounter = 0;
    private int step = 0;

    // Round-based minigame state
    private boolean firstRound = true;
    private Set<Integer> filledPots = new HashSet<>();
    private Set<Integer> roundCorrectPots = new HashSet<>();
    private int expectedPlacements = 2;
    private int placementsReceived = 0;
    private int correctPlacements = 0;
    private Map<Integer, Integer> wrongFlowerTimers = new HashMap<>();
    private Set<Integer> acknowledgedPots = new HashSet<>();
    private int[] wrongNotes = new int[SYMPHONY_LENGTH];
    private List<Integer> currentPairPots = new ArrayList<>();
    private int completedPairs = 0;
    private boolean minigameComplete = false;
    private int craftedColor = 0xFFFFFF;
    private int errorCount = 0;
    private int stepTimer = 0; // countdown timer for step 3

    public HerbalistTreeBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.HERBALIST_TREE_BLOCK_ENTITY.get(), pos, state);
    }

    public float getPetalScale() {
        return petalScale;
    }

    public void setPetalScale(float scale) {
        this.petalScale = Math.max(PETAL_SCALE_MIN, Math.min(PETAL_SCALE_MAX, scale));
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public int getPetalColor() {
        return petalColor;
    }

    public void setPetalColor(int argb) {
        this.petalColor = argb;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isCrafting() {
        return isCrafting;
    }

    public boolean isMinigameComplete() {
        return minigameComplete;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getStepTimer() {
        return stepTimer;
    }

    public int getStep() {
        return step;
    }

    public int getFilledPotsCount() {
        return filledPots.size();
    }

    // ==================== HP / FAME / SETE ====================

    public int healTree(int amount) {
        treeHp = Math.min(treeHp + amount, MAX_HP);
        syncAndSave();
        return treeHp;
    }

    public int damageTree(int amount) {
        treeHp = Math.max(treeHp - amount, 0);
        syncAndSave();
        return treeHp;
    }

    public int getTreeHp() {
        return treeHp;
    }

    public boolean isTreeDead() {
        return treeDead;
    }

    /** Returns HP status: 0=healthy, 1=weak, 2=critical, 3=dead */
    public int getHpStatus() {
        if (treeDead || treeHp <= 0)
            return 3;
        if (treeHp <= 10)
            return 2;
        if (treeHp <= 50)
            return 1;
        return 0;
    }

    /** Returns fame status: 0=sated, 1=hungry, 2=very hungry, 3=starving */
    public int getFameStatus() {
        if (fame >= 4)
            return 0;
        if (fame >= 2)
            return 1;
        if (fame >= 1)
            return 2;
        return 3;
    }

    /** Returns sete status: 0=hydrated, 1=thirsty, 2=dehydrated */
    public int getSeteStatus() {
        if (sete >= 3)
            return 0;
        if (sete >= 1)
            return 1;
        return 2;
    }

    /** Feed the tree with flowers only: +5 HP, +2 fame */
    public void feedFlowersOnly() {
        if (treeDead)
            return;
        treeHp = Math.min(MAX_HP, treeHp + 5);
        fame = Math.min(MAX_FAME, fame + 2);
        syncAndSave();
    }

    /** Feed the tree with manure: +3 HP, +2 fame */
    public void fertilizeTree() {
        if (treeDead) return;
        treeHp = Math.min(MAX_HP, treeHp + 3);
        fame = Math.min(MAX_FAME, fame + 2);
        syncAndSave();
    }

    /** Water the tree with water bucket: +2 sete */
    public void waterTree() {
        if (treeDead)
            return;
        sete = Math.min(MAX_SETE, sete + 2);
        syncAndSave();
    }

    /** Daily evaluation at tick 18000. Called from serverTick. */
    private void checkDailyEvaluation() {
        if (level == null || treeDead)
            return;
        long dayTime = level.getDayTime() % 24000;
        long currentDay = level.getDayTime() / 24000;

        if (dayTime >= DAY_EVALUATION_TICK && currentDay != lastDayChecked) {
            lastDayChecked = currentDay;

            // Decay fame and sete
            fame = Math.max(0, fame - FAME_DECAY);
            sete = Math.max(0, sete - SETE_DECAY);

            // Fame penalties
            int famePenalty = 0;
            if (fame < 4) {
                if (fame >= 2)
                    famePenalty = 3;
                else if (fame >= 1)
                    famePenalty = 6;
                else
                    famePenalty = 10;
            }

            // Sete penalties
            int setePenalty = 0;
            if (sete < 3) {
                if (sete >= 1)
                    setePenalty = 3;
                else
                    setePenalty = 8;
            }

            treeHp = Math.max(0, treeHp - famePenalty - setePenalty);
            if (treeHp <= 0) {
                treeDead = true;
            }

            syncAndSave();
        }
    }

    private void syncAndSave() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Called when a player right-clicks with empty hand after minigame completion.
     * Removes all flowers, resets petals, and gives 4 colored petal items.
     */
    public void collectPetals(Player player) {
        if (!minigameComplete || level == null)
            return;

        // Create 4 petal items with the crafted color
        ItemStack petalStack = new ItemStack(TharidiaThings.PETAL.get(), 4);
        petalStack.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                new net.minecraft.world.item.component.DyedItemColor(craftedColor, true));

        boolean hasFlower = false;
        boolean hasMushroom = false;

        // Remove all flowers from pots
        for (int i = 0; i < SYMPHONY_LENGTH; i++) {
            BlockPos potPos = getPotPositionForRoot(i + 1);
            BlockEntity be = level.getBlockEntity(potPos);
            if (be instanceof PotBlockEntity potBE && potBE.hasPlant()) {
                if (Plants.getPlantTypes(potBE.getPlant()).equals(Plants.PlantTypes.FLOWER))
                    hasFlower = true;
                else if (Plants.getPlantTypes(potBE.getPlant()).equals(Plants.PlantTypes.MUSHROOM))
                    hasMushroom = true;
                potBE.removePlant();
            }
        }

        if (hasFlower && hasMushroom) {
            // Mystical petal (flower + mushroom)
            petalStack.set(DataComponents.RARITY, Rarity.EPIC);
            petalStack.set(DataComponents.ITEM_NAME, Component.translatable("item.tharidiathings.petal_2")
                    .withStyle(style -> style
                            .withFont(MedievalGuiRenderer.MEDIEVAL_FONT)
                            .withColor(craftedColor)
                            .withBold(true)));

            damageTree(15);
        } else {
            // Petal (flower or mushroom)
            petalStack.set(DataComponents.ITEM_NAME, Component.translatable("item.tharidiathings.petal")
                    .withStyle(style -> style
                            .withFont(MedievalGuiRenderer.MEDIEVAL_FONT)));

            healTree(5);
        }

        if (!player.getInventory().add(petalStack)) {
            player.drop(petalStack, false);
        }

        // Reset petals to default
        petalScale = PETAL_SCALE_MIN;
        petalColor = 0xFFFFFFFF;
        minigameComplete = false;
        craftedColor = 0xFFFFFF;

        level.playSound(null, getBlockPos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
        setChanged();
        if (level instanceof ServerLevel) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public boolean hasAllPots() {
        for (int i = 1; i <= 8; i++) {
            if (!hasPotAtRoot(i))
                return false;
        }
        return true;
    }

    public void startCrafting() {
        if (level == null || !hasAllPots())
            return;

        Random rand = new Random();
        for (int i = 0; i < SYMPHONY_LENGTH; i++) {
            symphonyNotes[i] = rand.nextInt(25);
            symphonyPitches[i] = (float) Math.pow(2.0, (symphonyNotes[i] - 12) / 12.0);
        }
        symphonyInstrument = NOTEBLOCK_INSTRUMENTS[rand.nextInt(NOTEBLOCK_INSTRUMENTS.length)];

        for (int i = 0; i < 8; i++) {
            BlockPos potPos = getPotPositionForRoot(i + 1);
            if (level.getBlockEntity(potPos) instanceof PotBlockEntity potBe) {
                potBe.setTreePos(getBlockPos());
            }
        }

        filledPots.clear();
        roundCorrectPots.clear();
        acknowledgedPots.clear();
        wrongFlowerTimers.clear();
        currentPairPots.clear();
        completedPairs = 0;
        firstRound = true;
        expectedPlacements = 2;
        placementsReceived = 0;
        correctPlacements = 0;

        // Reset petals, errors, and minigame state
        petalScale = PETAL_SCALE_MIN;
        petalColor = 0xFFFFFFFF;
        minigameComplete = false;
        craftedColor = 0xFFFFFF;
        errorCount = 0;
        stepTimer = 0;

        // Remove any existing flowers from all pots before starting
        for (int i = 0; i < SYMPHONY_LENGTH; i++) {
            BlockPos potPos = getPotPositionForRoot(i + 1);
            BlockEntity be = level.getBlockEntity(potPos);
            if (be instanceof PotBlockEntity potBE && potBE.hasPlant()) {
                potBE.removePlant();
            }
        }

        isCrafting = true;
        currentNoteIndex = 0;
        tickCounter = 0;
        step = -1; // Pre-start delay with heartbeat signal

        setupRound();
        syncAndSave();
    }

    private void setupRound() {
        Random rand = new Random();
        roundCorrectPots.clear();
        acknowledgedPots.clear();
        placementsReceived = 0;
        correctPlacements = 0;
        wrongFlowerTimers.clear();

        // Collect empty pot indices
        List<Integer> emptyPots = new ArrayList<>();
        for (int i = 0; i < SYMPHONY_LENGTH; i++) {
            if (!filledPots.contains(i)) {
                emptyPots.add(i);
            }
        }

        // Pick expectedPlacements random empty pots as correct
        int toPickCount = Math.min(expectedPlacements, emptyPots.size());
        for (int i = 0; i < toPickCount; i++) {
            int idx = rand.nextInt(emptyPots.size());
            roundCorrectPots.add(emptyPots.remove(idx));
        }

        // Generate wrong notes for non-correct, non-filled pots
        for (int i = 0; i < SYMPHONY_LENGTH; i++) {
            if (!filledPots.contains(i) && !roundCorrectPots.contains(i)) {
                int wrongNote;
                do {
                    wrongNote = rand.nextInt(25);
                } while (wrongNote == symphonyNotes[i]);
                wrongNotes[i] = wrongNote;
            }
        }
    }

    /**
     * During music steps (0, 1, 2), remove any flower a player tries to place
     * in a non-filled pot and drop it back as an item.
     */
    private void enforceNoFlowers() {
        for (int i = 0; i < SYMPHONY_LENGTH; i++) {
            if (filledPots.contains(i))
                continue;
            BlockPos potPos = getPotPositionForRoot(i + 1);
            BlockEntity be = level.getBlockEntity(potPos);
            if (be instanceof PotBlockEntity potBE && potBE.hasPlant()) {
                ItemStack removed = potBE.removePlant();
                Containers.dropItemStack(level,
                        potPos.getX() + 0.5, potPos.getY() + 1.0, potPos.getZ() + 0.5,
                        removed);
            }
        }
    }

    public void serverTick() {
        if (level == null)
            return;

        // Daily evaluation runs always
        checkDailyEvaluation();

        if (!isCrafting)
            return;

        // Periodic sync to client for overlay updates
        if (tickCounter % SYNC_INTERVAL == 0) {
            syncAndSave();
        }

        // Step -1: pre-start delay with heartbeat
        if (step == -1) {
            enforceNoFlowers();
            tickCounter++;

            // Heartbeat crescendo: 3 beats at tick 10, 22, 34
            BlockPos treePos = getBlockPos();
            if (level instanceof ServerLevel serverLevel) {
                if (tickCounter == 10 || tickCounter == 22 || tickCounter == 34) {
                    float volume = 0.3f + (tickCounter / 34.0f) * 0.7f;
                    float pitch = 0.5f + (tickCounter / 34.0f) * 0.3f;
                    level.playSound(null, treePos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, volume, pitch);
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            treePos.getX() + 0.5, treePos.getY() + 1.5, treePos.getZ() + 0.5,
                            3, 0.3, 0.3, 0.3, 0.01);
                }
            }

            if (tickCounter >= PRESTART_DELAY) {
                // Transition: awakening burst
                level.playSound(null, treePos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 1.2F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            treePos.getX() + 0.5, treePos.getY() + 2.5, treePos.getZ() + 0.5,
                            20, 0.5, 0.5, 0.5, 0.05);
                }
                tickCounter = 0;
                step = firstRound ? 0 : 1;
                syncAndSave();
            }
            return;
        }

        // Step 3: waiting for player - runs every tick
        if (step == 3) {
            tickStep3();
            return;
        }

        // During music steps, block flower placement every tick
        enforceNoFlowers();

        tickCounter++;
        if (tickCounter % NOTE_INTERVAL != 0)
            return;

        // Step 0: tree plays symphony notes (only first round)
        if (step == 0 && currentNoteIndex < SYMPHONY_LENGTH) {
            int note = symphonyNotes[currentNoteIndex];
            float pitch = symphonyPitches[currentNoteIndex];
            BlockPos pos = getBlockPos();

            level.playSound(null, pos, symphonyInstrument, SoundSource.RECORDS, 3.0F, pitch);

            if (level instanceof ServerLevel serverLevel) {
                double noteColor = note / 24.0;
                serverLevel.sendParticles(ParticleTypes.NOTE,
                        pos.getX() + 0.5, pos.getY() + 3.2, pos.getZ() + 0.5,
                        0, noteColor, 0, 0, 1);
            }
        }

        // Step 1: each pot plays its correct note
        if (step == 1 && currentNoteIndex < SYMPHONY_LENGTH) {
            int note = symphonyNotes[currentNoteIndex];
            float pitch = symphonyPitches[currentNoteIndex];
            BlockPos pos = getPotPositionForRoot(currentNoteIndex + 1);

            level.playSound(null, pos, symphonyInstrument, SoundSource.RECORDS, 3.0F, pitch);

            if (level instanceof ServerLevel serverLevel) {
                double noteColor = note / 24.0;
                serverLevel.sendParticles(ParticleTypes.NOTE,
                        pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                        0, noteColor, 0, 0, 1);
            }
        }

        // Step 2: correct pots play right note, others play wrong note
        if (step == 2 && currentNoteIndex < SYMPHONY_LENGTH) {
            BlockPos potPos = getPotPositionForRoot(currentNoteIndex + 1);
            boolean playsCorrect = filledPots.contains(currentNoteIndex)
                    || roundCorrectPots.contains(currentNoteIndex);

            if (playsCorrect) {
                int note = symphonyNotes[currentNoteIndex];
                float pitch = symphonyPitches[currentNoteIndex];
                level.playSound(null, potPos, symphonyInstrument, SoundSource.RECORDS, 3.0F, pitch);

                if (level instanceof ServerLevel serverLevel) {
                    double noteColor = note / 24.0;
                    serverLevel.sendParticles(ParticleTypes.NOTE,
                            potPos.getX() + 0.5, potPos.getY() + 1, potPos.getZ() + 0.5,
                            0, noteColor, 0, 0, 1);
                }
                level.setBlock(potPos.below(), Blocks.GREEN_WOOL.defaultBlockState(), 3);
            } else {
                int wNote = wrongNotes[currentNoteIndex];
                float wPitch = (float) Math.pow(2.0, (wNote - 12) / 12.0);
                level.playSound(null, potPos, symphonyInstrument, SoundSource.RECORDS, 3.0F, wPitch);

                if (level instanceof ServerLevel serverLevel) {
                    double noteColor = wNote / 24.0;
                    serverLevel.sendParticles(ParticleTypes.NOTE,
                            potPos.getX() + 0.5, potPos.getY() + 1, potPos.getZ() + 0.5,
                            0, noteColor, 0, 0, 1);
                }
                level.setBlock(potPos.below(), Blocks.RED_WOOL.defaultBlockState(), 3);
            }
        }

        currentNoteIndex++;
        if (currentNoteIndex == SYMPHONY_LENGTH + 1) {
            currentNoteIndex = 0;
            step++;
            if (step == 3) {
                enterStep3();
            }
            syncAndSave();
        }
    }

    /** Transition into step 3 with a dramatic signal */
    private void enterStep3() {
        stepTimer = STEP3_TIME_LIMIT;
        BlockPos treePos = getBlockPos();

        if (level instanceof ServerLevel serverLevel) {
            // Ascending chime cascade: 3 notes rising in pitch
            level.playSound(null, treePos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.2F, 0.7F);
            level.playSound(null, treePos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.2F, 1.0F);
            level.playSound(null, treePos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.2F, 1.4F);

            // Bloom of particles spiraling outward from the tree canopy
            for (int i = 0; i < 8; i++) {
                BlockPos potPos = getPotPositionForRoot(i + 1);
                if (filledPots.contains(i))
                    continue;
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        potPos.getX() + 0.5, potPos.getY() + 1.2, potPos.getZ() + 0.5,
                        5, 0.2, 0.3, 0.2, 0.02);
            }

            // Central burst
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    treePos.getX() + 0.5, treePos.getY() + 3.0, treePos.getZ() + 0.5,
                    30, 1.5, 0.5, 1.5, 0.5);
        }
    }

    private void tickStep3() {
        // Countdown timer
        stepTimer--;

        // Sync timer to client every second
        if (stepTimer % SYNC_INTERVAL == 0) {
            syncAndSave();
        }

        // Timeout: treat as all-wrong if timer expires before all placements
        if (stepTimer <= 0 && placementsReceived < expectedPlacements) {
            errorCount++;
            BlockPos treePos = getBlockPos();
            level.playSound(null, treePos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);

            if (errorCount >= MAX_ERRORS) {
                handleWipe();
                return;
            }

            // Remove any flowers placed so far (that aren't in filledPots)
            for (int i = 0; i < SYMPHONY_LENGTH; i++) {
                if (!filledPots.contains(i)) {
                    BlockPos potPos = getPotPositionForRoot(i + 1);
                    BlockEntity be = level.getBlockEntity(potPos);
                    if (be instanceof PotBlockEntity potBE && potBE.hasPlant()) {
                        potBE.removePlant();
                    }
                }
            }

            // Retry same round with pre-start signal
            wrongFlowerTimers.clear();
            setupRound();
            currentNoteIndex = 0;
            tickCounter = 0;
            step = -1;
            syncAndSave();
            return;
        }

        // Decrement wrong flower timers and remove expired flowers
        Iterator<Map.Entry<Integer, Integer>> it = wrongFlowerTimers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            entry.setValue(entry.getValue() - 1);
            if (entry.getValue() <= 0) {
                int potIdx = entry.getKey();
                BlockPos potPos = getPotPositionForRoot(potIdx + 1);
                BlockEntity be = level.getBlockEntity(potPos);
                if (be instanceof PotBlockEntity potBE) {
                    potBE.removePlant();
                }
                it.remove();
            }
        }

        // Poll for new flower placements
        if (placementsReceived < expectedPlacements) {
            for (int i = 0; i < SYMPHONY_LENGTH; i++) {
                if (filledPots.contains(i) || acknowledgedPots.contains(i))
                    continue;

                BlockPos potPos = getPotPositionForRoot(i + 1);
                BlockEntity be = level.getBlockEntity(potPos);
                if (be instanceof PotBlockEntity potBE && potBE.hasPlant()) {
                    acknowledgedPots.add(i);
                    placementsReceived++;

                    if (roundCorrectPots.contains(i)) {
                        correctPlacements++;
                        spawnCorrectFeedback(potPos);
                    } else {
                        errorCount++;
                        wrongFlowerTimers.put(i, WRONG_FLOWER_TIMEOUT);
                        spawnWrongFeedback(potPos);

                        if (errorCount >= MAX_ERRORS) {
                            handleWipe();
                            return;
                        }
                    }

                    if (placementsReceived >= expectedPlacements)
                        break;
                }
            }
        }

        // Evaluate round when all placements received and all wrong timers expired
        if (placementsReceived >= expectedPlacements && wrongFlowerTimers.isEmpty()) {
            evaluateRound();
        }
    }

    private void handleWipe() {
        BlockPos treePos = getBlockPos();
        level.playSound(null, treePos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, 0.5F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    treePos.getX() + 0.5, treePos.getY() + 3.0, treePos.getZ() + 0.5,
                    30, 1.0, 1.0, 1.0, 0.1);
        }

        // Remove all flowers
        for (int i = 0; i < SYMPHONY_LENGTH; i++) {
            BlockPos potPos = getPotPositionForRoot(i + 1);
            BlockEntity be = level.getBlockEntity(potPos);
            if (be instanceof PotBlockEntity potBE && potBE.hasPlant()) {
                potBE.removePlant();
            }
        }

        // Full reset, no output
        isCrafting = false;
        minigameComplete = false;
        petalScale = PETAL_SCALE_MIN;
        petalColor = 0xFFFFFFFF;
        filledPots.clear();
        step = 0;
        errorCount = 0;
        syncAndSave();
    }

    private void evaluateRound() {
        BlockPos treePos = getBlockPos();

        if (correctPlacements == expectedPlacements) {
            // All correct
            level.playSound(null, treePos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        treePos.getX() + 0.5, treePos.getY() + 3.0, treePos.getZ() + 0.5,
                        15, 0.5, 0.5, 0.5, 0);
            }
            for (int potIdx : acknowledgedPots) {
                if (roundCorrectPots.contains(potIdx)) {
                    filledPots.add(potIdx);
                    currentPairPots.add(potIdx);
                }
            }
            // Check if a pair is complete (2 flowers accumulated)
            if (currentPairPots.size() >= 2) {
                applyPairPetalChanges();
            }
            expectedPlacements = 2;
        } else if (correctPlacements > 0) {
            // Partial: some correct, some wrong
            level.playSound(null, treePos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
            for (int potIdx : acknowledgedPots) {
                if (roundCorrectPots.contains(potIdx)) {
                    filledPots.add(potIdx);
                    currentPairPots.add(potIdx);
                }
            }
            expectedPlacements = expectedPlacements - correctPlacements;
        } else if (expectedPlacements >= 2) {
            // Both wrong in a full round: reset progress but keep errors
            level.playSound(null, treePos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        treePos.getX() + 0.5, treePos.getY() + 3.0, treePos.getZ() + 0.5,
                        20, 0.5, 0.5, 0.5, 0.05);
            }
            // Remove all flowers from non-filled pots
            for (int i = 0; i < SYMPHONY_LENGTH; i++) {
                if (!filledPots.contains(i)) {
                    BlockPos potPos = getPotPositionForRoot(i + 1);
                    BlockEntity be = level.getBlockEntity(potPos);
                    if (be instanceof PotBlockEntity potBE && potBE.hasPlant()) {
                        potBE.removePlant();
                    }
                }
            }
            // Reset petals, pairs, and restart from pre-start
            petalScale = PETAL_SCALE_MIN;
            petalColor = 0xFFFFFFFF;
            filledPots.clear();
            currentPairPots.clear();
            completedPairs = 0;
            firstRound = true;
            expectedPlacements = 2;
            setupRound();
            currentNoteIndex = 0;
            tickCounter = 0;
            step = -1;
            syncAndSave();
            return;
        } else {
            // Wrong on a single-flower retry: just loop again with same expectedPlacements
            level.playSound(null, treePos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
        }

        // Check if minigame is complete
        if (filledPots.size() >= SYMPHONY_LENGTH) {
            level.playSound(null, treePos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        treePos.getX() + 0.5, treePos.getY() + 3.0, treePos.getZ() + 0.5,
                        50, 1.0, 1.0, 1.0, 0.5);
            }
            isCrafting = false;
            minigameComplete = true;
            craftedColor = petalColor & 0x00FFFFFF;
            step = 0;
            syncAndSave();
            return;
        }

        // Setup next round with pre-start signal
        firstRound = false;
        setupRound();
        currentNoteIndex = 0;
        tickCounter = 0;

        int emptyCount = SYMPHONY_LENGTH - filledPots.size();
        if (emptyCount <= 1) {
            // Only 1 pot left: go straight to waiting with signal
            enterStep3();
            step = 3;
        } else {
            // Go through pre-start delay before next music cycle
            step = -1;
        }
        syncAndSave();
    }

    private void applyPairPetalChanges() {
        // Read flower colors from the two pots in the completed pair
        int color1 = 0xFFFFFF;
        int color2 = 0xFFFFFF;
        if (currentPairPots.size() >= 2) {
            color1 = readFlowerColor(currentPairPots.get(0));
            color2 = readFlowerColor(currentPairPots.get(1));
        }
        currentPairPots.clear();
        completedPairs++;

        // Mix the two flower colors
        int pairMix = mixColors(color1, color2);

        // Blend into petal color: first pair sets the color, subsequent pairs average
        // in
        if (completedPairs == 1) {
            setPetalColor(pairMix);
        } else {
            setPetalColor(mixColors(petalColor & 0x00FFFFFF, pairMix));
        }

        // Grow petals by 1.5x
        setPetalScale(petalScale * 1.5f);
    }

    private int readFlowerColor(int potIndex) {
        BlockPos potPos = getPotPositionForRoot(potIndex + 1);
        BlockEntity be = level.getBlockEntity(potPos);
        if (be instanceof PotBlockEntity potBE) {
            return Plants.getColor(potBE.getPlant());
        }
        return 0xFFFFFF;
    }

    private static int mixColors(int c1, int c2) {
        int r = (((c1 >> 16) & 0xFF) + ((c2 >> 16) & 0xFF)) / 2;
        int g = (((c1 >> 8) & 0xFF) + ((c2 >> 8) & 0xFF)) / 2;
        int b = ((c1 & 0xFF) + (c2 & 0xFF)) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void spawnCorrectFeedback(BlockPos potPos) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    potPos.getX() + 0.5, potPos.getY() + 1.0, potPos.getZ() + 0.5,
                    10, 0.3, 0.3, 0.3, 0);
            level.playSound(null, potPos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, 1.5F);
        }
    }

    private void spawnWrongFeedback(BlockPos potPos) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    potPos.getX() + 0.5, potPos.getY() + 1.0, potPos.getZ() + 0.5,
                    10, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, potPos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("petalScale", petalScale);
        tag.putInt("petalColor", petalColor);
        tag.putIntArray("symphonyNotes", symphonyNotes);
        tag.putInt("currentNoteIndex", currentNoteIndex);
        tag.putInt("tickCounter", tickCounter);
        if (symphonyInstrument != null) {
            tag.putString("symphonyInstrument",
                    BuiltInRegistries.SOUND_EVENT.getKey(symphonyInstrument).toString());
        }
        // HP / Fame / Sete
        tag.putInt("treeHp", treeHp);
        tag.putInt("fame", fame);
        tag.putInt("sete", sete);
        tag.putLong("lastDayChecked", lastDayChecked);
        tag.putBoolean("treeDead", treeDead);
        // Minigame state
        tag.putBoolean("isCrafting", isCrafting);
        tag.putInt("step", step);
        tag.putInt("errorCount", errorCount);
        tag.putInt("stepTimer", stepTimer);
        tag.putInt("filledPotsCount", filledPots.size());
        tag.putBoolean("minigameComplete", minigameComplete);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("petalScale")) {
            petalScale = tag.getFloat("petalScale");
        }
        if (tag.contains("petalColor")) {
            petalColor = tag.getInt("petalColor");
        }
        // HP / Fame / Sete
        if (tag.contains("treeHp"))
            treeHp = tag.getInt("treeHp");
        if (tag.contains("fame"))
            fame = tag.getInt("fame");
        if (tag.contains("sete"))
            sete = tag.getInt("sete");
        if (tag.contains("lastDayChecked"))
            lastDayChecked = tag.getLong("lastDayChecked");
        if (tag.contains("treeDead"))
            treeDead = tag.getBoolean("treeDead");
        // Minigame state (for client sync)
        if (tag.contains("isCrafting"))
            isCrafting = tag.getBoolean("isCrafting");
        if (tag.contains("step"))
            step = tag.getInt("step");
        if (tag.contains("errorCount"))
            errorCount = tag.getInt("errorCount");
        if (tag.contains("stepTimer"))
            stepTimer = tag.getInt("stepTimer");
        if (tag.contains("minigameComplete"))
            minigameComplete = tag.getBoolean("minigameComplete");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,
            HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, lookupProvider);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.loadAdditional(tag, lookupProvider);
        loadAdditional(tag, lookupProvider);
        if (tag.contains("symphonyNotes")) {
            int[] loaded = tag.getIntArray("symphonyNotes");
            if (loaded.length == SYMPHONY_LENGTH) {
                symphonyNotes = loaded;
            }
        }
        currentNoteIndex = tag.getInt("currentNoteIndex");
        tickCounter = tag.getInt("tickCounter");
        if (tag.contains("symphonyInstrument")) {
            ResourceLocation instrumentId = ResourceLocation.tryParse(tag.getString("symphonyInstrument"));
            if (instrumentId != null) {
                symphonyInstrument = BuiltInRegistries.SOUND_EVENT.get(instrumentId);
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean hasPotAtRoot(int rootIndex) {
        Level level = this.getLevel();
        if (level == null)
            return false;
        BlockPos potPos = getPotPositionForRoot(rootIndex);
        return level.getBlockState(potPos).getBlock() instanceof PotBlock;
    }

    public BlockPos getPotPositionForRoot(int rootIndex) {
        BlockPos pos = this.getBlockPos();
        Direction forwardDir = Direction.NORTH;

        for (int i = 1; i <= rootIndex; i++) {
            if (i == rootIndex) {
                BlockPos newPos = pos.relative(forwardDir, 2);
                Direction sideDir = (i % 2 != 0) ? forwardDir.getCounterClockWise() : forwardDir.getClockWise();
                return newPos.relative(sideDir, 1);
            }
            if (i % 2 == 0) {
                forwardDir = forwardDir.getClockWise();
            }
        }
        return pos;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "grow", 0, state -> {
            if (state.getController().getAnimationState() == AnimationController.State.STOPPED) {
                state.getController().setAnimation(GROW_ANIM);
            }
            return PlayState.CONTINUE;
        }));

        addRootController(controllers, 1, ROOT1_ANIM);
        addRootController(controllers, 3, ROOT3_ANIM);
        addRootController(controllers, 4, ROOT4_ANIM);
        addRootController(controllers, 8, ROOT2_ANIM);
    }

    private void addRootController(ControllerRegistrar controllers, int rootIndex, RawAnimation anim) {
        controllers.add(new AnimationController<>(this, "root" + rootIndex + "_ctrl", 0, state -> {
            if (hasPotAtRoot(rootIndex)) {
                state.getController().setAnimation(anim);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
