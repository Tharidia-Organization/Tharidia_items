package com.example.tharidia_items.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class TharidiaItemsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
    }
    
    private boolean isPositionWithinPlayerView(MinecraftClient client, BlockPos pos) {
        if (client.player == null) return false;
        
        // Simple distance check (can be optimized further)
        double distance = client.player.getBlockPos().getSquaredDistance(pos);
        return distance < 64 * 64; // 64 blocks view distance squared
    }
}