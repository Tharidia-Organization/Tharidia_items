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

public class BattleGauntleAttachments implements INBTSerializable<CompoundTag> {
    private boolean in_battle = false;
    private UUID challenger_uuid = null;
    private float player_health = 0;
    private int win_tick = 0;
    private int lose_tick = 0;

    public void setPlayerHealth(float health) {
        this.player_health = health;
    }

    public void setInBattle(boolean in_battle) {
        this.in_battle = in_battle;
    }

    public void setChallengerUUID(UUID challenger_uuid) {
        this.challenger_uuid = challenger_uuid;
    }

    public void setWinTick(int tick) {
        this.win_tick = tick;
    }

    public void setLoseTick(int tick) {
        this.lose_tick = tick;
    }

    public float getPlayerHealth() {
        return this.player_health;
    }

    public boolean getInBattle() {
        return this.in_battle;
    }

    public UUID getChallengerUUID() {
        return this.challenger_uuid;
    }

    public int getWinTick() {
        return this.win_tick;
    }

    public int getLoseTick() {
        return this.lose_tick;
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("player_health", this.player_health);
        nbt.putBoolean("in_battle", this.in_battle);
        nbt.putInt("win_tick", this.win_tick);
        nbt.putInt("lose_tick", this.lose_tick);
        // Only save UUID if it's not null
        if (this.challenger_uuid != null) {
            nbt.putUUID("challenger_uuid", this.challenger_uuid);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        this.player_health = nbt.getFloat("player_health");
        this.in_battle = nbt.getBoolean("in_battle");
        this.win_tick = nbt.getInt("win_tick");
        this.lose_tick = nbt.getInt("lose_tick");
        // Only load UUID if it exists in the NBT
        if (nbt.contains("challenger_uuid")) {
            this.challenger_uuid = nbt.getUUID("challenger_uuid");
        } else {
            this.challenger_uuid = null;
        }
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACKMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.ATTACHMENT_TYPES, TharidiaThings.MODID);

    public static final Supplier<AttachmentType<BattleGauntleAttachments>> BATTLE_GAUNTLE = ATTACKMENT_TYPES
            .register(
                    "battle_gauntlet",
                    () -> AttachmentType.serializable(BattleGauntleAttachments::new).build());

    public static void register(IEventBus eventBus) {
        ATTACKMENT_TYPES.register(eventBus);
    }
}
