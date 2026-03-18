package com.THproject.tharidia_things.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.ColorUtil;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = WearableItemLayer.class, remap = false)
public abstract class EpicFightArmorLayerMixin<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends HumanoidModel<E>, AM extends HumanoidMesh> {

    // --- SHADOW METHODS (Accessing Epic Fight's private internals) ---

    @Shadow
    private SkinnedMesh getArmorModel(HumanoidArmorLayer<E, M, M> originalRenderer, M originalModel,
            Model forgeHooksArmorModel, E entityliving, ArmorItem armorItem, ItemStack itemstack, EquipmentSlot slot) {
        return null;
    }

    @Shadow
    private void renderArmor(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight,
            SkinnedMesh model, Armature armature, float r, float g, float b, ResourceLocation armorTexture,
            OpenMatrix4f[] poses) {
    }

    @Shadow
    private void renderTrim(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight,
            SkinnedMesh model, Armature armature, Holder<ArmorMaterial> armorMaterial, ArmorTrim armorTrim,
            EquipmentSlot slot, OpenMatrix4f[] poses) {
    }

    @Shadow
    private void renderGlint(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight,
            SkinnedMesh model, Armature armature, OpenMatrix4f[] poses) {
    }

    // --- OUR INJECTION ---

   @Inject(method = "renderLayer", at = @At("TAIL"))
    private void tharidia_renderCustomSlot(T entitypatch, E livingentity, HumanoidArmorLayer<E, M, M> vanillaLayer, PoseStack poseStack, MultiBufferSource buffers, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks, CallbackInfo ci) {
        // --- FIRST PERSON CHECK ---
        // If Epic Fight tells us the camera is in first-person mode, cancel our custom render entirely.
        if (entitypatch.isFirstPerson()) {
            return;
        }
        
        // --- STEP 1: GET YOUR CUSTOM DATA ---
        // Fetch the attachment directly from the entity
        com.THproject.tharidia_things.compoundTag.CustomArmorAttachments customData = livingentity.getData(com.THproject.tharidia_things.compoundTag.CustomArmorAttachments.CUSTOM_ARMOR_DATA);

        // Loop through all 4 of your custom slots
        for (int i = 0; i < customData.getContainerSize(); i++) {
            ItemStack customArmorStack = customData.getItem(i);

            // If the slot is empty or not an armor item, skip to the next slot
            if (customArmorStack.isEmpty() || !(customArmorStack.getItem() instanceof ArmorItem armorItem)) {
                continue;
            }

            // We tell Epic Fight to treat this like whatever slot the armor piece is meant for (e.g., Chestplate).
            EquipmentSlot fakeSlot = armorItem.getEquipmentSlot();

            // --- VANILLA ARMOR CHECK ---
            // Check if the player is already wearing a vanilla item in this specific slot
            ItemStack vanillaStack = livingentity.getItemBySlot(fakeSlot);
            if (!vanillaStack.isEmpty()) {
                continue; // Skip rendering this custom armor piece because vanilla armor is covering it!
            }

            // VERY IMPORTANT: Push the pose stack INSIDE the loop so each item renders cleanly
            poseStack.pushPose();

            // 1. Fetch Models & Meshes
            // (Note: We use the Accessor here as discussed previously)
            @SuppressWarnings("unchecked")
            M vanillaModel = ((HumanoidArmorLayerAccessor<M>) vanillaLayer).invokeGetArmorModel(fakeSlot);
            Model armorModel = ClientHooks.getArmorModel(livingentity, customArmorStack, fakeSlot, vanillaModel);
            SkinnedMesh armorMesh = this.getArmorModel(vanillaLayer, vanillaModel, armorModel, livingentity, armorItem, customArmorStack, fakeSlot);

            if (armorMesh != null) {
                // 2. Setup Vanilla Model Animations (Required for Epic Fight's Baker)
                if (armorModel instanceof HumanoidModel humanoidModel) {
                    boolean shouldSit = livingentity.isPassenger() && (livingentity.getVehicle() != null && livingentity.getVehicle().shouldRiderSit());
                    float f8 = 0.0F;
                    float f5 = 0.0F;
                    
                    if (!shouldSit && livingentity.isAlive()) {
                        f8 = livingentity.walkAnimation.speed(partialTicks);
                        f5 = livingentity.walkAnimation.position(partialTicks);
                        if (livingentity.isBaby()) f5 *= 3.0F;
                        if (f8 > 1.0F) f8 = 1.0F;
                    }
                    
                    try {
                        humanoidModel.setupAnim(livingentity, f8, f5, bob, yRot, xRot);
                    } catch (ClassCastException ignored) {}
                    
                    humanoidModel.head.loadPose(humanoidModel.head.getInitialPose());
                    humanoidModel.hat.loadPose(humanoidModel.hat.getInitialPose());
                    humanoidModel.body.loadPose(humanoidModel.body.getInitialPose());
                    humanoidModel.leftArm.loadPose(humanoidModel.leftArm.getInitialPose());
                    humanoidModel.rightArm.loadPose(humanoidModel.rightArm.getInitialPose());
                    humanoidModel.leftLeg.loadPose(humanoidModel.leftLeg.getInitialPose());
                    humanoidModel.rightLeg.loadPose(humanoidModel.rightLeg.getInitialPose());
                }

                armorMesh.initialize();

                // 3. Render Armor Textures and Tints
                ArmorMaterial armormaterial = armorItem.getMaterial().value();
                IClientItemExtensions extensions = IClientItemExtensions.of(customArmorStack);
                int fallbackColor = extensions.getDefaultDyeColor(customArmorStack);
                boolean innerModel = (fakeSlot == EquipmentSlot.LEGS);

                for (int layerIdx = 0; layerIdx < armormaterial.layers().size(); layerIdx++) {
                    ArmorMaterial.Layer armormaterialLayer = armormaterial.layers().get(layerIdx);
                    int packedColor = extensions.getArmorLayerTintColor(customArmorStack, livingentity, armormaterialLayer, layerIdx, fallbackColor);

                    if (packedColor != 0) {
                        Vector4f color = ColorUtil.unpackToARGBF(packedColor);
                        ResourceLocation texture = ParseUtil.tryGetOr(() -> armorMesh.getRenderProperties().customTexturePath(), () -> ClientHooks.getArmorTexture(livingentity, customArmorStack, armormaterialLayer, innerModel, fakeSlot));
                        this.renderArmor(poseStack, buffers, packedLight, armorMesh, entitypatch.getArmature(), color.x(), color.y(), color.z(), texture, poses);
                    }
                }

                // 4. Render Trims
                ArmorTrim armorTrim = customArmorStack.get(net.minecraft.core.component.DataComponents.TRIM);
                if (armorTrim != null) {
                    this.renderTrim(poseStack, buffers, packedLight, armorMesh, entitypatch.getArmature(), armorItem.getMaterial(), armorTrim, fakeSlot, poses);
                }

                // 5. Render Enchantment Glint
                if (customArmorStack.hasFoil()) {
                    this.renderGlint(poseStack, buffers, packedLight, armorMesh, entitypatch.getArmature(), poses);
                }
            }

            // VERY IMPORTANT: Pop the pose stack before moving to the next item
            poseStack.popPose();
        }
    }
}