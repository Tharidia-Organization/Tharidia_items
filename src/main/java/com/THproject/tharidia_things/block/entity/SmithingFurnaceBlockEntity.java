package com.THproject.tharidia_things.block.entity;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.SmithingFurnaceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Block entity for the Smithing Furnace - a GeckoLib animated multiblock.
 * Features:
 * - Permanent "levitate2" animation always active
 * - Coal system with visual feedback (coal_1 to coal_4 bones)
 * - Tier system (0-4) for future upgrades with bone visibility
 */
public class SmithingFurnaceBlockEntity extends BlockEntity implements GeoBlockEntity {

    // GeckoLib animation cache
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Animation that loops permanently
    private static final RawAnimation LEVITATE_ANIM = RawAnimation.begin().thenLoop("levitate2");

    // Tier system (0-4) for future bone visibility changes
    private int tier = 0;

    // Active state - coal is burning (lit with flint and steel)
    private boolean active = false;

    // ==================== Coal System ====================
    // Coal count (0-8), every 2 coal = 1 visible bone
    private int coalCount = 0;
    private static final int MAX_COAL = 8;
    // Coal consumption: 1 coal per minute = 1200 ticks
    private static final int TICKS_PER_COAL = 1200;
    private int coalBurnTicks = 0;

    // Hoover (mantice) animation state - the bellows mechanism
    private boolean hooverActive = false;

    // Cogiuolo animation state
    private boolean cogiuoloActive = false;

    // Door animation state (true = open, false = closed)
    private boolean doorOpen = false;

    // ==================== Installed Components ====================
    // These track which upgrade components have been installed
    private boolean hasBellows = false;   // Bellows top part (stage_1)
    private boolean hasCrucible = false;  // Large crucible (stage_2) - enables cogiuolo animation
    private boolean hasHoover = false;    // Hoover/mantice (stage_3) - enables mantice animation
    private boolean hasChimney = false;   // Chimney (stage_4)
    private boolean hasDoor = false;      // Door (stage_5)

    // Animation for hoover/mantice (the bellows mechanism)
    private static final RawAnimation HOOVER_ANIM = RawAnimation.begin().thenLoop("mantice");

    // Animation for cogiuolo
    private static final RawAnimation COGIUOLO_ANIM = RawAnimation.begin().thenPlay("cogiuolo");

    // Animation for door
    private static final RawAnimation DOOR_OPEN_ANIM = RawAnimation.begin().thenPlayAndHold("door_open");
    private static final RawAnimation DOOR_CLOSE_ANIM = RawAnimation.begin().thenPlayAndHold("door_close");

    // ==================== Ash System ====================
    // Ash accumulates while furnace is active, max 6 levels
    private int ashCount = 0;
    private static final int MAX_ASH = 6;
    private static final int TICKS_PER_ASH = 2400; // 2 minutes = 2400 ticks
    private int ashAccumulationTicks = 0;

    // Overpressure: when ash is full, pressure builds up → smoke then explosion
    private int ashFullTicks = 0;
    private static final int ASH_SMOKE_START_TICKS = 1200;  // 1 min → smoke particles start
    private static final int ASH_EXPLODE_TICKS = 4800;      // 4 min total (1 wait + 3 smoke) → boom
    private boolean ashOverpressure = false; // synced to client for particles

    // Tick counter for hoover smoke puffs (2 seconds = 40 ticks)
    private int hooverParticleTicks = 0;
    private static final int HOOVER_PUFF_INTERVAL = 40; // 2 seconds

    // ==================== Tiny Crucible State ====================
    // Whether the tiny crucible is present in the furnace (removed when picked up with pinza)
    private boolean hasTinyCrucible = true;

    // ==================== Smelting System ====================
    // Raw ore smelting in the tiny crucible
    private String smeltingRawType = "";   // "iron", "gold", "copper" or "" if empty
    private int smeltingTicks = 0;
    private static final int SMELTING_TIME = 600;        // 30 seconds
    private static final int SMELTING_TIME_HOOVER = 400;  // 20 seconds with bellows (unused constant for reference)
    private boolean hasMoltenMetal = false;  // true when smelting complete
    private String moltenMetalType = "";     // "iron", "gold", "copper"

    // ==================== Cast Ingot System ====================
    // Direct pour into the cast mold (no big crucible installed)
    private boolean hasCastMetal = false;
    private String castMetalType = "";
    private int castSolidifyTicks = 0;
    private boolean castSolidified = false;
    private static final int CAST_SOLIDIFY_TIME = 60; // 3 seconds = 60 ticks

    // ==================== Big Crucible System ====================
    // Large crucible pour (when big crucible is installed), max 4 units
    private int bigCrucibleCount = 0;
    private static final int MAX_BIG_CRUCIBLE = 8;
    private String bigCrucibleMetalType = "";

    // ==================== Expiration System ====================
    // Molten metal expires after 1 minute of inactivity, turning into useless gray mass
    private static final int EXPIRE_TIME = 1200; // 1 minute = 1200 ticks
    private static final int BIG_CRUCIBLE_EXPIRE_TIME = 24000; // 20 minutes = 24000 ticks
    // Tiny crucible: timer counts when fire is off and molten metal exists
    private int tinyCrucibleExpireTicks = 0;
    private boolean tinyCrucibleExpired = false;
    // Big crucible: timer counts from when metal is poured
    private int bigCrucibleExpireTicks = 0;
    private boolean bigCrucibleExpired = false;
    // Cast mold: timer counts from when solidification completes
    private int castExpireTicks = 0;
    private boolean castExpired = false;

