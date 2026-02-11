package com.THproject.tharidia_things.client.renderer;

import java.util.List;

import javax.annotation.Nullable;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.washer.sink.SinkBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SinkItemRenderer extends GeoItemRenderer<SinkBlockItem> {
    private static final float TARGET_SIZE = 0.85f;
    private static final float FILL_FACTOR = 0.95f;
    private static final float MIN_SCALE = 0.2f;
    private static final float MAX_SCALE = 1.4f;

    private boolean boundsComputed;
    private float centerX, centerY, centerZ;
    private float autoScale = 1.0f;

    public SinkItemRenderer() {
        super(new GeoModel<SinkBlockItem>() {

            @Override
            public ResourceLocation getModelResource(SinkBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "geo/sink.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(SinkBlockItem animatable) {
                return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "textures/block/sink.png");
            }

            @Override
            public ResourceLocation getAnimationResource(SinkBlockItem animatable) {
                return null;
            }
        });
    }

    @Override
    public void preRender(PoseStack poseStack, SinkBlockItem animatable, BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
            boolean isReRender, float partialTick, int packedLight,
            int packedOverlay, int colour) {
        for (GeoBone bone : model.topLevelBones()) {
            unhideRecursive(bone);
        }

        if (!boundsComputed) {
            computeBounds(model);
            boundsComputed = true;
        }

        // Store base transform for GeckoLib internal animation tracking
        this.itemRenderTranslations = new Matrix4f(poseStack.last().pose());

        if (!isReRender) {
            // Auto-center: move to slot center, scale to fit, offset model center to origin
            poseStack.translate(0.5f, 0.5f, 0.5f);
            poseStack.scale(autoScale, autoScale, autoScale);
            poseStack.translate(-centerX, -centerY, -centerZ);
        }
    }

    private void computeBounds(BakedGeoModel model) {
        float[] bounds = {
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE
        };

        collectBounds(model.topLevelBones(), bounds);

        if (bounds[0] > bounds[3]) {
            return;
        }

        centerX = (bounds[0] + bounds[3]) / 2f;
        centerY = (bounds[1] + bounds[4]) / 2f;
        centerZ = (bounds[2] + bounds[5]) / 2f;

        float maxDim = Math.max(bounds[3] - bounds[0],
                Math.max(bounds[4] - bounds[1], bounds[5] - bounds[2]));
        if (maxDim > 0) {
            autoScale = clamp((TARGET_SIZE * FILL_FACTOR) / maxDim, MIN_SCALE, MAX_SCALE);
        }
    }

    private void collectBounds(List<GeoBone> bones, float[] bounds) {
        for (GeoBone bone : bones) {
            for (GeoCube cube : bone.getCubes()) {
                for (GeoQuad quad : cube.quads()) {
                    for (GeoVertex vertex : quad.vertices()) {
                        Vector3f pos = vertex.position();
                        bounds[0] = Math.min(bounds[0], pos.x());
                        bounds[1] = Math.min(bounds[1], pos.y());
                        bounds[2] = Math.min(bounds[2], pos.z());
                        bounds[3] = Math.max(bounds[3], pos.x());
                        bounds[4] = Math.max(bounds[4], pos.y());
                        bounds[5] = Math.max(bounds[5], pos.z());
                    }
                }
            }
            collectBounds(bone.getChildBones(), bounds);
        }
    }

    private static void unhideRecursive(GeoBone bone) {
        bone.setHidden(false);
        for (GeoBone child : bone.getChildBones()) {
            unhideRecursive(child);
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
