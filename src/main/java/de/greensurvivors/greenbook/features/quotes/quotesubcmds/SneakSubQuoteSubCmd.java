package de.greensurvivors.greenbook.features.quotes.quotesubcmds;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.ASubCommand;
import de.greensurvivors.greenbook.features.quotes.QuoteConfig;
import de.greensurvivors.greenbook.features.quotes.QuotePermissions;
import de.greensurvivors.greenbook.features.quotes.QuotesLangPath;
import de.greensurvivors.greenbook.language.StandardLangPath;
import de.greensurvivors.greenbook.language.StandartPlaceHolders;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class SneakSubQuoteSubCmd extends ASubCommand {
    private static final String SNEAK = "sneak";
    private final @NotNull QuoteConfig quoteConfig;

    public SneakSubQuoteSubCmd(@NotNull GreenBook plugin, @NotNull QuoteConfig config, @NotNull Permission parentPerm) {
        super(plugin);
        quoteConfig = config;

        QuotePermissions.CMD_SNEAK.getPermission().addParent(parentPerm, true);
    }

    @Override
    public boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(QuotePermissions.CMD_SNEAK.getPermission());
    }

    @Override
    @NotNull
    public List<LiteralCommandNode<CommandSourceStack>> getCmdNodes() {
        return List.of(Commands.literal(SNEAK).
            requires(commandSourceStack -> checkPermission(commandSourceStack.getSender())).
            then(Commands.argument("shouldRequireSneak", BoolArgumentType.bool()).
                executes(context -> onCommand(context, BoolArgumentType.getBool(context, "shouldRequireSneak")))
            ).build());
    }

    @Override
    @NotNull
    public Set<@NotNull String> getAliases() {
        return Set.of(SNEAK);
    }

    @Override
    @NotNull
    public Component getHelpText() {
        return Component.text("TODO");
    }

    private int onCommand(@NotNull CommandContext<CommandSourceStack> context, boolean shouldRequireSneak) {
        if (checkPermission(context.getSource().getSender())) {
            //save the value to file and set the current value
            quoteConfig.setRequireSneak(shouldRequireSneak);
            //give feedback
            plugin.getMessageManager().sendLang(context.getSource().getSender(), QuotesLangPath.CMD_SUB_SNEAK_SUCCESS,
                Placeholder.unparsed(StandartPlaceHolders.BOOL.getPlaceholder(), String.valueOf(shouldRequireSneak)));

        } else {
            plugin.getMessageManager().sendLang(context.getSource().getSender(), StandardLangPath.NO_PERMISSION);
        }

        return Command.SINGLE_SUCCESS;
    }
}