    // ==================== Ingots on Embers System ====================
    private static final int INGOT_HEAT_TIME = 1200; // 1 minute to fully heat
    private static final int MAX_INGOTS = 4;
    private int ingotCount = 0;
    private String ingotMetalType = "";
    private int ingotHeatTicks = 0;

    public SmithingFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.SMITHING_FURNACE_BLOCK_ENTITY.get(), pos, state);
    }

    // ==================== Coal System ====================

    /**
     * Gets the current coal count (0-8)
     */
    public int getCoalCount() {
        return coalCount;
    }

    /**
     * Checks if furnace has any coal
     */
    public boolean hasCoal() {
        return coalCount > 0;
    }

    /**
     * Checks if furnace can accept more coal
     */
    public boolean canAddCoal() {
        return coalCount < MAX_COAL;
    }

    /**
     * Adds coal to the furnace
     * @param amount amount of coal to add
     * @return actual amount added (may be less if near max)
     */
    public int addCoal(int amount) {
        int spaceLeft = MAX_COAL - coalCount;
        int toAdd = Math.min(amount, spaceLeft);
        if (toAdd > 0) {
            coalCount += toAdd;
            syncToClient();
        }
        return toAdd;
    }

    /**
     * Lights the furnace (called when player uses flint and steel)
     * @return true if successfully lit, false if no coal or already lit
     */
    public boolean lightFurnace() {
        if (active || coalCount <= 0) {
            return false;
        }
        active = true;
        coalBurnTicks = 0;
        syncToClient();
        return true;
    }

    /**
     * Extinguishes the furnace
     */
    public void extinguish() {
        if (active) {
            active = false;
            coalBurnTicks = 0;
            syncToClient();
        }
    }

    // ==================== Ash System ====================

    /**
     * Gets the current ash count (0-6)
     */
    public int getAshCount() {
        return ashCount;
    }

    /**
     * Whether the furnace is in overpressure state (smoke particles on client)
     */
    public boolean isAshOverpressure() {
        return ashOverpressure;
    }

    /**
     * Removes 1 ash level from the furnace. Resets overpressure timer.
     * @return true if ash was removed, false if no ash present
     */
    public boolean removeAsh() {
        if (ashCount <= 0) {
            return false;
        }
        ashCount--;
        // Reset overpressure when ash is removed
        ashFullTicks = 0;
        ashOverpressure = false;
        syncToClient();
        return true;
    }

    // ==================== Hoover System (Mantice Animation) ====================

    /**
     * Gets whether the hoover/mantice is active (animating)
     */
    public boolean isHooverActive() {
        return hooverActive;
    }

    /**
     * Sets the hoover/mantice active state
     */
    public void setHooverActive(boolean active) {
        this.hooverActive = active;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Toggles the hoover/mantice animation
     */
    public void toggleHoover() {
        setHooverActive(!this.hooverActive);
    }

    // ==================== Cogiuolo System ====================

    /**
     * Gets whether the cogiuolo is active (animating)
     */
    public boolean isCogiuoloActive() {
        return cogiuoloActive;
    }

    /**
     * Sets the cogiuolo active state
     */
    public void setCogiuoloActive(boolean active) {
        this.cogiuoloActive = active;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Triggers the cogiuolo animation (plays once).
     * Pours 1 unit from the big crucible into the cast mold.
     * Blocked if big crucible is empty/expired or cast mold is occupied/expired.
     */
    public void toggleCogiuolo() {
        if (!this.hasCrucible || this.bigCrucibleCount <= 0 || this.bigCrucibleExpired) {
            return;
        }
        if (this.hasCastMetal || this.castExpired) {
            return;
        }
        // Pour 1 unit into cast
        this.bigCrucibleCount--;
        String metalType = this.bigCrucibleMetalType;
        if (this.bigCrucibleCount <= 0) {
            this.bigCrucibleMetalType = "";
            this.bigCrucibleExpireTicks = 0;
        }
        pourIntoCast(metalType);
        triggerAnim("cogiuolo", "pour");
    }

    // ==================== Door System ====================

    /**
     * Gets whether the door is open
     */
    public boolean isDoorOpen() {
        return doorOpen;
    }

    /**
     * Sets the door open state
     */
    public void setDoorOpen(boolean open) {
        this.doorOpen = open;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Toggles the door animation (open/close)
     */
    public void toggleDoor() {
        if (hasDoor) setDoorOpen(!this.doorOpen);
    }

    // ==================== Component Installation ====================

    public boolean hasBellows() { return hasBellows; }
    public boolean hasCrucible() { return hasCrucible; }
    public boolean hasHoover() { return hasHoover; }
    public boolean hasChimney() { return hasChimney; }
    public boolean hasDoor() { return hasDoor; }

    /**
     * Installs a component on the furnace
     * @return true if installation was successful, false if already installed
     */
    public boolean installBellows() {
        if (hasBellows) return false;
        hasBellows = true;
        syncToClient();
        return true;
    }

    public boolean installCrucible() {
        if (hasCrucible) return false;
        hasCrucible = true;
        syncToClient();
        return true;
    }

    public boolean installHoover() {
        if (hasHoover) return false;
        hasHoover = true;
        syncToClient();
        return true;
    }

    public boolean installChimney() {
        if (hasChimney) return false;
        hasChimney = true;
        syncToClient();
        return true;
    }

    public boolean installDoor() {
        if (hasDoor) return false;
        hasDoor = true;
        syncToClient();
        return true;
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ==================== Tiny Crucible System ====================

    /**
     * Whether the tiny crucible is present in the furnace.
     */
    public boolean hasTinyCrucible() {
        return hasTinyCrucible;
    }

    /**
     * Removes the tiny crucible from the furnace (player picks it up with pinza).
     * Also removes the molten metal from the crucible.
     * @return the type of molten metal that was in the crucible, or "" if none
     */
    public String removeTinyCrucible() {
        hasTinyCrucible = false;
        String metal = removeMoltenMetal();
        tinyCrucibleExpireTicks = 0;
        tinyCrucibleExpired = false;
        syncToClient();
        return metal;
    }

    /**
     * Returns the tiny crucible to the furnace (player places it back with empty pinza_crucible).
     */
    public boolean returnTinyCrucible() {
        if (ingotCount > 0) return false;
        hasTinyCrucible = true;
        syncToClient();
        return true;
    }

    /**
     * Returns the tiny crucible to the furnace with molten metal inside.
     * @param metalType the type of molten metal
     * @param expired whether the metal is already expired
     * @return true if returned successfully, false if ingots block it
     */
    public boolean returnTinyCrucibleWithMetal(String metalType, boolean expired) {
        if (ingotCount > 0) return false;
        hasTinyCrucible = true;
        hasMoltenMetal = true;
        moltenMetalType = metalType;
        tinyCrucibleExpired = expired;
        tinyCrucibleExpireTicks = 0;
        syncToClient();
        return true;
    }

    // ==================== Ingots on Embers System ====================

    public int getIngotCount() {
        return ingotCount;
    }

    public String getIngotMetalType() {
        return ingotMetalType;
    }

    public float getIngotHeatProgress() {
        return ingotHeatTicks / (float) INGOT_HEAT_TIME;
    }

    public boolean areIngotsFullyHeated() {
        return ingotCount > 0 && ingotHeatTicks >= INGOT_HEAT_TIME;
    }

    /**
     * Places an ingot on the embers. Requires no tiny crucible, same metal type, and space.
     * @return true if placed successfully
     */
    public boolean placeIngot(String metalType) {
        if (hasTinyCrucible || ingotCount >= MAX_INGOTS) {
            return false;
        }
        if (ingotCount > 0 && !ingotMetalType.equals(metalType)) {
            return false;
        }
        ingotCount++;
        ingotMetalType = metalType;
        syncToClient();
        return true;
    }

    /**
     * Removes one fully heated ingot from the embers.
     * @return the metal type of the removed ingot, or "" if none
     */
    public String removeOneHotIngot() {
        if (ingotCount <= 0 || ingotHeatTicks < INGOT_HEAT_TIME) {
            return "";
        }
        String type = ingotMetalType;
        ingotCount--;
        if (ingotCount <= 0) {
            ingotMetalType = "";
            ingotHeatTicks = 0;
        }
        syncToClient();
        return type;
    }

    // ==================== Smelting System ====================

    /**
     * Inserts raw ore into the crucible for smelting.
     * Requires the tiny crucible to be present.
     * @param type "iron", "gold", or "copper"
     * @return true if insertion was successful
     */
    public boolean insertRawOre(String type) {
        if (!hasTinyCrucible || !smeltingRawType.isEmpty() || hasMoltenMetal) {
            return false;
        }
        smeltingRawType = type;
        smeltingTicks = 0;
        syncToClient();
        return true;
    }

    public String getSmeltingRawType() {
        return smeltingRawType;
    }

    public boolean isSmeltingInProgress() {
        return !smeltingRawType.isEmpty() && !hasMoltenMetal;
    }

    public boolean hasMoltenMetal() {
        return hasMoltenMetal;
    }

    public String getMoltenMetalType() {
        return moltenMetalType;
    }

    /**
     * Removes molten metal from the crucible (picked up by pinza_crucible).
     * @return the type of metal removed, or "" if none
     */
    public String removeMoltenMetal() {
        if (!hasMoltenMetal) {
            return "";
        }
        String type = moltenMetalType;
        hasMoltenMetal = false;
        moltenMetalType = "";
        syncToClient();
        return type;
    }

    // ==================== Cast Ingot System ====================

    public boolean hasCastMetal() {
        return hasCastMetal;
    }

    public String getCastMetalType() {
        return castMetalType;
    }

    public boolean isCastSolidified() {
        return castSolidified;
    }

    public int getCastSolidifyTicks() {
        return castSolidifyTicks;
    }

    public float getCastSolidifyProgress() {
        return Math.min(1.0f, (float) castSolidifyTicks / CAST_SOLIDIFY_TIME);
    }

    /**
     * Pours molten metal into the cast ingot mold.
     * @param metalType the type of metal to pour
     * @return true if successful, false if cast is already full
     */
    public boolean pourIntoCast(String metalType) {
        if (hasCastMetal || castExpired) {
            return false;
        }
        hasCastMetal = true;
        castMetalType = metalType;
        castSolidifyTicks = 0;
        castSolidified = false;
        syncToClient();
        return true;
    }

    /**
     * Removes the cast metal (future: when player picks up the cooled ingot).
     * @return the type of metal removed, or "" if none
     */
    public String removeCastMetal() {
        if (!hasCastMetal) {
            return "";
        }
        String type = castMetalType;
        hasCastMetal = false;
        castMetalType = "";
        castSolidifyTicks = 0;
        castSolidified = false;
        castExpireTicks = 0;
        castExpired = false;
        syncToClient();
        return type;
    }

    // ==================== Big Crucible System ====================

    public int getBigCrucibleCount() {
        return bigCrucibleCount;
    }

    public String getBigCrucibleMetalType() {
        return bigCrucibleMetalType;
    }

    public boolean isBigCrucibleFull() {
        return bigCrucibleCount >= MAX_BIG_CRUCIBLE;
    }

    /**
     * Pours molten metal into the big crucible.
     * @param metalType the type of metal to pour
     * @return true if successful, false if full or type mismatch
     */
    public boolean pourIntoBigCrucible(String metalType) {
        if (bigCrucibleExpired) return false;
        if (bigCrucibleCount >= MAX_BIG_CRUCIBLE) {
            return false;
        }
        if (bigCrucibleCount > 0 && !bigCrucibleMetalType.equals(metalType)) {
            return false;
        }
        bigCrucibleMetalType = metalType;
        bigCrucibleCount++;
        bigCrucibleExpireTicks = 0; // Reset timer on each pour
        syncToClient();
        return true;
    }

    /**
     * Empties the big crucible (future: pour into cast).
     * @return the type of metal removed, or "" if empty
     */
    public String emptyBigCrucible() {
        if (bigCrucibleCount <= 0) {
            return "";
        }
        String type = bigCrucibleMetalType;
        bigCrucibleCount = 0;
        bigCrucibleMetalType = "";
        bigCrucibleExpireTicks = 0;
        bigCrucibleExpired = false;
        syncToClient();
        return type;
    }

    // ==================== Expiration Getters & Cleanup ====================

    public boolean isTinyCrucibleExpired() { return tinyCrucibleExpired; }
    public boolean isBigCrucibleExpired() { return bigCrucibleExpired; }
    public boolean isCastExpired() { return castExpired; }

    /**
     * Cleans expired solidified metal from the big crucible.
     * @return true if cleanup was performed
     */
    public boolean cleanExpiredBigCrucible() {
        if (!bigCrucibleExpired || bigCrucibleCount <= 0) return false;
        bigCrucibleCount = 0;
        bigCrucibleMetalType = "";
        bigCrucibleExpireTicks = 0;
        bigCrucibleExpired = false;
        syncToClient();
        return true;
    }

    /**
     * Cleans expired solidified metal from the cast mold.
     * @return true if cleanup was performed
     */
    public boolean cleanExpiredCast() {
        if (!castExpired || !hasCastMetal) return false;
        hasCastMetal = false;
        castMetalType = "";
        castSolidifyTicks = 0;
        castSolidified = false;
        castExpireTicks = 0;
        castExpired = false;
        syncToClient();
        return true;
    }

    // ==================== Tier System ====================

    /**
     * Gets the current tier of the smithing furnace (0-4)
     */
    public int getTier() {
        return tier;
    }

    /**
     * Sets the tier of the smithing furnace (0-4)
     */
    public void setTier(int tier) {
        this.tier = Math.max(0, Math.min(4, tier));
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Upgrades the tier by 1, up to maximum of 4
     * @return true if upgrade was successful, false if already at max tier
     */
    public boolean upgradeTier() {
        if (tier >= 4) {
            return false;
        }
        setTier(tier + 1);
        return true;
    }

    // ==================== Active State (Coal Burning) ====================

    /**
     * Gets whether the furnace is active (coal is burning)
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Toggles the active state (legacy - use lightFurnace() instead)
     */
    public void toggleActive() {
        if (active) {
            extinguish();
        } else {
            lightFurnace();
        }
    }

    // ==================== NBT Persistence ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Tier", tier);
        tag.putBoolean("Active", active);
        tag.putInt("CoalCount", coalCount);
        tag.putInt("CoalBurnTicks", coalBurnTicks);
        tag.putBoolean("HooverActive", hooverActive);
        tag.putBoolean("CogiuoloActive", cogiuoloActive);
        tag.putBoolean("DoorOpen", doorOpen);
        tag.putInt("AshCount", ashCount);
        tag.putInt("AshAccumulationTicks", ashAccumulationTicks);
        tag.putInt("AshFullTicks", ashFullTicks);
        tag.putBoolean("AshOverpressure", ashOverpressure);
        // Installed components
        tag.putBoolean("HasBellows", hasBellows);
        tag.putBoolean("HasCrucible", hasCrucible);
        tag.putBoolean("HasHoover", hasHoover);
        tag.putBoolean("HasChimney", hasChimney);
        tag.putBoolean("HasDoor", hasDoor);
        // Tiny crucible state
        tag.putBoolean("HasTinyCrucible", hasTinyCrucible);
        // Smelting system
        tag.putString("SmeltingRawType", smeltingRawType);
        tag.putInt("SmeltingTicks", smeltingTicks);
        tag.putBoolean("HasMoltenMetal", hasMoltenMetal);
        tag.putString("MoltenMetalType", moltenMetalType);
        // Cast ingot system
        tag.putBoolean("HasCastMetal", hasCastMetal);
        tag.putString("CastMetalType", castMetalType);
        tag.putInt("CastSolidifyTicks", castSolidifyTicks);
        tag.putBoolean("CastSolidified", castSolidified);
        // Big crucible system
        tag.putInt("BigCrucibleCount", bigCrucibleCount);
        tag.putString("BigCrucibleMetalType", bigCrucibleMetalType);
        // Expiration system
        tag.putInt("TinyCrucibleExpireTicks", tinyCrucibleExpireTicks);
        tag.putBoolean("TinyCrucibleExpired", tinyCrucibleExpired);
        tag.putInt("BigCrucibleExpireTicks", bigCrucibleExpireTicks);
        tag.putBoolean("BigCrucibleExpired", bigCrucibleExpired);
        tag.putInt("CastExpireTicks", castExpireTicks);
        tag.putBoolean("CastExpired", castExpired);
        // Ingots on embers
        tag.putInt("IngotCount", ingotCount);
        tag.putString("IngotMetalType", ingotMetalType);
        tag.putInt("IngotHeatTicks", ingotHeatTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = tag.getInt("Tier");
        active = tag.getBoolean("Active");
        coalCount = tag.getInt("CoalCount");
        coalBurnTicks = tag.getInt("CoalBurnTicks");
        hooverActive = tag.getBoolean("HooverActive");
        cogiuoloActive = tag.getBoolean("CogiuoloActive");
        doorOpen = tag.getBoolean("DoorOpen");
        ashCount = tag.getInt("AshCount");
        ashAccumulationTicks = tag.getInt("AshAccumulationTicks");
        ashFullTicks = tag.getInt("AshFullTicks");
        ashOverpressure = tag.getBoolean("AshOverpressure");
        // Installed components
        hasBellows = tag.getBoolean("HasBellows");
        hasCrucible = tag.getBoolean("HasCrucible");
        hasHoover = tag.getBoolean("HasHoover");
        hasChimney = tag.getBoolean("HasChimney");
        hasDoor = tag.getBoolean("HasDoor");
        // Tiny crucible state
        hasTinyCrucible = !tag.contains("HasTinyCrucible") || tag.getBoolean("HasTinyCrucible");
        // Smelting system
        smeltingRawType = tag.getString("SmeltingRawType");
        smeltingTicks = tag.getInt("SmeltingTicks");
        hasMoltenMetal = tag.getBoolean("HasMoltenMetal");
        moltenMetalType = tag.getString("MoltenMetalType");
        // Cast ingot system
        hasCastMetal = tag.getBoolean("HasCastMetal");
        castMetalType = tag.getString("CastMetalType");
        castSolidifyTicks = tag.getInt("CastSolidifyTicks");
        castSolidified = tag.getBoolean("CastSolidified");
        // Big crucible system
        bigCrucibleCount = tag.getInt("BigCrucibleCount");
        bigCrucibleMetalType = tag.getString("BigCrucibleMetalType");
        // Expiration system
        tinyCrucibleExpireTicks = tag.getInt("TinyCrucibleExpireTicks");
        tinyCrucibleExpired = tag.getBoolean("TinyCrucibleExpired");
        bigCrucibleExpireTicks = tag.getInt("BigCrucibleExpireTicks");
        bigCrucibleExpired = tag.getBoolean("BigCrucibleExpired");
        castExpireTicks = tag.getInt("CastExpireTicks");
        castExpired = tag.getBoolean("CastExpired");
        // Ingots on embers
        ingotCount = tag.getInt("IngotCount");
        ingotMetalType = tag.getString("IngotMetalType");
        ingotHeatTicks = tag.getInt("IngotHeatTicks");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ==================== Server Tick (Coal Consumption) ====================

    /**
     * Server-side tick for coal consumption.
     * Consumes 1 coal per minute when active.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SmithingFurnaceBlockEntity blockEntity) {
        // Cast solidification progresses always (metal cools naturally), even when furnace is off
        if (blockEntity.hasCastMetal && !blockEntity.castSolidified) {
            blockEntity.castSolidifyTicks++;
            if (blockEntity.castSolidifyTicks >= CAST_SOLIDIFY_TIME) {
                blockEntity.castSolidified = true;
                blockEntity.syncToClient();
            }
        }

        // ==================== Expiration Timers (always tick) ====================
        // Tiny crucible: counts when fire is off and molten metal present, resets if fire relit
        if (blockEntity.hasMoltenMetal && !blockEntity.tinyCrucibleExpired) {
            if (!blockEntity.active) {
                blockEntity.tinyCrucibleExpireTicks++;
                if (blockEntity.tinyCrucibleExpireTicks >= EXPIRE_TIME) {
                    blockEntity.tinyCrucibleExpired = true;
                    blockEntity.syncToClient();
                }
            } else if (blockEntity.tinyCrucibleExpireTicks > 0) {
                blockEntity.tinyCrucibleExpireTicks = 0;
            }
        }
        // Big crucible: always counts when metal is present
        if (blockEntity.bigCrucibleCount > 0 && !blockEntity.bigCrucibleExpired) {
            blockEntity.bigCrucibleExpireTicks++;
            if (blockEntity.bigCrucibleExpireTicks >= BIG_CRUCIBLE_EXPIRE_TIME) {
                blockEntity.bigCrucibleExpired = true;
                blockEntity.syncToClient();
            }
        }
        // Cast mold: counts after solidification completes
        if (blockEntity.castSolidified && !blockEntity.castExpired) {
            blockEntity.castExpireTicks++;
            if (blockEntity.castExpireTicks >= EXPIRE_TIME) {
                blockEntity.castExpired = true;
                blockEntity.syncToClient();
            }
        }

        if (!blockEntity.active) {
            // Reset overpressure when furnace goes inactive
            if (blockEntity.ashFullTicks > 0) {
                blockEntity.ashFullTicks = 0;
                if (blockEntity.ashOverpressure) {
                    blockEntity.ashOverpressure = false;
                    blockEntity.syncToClient();
                }
            }
            return;
        }

        // Increment burn ticks - 2x speed when hoover/mantice is active
        blockEntity.coalBurnTicks += (blockEntity.hasHoover && blockEntity.hooverActive) ? 2 : 1;

        // Every minute (1200 ticks), consume 1 coal (30s with mantice active)
        if (blockEntity.coalBurnTicks >= TICKS_PER_COAL) {
            blockEntity.coalBurnTicks = 0;
            blockEntity.coalCount--;

            // If coal runs out, extinguish the furnace and reset overpressure
            if (blockEntity.coalCount <= 0) {
                blockEntity.coalCount = 0;
                blockEntity.active = false;
                blockEntity.ashFullTicks = 0;
                blockEntity.ashOverpressure = false;
            }

            blockEntity.syncToClient();
        }

        // Ash accumulation: every 2 minutes (2400 ticks) while active, add 1 ash (max 6)
        // 2x speed when hoover/mantice is active (same as coal consumption)
        // Half speed when door is open (better ventilation)
        if (blockEntity.ashCount < MAX_ASH) {
            int ashRate = (blockEntity.hasHoover && blockEntity.hooverActive) ? 2 : 1;
            boolean doorVentilation = blockEntity.hasDoor && blockEntity.doorOpen;
            blockEntity.ashAccumulationTicks += ashRate;
            int ashThreshold = doorVentilation ? TICKS_PER_ASH * 2 : TICKS_PER_ASH;
            if (blockEntity.ashAccumulationTicks >= ashThreshold) {
                blockEntity.ashAccumulationTicks = 0;
                blockEntity.ashCount++;
                blockEntity.syncToClient();
            }
        }

        // ==================== Ash Overpressure ====================
        // When ash is at max, pressure builds: 1 min → smoke, 4 min total → explosion
        if (blockEntity.ashCount >= MAX_ASH) {
            blockEntity.ashFullTicks++;

            // Start smoke phase (synced once to client)
            if (!blockEntity.ashOverpressure && blockEntity.ashFullTicks >= ASH_SMOKE_START_TICKS) {
                blockEntity.ashOverpressure = true;
                blockEntity.syncToClient();
            }

            // Explosion
            if (blockEntity.ashFullTicks >= ASH_EXPLODE_TICKS) {
                double ex = pos.getX() + 0.5;
                double ey = pos.getY() + 0.5;
                double ez = pos.getZ() + 0.5;
                level.explode(null, ex, ey, ez, 12.0f, Level.ExplosionInteraction.TNT);
                return; // block entity is gone
            }
        } else if (blockEntity.ashFullTicks > 0) {
            // Ash dropped below max (e.g. player removed some) → reset
            blockEntity.ashFullTicks = 0;
            if (blockEntity.ashOverpressure) {
                blockEntity.ashOverpressure = false;
                blockEntity.syncToClient();
            }
        }

        // ==================== Smelting System ====================
        // Process raw ore smelting in the tiny crucible
        if (!blockEntity.smeltingRawType.isEmpty() && !blockEntity.hasMoltenMetal) {
            // Smelting progresses: 2x speed when hoover/mantice is active
            blockEntity.smeltingTicks += (blockEntity.hasHoover && blockEntity.hooverActive) ? 2 : 1;

            if (blockEntity.smeltingTicks >= SMELTING_TIME) {
                blockEntity.hasMoltenMetal = true;
                blockEntity.moltenMetalType = blockEntity.smeltingRawType;
                blockEntity.smeltingRawType = "";  // raw ore consumed
                blockEntity.smeltingTicks = 0;
                blockEntity.syncToClient();
            }
        }

        // ==================== Ingots on Embers Heating/Cooling ====================
        if (blockEntity.ingotCount > 0) {
            if (blockEntity.active && blockEntity.coalCount > 0) {
                // Heating
                if (blockEntity.ingotHeatTicks < INGOT_HEAT_TIME) {
                    blockEntity.ingotHeatTicks++;
                    if (blockEntity.ingotHeatTicks % 20 == 0) {
                        blockEntity.syncToClient();
                    }
                }
            } else {
                // Cooling
                if (blockEntity.ingotHeatTicks > 0) {
                    blockEntity.ingotHeatTicks--;
                    if (blockEntity.ingotHeatTicks % 20 == 0) {
                        blockEntity.syncToClient();
                    }
                }
            }
        }
    }

    // ==================== Client Tick (Particles) ====================

    /**
     * Client-side tick for spawning fire particles when furnace is active.
     * Particles spawn above the "Carbone" bone area - small delicate flames and occasional smoke.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, SmithingFurnaceBlockEntity blockEntity) {
        RandomSource random = level.getRandom();
        Direction facing = state.getValue(SmithingFurnaceBlock.FACING);

        // Calculate base position offset based on facing
        double offsetX = 0, offsetZ = 0;
        switch (facing) {
            case NORTH -> offsetZ = -0.5;
            case SOUTH -> offsetZ = 0.5;
            case EAST -> offsetX = 0.5;
            case WEST -> offsetX = -0.5;
        }

        // ==================== Overpressure Particles (fast smoke + fire sparks in all directions) ====================
        if (blockEntity.ashOverpressure) {
            // Dense smoke burst - 9-15 particles per tick
            int smokeCount = 9 + random.nextInt(7);
            for (int i = 0; i < smokeCount; i++) {
                double px = pos.getX() + 0.5 + offsetX + (random.nextDouble() - 0.5) * 1.5;
                double py = pos.getY() + 0.5 + random.nextDouble() * 0.8;
                double pz = pos.getZ() + 0.5 + offsetZ + (random.nextDouble() - 0.5) * 1.5;
                double vx = (random.nextDouble() - 0.5) * 0.6;
                double vy = (random.nextDouble() - 0.5) * 0.6;
                double vz = (random.nextDouble() - 0.5) * 0.6;
                level.addParticle(ParticleTypes.SMOKE, px, py, pz, vx, vy, vz);
            }
            // Fire sparks shooting out fast - 3-5 per tick
            int fireCount = 3 + random.nextInt(3);
            for (int i = 0; i < fireCount; i++) {
                double px = pos.getX() + 0.5 + offsetX + (random.nextDouble() - 0.5);
                double py = pos.getY() + 0.5 + random.nextDouble() * 0.6;
                double pz = pos.getZ() + 0.5 + offsetZ + (random.nextDouble() - 0.5);
                double vx = (random.nextDouble() - 0.5) * 0.5;
                double vy = (random.nextDouble() - 0.5) * 0.5;
                double vz = (random.nextDouble() - 0.5) * 0.5;
                level.addParticle(ParticleTypes.FLAME, px, py, pz, vx, vy, vz);
            }
            // Lava ember sparks - 2-3 per tick
            int emberCount = 2 + random.nextInt(2);
            for (int i = 0; i < emberCount; i++) {
                double px = pos.getX() + 0.5 + offsetX + (random.nextDouble() - 0.5);
                double py = pos.getY() + 0.8;
                double pz = pos.getZ() + 0.5 + offsetZ + (random.nextDouble() - 0.5);
                double vx = (random.nextDouble() - 0.5) * 0.4;
                double vy = 0.1 + random.nextDouble() * 0.3;
                double vz = (random.nextDouble() - 0.5) * 0.4;
                level.addParticle(ParticleTypes.LAVA, px, py, pz, vx, vy, vz);
            }
        }

        // ==================== Hoover Particles (rhythmic puffs when mantice is active AND coal is burning) ====================
        // Smoke puffs appear ONLY when: hoover installed AND hoover active AND coal is burning
        boolean shouldPuff = blockEntity.hasHoover && blockEntity.hooverActive && blockEntity.active;

        if (shouldPuff) {
            blockEntity.hooverParticleTicks++;

            // Every 2 seconds (40 ticks), spawn a dense smoke puff
            if (blockEntity.hooverParticleTicks >= HOOVER_PUFF_INTERVAL) {
                blockEntity.hooverParticleTicks = 0;

                // Spawn a burst of dense smoke particles (8-12 particles)
                int particleCount = 8 + random.nextInt(5);
                for (int i = 0; i < particleCount; i++) {
                    // Random position in carbone area
                    double localX = (random.nextDouble() * 12.0 - 6.0) / 16.0;
                    double localY = 11.0 / 16.0 + 0.3;  // Slightly above ember
                    double localZ = (random.nextDouble() * 8.0 - 6.0) / 16.0;

                    double worldX, worldZ;
                    switch (facing) {
                        case NORTH -> { worldX = -localX; worldZ = -localZ; }
                        case SOUTH -> { worldX = localX; worldZ = localZ; }
                        case EAST -> { worldX = localZ; worldZ = -localX; }
                        case WEST -> { worldX = -localZ; worldZ = localX; }
                        default -> { worldX = localX; worldZ = localZ; }
                    }

                    double particleX = pos.getX() + 0.5 + worldX + offsetX;
                    double particleY = pos.getY() + localY;
                    double particleZ = pos.getZ() + 0.5 + worldZ + offsetZ;

                    // Dense large smoke puff - using LARGE_SMOKE for visibility
                    level.addParticle(ParticleTypes.LARGE_SMOKE,
                            particleX + (random.nextDouble() - 0.5) * 0.2,
                            particleY,
                            particleZ + (random.nextDouble() - 0.5) * 0.2,
                            (random.nextDouble() - 0.5) * 0.02,
                            0.05 + random.nextDouble() * 0.03,  // Strong upward velocity
                            (random.nextDouble() - 0.5) * 0.02);
                }

                // Add a few fire sparks (2-4 sparks)
                int sparkCount = 2 + random.nextInt(3);
                for (int i = 0; i < sparkCount; i++) {
                    double localX = (random.nextDouble() * 12.0 - 6.0) / 16.0;
                    double localY = 11.0 / 16.0 + 0.1;
                    double localZ = (random.nextDouble() * 8.0 - 6.0) / 16.0;

                    double worldX, worldZ;
                    switch (facing) {
                        case NORTH -> { worldX = -localX; worldZ = -localZ; }
                        case SOUTH -> { worldX = localX; worldZ = localZ; }
                        case EAST -> { worldX = localZ; worldZ = -localX; }
                        case WEST -> { worldX = -localZ; worldZ = localX; }
                        default -> { worldX = localX; worldZ = localZ; }
                    }

                    double particleX = pos.getX() + 0.5 + worldX + offsetX;
                    double particleY = pos.getY() + localY;
                    double particleZ = pos.getZ() + 0.5 + worldZ + offsetZ;

                    // Fire spark - small flame that shoots up
                    level.addParticle(ParticleTypes.SMALL_FLAME,
                            particleX,
                            particleY,
                            particleZ,
                            (random.nextDouble() - 0.5) * 0.03,
                            0.08 + random.nextDouble() * 0.05,  // Strong upward burst
                            (random.nextDouble() - 0.5) * 0.03);
                }
            }
        }

        // ==================== Normal Active Particles (constant when coal is burning) ====================
        if (!blockEntity.active) {
            return;
        }

        // Light gray rising smoke - ~8% chance per tick
        // Spawns 0.5 blocks above the ember area
        if (random.nextFloat() < 0.08f) {
            // Random position in carbone area
            double localX = (random.nextDouble() * 12.0 - 6.0) / 16.0;
            double localY = 11.0 / 16.0 + 0.5;  // 0.5 blocks higher than ember
            double localZ = (random.nextDouble() * 8.0 - 6.0) / 16.0;

            double worldX, worldZ;
            switch (facing) {
                case NORTH -> { worldX = -localX; worldZ = -localZ; }
                case SOUTH -> { worldX = localX; worldZ = localZ; }
                case EAST -> { worldX = localZ; worldZ = -localX; }
                case WEST -> { worldX = -localZ; worldZ = localX; }
                default -> { worldX = localX; worldZ = localZ; }
            }

            double particleX = pos.getX() + 0.5 + worldX + offsetX;
            double particleY = pos.getY() + localY;
            double particleZ = pos.getZ() + 0.5 + worldZ + offsetZ;

            // Light gray campfire smoke, rises ~6 blocks
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    particleX + (random.nextDouble() - 0.5) * 0.1,
                    particleY,
                    particleZ + (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.003,
                    0.04,  // higher velocity to reach ~6 blocks
                    (random.nextDouble() - 0.5) * 0.003);
        }

        // Small fire particles - only ~15% chance per tick
        if (random.nextFloat() > 0.15f) {
            return;
        }

        // Carbone bone area in Blockbench units:
        // X: -6 to 6, Y: ~11, Z: -6 to 2
        // Convert to block coordinates (16 units = 1 block)
        double localX = (random.nextDouble() * 12.0 - 6.0) / 16.0;
        double localY = 11.0 / 16.0;
        double localZ = (random.nextDouble() * 8.0 - 6.0) / 16.0;

        double worldX, worldZ;
        switch (facing) {
            case NORTH -> { worldX = -localX; worldZ = -localZ; }
            case SOUTH -> { worldX = localX; worldZ = localZ; }
            case EAST -> { worldX = localZ; worldZ = -localX; }
            case WEST -> { worldX = -localZ; worldZ = localX; }
            default -> { worldX = localX; worldZ = localZ; }
        }

        // Calculate final world position with offset correction
        double particleX = pos.getX() + 0.5 + worldX + offsetX;
        double particleY = pos.getY() + localY;
        double particleZ = pos.getZ() + 0.5 + worldZ + offsetZ;

        // Small flame particle - using SMALL_FLAME for half-size effect
        level.addParticle(ParticleTypes.SMALL_FLAME,
                particleX, particleY, particleZ,
                (random.nextDouble() - 0.5) * 0.003,
                0.005 + random.nextDouble() * 0.01,
                (random.nextDouble() - 0.5) * 0.003);

        // Occasional delicate smoke - ~10% chance when spawning fire
        if (random.nextFloat() < 0.1f) {
            level.addParticle(ParticleTypes.SMOKE,
                    particleX + (random.nextDouble() - 0.5) * 0.05,
                    particleY + 0.03,
                    particleZ + (random.nextDouble() - 0.5) * 0.05,
                    0.0, 0.005, 0.0);
        }
    }

    // ==================== GeckoLib Implementation ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Register the idle/levitate animation controller
        // This animation runs permanently and loops forever
        controllers.add(new AnimationController<>(this, "idle", 0, state -> {
            // Only set animation if not already playing to prevent restarts
            if (state.getController().getAnimationState() == AnimationController.State.STOPPED) {
                state.getController().setAnimation(LEVITATE_ANIM);
            }
            return PlayState.CONTINUE;
        }));

        // Register the hoover (mantice) animation controller
        // Only plays when hoover is installed AND hooverActive is true
        controllers.add(new AnimationController<>(this, "hoover", 5, state -> {
            if (this.hasHoover && this.hooverActive) {
                state.getController().setAnimation(HOOVER_ANIM);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));

        // Register the cogiuolo animation controller (one-shot, triggered)
        controllers.add(new AnimationController<>(this, "cogiuolo", 5, state -> PlayState.STOP)
                .triggerableAnim("pour", COGIUOLO_ANIM));

        // Register the door animation controller
        // Plays door_open when opening, door_close when closing
        controllers.add(new AnimationController<>(this, "door", 5, state -> {
            if (!this.hasDoor) return PlayState.STOP;
            if (this.doorOpen) {
                state.getController().setAnimation(DOOR_OPEN_ANIM);
            } else {
                state.getController().setAnimation(DOOR_CLOSE_ANIM);
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
