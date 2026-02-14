package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.Config;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.THproject.tharidia_things.block.entity.HotIronAnvilEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

/**
 * Renders the hot iron on the anvil with temperature gradient,
 * pulsing hotspot indicator, and phase progress diamonds.
 */
public class HotIronAnvilRenderer implements BlockEntityRenderer<HotIronAnvilEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private BakedModel[] hotIronModels;

    public HotIronAnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.hotIronModels = new BakedModel[5];
    }

    @Override
    public boolean shouldRenderOffScreen(HotIronAnvilEntity entity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 48;
    }

    @Override
    public void render(HotIronAnvilEntity entity, float partialTick, PoseStack poseStack,
                      MultiBufferSource buffer, int combinedLight, int combinedOverlay) {

        int strikes = Math.min(entity.getHammerStrikes(), 4);

        if (hotIronModels[strikes] == null) {
            var modelManager = Minecraft.getInstance().getModelManager();
            var modelLocation = ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/hot_iron_anvil_" + strikes)
            );
            hotIronModels[strikes] = modelManager.getModel(modelLocation);
        }

        // Temperature gradient: tint model based on cooling progress
        float cooling = getCoolingProgress(entity);
        float[] tempColor = getTemperatureColor(cooling);
        int light = cooling < 0.65f ? 0xF000F0 : combinedLight;

        poseStack.pushPose();

        var vertexConsumer = buffer.getBuffer(RenderType.cutout());
        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            vertexConsumer,
            null,
            hotIronModels[strikes],
            tempColor[0], tempColor[1], tempColor[2],
            light,
            combinedOverlay,
            ModelData.EMPTY,
            null
        );

        poseStack.popPose();

        // Render hotspot when minigame active
        if (entity.isMinigameActive()) {
            renderHotspot(entity, poseStack, buffer);
        }

        // Render progress diamonds
        if (entity.getHammerStrikes() > 0 || entity.isMinigameActive()) {
            renderProgressDiamonds(entity, poseStack, buffer);
        }

        // Render "ready to pick up" indicator when forging is complete
        if (entity.isFinished()) {
            renderReadyIndicator(poseStack, buffer, partialTick);
        }
    }

    private float getCoolingProgress(HotIronAnvilEntity entity) {
        long placementTime = entity.getPlacementTime();
        if (placementTime <= 0) return 0;
        var level = Minecraft.getInstance().level;
        if (level == null) return 0;
        long elapsed = level.getGameTime() - placementTime;
        long coolingTicks = Config.SMITHING_COOLING_TIME.get() * 20L;
        return Math.min(1.0f, (float) elapsed / coolingTicks);
    }

    private float[] getTemperatureColor(float cooling) {
        if (cooling < 0.25f) {
            // White-yellow glow (fresh)
            float t = cooling / 0.25f;
            return new float[]{1.0f, 1.0f - t * 0.15f, 1.0f - t * 0.5f};
        } else if (cooling < 0.5f) {
            // Yellow to orange
            float t = (cooling - 0.25f) / 0.25f;
            return new float[]{1.0f, 0.85f - t * 0.35f, 0.5f - t * 0.3f};
        } else if (cooling < 0.75f) {
            // Orange to dark red
            float t = (cooling - 0.5f) / 0.25f;
            return new float[]{1.0f - t * 0.3f, 0.5f - t * 0.3f, 0.2f - t * 0.1f};
        } else {
            // Dark red to grey
            float t = (cooling - 0.75f) / 0.25f;
            return new float[]{0.7f - t * 0.25f, 0.2f + t * 0.15f, 0.1f + t * 0.25f};
        }
    }

    private void renderHotspot(HotIronAnvilEntity entity, PoseStack poseStack,
                                MultiBufferSource buffer) {
        poseStack.pushPose();

        float hotspotX = entity.getEffectiveHotspotX();
        float hotspotZ = entity.getEffectiveHotspotZ();
        float hotspotSize = entity.getEffectiveHotspotSize();
        float pulse = entity.getCurrentPulse();
        float yOffset = 0.14f;

        poseStack.translate(hotspotX, yOffset, hotspotZ);

        // 5 concentric rings that pulse with timing
        for (int ring = 0; ring < 5; ring++) {
            float ringFraction = (ring + 1) / 5.0f;
            float radius = hotspotSize * ringFraction;
            // Inner rings brighter, all pulse with timing
            float baseAlpha = (1.0f - ringFraction * 0.6f);
            float alpha = baseAlpha * (0.2f + pulse * 0.8f);
            // Orange-yellow glow
            renderCircle(poseStack, buffer, radius, 1.0f, 0.7f, 0.2f, alpha);
        }

        // Bright center dot when pulse is high
        if (pulse > 0.6f) {
            float centerAlpha = (pulse - 0.6f) / 0.4f * 0.9f;
            renderCircle(poseStack, buffer, hotspotSize * 0.08f, 1.0f, 1.0f, 0.6f, centerAlpha);
        }

        poseStack.popPose();
    }

    private void renderProgressDiamonds(HotIronAnvilEntity entity, PoseStack poseStack,
                                         MultiBufferSource buffer) {
        poseStack.pushPose();

        int strikes = entity.getHammerStrikes();
        float yOffset = 0.16f;

        // 4 diamonds arranged in a row along one side of the anvil
        for (int i = 0; i < 4; i++) {
            float x = 0.25f + i * 0.17f;
            float z = 0.08f;
            boolean completed = i < strikes;

            float r = completed ? 1.0f : 0.4f;
            float g = completed ? 0.75f : 0.4f;
            float b = completed ? 0.15f : 0.4f;
            float alpha = completed ? 0.9f : 0.25f;

            renderDiamond(poseStack, buffer, x, yOffset, z, 0.025f, r, g, b, alpha);
        }

        poseStack.popPose();
    }

    private void renderReadyIndicator(PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        poseStack.pushPose();

        var level = Minecraft.getInstance().level;
        float time = level != null ? (level.getGameTime() + partialTick) / 20.0f : 0;
        float pulse = (float) (Math.sin(time * 1.5) + 1.0) / 2.0f; // Slow gentle pulse

        float yOffset = 0.16f;
        poseStack.translate(0.5f, yOffset, 0.5f);

        // 3 concentric pulsing circles
        for (int ring = 0; ring < 3; ring++) {
            float radius = 0.06f + ring * 0.04f;
            float alpha = (0.5f + pulse * 0.5f) * (1.0f - ring * 0.25f);
            renderCircle(poseStack, buffer, radius, 0.4f, 1.0f, 0.5f, alpha);
        }

        // Slowly rotating diamond in center
        float rotation = time * 0.8f;
        float diamondSize = 0.025f + pulse * 0.008f;
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);

        VertexConsumer vc = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        float[] dx = {0, diamondSize, 0, -diamondSize};
        float[] dz = {-diamondSize, 0, diamondSize, 0};
        float alpha = 0.6f + pulse * 0.4f;
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            float rx1 = dx[i] * cos - dz[i] * sin;
            float rz1 = dx[i] * sin + dz[i] * cos;
            float rx2 = dx[next] * cos - dz[next] * sin;
            float rz2 = dx[next] * sin + dz[next] * cos;
            vc.addVertex(matrix, rx1, 0, rz1).setColor(0.4f, 1.0f, 0.5f, alpha).setNormal(0, 1, 0);
            vc.addVertex(matrix, rx2, 0, rz2).setColor(0.4f, 1.0f, 0.5f, alpha).setNormal(0, 1, 0);
        }

        poseStack.popPose();
    }

    private void renderCircle(PoseStack poseStack, MultiBufferSource buffer, float radius,
                              float r, float g, float b, float alpha) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        int segments = 48;
        float angleStep = (float) (2 * Math.PI / segments);

        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = ((i + 1) % segments) * angleStep;

            float x1 = (float) Math.cos(angle1) * radius;
            float z1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float z2 = (float) Math.sin(angle2) * radius;

            vertexConsumer.addVertex(matrix, x1, 0, z1)
                .setColor(r, g, b, alpha)
                .setNormal(0, 1, 0);
            vertexConsumer.addVertex(matrix, x2, 0, z2)
                .setColor(r, g, b, alpha)
                .setNormal(0, 1, 0);
        }
    }

    private void renderDiamond(PoseStack poseStack, MultiBufferSource buffer,
                               float x, float y, float z, float size,
                               float r, float g, float b, float alpha) {
        VertexConsumer vc = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        // Diamond: top, right, bottom, left
        float[] dx = {0, size, 0, -size};
        float[] dz = {-size, 0, size, 0};

        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            vc.addVertex(matrix, x + dx[i], y, z + dz[i])
                .setColor(r, g, b, alpha).setNormal(0, 1, 0);
            vc.addVertex(matrix, x + dx[next], y, z + dz[next])
                .setColor(r, g, b, alpha).setNormal(0, 1, 0);
        }
    }
}
