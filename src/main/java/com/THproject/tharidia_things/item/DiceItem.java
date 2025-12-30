package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.entity.DiceEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Throwable dice item that spawns a dice entity with bouncing physics.
 */
public class DiceItem extends Item {

    private static final float THROW_SPEED = 0.3F;
    private static final float THROW_INACCURACY = 0.25F;

    public DiceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
            DiceEntity dice = new DiceEntity(level, player, eyePos);
            dice.setItem(stack.copyWithCount(1));
            
            net.minecraft.world.phys.Vec3 lookVec = player.getViewVector(1.0F);
            double spread = THROW_INACCURACY * 0.0175;
            net.minecraft.world.phys.Vec3 direction = lookVec.add(
                level.getRandom().triangle(0.0, spread),
                level.getRandom().triangle(0.0, spread),
                level.getRandom().triangle(0.0, spread)
            );
            
            dice.shoot(direction, THROW_SPEED);
            dice.randomizeInitialSpin();
            level.addFreshEntity(dice);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6F, 0.8F + level.getRandom().nextFloat() * 0.2F);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
