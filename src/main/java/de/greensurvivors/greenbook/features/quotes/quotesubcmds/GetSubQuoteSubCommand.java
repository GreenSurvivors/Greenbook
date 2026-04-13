package de.greensurvivors.greenbook.features.quotes.quotesubcmds;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.ASubCommand;
import de.greensurvivors.greenbook.features.quotes.QuoteConfigManager;
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
 * /greenbook quote get [id] - get a quote by its id
 */
public class GetSubQuoteSubCommand extends ASubCommand {
    private static final String GET = "get";

    private final @NotNull QuoteConfigManager quoteConfig;

    public GetSubQuoteSubCommand(@NotNull GreenBook plugin, @NotNull QuoteConfigManager config, @NotNull Permission parentPerm) {
        super(plugin);
        quoteConfig = config;

        QuotePermissions.CMD_GET.getPermission().addParent(parentPerm, true);
    }

    @Override
    public boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(QuotePermissions.CMD_GET.getPermission());
    }

    @Override
    @NotNull
    public List<LiteralCommandNode<CommandSourceStack>> getCmdNodes() {
        return List.of(Commands.literal(GET).
            requires(commandSourceStack -> checkPermission(commandSourceStack.getSender())).
            then(Commands.argument("quoteId", new BookIDArgument(plugin)).
                executes(context -> onCommand(context, BookIDArgument.getID(context, "quoteId")))
            ).build());
    }

    @Override
    @NotNull
    public Set<@NotNull String> getAliases() {
        return Set.of(GET);
    }

    @Override
    @NotNull
    public Component getHelpText() {
        return Component.text("TODO");
    }

    private int onCommand(@NotNull CommandContext<CommandSourceStack> context, int quoteId) {
        if (checkPermission(context.getSource().getSender())) {

            Component quote = quoteConfig.getQuote(quoteId);

            if (quote == null) {
                plugin.getMessageManager().sendLang(context.getSource().getSender(), QuotesLangPath.CMD_SUB_GET_UNKNOWN,
                    Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(quoteId)));

            } else { // success
                plugin.getMessageManager().sendLang(context.getSource().getSender(), QuotesLangPath.CMD_SUB_GET_SUCCESS,
                    Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(quoteId)),
                    Placeholder.component(StandartPlaceHolders.TEXT.getPlaceholder(), quote));

                return Command.SINGLE_SUCCESS;
            }
        } else { //no permission
            plugin.getMessageManager().sendLang(context.getSource().getSender(), StandardLangPath.NO_PERMISSION);
        }

        return Command.SINGLE_SUCCESS;
    }
}
