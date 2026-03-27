package com.THproject.tharidia_things.command;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.THproject.tharidia_things.diet.DietAttachments;
import com.THproject.tharidia_things.diet.DietCategory;
import com.THproject.tharidia_things.diet.DietData;
import com.THproject.tharidia_things.diet.DietProfile;
import com.THproject.tharidia_things.diet.DietProfileCache;
import com.THproject.tharidia_things.diet.DietRegistry;
import com.THproject.tharidia_things.network.DietSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.suggestion.Suggestions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Admin commands for managing player diet
 */
public class DietCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tharidia")
                .then(Commands.literal("diet")
                        .then(Commands.literal("reset")
                                .executes(DietCommands::resetDietSelf)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(4))
                                        .executes(DietCommands::resetDiet)))
                        .then(Commands.literal("check")
                                .requires(source -> source.hasPermission(4))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(DietCommands::checkDiet)))
                        .then(Commands.literal("recalculate")
                                .requires(source -> source.hasPermission(4))
                                .executes(DietCommands::recalculateCache))
                        .then(Commands.literal("version")
                                .requires(source -> source.hasPermission(4))
                                .executes(DietCommands::showVersion))
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(4))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("diet_type", StringArgumentType.string())
                                                .suggests(DietCommands::suggestDiet)
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                                        .executes(DietCommands::setDietValue)))))));
    }

    private static CompletableFuture<Suggestions> suggestDiet(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (DietCategory category : DietCategory.VALUES) {
            builder.suggest(category.name().toLowerCase());
        }
        return builder.buildFuture();
    };
    
    private static int setDietValue(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String dietTypeStr = StringArgumentType.getString(context, "diet_type").toLowerCase();
        int value = IntegerArgumentType.getInteger(context, "value");
        
        Optional<DietCategory> categoryOpt = java.util.Arrays.stream(DietCategory.VALUES)
                .filter(c -> c.name().equalsIgnoreCase(dietTypeStr))
                .findFirst();

        if(categoryOpt.isPresent()){
            DietCategory dietCategory = categoryOpt.get();
            DietData dietData = target.getData(DietAttachments.DIET_DATA.get());
            dietData.set(dietCategory, value, 100);
            PacketDistributor.sendToAllPlayers(new DietSyncPacket(target.getId(), dietData.copyValues()));
            context.getSource().sendSuccess(() -> Component.literal(String.format("Set %s's %s to %d", target.getName().getString(), dietCategory, value)), true);
        }else{
            context.getSource().sendFailure(Component.literal("Invalid diet type: " + dietTypeStr));
        }
        return 1;
    }
    
    private static int resetDietSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = context.getSource().getPlayerOrException();
        return performDietReset(context.getSource(), target);
    }

    private static int resetDiet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        return performDietReset(context.getSource(), target);
    }

    private static int performDietReset(CommandSourceStack source, ServerPlayer target) {
        
        // Get player's diet data
        DietData dietData = target.getData(DietAttachments.DIET_DATA);
        
        // Reset to 80% of max values
        dietData.reset(DietRegistry.getMaxValues(), 0.8f);
        dietData.setLastDecayTimeMs(System.currentTimeMillis());
        
        // Sync to client
        PacketDistributor.sendToAllPlayers(new DietSyncPacket(target.getId(), dietData.copyValues()));
        
        // Send feedback
        source.sendSuccess(() -> Component.literal("Reset " + target.getName().getString() + "'s diet to 80%"), true);
        target.sendSystemMessage(Component.literal("Your diet has been reset to 80%"));
        
        return 1;
    }
    
    private static int checkDiet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        CommandSourceStack source = context.getSource();
        
        DietData dietData = target.getData(DietAttachments.DIET_DATA);
        DietProfile maxValues = DietRegistry.getMaxValues();
        
        Component message = Component.literal(target.getName().getString() + "'s Diet Values:\n")
            .append(formatDietLine("Cereali: ", dietData.get(DietCategory.GRAIN), maxValues.get(DietCategory.GRAIN)))
            .append(formatDietLine("Proteine: ", dietData.get(DietCategory.PROTEIN), maxValues.get(DietCategory.PROTEIN)))
            .append(formatDietLine("Verdure: ", dietData.get(DietCategory.VEGETABLE), maxValues.get(DietCategory.VEGETABLE)))
            .append(formatDietLine("Frutta: ", dietData.get(DietCategory.FRUIT), maxValues.get(DietCategory.FRUIT)))
            .append(formatDietLine("Zuccheri: ", dietData.get(DietCategory.SUGAR), maxValues.get(DietCategory.SUGAR)))
            .append(formatDietLine("Idratazione: ", dietData.get(DietCategory.WATER), maxValues.get(DietCategory.WATER)));
        
        source.sendSuccess(() -> message, true);
        
        return 1;
    }
    
    private static Component formatDietLine(String label, float value, float max) {
        return Component.literal(String.format("%s%.1f / %.1f%n", label, value, max));
    }
    
    private static int recalculateCache(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("Starting diet profile recalculation..."), true);
        
        // Clear and recalculate cache
        DietProfileCache cache = DietRegistry.getPersistentCache();
        if (cache != null) {
            cache.clear();
            cache.calculateAsync(source.getServer(), DietRegistry.getSettings())
                .thenRun(() -> {
                    source.sendSuccess(() -> Component.literal("Diet profile recalculation completed! Syncing to online players..."), true);
                    
                    // Sync updated profiles to all online players
                    syncProfilesToAllPlayers(source.getServer(), cache);
                });
        } else {
            source.sendFailure(Component.literal("Diet cache not initialized!"));
        }
        
        return 1;
    }
    
    private static void syncProfilesToAllPlayers(net.minecraft.server.MinecraftServer server, DietProfileCache cache) {
        java.util.Map<net.minecraft.resources.ResourceLocation, DietProfile> profiles = cache.getAllProfiles();
        if (!profiles.isEmpty()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                    player, 
                    new com.THproject.tharidia_things.network.DietProfileSyncPacket(profiles)
                );
            }
        }
    }
    
    private static int showVersion(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        String version = DietProfileCache.getAnalysisVersion();
        source.sendSuccess(() -> Component.literal("Diet Analysis Version: " + version), false);
        
        return 1;
    }
}
