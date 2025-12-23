package com.THproject.tharidia_things.item;

import com.THproject.tharidia_things.entity.TrebuchetEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Places the Trebuchet entity into the world.
 */
public class TrebuchetItem extends Item {

    public TrebuchetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        Vec3 placePos = Vec3.atBottomCenterOf(context.getClickedPos().above());

        TrebuchetEntity trebuchet = TrebuchetEntity.create(level, placePos, player != null ? player.getYRot() : 0f);
        level.addFreshEntity(trebuchet);

        level.playSound(null, placePos.x, placePos.y, placePos.z, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);

        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.CONSUME;
    }
}
