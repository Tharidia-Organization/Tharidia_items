package com.THproject.tharidia_things.command;

import java.util.List;

import com.THproject.tharidia_things.config.ItemCatalogueConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ItemCatalogueCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("itemCatalogue")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("LAMA_CORTA")
                        .executes(context -> getItemCatalogue(context, "LAMA_CORTA")))
                .then(Commands.literal("LANCIA")
                        .executes(context -> getItemCatalogue(context, "LANCIA")))
                .then(Commands.literal("MARTELLI")
                        .executes(context -> getItemCatalogue(context, "MARTELLI")))
                .then(Commands.literal("MAZZE")
                        .executes(context -> getItemCatalogue(context, "MAZZE")))
                .then(Commands.literal("SPADE_2_MANI")
                        .executes(context -> getItemCatalogue(context, "SPADE_2_MANI")))
                .then(Commands.literal("ASCE")
                        .executes(context -> getItemCatalogue(context, "ASCE")))
                .then(Commands.literal("SOCCHI")
                        .executes(context -> getItemCatalogue(context, "SOCCHI")))
                .then(Commands.literal("ARCHI")
                        .executes(context -> getItemCatalogue(context, "ARCHI")))
                .then(Commands.literal("ARMI_DA_FUOCO")
                        .executes(context -> getItemCatalogue(context, "ARMI_DA_FUOCO"))));
    }

    private static int getItemCatalogue(CommandContext<CommandSourceStack> context, String catalogue) {
        CommandSourceStack source = context.getSource();
        List<?> items = switch (catalogue) {
            case "LAMA_CORTA" -> (List<?>) ItemCatalogueConfig.config.LAMA_CORTA_ITEMS.get("Value");
            case "LANCIA" -> (List<?>) ItemCatalogueConfig.config.LANCIA_ITEMS.get("Value");
            case "MARTELLI" -> (List<?>) ItemCatalogueConfig.config.MARTELLI_ITEMS.get("Value");
            case "MAZZE" -> (List<?>) ItemCatalogueConfig.config.MAZZE_ITEMS.get("Value");
            case "SPADE_2_MANI" -> (List<?>) ItemCatalogueConfig.config.SPADE_2_MANI_ITEMS.get("Value");
            case "ASCE" -> (List<?>) ItemCatalogueConfig.config.ASCE_ITEMS.get("Value");
            case "SOCCHI" -> (List<?>) ItemCatalogueConfig.config.SOCCHI_ITEMS.get("Value");
            case "ARCHI" -> (List<?>) ItemCatalogueConfig.config.ARCHI_ITEMS.get("Value");
            case "ARMI_DA_FUOCO" -> (List<?>) ItemCatalogueConfig.config.ARMI_DA_FUOCO_ITEMS.get("Value");
            default -> null;
        };

        if (items == null || items.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Nessun item trovato nella categoria " + catalogue + "."),
                    false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal(catalogue + ":"), false);
        for (Object of : items) {
            source.sendSuccess(() -> Component.literal("    " + of.toString()), false);
        }
        return 1;
    }
}
