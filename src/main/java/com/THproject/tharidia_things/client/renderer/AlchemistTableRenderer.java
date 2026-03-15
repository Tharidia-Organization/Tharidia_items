package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.block.alchemist.AlchemistTableBlock;
import com.THproject.tharidia_things.block.alchemist.AlchemistTableBlockEntity;
import com.THproject.tharidia_things.client.model.AlchemistTableModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * GeckoLib renderer for the Alchemist Table multiblock.
 *
 * The model faces SOUTH in Blockbench, but GeckoLib's default
 * rotation assumes models face NORTH. We override render to compensate.
 */
public class AlchemistTableRenderer extends GeoBlockRenderer<AlchemistTableBlockEntity> {

    public AlchemistTableRenderer(BlockEntityRendererProvider.Context context) {
        super(new AlchemistTableModel());
    }

    @Override
    public void render(AlchemistTableBlockEntity animatable, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        renderMagicCircle(animatable, partialTick, poseStack, bufferSource);
    }

    private void renderMagicCircle(AlchemistTableBlockEntity entity, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer) {
        var level = entity.getLevel();
        if (level == null)
            return;

        // BlockPos masterPos = entity.getBlockPos();
        // Direction facing =
        // entity.getBlockState().getValue(AlchemistTableBlock.FACING);

        // // Dummy block at index 6 is the end of the Z-column
        // BlockPos dummyPos = AlchemistTableBlock.getDummyPos(masterPos, 6, facing);

        // // Calculate relative offset from master TE to dummy block
        // float relX = dummyPos.getX() - masterPos.getX();
        // float relZ = dummyPos.getZ() - masterPos.getZ();

        // poseStack.pushPose();
        // // Translate to the center of the dummy block 6, slightly above surface
        // poseStack.translate(relX + 0.5f, 1.05f, relZ + 0.5f);

        // long time = level.getGameTime();
        // float rotation = (time + partialTick) * 3.0f; // Rotation speed

        // // Rotate around Y axis
        // poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.pushPose();
        BlockPos masterPos = entity.getBlockPos();
        Direction facing = entity.getBlockState().getValue(AlchemistTableBlock.FACING);

        // Dummy block at index 6 is the end of the Z-column
        BlockPos dummyPos = AlchemistTableBlock.getDummyPos(masterPos, 6, facing);

        // Calculate relative offset from master TE to dummy block
        float relX = dummyPos.getX() - masterPos.getX();
        float relZ = dummyPos.getZ() - masterPos.getZ();

        poseStack.translate(relX + 0.5, 0, relZ + 0.5);

        float[] pos = entity.getCauldronHotspot(partialTick);
        float posX = pos[0];
        float posY = pos[1];
        float posZ = pos[2];
        float radius = pos[3];

        poseStack.translate(posX, posY, posZ);

        // Pulsing alpha
        // float pulse = (float) Math.sin((time + partialTick) * 0.15f) * 0.5f + 0.5f;
        // float alpha = 0.5f + pulse * 0.5f;

        // Render outer circle
        renderCircle(poseStack, buffer, radius, 1.0f, 0.6f, 0.2f, 0.7f);

        // Render diamond to visualize rotation
        renderDiamond(poseStack, buffer, 0, 0, 0, 0.05f, 1.0f, 0.8f, 0.4f, 0.7f);

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
        float[] dx = { 0, size, 0, -size };
        float[] dz = { -size, 0, size, 0 };

        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            vc.addVertex(matrix, x + dx[i], y, z + dz[i])
                    .setColor(r, g, b, alpha).setNormal(0, 1, 0);
            vc.addVertex(matrix, x + dx[next], y, z + dz[next])
                    .setColor(r, g, b, alpha).setNormal(0, 1, 0);
        }
    }

    /**
     * Called by GeckoLib for each bone before it is rendered.
     * We intercept the "Mestolone" bone to force its Y rotation to match
     * the stored craftingAngle — guaranteeing it stays in sync with the hotspot.
     */
    @Override
    public void renderRecursively(PoseStack poseStack, AlchemistTableBlockEntity animatable,
            GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
            VertexConsumer buffer, boolean isReRender, float partialTick,
            int packedLight, int packedOverlay, int colour) {

        if (bone.getName().equals("Mestolone")) {
            float angleDeg = animatable.getInterpolatedCraftingAngle(partialTick);
            // Negative to match the animation's original -360° direction.
            // If the ladle rotates the wrong way in-game, remove the minus sign.
            bone.setRotY((float) Math.toRadians(-angleDeg));
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource,
                buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public RenderType getRenderType(AlchemistTableBlockEntity animatable, ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public boolean shouldRenderOffScreen(AlchemistTableBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(AlchemistTableBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        // Generous bounding box for the L-shape multiblock
        return new AABB(
                pos.getX() - 5, pos.getY(), pos.getZ() - 5,
                pos.getX() + 8, pos.getY() + 3, pos.getZ() + 8);
    }
}
