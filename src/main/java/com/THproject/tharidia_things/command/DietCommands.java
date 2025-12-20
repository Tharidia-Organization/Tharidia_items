package com.THproject.tharidia_things.command;

import com.THproject.tharidia_things.diet.DietAttachments;
import com.THproject.tharidia_things.diet.DietCategory;
import com.THproject.tharidia_things.diet.DietData;
import com.THproject.tharidia_things.diet.DietProfile;
import com.THproject.tharidia_things.diet.DietRegistry;
import com.THproject.tharidia_things.network.DietSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
            .requires(source -> source.hasPermission(4)) // OP level 2
            .then(Commands.literal("diet")
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(DietCommands::resetDiet)))
                .then(Commands.literal("check")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(DietCommands::checkDiet)))));
    }
    
    private static int resetDiet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        CommandSourceStack source = context.getSource();
        
        // Get player's diet data
        DietData dietData = target.getData(DietAttachments.DIET_DATA);
        
        // Reset to 80% of max values
        dietData.reset(DietRegistry.getMaxValues(), 0.8f);
        dietData.setLastDecayTimeMs(System.currentTimeMillis());
        
        // Sync to client
        PacketDistributor.sendToPlayer(target, new DietSyncPacket(dietData.copyValues()));
        
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
}
