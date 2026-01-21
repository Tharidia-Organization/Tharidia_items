package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.houseboundry.AnimalWellnessAttachments;
import com.THproject.tharidia_things.houseboundry.AnimalWellnessData;
import com.THproject.tharidia_things.houseboundry.config.AnimalConfigRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Brush item for improving animal wellness.
 * +12 comfort, -3 stress, 60 second cooldown per animal.
 */
public class AnimalBrushItem extends Item {

    private static final int COMFORT_BONUS = 12;
    private static final int STRESS_REDUCTION = 3;
    private static final long COOLDOWN_MS = 60000L; // 60 seconds

    public AnimalBrushItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.PASS;
        }

        // Check if entity is an animal or has wellness config
        boolean isAnimal = entity instanceof Animal;
        boolean hasConfig = AnimalConfigRegistry.hasConfig(entity.getType());

        if (!isAnimal && !hasConfig) {
            return InteractionResult.PASS;
        }

        // Get wellness data
        AnimalWellnessData data = entity.getData(AnimalWellnessAttachments.WELLNESS_DATA);

        // Check cooldown
        if (data.isBrushOnCooldown(COOLDOWN_MS)) {
            return InteractionResult.FAIL;
        }

        // Apply brush effects
        data.addComfort(COMFORT_BONUS);
        data.addStress(-STRESS_REDUCTION);
        data.setLastBrushTimestamp(System.currentTimeMillis());

        // Effects
        player.level().playSound(null, entity.blockPosition(),
            SoundEvents.HORSE_ARMOR, SoundSource.NEUTRAL, 1.0F, 1.2F);

        // Damage brush
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));

        return InteractionResult.SUCCESS;
    }
}
