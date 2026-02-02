package com.THproject.tharidia_things.compoundTag;

import java.util.UUID;
import java.util.function.Supplier;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ReviveAttachments implements INBTSerializable<CompoundTag> {
    // Max time of invulnerability after fall
    public static int INVULNERABILITY_TICK = 200;
    // Max time to be fallen after death
    public static int MAX_FALLEN_TICK = 400;
    // Time to res
    public static int MAX_RES_TICK = 50;

    // Time to res the player
    private int res_time = 50;

    // Tick count when player fallen (Used for kill after certain time)
    private int fallen_time = 0;

    // Determine if player fall when die
    private boolean can_fall = true;

    // Determine if player can be revived when fallen
    private boolean can_revive = false;

    // Check if player is fallen
    private boolean is_fallen = false;

    // The player that is beign reviving
    private UUID revivingPlayer = null;

    public void setIsFallen(boolean isFallen) {
        this.is_fallen = isFallen;
    }

    public boolean isFallen() {
        return is_fallen;
    }

    public void setRevivingPlayer(UUID playerUUID) {
        this.revivingPlayer = playerUUID;
    }

    public UUID getRevivingPlayer() {
        return revivingPlayer;
    }

    public void resetResTime() {
        this.res_time = MAX_RES_TICK;
    }

    public void setResTime(int time) {
        this.res_time = time;
    }

    public int getResTick() {
        return res_time;
    }

    public int getTimeFallen() {
        return fallen_time;
    }

    public void setTimeFallen(int time) {
        this.fallen_time = time;
    }

    public void increaseTimeFallen() {
        this.fallen_time++;
    }

    public void setCanRevive(boolean val) {
        this.can_revive = val;
    }

    public void setCanFall(boolean val) {
        this.can_fall = val;
    }

    public boolean canRevive() {
        return can_revive;
    }

    public boolean canFall() {
        return can_fall;
    }

    public void decreaseResTick() {
        if (this.res_time > 0)
            this.res_time -= 1;
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("res_time", this.res_time);
        nbt.putInt("time_fallen", this.fallen_time);
        nbt.putBoolean("can_revive", this.can_revive);
        nbt.putBoolean("is_fallen", this.is_fallen);
        nbt.putBoolean("can_fall", this.can_fall);
        if (revivingPlayer != null)
            nbt.putUUID("reviving_player", revivingPlayer);
        return nbt;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        this.res_time = nbt.getInt("res_time");
        this.fallen_time = nbt.getInt("time_fallen");
        this.can_revive = nbt.getBoolean("can_revive");
        this.can_fall = nbt.getBoolean("can_fall");
        this.is_fallen = nbt.getBoolean("is_fallen");
        if (nbt.hasUUID("reviving_player"))
            this.revivingPlayer = nbt.getUUID("reviving_player");
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.ATTACHMENT_TYPES, TharidiaThings.MODID);

    public static final Supplier<AttachmentType<ReviveAttachments>> REVIVE_DATA = ATTACHMENT_TYPES
            .register(
                    "revive_data",
                    () -> AttachmentType.serializable(ReviveAttachments::new).build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
