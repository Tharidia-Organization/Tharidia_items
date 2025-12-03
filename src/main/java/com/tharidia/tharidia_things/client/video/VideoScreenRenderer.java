package com.tharidia.tharidia_things.client.video;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tharidia.tharidia_things.video.VideoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders video screens in the world
 */
public class VideoScreenRenderer {

    /**
     * Render all video screens
     */
    public static void renderScreens(PoseStack poseStack, Vec3 cameraPos) {
        ClientVideoScreenManager manager = ClientVideoScreenManager.getInstance();

        for (var entry : manager.getAllScreens().entrySet()) {
            VideoScreen screen = entry.getValue();
            VLCVideoPlayer player = manager.getPlayer(entry.getKey());

            if (player != null && player.isInitialized()) {
                renderScreen(poseStack, cameraPos, screen, player);
            }
            // Don't log warning - screen might be stopped/paused which is normal
        }
    }

    /**
     * Render a single video screen
     */
    private static void renderScreen(PoseStack poseStack, Vec3 cameraPos, VideoScreen screen, VLCVideoPlayer player) {
        DynamicTexture texture = player.getTexture();
        if (texture == null) {
            com.tharidia.tharidia_things.TharidiaThings.LOGGER.warn("[RENDER] Screen {} has null texture", screen.getId());
            return;
        }

        com.tharidia.tharidia_things.TharidiaThings.LOGGER.debug("[RENDER] Rendering screen {} at {},{}",
                screen.getId(), screen.getCorner1(), screen.getCorner2());

        poseStack.pushPose();

        // Translate to screen position (relative to camera)
        BlockPos corner1 = screen.getCorner1();
        BlockPos corner2 = screen.getCorner2();

        double minX = Math.min(corner1.getX(), corner2.getX()) - cameraPos.x;
        double minY = Math.min(corner1.getY(), corner2.getY()) - cameraPos.y;
        double minZ = Math.min(corner1.getZ(), corner2.getZ()) - cameraPos.z;
        double maxX = Math.max(corner1.getX(), corner2.getX()) + 1 - cameraPos.x;
        double maxY = Math.max(corner1.getY(), corner2.getY()) + 1 - cameraPos.y;
        double maxZ = Math.max(corner1.getZ(), corner2.getZ()) + 1 - cameraPos.z;

        // Set up rendering
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture.getId());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); // GL_LEQUAL
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        // Render based on screen axis
        Direction.Axis axis = screen.getAxis();
        Direction facing = screen.getFacing();

        // Offset to prevent z-fighting with block textures (2 pixels = 0.002 blocks)
        float offset = -0.002f;

        switch (axis) {
            case X -> {
                // Screen on X axis (YZ plane)
                float x = (float) (facing == Direction.EAST ? maxX - offset : minX + offset);
                // Vertex order: bottom-left, bottom-right, top-right, top-left
                renderQuad(bufferBuilder, matrix,
                        x, (float) minY, (float) minZ,  // bottom-left
                        x, (float) minY, (float) maxZ,  // bottom-right
                        x, (float) maxY, (float) maxZ,  // top-right
                        x, (float) maxY, (float) minZ,  // top-left
                        facing == Direction.EAST
                );
            }
            case Y -> {
                // Screen on Y axis (XZ plane)
                float y = (float) (facing == Direction.UP ? maxY - offset : minY + offset);
                // Vertex order: bottom-left, bottom-right, top-right, top-left
                renderQuad(bufferBuilder, matrix,
                        (float) minX, y, (float) minZ,  // bottom-left
                        (float) maxX, y, (float) minZ,  // bottom-right
                        (float) maxX, y, (float) maxZ,  // top-right
                        (float) minX, y, (float) maxZ,  // top-left
                        facing == Direction.UP
                );
            }
            case Z -> {
                // Screen on Z axis (XY plane)
                float z = (float) (facing == Direction.SOUTH ? maxZ - offset : minZ + offset);
                // Vertex order: bottom-left, bottom-right, top-right, top-left
                renderQuad(bufferBuilder, matrix,
                        (float) minX, (float) minY, z,  // bottom-left
                        (float) maxX, (float) minY, z,  // bottom-right
                        (float) maxX, (float) maxY, z,  // top-right
                        (float) minX, (float) maxY, z,  // top-left
                        facing == Direction.SOUTH
                );
            }
        }

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /**
     * Helper method to render a quad with texture coordinates
     */
    private static void renderQuad(BufferBuilder builder, Matrix4f matrix,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2,
                                   float x3, float y3, float z3,
                                   float x4, float y4, float z4,
                                   boolean flipWinding) {
        // UV coordinates: (0,0) = top-left, (1,0) = top-right, (1,1) = bottom-right, (0,1) = bottom-left
        // Vertex order: bottom-left, bottom-right, top-right, top-left (counter-clockwise)
        
        if (flipWinding) {
            // Back-facing: reverse winding order
            builder.addVertex(matrix, x1, y1, z1).setUv(0, 1);  // bottom-left
            builder.addVertex(matrix, x4, y4, z4).setUv(0, 0);  // top-left
            builder.addVertex(matrix, x3, y3, z3).setUv(1, 0);  // top-right
            builder.addVertex(matrix, x2, y2, z2).setUv(1, 1);  // bottom-right
        } else {
            // Front-facing: normal winding order
            builder.addVertex(matrix, x1, y1, z1).setUv(0, 1);  // bottom-left
            builder.addVertex(matrix, x2, y2, z2).setUv(1, 1);  // bottom-right
            builder.addVertex(matrix, x3, y3, z3).setUv(1, 0);  // top-right
            builder.addVertex(matrix, x4, y4, z4).setUv(0, 0);  // top-left
        }
    }
}
