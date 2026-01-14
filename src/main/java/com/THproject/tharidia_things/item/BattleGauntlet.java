package com.THproject.tharidia_things.item;

import com.google.common.base.Predicate;
import com.THproject.tharidia_things.compoundTag.BattleGauntleAttachments;
import com.THproject.tharidia_things.gui.BattleInviteMenu;
import com.THproject.tharidia_things.util.PlayerNameHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BattleGauntlet extends Item {
    public BattleGauntlet(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 10;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
            InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        try {
            var hitTarget = getPlayerLookAt(player, 5);
            if (hitTarget instanceof EntityHitResult entityHit &&
                    entityHit.getEntity() instanceof Player target) {
                BattleGauntleAttachments playerAttachments = player
                        .getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
                BattleGauntleAttachments targetAttachments = target
                        .getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

                if (playerAttachments.getInBattle() || playerAttachments.getLoseTick() > 0) {
                    player.displayClientMessage(
                            Component.translatable("message.tharidiathings.battle.player_in_battle"),
                            false);
                    return InteractionResultHolder.fail(itemstack);
                }
                if (targetAttachments.getInBattle() || targetAttachments.getLoseTick() > 0) {
                    player.displayClientMessage(Component.translatable(
                            "message.tharidiathings.battle.target_in_battle"),
                            false);
                    return InteractionResultHolder.fail(itemstack);
                }

                player.startUsingItem(usedHand);
                return InteractionResultHolder.success(itemstack);
            } else {
                return InteractionResultHolder.fail(itemstack);
            }
        } catch (NullPointerException e) {
            return InteractionResultHolder.fail(itemstack);
        } catch (Exception e) {
            return InteractionResultHolder.fail(itemstack);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!livingEntity.level().isClientSide()) {
            if (livingEntity instanceof Player player) {
                var hitTarget = getPlayerLookAt(player, 5);
                if (hitTarget instanceof EntityHitResult entityHit &&
                        entityHit.getEntity() instanceof Player player_target) {
                    player_target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 2, 0, false, false, false));
                }
            }
        }
        super.onUseTick(level, livingEntity, stack, remainingUseDuration);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!livingEntity.level().isClientSide()) {
            if (livingEntity instanceof Player player) {
                var hitTarget = getPlayerLookAt(player, 5);
                if (hitTarget instanceof EntityHitResult entityHit &&
                        entityHit.getEntity() instanceof Player player_target) {
                    player.displayClientMessage(Component.translatable(
                            "message.tharidiathings.battle.player_invite",
                            PlayerNameHelper.getChosenName((ServerPlayer) player_target)), false);
                    player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), 100);

                    MenuProvider menuProvider = new SimpleMenuProvider(
                            (containerId, playerInventory, target) -> {
                                return new BattleInviteMenu(containerId, playerInventory);
                            },
                            Component.translatable("gui.tharidiathings.battle_invitation.title"));

                    player_target.openMenu(menuProvider, (buffer) -> {
                        buffer.writeUUID(player.getUUID());
                        buffer.writeUtf(PlayerNameHelper.getChosenName((ServerPlayer) player));
                    });
                }
            }
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }

    /**
     * Gets the entity the player is currently looking at.
     *
     * @param player        The player to raycast from.
     * @param reachDistance The maximum distance to check.
     * @param partialTicks  The partial ticks for smooth camera rotation (can pass
     *                      1.0F if not in a render event).
     * @return A HitResult (can be EntityHitResult, or MISS).
     */
    public static HitResult getPlayerLookAt(Player player, double reachDistance) {
        // 1. Get Player's "eyes" and "look" vectors
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getViewVector(1.0F);
        Vec3 endPosition = eyePosition.add(lookVector.scale(reachDistance));

        // 2. Perform a block raycast
        BlockHitResult blockHitResult = player.level().clip(new ClipContext(
                eyePosition,
                endPosition,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player));

        // 3. Perform an entity raycast
        // We adjust the end position to the block hit if it's closer
        double currentReach = reachDistance;
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            currentReach = blockHitResult.getLocation().distanceTo(eyePosition);
            endPosition = blockHitResult.getLocation();
        }

        // Predicate to filter which entities to target (e.g., not the player
        // themselves, can be picked)
        Predicate<Entity> entityFilter = (entity) -> !entity.isSpectator() && entity.isPickable() && !entity.is(player);

        // This is the vanilla method for finding the entity the crosshair targets
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                player,
                eyePosition,
                endPosition,
                player.getBoundingBox().expandTowards(lookVector.scale(reachDistance)).inflate(1.0D, 1.0D, 1.0D),
                entityFilter,
                currentReach * currentReach // Use squared distance
        );

        // 4. Compare results and return the closer hit
        if (entityHitResult != null) {
            // Check if the entity is closer than the block
            double entityDistance = eyePosition.distanceToSqr(entityHitResult.getLocation());
            if (blockHitResult.getType() == HitResult.Type.MISS
                    || entityDistance < eyePosition.distanceToSqr(blockHitResult.getLocation())) {
                return entityHitResult;
            }
        }

        return null;
    }
}
