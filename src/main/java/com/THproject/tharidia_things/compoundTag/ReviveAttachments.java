package com.THproject.tharidia_things.compoundTag;

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
    private long last_revived_time = 0;
    private boolean death_from_battle = false;
    private int invulnerability_tick = 0; // 10 seconds

    public void resetResTime() {
        this.res_time = Integer.parseInt(ReviveConfig.config.TIME_TO_RES.get("Value").toString());
    }

    public void setResTime(int time) {
        this.res_time = time;
    }

    public void setLastRevivedTime(long time) {
        this.last_revived_time = time;
    }

    public void setCanRevive(boolean val) {
        this.death_from_battle = val;
    }

    public void setInvulnerabilityTick(int tick) {
        this.invulnerability_tick = tick;
    }

    public int getResTime() {
        return res_time;
    }

    public long getLastRevivedTime() {
        return last_revived_time;
    }

    public boolean canRevive() {
        return death_from_battle;
    }

    public int getInvulnerabilityTick() {
        return invulnerability_tick;
    }

    public void decreaseResTime() {
        if (this.res_time > 0)
            this.res_time -= 1;
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("res_time", this.res_time);
        nbt.putLong("last_revived_time", last_revived_time);
        nbt.putBoolean("death_from_battle", death_from_battle);
        nbt.putInt("invulnerability_time", invulnerability_tick);
        return nbt;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        this.res_time = nbt.getInt("res_time");
        this.last_revived_time = nbt.getLong("last_revived_time");
        this.death_from_battle = nbt.getBoolean("death_from_battle");
        this.invulnerability_tick = nbt.getInt("invulnerability_time");
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
