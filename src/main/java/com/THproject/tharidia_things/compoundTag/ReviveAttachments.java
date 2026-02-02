package com.THproject.tharidia_things.compoundTag;

import java.util.UUID;
import java.util.function.Supplier;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.config.ReviveConfig;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ReviveAttachments implements INBTSerializable<CompoundTag> {
    private int res_time = 0;
    private int time_fallen = 0;
    private boolean death_from_battle = false;
    private int invulnerability_tick = 0; // 10 seconds
    private boolean can_fall = true;
    private UUID revivingPlayer = null;

    public void setRevivingPlayer(UUID playerUUID) {
        this.revivingPlayer = playerUUID;
    }

    public UUID getRevivingPlayer() {
        return revivingPlayer;
    }

    public void resetResTime() {
        this.res_time = Integer.parseInt(ReviveConfig.config.TIME_TO_RES.get("Value").toString());
    }

    public void setResTime(int time) {
        this.res_time = time;
    }

    public int getTimeFallen() {
        return time_fallen;
    }

    public void setTimeFallen(int time) {
        this.time_fallen = time;
    }

    public void increaseTimeFallen() {
        this.time_fallen++;
    }

    public void setCanRevive(boolean val) {
        this.death_from_battle = val;
    }

    public void setInvulnerabilityTick(int tick) {
        this.invulnerability_tick = tick;
    }

    public void setCanFall(boolean val) {
        this.can_fall = val;
    }

    public int getResTime() {
        return res_time;
    }

    public boolean canRevive() {
        return death_from_battle;
    }

    public int getInvulnerabilityTick() {
        return invulnerability_tick;
    }

    public boolean canFall() {
        return can_fall;
    }

    public void decreaseResTime() {
        if (this.res_time > 0)
            this.res_time -= 1;
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("res_time", this.res_time);
        nbt.putInt("time_fallen", this.time_fallen);
        nbt.putBoolean("death_from_battle", death_from_battle);
        nbt.putInt("invulnerability_time", invulnerability_tick);
        if (revivingPlayer != null)
            nbt.putUUID("reviving_player", revivingPlayer);
        return nbt;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        this.res_time = nbt.getInt("res_time");
        if (nbt.contains("time_fallen"))
            this.time_fallen = nbt.getInt("time_fallen");
        this.death_from_battle = nbt.getBoolean("death_from_battle");
        this.invulnerability_tick = nbt.getInt("invulnerability_time");
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
