package com.THproject.tharidia_things.item.alchemist_potion;

import java.util.stream.StreamSupport;
import java.util.List;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;

public class AlchemistPotions {
    public static final long POTION_DECAY_TIME = 1000L * 20L;
    public static final long DECAY_TOLERANCE = 1000L * 10L;

    public static final DeferredItem<Item> BALL_POTION = TharidiaThings.ITEMS.register("ball_potion",
            () -> new Item(new Item.Properties()) {
                @Override
                public int getUseDuration(ItemStack stack, LivingEntity entity) {
                    return 10; // faster than normal (vanilla = 32, previous = 20)
                };

                @Override
                public UseAnim getUseAnimation(ItemStack stack) {
                    return UseAnim.BOW;
                }

                @Override
                public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
                    PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
                    long time = stack.getOrDefault(PotionComponents.CRAFTED_TIME.get(), -1L);
                    if (time > -1L && (System.currentTimeMillis() - time) < POTION_DECAY_TIME
                            && potionContents != null) {
                        potionContents.getAllEffects().forEach(effect -> {
                            livingEntity.addEffect(effect);
                        });
                    }
                    if (livingEntity instanceof Player player && player.isCreative())
                        return stack; // don't consume potion for creative players
                    return ItemStack.EMPTY;
                }

                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(player.getItemInHand(hand));
                }

                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                        TooltipFlag flag) {
                    setPotionTooltip(stack, tooltip);
                }
            });

    // Splash potion — thrown, applies effects in AoE on impact
    public static final DeferredItem<Item> TRIANG_POTION = TharidiaThings.ITEMS.register(
            "triangle_potion", () -> new Item(new Item.Properties()) {
                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    ItemStack stack = player.getItemInHand(hand);
                    if (!level.isClientSide()) {
                        // Copy contents into a splash potion stack so ThrownPotion uses splash
                        // behaviour
                        ItemStack thrownStack = new ItemStack(Items.SPLASH_POTION);
                        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
                        long time = stack.getOrDefault(PotionComponents.CRAFTED_TIME.get(), -1L);
                        if (time > -1L && (System.currentTimeMillis() - time) < POTION_DECAY_TIME
                                && contents != null) {
                            contents.getAllEffects().forEach(effect -> {
                                thrownStack.set(DataComponents.POTION_CONTENTS, contents);
                            });
                        }

                        ThrownPotion entity = new ThrownPotion(level, player);
                        entity.setItem(thrownStack);
                        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
                        level.addFreshEntity(entity);
                    }
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }

                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                        TooltipFlag flag) {
                    setPotionTooltip(stack, tooltip);
                }
            });

    // Normal drink potion — vanilla speed (32 ticks)
    public static final DeferredItem<Item> DROP_POTION = TharidiaThings.ITEMS.register(
            "drop_potion", () -> new Item(new Item.Properties()) {
                @Override
                public int getUseDuration(ItemStack stack, LivingEntity entity) {
                    return 32; // normal vanilla drink speed
                };

                @Override
                public UseAnim getUseAnimation(ItemStack stack) {
                    return UseAnim.DRINK;
                }

                @Override
                public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
                    PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
                    long time = stack.getOrDefault(PotionComponents.CRAFTED_TIME.get(), -1L);
                    if (time > -1L && (System.currentTimeMillis() - time) < POTION_DECAY_TIME
                            && potionContents != null) {
                        potionContents.getAllEffects().forEach(effect -> {
                            livingEntity.addEffect(effect);
                        });
                    }
                    if(livingEntity instanceof Player player && player.isCreative())
                        return stack; // don't consume potion for creative players
                    return ItemStack.EMPTY;
                }

                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(player.getItemInHand(hand));
                }

                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                        TooltipFlag flag) {
                    setPotionTooltip(stack, tooltip);
                }
            });

    // Lingering (haul) potion — thrown, leaves an effect cloud on impact
    public static final DeferredItem<Item> FANTASY_POTION = TharidiaThings.ITEMS.register(
            "fantasy_potion", () -> new Item(new Item.Properties()) {
                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    ItemStack stack = player.getItemInHand(hand);
                    if (!level.isClientSide()) {
                        // Copy contents into a lingering potion stack so ThrownPotion creates an
                        // AreaEffectCloud
                        ItemStack thrownStack = new ItemStack(Items.LINGERING_POTION);
                        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
                        long time = stack.getOrDefault(PotionComponents.CRAFTED_TIME.get(), -1L);
                        if (time > -1L && (System.currentTimeMillis() - time) < POTION_DECAY_TIME
                                && contents != null) {
                            contents.getAllEffects().forEach(effect -> {
                                thrownStack.set(DataComponents.POTION_CONTENTS, contents);
                            });
                        }

                        ThrownPotion entity = new ThrownPotion(level, player);
                        entity.setItem(thrownStack);
                        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
                        level.addFreshEntity(entity);
                    }
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }

                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                        TooltipFlag flag) {
                    setPotionTooltip(stack, tooltip);
                }
            });

    private static void setPotionTooltip(ItemStack stack, List<Component> tooltip) {
        // Display Potion Decay
        long craftedTime = stack.getOrDefault(PotionComponents.CRAFTED_TIME.get(), -1L);
        if(craftedTime > -1L){
            long passedTime = System.currentTimeMillis() - craftedTime;
            if (passedTime > POTION_DECAY_TIME){
                tooltip.add(Component.translatable("tharidiathings.potion_expired").withColor(0xFF0000));
            } else {
                // Calculate the percentage of time remaining (0.0 to 1.0)
                double remainingPercent = 1.0 - ((double) passedTime / POTION_DECAY_TIME);
                MutableComponent statusReq;

                if (remainingPercent > 0.75) {
                    // 100% to 75%
                    statusReq = Component.translatable("tooltip.tharidiathings.potion_decay_1");
                } else if (remainingPercent > 0.50) {
                    // 75% to 50%
                    statusReq = Component.translatable("tooltip.tharidiathings.potion_decay_2");
                } else if (remainingPercent > 0.25) {
                    // 50% to 25%
                    statusReq = Component.translatable("tooltip.tharidiathings.potion_decay_3");
                } else {
                    // 25% to 0%
                    statusReq = Component.translatable("tooltip.tharidiathings.potion_decay_4");
                }

                tooltip.add(statusReq);
            }
        }
        
        // Display Potion Content
        PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
        if (potionContents != null) {
            List<Holder<MobEffect>> effects = StreamSupport
                    .stream(potionContents.getAllEffects().spliterator(), false)
                    .map(effectInstance -> effectInstance.getEffect())
                    .toList();
            if (!effects.isEmpty()) {
                tooltip.add(Component.literal("Potions contained:"));
                potionContents.getAllEffects().forEach(effect -> {
                    MutableComponent text = Component.empty();
                    text.append(Component
                            .literal("  " + effect.getEffect().value().getDisplayName().getString()));

                    int duration = effect.getDuration();
                    if (duration > 1) {
                        String effectTime = String.format("%d:%02d", (duration / 20) / 60, (duration / 20) % 60);
                        text.append(Component.literal(" - " + effectTime));
                    }
                    text.withColor(effect.getEffect().value().getColor());
                    tooltip.add(text);
                });
            }
        }
    }

    public static List<ItemStack> getAllPotions() {
        return List.of(
                new ItemStack(BALL_POTION.get()),
                new ItemStack(TRIANG_POTION.get()),
                new ItemStack(DROP_POTION.get()),
                new ItemStack(FANTASY_POTION.get()));
    }

    public static boolean isPotion(ItemStack stack) {
        for (ItemStack potion : getAllPotions()) {
            if (potion.getItem() == stack.getItem())
                return true;
        }
        return false;
    }

    /**
     * Called from TharidiaThings to trigger static initialization of all
     * DeferredItem fields.
     */
    public static void register() {
    }
}
