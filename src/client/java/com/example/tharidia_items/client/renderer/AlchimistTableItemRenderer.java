package com.example.tharidia_items.client.renderer;

import com.example.tharidia_items.client.model.AlchimistTableItemModel;
import com.example.tharidia_items.item.AlchimistTableItem;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class AlchimistTableItemRenderer extends GeoItemRenderer<AlchimistTableItem> {
    public AlchimistTableItemRenderer() {
        super(new AlchimistTableItemModel());
    }

    @Override
    public RenderLayer getRenderType(AlchimistTableItem animatable, Identifier texture, VertexConsumerProvider bufferSource, float partialTick) {
        // Usa un layer opaco (cutout senza cull) per evitare artefatti di trasparenza
        return RenderLayer.getEntityCutoutNoCull(texture);
    }

    @Override
    public void render(ItemStack stack, net.minecraft.client.render.model.json.ModelTransformationMode transformMode, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight, int packedOverlay) {
        // GUI (inventory/containers): adatta il modello alla dimensione dello slot
        if (transformMode == net.minecraft.client.render.model.json.ModelTransformationMode.GUI) {
            poseStack.push();
            // Centra e scala per riempire bene lo slot 16x16 mantenendo margine
            poseStack.translate(0.55f, 0.2f, 0.0f);
            float scale = 0.25f; // dimensione slot (regolabile)
            poseStack.scale(scale, scale, scale);
            // Ruota per mostrare il "fronte" del modello in GUI
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(140));
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(345));
            super.render(stack, transformMode, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.pop();
            return;
        }

        // Droppato a terra
        if (transformMode == net.minecraft.client.render.model.json.ModelTransformationMode.GROUND) {
            poseStack.push();
            poseStack.translate(0.5f, 0.0f, 0.5f);
            float scale = 0.35f;
            poseStack.scale(scale, scale, scale);
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((System.currentTimeMillis() / 20L) % 360));
            super.render(stack, transformMode, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.pop();
            return;
        }

        // In mano (prima persona)
        if (transformMode == net.minecraft.client.render.model.json.ModelTransformationMode.FIRST_PERSON_RIGHT_HAND ||
            transformMode == net.minecraft.client.render.model.json.ModelTransformationMode.FIRST_PERSON_LEFT_HAND) {
            poseStack.push();
            // Dimensione più piccola per evitare modello enorme in mano
            float scale = 0.24f;
            poseStack.scale(scale, scale, scale);
            // Centra e orienta un po' il modello
            poseStack.translate(0.5f, 0.45f, 0.5f);
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20f));
            super.render(stack, transformMode, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.pop();
            return;
        }

        // Terza persona
        if (transformMode == net.minecraft.client.render.model.json.ModelTransformationMode.THIRD_PERSON_RIGHT_HAND ||
            transformMode == net.minecraft.client.render.model.json.ModelTransformationMode.THIRD_PERSON_LEFT_HAND) {
            poseStack.push();
            float scale = 0.20f;
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0.5f, 0.4f, 0.5f);
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15f));
            super.render(stack, transformMode, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.pop();
            return;
        }

        // Item frame / FIXED
        if (transformMode == net.minecraft.client.render.model.json.ModelTransformationMode.FIXED) {
            poseStack.push();
            float scale = 0.30f;
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0.5f, 0.5f, 0.5f);
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
            super.render(stack, transformMode, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.pop();
            return;
        }

        // Altri contesti: proteggi comunque le matrici
        poseStack.push();
        super.render(stack, transformMode, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pop();
    }
}