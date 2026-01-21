package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.client.gui.PietroScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Handles the spiral particle effect above the Pietro block when players are in the group queue.
 * Creates an elegant, sinuous spiral that extends 3 blocks upward.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class GroupQueueParticleHandler {

    private static double spiralAngle = 0;
    private static final double SPIRAL_SPEED = 0.15; // Radians per tick
    private static final double SPIRAL_HEIGHT = 3.0; // 3 blocks high
    private static final int PARTICLES_PER_TICK = 8; // Number of particles spawned per tick

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        BlockPos blockPos = PietroScreen.getActiveGroupQueueBlockPos();
        if (blockPos == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }

        // Increment spiral angle
        spiralAngle += SPIRAL_SPEED;
        if (spiralAngle > Math.PI * 2) {
            spiralAngle -= Math.PI * 2;
        }

        // Spawn spiral particles
        spawnSpiralParticles(level, blockPos);
    }

    private static void spawnSpiralParticles(Level level, BlockPos blockPos) {
        double centerX = blockPos.getX() + 0.5;
        double baseY = blockPos.getY() + 1.0; // Start from top of block
        double centerZ = blockPos.getZ() + 0.5;

        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            // Calculate position along the spiral
            double heightOffset = (double) i / PARTICLES_PER_TICK;
            double currentHeight = heightOffset * SPIRAL_HEIGHT;

            // Double helix effect - two intertwined spirals
            double angle1 = spiralAngle + (heightOffset * Math.PI * 4); // 2 full rotations over height
            double angle2 = angle1 + Math.PI; // Second spiral offset by 180 degrees

            // Radius varies with height - starts small, expands, then contracts
            double radiusMultiplier = Math.sin(heightOffset * Math.PI); // 0 -> 1 -> 0
            double baseRadius = 0.3;
            double maxRadius = 0.8;
            double radius = baseRadius + (maxRadius - baseRadius) * radiusMultiplier;

            // First spiral (green/emerald particles)
            double x1 = centerX + Math.cos(angle1) * radius;
            double z1 = centerZ + Math.sin(angle1) * radius;
            double y1 = baseY + currentHeight;

            // Second spiral (enchantment particles for magical effect)
            double x2 = centerX + Math.cos(angle2) * radius;
            double z2 = centerZ + Math.sin(angle2) * radius;
            double y2 = baseY + currentHeight;

            // Spawn particles with slight upward velocity
            double velocityY = 0.02;

            // Primary spiral - happy villager (green sparkles)
            level.addParticle(
                ParticleTypes.HAPPY_VILLAGER,
                x1, y1, z1,
                0, velocityY, 0
            );

            // Secondary spiral - enchant particles (magical swirl)
            level.addParticle(
                ParticleTypes.ENCHANT,
                x2, y2, z2,
                (centerX - x2) * 0.1, velocityY, (centerZ - z2) * 0.1
            );
        }

        // Add some end rod particles at the top for a crown effect
        if (Math.random() < 0.3) {
            double topY = baseY + SPIRAL_HEIGHT;
            double crownAngle = spiralAngle * 2;
            double crownRadius = 0.4;

            level.addParticle(
                ParticleTypes.END_ROD,
                centerX + Math.cos(crownAngle) * crownRadius,
                topY,
                centerZ + Math.sin(crownAngle) * crownRadius,
                0, 0.05, 0
            );
        }

        // Add some soul fire flame particles rising from the base
        if (Math.random() < 0.2) {
            double flameAngle = Math.random() * Math.PI * 2;
            double flameRadius = 0.3 + Math.random() * 0.2;

            level.addParticle(
                ParticleTypes.SOUL_FIRE_FLAME,
                centerX + Math.cos(flameAngle) * flameRadius,
                baseY,
                centerZ + Math.sin(flameAngle) * flameRadius,
                0, 0.03 + Math.random() * 0.02, 0
            );
        }
    }
}
