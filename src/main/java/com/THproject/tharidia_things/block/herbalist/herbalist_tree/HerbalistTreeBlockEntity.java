package com.THproject.tharidia_things.block.herbalist.herbalist_tree;

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
    private SoundEvent symphonyInstrument = null;
    private int currentNoteIndex = 0;
    private int tickCounter = 0;

    public HerbalistTreeBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.HERBALIST_TREE_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isCrafting() {
        return isCrafting;
    }

    public boolean hasAllPots() {
        for (int i = 1; i <= 8; i++) {
            if (!hasPotAtRoot(i)) return false;
        }
        return true;
    }

    public void startCrafting() {
        if (level == null || !hasAllPots()) return;

        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < SYMPHONY_LENGTH; i++) {
            symphonyNotes[i] = rand.nextInt(25); // 0-24
        }
        symphonyInstrument = NOTEBLOCK_INSTRUMENTS[rand.nextInt(NOTEBLOCK_INSTRUMENTS.length)];

        isCrafting = true;
        currentNoteIndex = 0;
        tickCounter = 0;
        setChanged();
    }

    public void serverTick() {
        if (!isCrafting || level == null) return;

        tickCounter++;
        if (tickCounter % NOTE_INTERVAL != 0) return;

        if (currentNoteIndex < SYMPHONY_LENGTH) {
            int note = symphonyNotes[currentNoteIndex];
            float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
            BlockPos pos = getBlockPos();

            level.playSound(null, pos, symphonyInstrument, SoundSource.RECORDS, 3.0F, pitch);

            if (level instanceof ServerLevel serverLevel) {
                double noteColor = note / 24.0;
                serverLevel.sendParticles(ParticleTypes.NOTE,
                        pos.getX() + 0.5, pos.getY() + 3.2, pos.getZ() + 0.5,
                        0, noteColor, 0, 0, 1);
            }

            currentNoteIndex++;
        }
        // Symphony completed: isCrafting stays true for future minigame
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isCrafting", isCrafting);
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
        isCrafting = tag.getBoolean("isCrafting");
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
    }

    public boolean hasPotAtRoot(int rootIndex) {
        Level level = this.getLevel();
        if (level == null) return false;
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
        addRootController(controllers, 1, ROOT1_ANIM);  // Radice1 -> "root"
        addRootController(controllers, 3, ROOT3_ANIM);  // Radice3 -> "root3"
        addRootController(controllers, 4, ROOT4_ANIM);  // Radice4 -> "root4"
        addRootController(controllers, 8, ROOT2_ANIM);  // Radice8 -> "root2"
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
