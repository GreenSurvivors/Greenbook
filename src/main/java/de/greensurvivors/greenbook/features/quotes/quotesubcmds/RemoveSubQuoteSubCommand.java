package de.greensurvivors.greenbook.features.quotes.quotesubcmds;

import com.mojang.brigadier.Command;
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

/**
 * /greenbook quote remove [quote number] - remove a quote (aka book) by its id
 */
@SuppressWarnings("UnstableApiUsage") // brigadier api
public class RemoveSubQuoteSubCommand extends ASubCommand {
    private static final String REMOVE_SHORT = "rm", REMOVE_LONG = "remove";

    private final @NotNull QuoteConfig quoteConfig;

    public RemoveSubQuoteSubCommand(@NotNull GreenBook plugin,
                                    @NotNull QuoteConfig config,
                                    @NotNull Permission parentPerm) {
        super(plugin);
        quoteConfig = config;

        QuotePermissions.CMD_REMOVE.getPermission().addParent(parentPerm, true);
    }

    @Override
    public boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(QuotePermissions.CMD_REMOVE.getPermission());
    }

    @Override
    @NotNull
    public List<LiteralCommandNode<CommandSourceStack>> getCmdNodes() {
        // create subcommand
        LiteralCommandNode<CommandSourceStack> subCmdNode = Commands.literal(REMOVE_LONG).
            requires(commandSourceStack -> checkPermission(commandSourceStack.getSender())).
            then(Commands.argument("quoteId", new BookIDArgument(plugin)).
                executes(context ->
                    onCommand(context, BookIDArgument.getID(context, "quoteId")))
            ).build();

        // register subcommand and alias
        return List.of(subCmdNode,
            Commands.
                literal(REMOVE_SHORT).redirect(subCmdNode).
                build()
        );
    }

    @Override
    @NotNull
    public Set<@NotNull String> getAliases() {
        return Set.of(REMOVE_LONG, REMOVE_SHORT);
    }

    @Override
    @NotNull
    public Component getHelpText() {
        return Component.text("TODO");
    }

    private int onCommand(@NotNull CommandContext<CommandSourceStack> context, int quoteId) {
        if (checkPermission(context.getSource().getSender())) {
            if (quoteConfig.hasQuote(quoteId)) {
                //it is a quote. now remove it from config and current active list
                quoteConfig.removeQuote(quoteId);
                // send feedback
                plugin.getMessageManager().sendLang(context.getSource().getSender(), QuotesLangPath.CMD_SUB_REMOVE_SUCCESS,
                    Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(quoteId)));
            } else {
                //no quote with this id
                plugin.getMessageManager().sendLang(context.getSource().getSender(), QuotesLangPath.CMD_SUB_REMOVE_NO_ID,
                    Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(quoteId)));

                return 0;
            }
        } else {
            //no permission
            plugin.getMessageManager().sendLang(context.getSource().getSender(), StandardLangPath.NO_PERMISSION);
        }

        return Command.SINGLE_SUCCESS;
    }
}
