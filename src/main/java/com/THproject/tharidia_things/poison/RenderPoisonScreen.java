package com.THproject.tharidia_things.poison;

import java.lang.reflect.Field;
import java.util.List;

import com.mojang.blaze3d.shaders.Uniform;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class RenderPoisonScreen implements LayeredDraw.Layer {
    private static final ResourceLocation BLUR_SHADER = ResourceLocation.withDefaultNamespace("shaders/post/blur.json");

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;

        PoisonAttachments attachment = PoisonHelper.getAttachment(player);
        float alpha = (attachment != null) ? (attachment.getProgress()) : 0;

        // 1. Manage the Loading/Unloading of the shader
        PostChain currentEffect = mc.gameRenderer.currentEffect();

        if (alpha > 0) {
            // Only load if it's not already the blur shader
            if (currentEffect == null || !currentEffect.getName().equals(BLUR_SHADER.toString())) {
                mc.gameRenderer.loadEffect(BLUR_SHADER);
            }

            // 2. Inject your alpha into the shader uniforms
            updateBlurIntensity(alpha);
        } else {
            // If alpha is 0 and we have a blur effect, shut it down
            if (currentEffect != null && currentEffect.getName().equals(BLUR_SHADER.toString())) {
                mc.gameRenderer.shutdownEffect();
            }
        }
    }

    private void updateBlurIntensity(float alpha) {
        PostChain effect = Minecraft.getInstance().gameRenderer.currentEffect();
        if (effect == null)
            return;

        try {
            // We need to find the "passes" field.
            // Note: In a development environment, the name is "passes".
            // In a production build, it might be obfuscated (e.g., "field_148007_d").
            Field passesField = PostChain.class.getDeclaredField("passes");
            passesField.setAccessible(true);

            List<PostPass> passes = (List<PostPass>) passesField.get(effect);
            float maxBlurRadius = 10.0f;
            float currentRadius = alpha * maxBlurRadius;

            for (PostPass pass : passes) {
                Uniform uniform = pass.getEffect().getUniform("Radius");
                if (uniform != null) {
                    uniform.set(currentRadius);
                }
            }
        } catch (Exception e) {
            // Log the error once so you don't spam the console
            e.printStackTrace();
        }
    }
}