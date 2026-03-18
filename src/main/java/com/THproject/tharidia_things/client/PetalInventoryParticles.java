package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.PetalItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class PetalInventoryParticles {

    private static final List<InvParticle> particles = new ArrayList<>();
    private static final Random rand = new Random();
    private static long lastSpawnTick = 0;
    private static final int SPAWN_INTERVAL = 6;
    private static final int MAX_PARTICLES = 40;

    private static class InvParticle {
        float x, y;
        float vx, vy;
        int maxLife, age;
        int color;
        float baseSize;
        boolean mystical;
        float pulseOffset; // per-particle phase offset for pulsing

        InvParticle(float x, float y, int color, boolean mystical) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.mystical = mystical;
            this.age = 0;
            this.pulseOffset = rand.nextFloat() * (float) (Math.PI * 2);

            if (mystical) {
                // Mystical: slow float upward like a glowing spore
                float angle = rand.nextFloat() * (float) (Math.PI * 2);
                this.vx = (float) Math.cos(angle) * 0.15f;
                this.vy = -0.15f - rand.nextFloat() * 0.25f;
                this.maxLife = 35 + rand.nextInt(20);
                this.baseSize = 1.0f + rand.nextFloat() * 0.8f;
            } else {
                // Normal: petal drifting gently, like falling from a flower
                this.vx = -0.1f + rand.nextFloat() * 0.2f;
                this.vy = 0.08f + rand.nextFloat() * 0.15f; // slow fall
                this.maxLife = 30 + rand.nextInt(15);
                this.baseSize = 1.0f + rand.nextFloat() * 0.5f;
            }
        }

        void tick() {
            age++;
            x += vx;
            y += vy;

            if (mystical) {
                // Gentle float with soft sway
                vx += (float) Math.sin(age * 0.15 + pulseOffset) * 0.02f;
                vy *= 0.99f;
            } else {
                // Leaf-like swaying as it falls
                vx += (float) Math.sin(age * 0.12 + pulseOffset) * 0.015f;
            }
        }

        boolean isDead() {
            return age >= maxLife;
        }

        float getAlpha() {
            float life = (float) age / maxLife;
            if (life < 0.08f) return life / 0.08f;
            if (life > 0.5f) return 1.0f - ((life - 0.5f) / 0.5f);
            return 1.0f;
        }

        float getSize() {
            // Gentle breathing
            float pulse = 1.0f + (float) Math.sin(age * 0.2 + pulseOffset) * 0.15f;
            // Shrink softly at end of life
            float life = (float) age / maxLife;
            float shrink = life > 0.7f ? 1.0f - ((life - 0.7f) / 0.3f) * 0.4f : 1.0f;
            return baseSize * pulse * shrink;
        }
    }

    @SubscribeEvent
    public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof AbstractContainerScreen<?> screen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        long tick = mc.level != null ? mc.level.getGameTime() : 0;

        // Spawn new particles from petal slots
        if (tick != lastSpawnTick && tick % SPAWN_INTERVAL == 0) {
            lastSpawnTick = tick;
            spawnFromSlots(screen);
        }

        // Tick and render particles
        Iterator<InvParticle> it = particles.iterator();
        while (it.hasNext()) {
            InvParticle p = it.next();
            p.tick();
            if (p.isDead()) {
                it.remove();
                continue;
            }
            renderParticle(gui, p);
        }
    }

    private static void spawnFromSlots(AbstractContainerScreen<?> screen) {
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !(stack.getItem() instanceof PetalItem)) continue;

            DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
            if (dyed == null) continue;

            int color = dyed.rgb();
            Rarity rarity = stack.get(DataComponents.RARITY);
            boolean mystical = rarity == Rarity.EPIC;

            if (particles.size() >= MAX_PARTICLES) return;

            float slotX = slot.x + 8;
            float slotY = slot.y + 8;

            int spawnCount = 1;
            for (int i = 0; i < spawnCount; i++) {
                float offX = -4 + rand.nextFloat() * 8;
                float offY = -4 + rand.nextFloat() * 8;
                particles.add(new InvParticle(slotX + offX, slotY + offY, color, mystical));
            }
        }
    }

    private static void renderParticle(GuiGraphics gui, InvParticle p) {
        float alpha = p.getAlpha();
        if (alpha <= 0) return;

        float size = p.getSize();
        int r = (p.color >> 16) & 0xFF;
        int g = (p.color >> 8) & 0xFF;
        int b = p.color & 0xFF;

        if (p.mystical) {
            // Mystical: soft glow + bright core
            float glowSize = size + 0.5f;
            int glowA = (int) (alpha * 100);
            int glowColor = (glowA << 24) | (r << 16) | (g << 8) | b;
            gui.fill(
                    (int) (p.x - glowSize), (int) (p.y - glowSize),
                    (int) (p.x + glowSize), (int) (p.y + glowSize),
                    glowColor);

            float coreSize = size * 0.5f;
            int coreA = (int) (alpha * 200);
            int coreR = Math.min(255, r + 120);
            int coreG = Math.min(255, g + 120);
            int coreB = Math.min(255, b + 120);
            int coreColor = (coreA << 24) | (coreR << 16) | (coreG << 8) | coreB;
            gui.fill(
                    (int) (p.x - coreSize), (int) (p.y - coreSize),
                    (int) (p.x + coreSize), (int) (p.y + coreSize),
                    coreColor);
        } else {
            // Normal: single soft dot, like a tiny petal fragment
            int a = (int) (alpha * 180);
            int argb = (a << 24) | (r << 16) | (g << 8) | b;
            gui.fill(
                    (int) (p.x - size * 0.5f), (int) (p.y - size * 0.5f),
                    (int) (p.x + size * 0.5f), (int) (p.y + size * 0.5f),
                    argb);
        }
    }

    public static void clear() {
        particles.clear();
    }
}
