package com.THproject.tharidia_things.block.herbalist.herbalist_tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.pot.PotBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
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

    private static final int PETAL_COUNT = 22;
    private static final float PETAL_SCALE_MIN = 1.0f;
    private static final float PETAL_SCALE_MAX = 3.0f;

    private float petalScale = PETAL_SCALE_MIN;
    private int petalColor = 0xFFFFFFFF; // ARGB packed, default white (no tint)

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

    private boolean isCrafting = false;
    private int[] symphonyNotes = new int[SYMPHONY_LENGTH];
    private float[] symphonyPitches = new float[SYMPHONY_LENGTH];
    private SoundEvent symphonyInstrument = null;
    private int currentNoteIndex = 0;
    private int tickCounter = 0;

    private int step = 0;

    private Map<Integer, Boolean> pots = new HashMap<>() {
        {
            for (int i = 0; i < SYMPHONY_LENGTH; i++)
                put(i, false);
        }
    };

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

    public void setPetalColor(float r, float g, float b) {
        int ri = Math.round(Math.clamp(r, 0f, 1f) * 255);
        int gi = Math.round(Math.clamp(g, 0f, 1f) * 255);
        int bi = Math.round(Math.clamp(b, 0f, 1f) * 255);
        setPetalColor(0xFF000000 | (ri << 16) | (gi << 8) | bi);
    }

    public static int getPetalCount() {
        return PETAL_COUNT;
    }

    public boolean isCrafting() {
        return isCrafting;
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

        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < SYMPHONY_LENGTH; i++) {
            symphonyNotes[i] = rand.nextInt(25); // 0-24
            symphonyPitches[i] = (float) Math.pow(2.0, (symphonyNotes[i] - 12) / 12.0);
        }
        symphonyInstrument = NOTEBLOCK_INSTRUMENTS[rand.nextInt(NOTEBLOCK_INSTRUMENTS.length)];

        // Ensure all pots for the symphony are initialized to false
        pots.clear();
        for (int i = 0; i < SYMPHONY_LENGTH; i++) {
            pots.put(i, false);
        }

        isCrafting = true;
        currentNoteIndex = 0;
        tickCounter = 0;
        setChanged();
    }

    public void serverTick() {
        if (!isCrafting || level == null)
            return;

        tickCounter++;
        if (tickCounter % NOTE_INTERVAL != 0)
            return;

        currentNoteIndex++;
        if (currentNoteIndex == SYMPHONY_LENGTH + 1) {
            currentNoteIndex = 0;
            step++;
        }

        // Step 0: tree note
        if (step == 0 && currentNoteIndex < SYMPHONY_LENGTH) {
            int note = symphonyNotes[currentNoteIndex];
            float pitch = symphonyPitches[currentNoteIndex];
            System.out.println(pitch);
            BlockPos pos = getBlockPos();

            level.playSound(null, pos, symphonyInstrument, SoundSource.RECORDS, 3.0F, pitch);

            if (level instanceof ServerLevel serverLevel) {
                double noteColor = note / 24.0;
                serverLevel.sendParticles(ParticleTypes.NOTE,
                        pos.getX() + 0.5, pos.getY() + 3.2, pos.getZ() + 0.5,
                        0, noteColor, 0, 0, 1);
            }

        }

        // Step 1: pot note
        if (step == 1 && currentNoteIndex < SYMPHONY_LENGTH) {
            // Randomly select two distinct pots and set them to true
            if (currentNoteIndex == 0) {
                List<Integer> keys = new ArrayList<>(pots.keySet());
                Random rand = new Random();
                int firstIdx = rand.nextInt(keys.size());
                int secondIdx;
                do {
                    secondIdx = rand.nextInt(keys.size());
                } while (secondIdx == firstIdx);
                pots.put(keys.get(firstIdx), true);
                pots.put(keys.get(secondIdx), true);
            }

            int note = symphonyNotes[currentNoteIndex];
            float pitch = symphonyPitches[currentNoteIndex];
            BlockPos potPos = getPotPositionForRoot(currentNoteIndex + 1);
            boolean value = pots.get(currentNoteIndex);

            double noteColor = note / 24.0;
            if (value) {
                level.playSound(null, potPos, symphonyInstrument, SoundSource.RECORDS, 3.0F, pitch);
            } else {
                Random rand = new Random();

                int newNote;
                while (true) {
                    newNote = symphonyNotes[rand.nextInt(symphonyNotes.length)];
                    boolean isNoteRight = false;
                    for (float n : symphonyNotes) {
                        if (n != newNote)
                            isNoteRight = true;
                    }
                    if (isNoteRight)
                        break;
                }

                float newPitch = (float) Math.pow(2.0, (newNote - 12) / 12.0);

                level.playSound(null, potPos, symphonyInstrument, SoundSource.RECORDS, 3.0F, newPitch);
            }

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.NOTE,
                        potPos.getX() + 0.5, potPos.getY() + 1, potPos.getZ() + 0.5,
                        0, noteColor, 0, 0, 1);

                if (value)
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            potPos.getX() + 0.5, potPos.getY() + 1, potPos.getZ() + 0.5,
                            0, 0, 0, 0, 1);
                else {
                    serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                            potPos.getX() + 0.5, potPos.getY() + 1, potPos.getZ() + 0.5,
                            0, 0, 0, 0, 1);
                }
            }
        }

        if (step == 2) {
            isCrafting = false;
            step = 0;
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
        // Tree growth animation - plays once on placement and holds at final frame
        controllers.add(new AnimationController<>(this, "grow", 0, state -> {
            if (state.getController().getAnimationState() == AnimationController.State.STOPPED) {
                state.getController().setAnimation(GROW_ANIM);
            }
            return PlayState.CONTINUE;
        }));

        // Branch animation controllers for roots that have animations
        addRootController(controllers, 1, ROOT1_ANIM); // Radice1 -> "root"
        addRootController(controllers, 3, ROOT3_ANIM); // Radice3 -> "root3"
        addRootController(controllers, 4, ROOT4_ANIM); // Radice4 -> "root4"
        addRootController(controllers, 8, ROOT2_ANIM); // Radice8 -> "root2"
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
