package com.tharidia.tharidia_things.compoundTag;

import java.util.function.Supplier;

import com.tharidia.tharidia_things.TharidiaThings;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class BattleGauntleAttachments implements INBTSerializable<CompoundTag> {
    private boolean in_battle;
    private float player_health;

    public void setPlayerHealth(float health) {
        this.player_health = health;
    }

    public void setInBattle(boolean in_battle) {
        this.in_battle = in_battle;
    }

    public float getBattleGauntle() {
        return this.player_health;
    }

    public boolean getInBattle() {
        return this.in_battle;
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("player_health", this.player_health);
        nbt.putBoolean("in_battle", this.in_battle);
        return nbt;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        this.player_health = nbt.getFloat("player_health");
        this.in_battle = nbt.getBoolean("in_battle");
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACKMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.ATTACHMENT_TYPES, TharidiaThings.MODID);

    public static final Supplier<AttachmentType<BattleGauntleAttachments>> BATTLE_GAUNTLE = ATTACKMENT_TYPES
            .register(
                    "battle_gauntle",
                    () -> AttachmentType.serializable(BattleGauntleAttachments::new).build());

    public static void register(IEventBus eventBus) {
        ATTACKMENT_TYPES.register(eventBus);
    }
}
