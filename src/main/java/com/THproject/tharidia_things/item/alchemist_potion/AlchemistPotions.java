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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredItem;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class AlchemistPotions {
    public static final DeferredItem<Item> BALL_POTION = TharidiaThings.ITEMS.register(
            "ball_potion", () -> new Item(new Item.Properties()) {
                @Override
                public int getUseDuration(ItemStack stack, LivingEntity entity) {
                    return 20;
                };

                @Override
                public UseAnim getUseAnimation(ItemStack stack) {
                    return UseAnim.DRINK;
                }

                @Override
                public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
                    PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
                    if (potionContents != null) {
                        potionContents.getAllEffects().forEach(effect -> {
                            livingEntity.addEffect(effect);
                        });
                    }
                    return stack;
                }

                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(player.getItemInHand(hand));
                }

                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                        TooltipFlag flag) {
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
                                text.append(Component.literal("  " + effect.getEffect().value().getDisplayName().getString()));

                                int duration = effect.getDuration();
                                if (duration > 1) {
                                    String time = String.format("%d:%02d", (duration / 20) / 60, (duration / 20) % 60);
                                    text.append(Component.literal(" - " + time));
                                }
                                text.withColor(effect.getEffect().value().getColor());
                                tooltip.add(text);
                            });
                        }
                    }
                }
            });

    public static void register() {
    }

    public static List<ItemStack> getAllPotions() {
        return List.of(
                new ItemStack(BALL_POTION.get()));
    }

    @SubscribeEvent
    public static void asd(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().level().isClientSide)
            return;

        ItemStack stack = new ItemStack(BALL_POTION.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFF0000, false));
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Potions.REGENERATION).withEffectAdded(Potions.HEALING.value().getEffects().get(0)));

        event.getEntity().getInventory().add(stack);
    }
}
