package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.entity.TrebuchetEntity;
import com.THproject.tharidia_things.entity.projectile.TrebuchetProjectileEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Forces camera adjustments and renders aiming helpers when riding the Trebuchet.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public final class TrebuchetClientHandler {

    private static CameraType previousCameraType;
    private static boolean cameraForced;
    private static TrebuchetEntity activeTrebuchet;

    private TrebuchetClientHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != player) {
            return;
        }

        if (player.getVehicle() instanceof TrebuchetEntity trebuchet) {
            activeTrebuchet = trebuchet;
            if (!cameraForced) {
                previousCameraType = minecraft.options.getCameraType();
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                cameraForced = true;
            }
        } else if (cameraForced) {
            resetCamera();
        } else {
            activeTrebuchet = null;
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (activeTrebuchet == null || activeTrebuchet.isRemoved()) {
            activeTrebuchet = null;
            return;
        }

        if (activeTrebuchet.getState() != TrebuchetEntity.TrebuchetState.LOADED) {
            return;
        }

        List<Vec3> points = buildTrajectoryPoints(minecraft.level, activeTrebuchet);
        if (points.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        poseStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0f);

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        int total = points.size();
        for (int i = 0; i < total; i++) {
            Vec3 point = points.get(i);
            float progress = total > 1 ? (float) i / (total - 1) : 0.0f;
            float alpha = 0.9f - (progress * 0.5f);
            float red = 0.95f;
            float green = 0.55f + progress * 0.2f;
            float blue = 0.2f + progress * 0.3f;

            float x = (float) (point.x - cameraPos.x);
            float y = (float) (point.y - cameraPos.y);
            float z = (float) (point.z - cameraPos.z);

            builder.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private static List<Vec3> buildTrajectoryPoints(Level level, TrebuchetEntity trebuchet) {
        List<Vec3> samples = new ArrayList<>();
        if (level == null) {
            return samples;
        }

        Vec3 start = trebuchet.position().add(0.0, 1.25, 0.0);
        Vec3 horizontal = Vec3.directionFromRotation(0.0f, trebuchet.getYRot());
        Vec3 initialDirection = new Vec3(horizontal.x, 0.25f, horizontal.z).normalize();
        Vec3 velocity = initialDirection.scale(1.6f);

        double step = 0.25d;
        Vec3 position = start;
        for (int i = 0; i < 64; i++) {
            samples.add(position);

            Vec3 gravity = new Vec3(0.0, -TrebuchetProjectileEntity.GRAVITY * step, 0.0);
            velocity = velocity.add(gravity);
            position = position.add(velocity.scale(step));

            if (position.y < level.getMinBuildHeight()) {
                break;
            }
        }

        return samples;
    }

    public static void resetCamera() {
        Minecraft minecraft = Minecraft.getInstance();
        if (cameraForced && previousCameraType != null) {
            minecraft.options.setCameraType(previousCameraType);
        }
        cameraForced = false;
        previousCameraType = null;
        activeTrebuchet = null;
    }
}
