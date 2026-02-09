package com.THproject.tharidia_things.client.renderer;

import com.THproject.tharidia_things.compoundTag.CustomArmorAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;

public class CustomArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final HumanoidModel<AbstractClientPlayer> innerModel;
    private final HumanoidModel<AbstractClientPlayer> outerModel;

    public CustomArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer,
            EntityModelSet modelSet) {
        super(renderer);
        this.innerModel = new HumanoidModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        this.outerModel = new HumanoidModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        Container container = player.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());

        // Render each slot
        renderArmorPiece(poseStack, buffer, container.getItem(0), EquipmentSlot.HEAD, packedLight, player);
        renderArmorPiece(poseStack, buffer, container.getItem(1), EquipmentSlot.CHEST, packedLight, player);
        renderArmorPiece(poseStack, buffer, container.getItem(2), EquipmentSlot.LEGS, packedLight, player);
        renderArmorPiece(poseStack, buffer, container.getItem(3), EquipmentSlot.FEET, packedLight, player);
    }

    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource buffer, ItemStack stack, EquipmentSlot slot,
            int packedLight, AbstractClientPlayer player) {
        if (stack.isEmpty())
            return;

        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            return;
        }

        // Do not render if the player has armor in the vanilla slot
        if (!player.getItemBySlot(slot).isEmpty()) {
            return;
        }

        // Determine default model (Inner for Legs, Outer for others)
        HumanoidModel<AbstractClientPlayer> defaultModel = (slot == EquipmentSlot.LEGS) ? this.innerModel
                : this.outerModel;

        // Get the actual model to use (handles custom item models)
        HumanoidModel<AbstractClientPlayer> model = (HumanoidModel<AbstractClientPlayer>) ClientHooks
                .getArmorModel(player, stack, slot, defaultModel);

        // Setup model attributes
        this.getParentModel().copyPropertiesTo(model);
        setPartVisibility(model, slot);

        net.minecraft.world.item.ArmorMaterial material = armorItem.getMaterial().value();
        boolean inner = slot == EquipmentSlot.LEGS;

        // Iterate over layers (fix for Leather rendering and correct material IDs)
        // Using var to avoid import collision issues just in case, though fully
        // qualified name used below
        for (net.minecraft.world.item.ArmorMaterial.Layer layer : material.layers()) {
            int color = 0xFFFFFFFF;
            if (layer.dyeable()) {
                if (stack.has(net.minecraft.core.component.DataComponents.DYED_COLOR)) {
                    int rgb = stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR).rgb();
                    color = (rgb | 0xFF000000); // Ensure alpha is 255
                } else {
                    color = 0xFFA06540; // Default Leather Brown
                }
            }

            // Using the texture(boolean) helper method which should exist in 1.21
            // If this fails, we will need to know the exact field names (id/textureId,
            // suffix)
            ResourceLocation texture = layer.texture(inner);

            VertexConsumer vertexConsumer = net.minecraft.client.renderer.entity.ItemRenderer.getArmorFoilBuffer(buffer,
                    RenderType.armorCutoutNoCull(texture), stack.hasFoil());
            model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, color);
        }
    }

    private void setPartVisibility(HumanoidModel<AbstractClientPlayer> model, EquipmentSlot slot) {
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
                break;
            }
        }
    }
}
