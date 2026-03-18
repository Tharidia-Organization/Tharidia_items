package com.THproject.tharidia_things.sounds;

import com.THproject.tharidia_things.block.pulverizer.PulverizerBlockEntity;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PulverizerWorkingSound extends AbstractTickableSoundInstance {
    private final PulverizerBlockEntity pulverizer;

    public PulverizerWorkingSound(PulverizerBlockEntity blockEntity, SoundEvent soundEvent) {
        super(soundEvent, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.pulverizer = blockEntity;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.x = blockEntity.getBlockPos().getX() + 0.5F;
        this.y = blockEntity.getBlockPos().getY() + 0.5F;
        this.z = blockEntity.getBlockPos().getZ() + 0.5F;
    }

    @Override
    public void tick() {
        if (this.pulverizer.isRemoved() || !this.pulverizer.isActive()) {
            this.stop();
        }
    }
}
