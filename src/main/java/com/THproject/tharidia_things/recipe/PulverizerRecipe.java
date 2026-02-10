package com.THproject.tharidia_things.recipe;

import java.util.List;
import java.util.Random;

import com.THproject.tharidia_things.TharidiaThings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class PulverizerRecipe implements Recipe<RecipeWrapper> {
    private final Ingredient input;
    private final List<WeightedOutput> outputs;
    private final int processingTime;

    public PulverizerRecipe(Ingredient input, List<WeightedOutput> outputs, int processingTime) {
        this.input = input;
        this.outputs = outputs;
        this.processingTime = processingTime;
    }

    public Ingredient getInput() {
        return input;
    }

    public List<WeightedOutput> getOutputs() {
        return outputs;
    }

    public List<ItemStack> getOutputStacks() {
        return outputs.stream().map(WeightedOutput::item).toList();
    }

    public int getProcessingTime() {
        return processingTime;
    }

    @Override
    public boolean matches(RecipeWrapper pContainer, Level pLevel) {
        // Slot 0 is input
        return this.input.test(pContainer.getItem(0));
    }

    @Override
    public ItemStack assemble(RecipeWrapper pContainer, HolderLookup.Provider pRegistries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).item().copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(Provider registries) {
        if (outputs.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Random random = new Random();
        double totalWeight = 0.0;
        for (WeightedOutput output : outputs) {
            totalWeight += output.chance();
        }

        double randomRange = Math.max(1.0, totalWeight);

        double value = random.nextDouble() * randomRange;

        for (WeightedOutput output : outputs) {
            value -= output.chance();
            if (value <= 0)
                return output.item().copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TharidiaThings.PULVERIZER_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TharidiaThings.PULVERIZER_RECIPE_TYPE.get();
    }

    public static record WeightedOutput(ItemStack item, float chance) {
        public static final Codec<WeightedOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("item").forGetter(WeightedOutput::item),
                Codec.FLOAT.fieldOf("chance").orElse(1.0f).forGetter(WeightedOutput::chance))
                .apply(instance, WeightedOutput::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, WeightedOutput> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, WeightedOutput::item,
                net.minecraft.network.codec.ByteBufCodecs.FLOAT, WeightedOutput::chance,
                WeightedOutput::new);
    }

    public static class Serializer implements RecipeSerializer<PulverizerRecipe> {
        public static final MapCodec<PulverizerRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(PulverizerRecipe::getInput),
                        WeightedOutput.CODEC.listOf().fieldOf("outputs").forGetter(PulverizerRecipe::getOutputs),
                        Codec.INT.fieldOf("processingTime").orElse(40).forGetter(PulverizerRecipe::getProcessingTime))
                        .apply(instance, PulverizerRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, PulverizerRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, PulverizerRecipe::getInput,
                WeightedOutput.STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list()),
                PulverizerRecipe::getOutputs,
                net.minecraft.network.codec.ByteBufCodecs.INT, PulverizerRecipe::getProcessingTime,
                PulverizerRecipe::new);

        @Override
        public MapCodec<PulverizerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PulverizerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
