package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.tank.TankBlockEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public class TankRenderer extends GeoBlockRenderer<TankBlockEntity> {
    public static final ResourceLocation WATER_MODEL = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
            "geo/tank_water.geo.json");

    public TankRenderer() {
        super(new GeoModel<TankBlockEntity>() {
            @Override
            public ResourceLocation getModelResource(TankBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/tank.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(TankBlockEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/tank.png");
            }

            @Override
            public ResourceLocation getAnimationResource(TankBlockEntity animatable) {
                return null;
            }
        });
        addRenderLayer(new TankFluidLayer(this));
    }

    @Override
    public void render(TankBlockEntity animatable, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        if (animatable.tank.getFluidAmount() > 0) {
            renderFluid(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private void renderFluid(TankBlockEntity animatable, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FluidStack fluidStack = animatable.tank.getFluid();
        if (fluidStack.isEmpty())
            return;

        IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(props.getStillTexture(fluidStack));

        int color = props.getTintColor(fluidStack);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        poseStack.pushPose();

        float xMin = 2.6f / 16.0f;
        float xMax = 13.4f / 16.0f;
        float zMin = 3.2f / 16.0f;
        float zMax = 13.0f / 16.0f;

        // 3. Altezza
        float yFloor = 26.05f / 16.0f;
        float fillRatio = (float) animatable.tank.getFluidAmount() / (float) animatable.tank.getCapacity();
        float yMax = yFloor + (21.0f / 16.0f * fillRatio);

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer builder = bufferSource.getBuffer(RenderType.translucent());

        // 4. Disegna la faccia superiore
        addVertex(builder, matrix, xMin, yMax, zMin, r, g, b, a, sprite.getU0(), sprite.getV0(), packedLight,
                packedOverlay);
        addVertex(builder, matrix, xMin, yMax, zMax, r, g, b, a, sprite.getU0(), sprite.getV1(), packedLight,
                packedOverlay);
        addVertex(builder, matrix, xMax, yMax, zMax, r, g, b, a, sprite.getU1(), sprite.getV1(), packedLight,
                packedOverlay);
        addVertex(builder, matrix, xMax, yMax, zMin, r, g, b, a, sprite.getU1(), sprite.getV0(), packedLight,
                packedOverlay);

        poseStack.popPose();
    }

    private void addVertex(VertexConsumer builder, Matrix4f matrix, float x, float y, float z, float r, float g,
            float b, float a, float u, float v, int packedLight, int packedOverlay) {
        builder.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(0, 1, 0); // Simplified normal
    }

    @Override
    public AABB getRenderBoundingBox(TankBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 3, pos.getY(), pos.getZ() - 3,
                pos.getX() + 5, pos.getY() + 3, pos.getZ() + 5);
    }
}
