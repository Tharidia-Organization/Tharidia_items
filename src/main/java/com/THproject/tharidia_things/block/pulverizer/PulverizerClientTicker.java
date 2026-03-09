package com.THproject.tharidia_things.block.pulverizer;

import com.THproject.tharidia_things.sounds.ModSounds;
import com.THproject.tharidia_things.sounds.PulverizerWorkingSound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PulverizerClientTicker {
    public static void clientTick(Level level, BlockPos pos, BlockState state, PulverizerBlockEntity pulverizer) {
        PulverizerBlockEntity.tick(level, pos, state, pulverizer);
        if (pulverizer.isActive()) {
            Minecraft mc = Minecraft.getInstance();
            if (!mc.getSoundManager().isActive((SoundInstance) pulverizer.getWorkingSound())) {
                PulverizerWorkingSound sound = new PulverizerWorkingSound(pulverizer,
                        ModSounds.PULVERIZER_WORKING.get());
                pulverizer.setWorkingSound(sound);
                mc.getSoundManager().play(sound);
            }
        }
    }
}
