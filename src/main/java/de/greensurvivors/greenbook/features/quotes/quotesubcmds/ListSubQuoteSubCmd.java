package de.greensurvivors.greenbook.features.quotes.quotesubcmds;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.ASubCommand;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.commands.ListBuilder;
import de.greensurvivors.greenbook.features.quotes.QuoteConfigManager;
import de.greensurvivors.greenbook.features.quotes.QuotePermissions;
import de.greensurvivors.greenbook.features.quotes.QuoteSubCmd;
import de.greensurvivors.greenbook.features.quotes.QuotesLangPath;
import de.greensurvivors.greenbook.language.MessageManager;
import de.greensurvivors.greenbook.language.StandardLangPath;
import de.greensurvivors.greenbook.language.StandartPlaceHolders;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;

/**
 * /geenbook quote list < page > - list all known quotes (quotes) neatly arranged in pages.
 */
public class ListSubQuoteSubCmd extends ASubCommand {
    private static final String LIST = "list";
    private static final int QUOTES_PER_PAGE = 5;

    private final @NotNull QuoteConfigManager quoteConfig;

    public ListSubQuoteSubCmd(final @NotNull GreenBook plugin,
                              final @NotNull QuoteConfigManager config,
                              final @NotNull Permission parentPerm) {
        super(plugin);
        this.quoteConfig = config;

        QuotePermissions.CMD_LIST.getPermission().addParent(parentPerm, true);
    }

    @Override
    public boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(QuotePermissions.CMD_LIST.getPermission());
    }

    @Override
    @NotNull
    public List<LiteralCommandNode<CommandSourceStack>> getCmdNodes() {
        return List.of(Commands.literal(LIST).
            requires(commandSourceStack -> checkPermission(commandSourceStack.getSender())).
            then(Commands.argument("pageNumber", IntegerArgumentType.integer(1)).
                requires(stack -> getNumberOfPages() > 0). // only allow an argument if we have quotes at all.
                suggests((context, builder) -> {
                    for (int i = 1; i <= getNumberOfPages(); i++) {
                        builder.suggest(String.valueOf(i));
                    }
                    return builder.buildFuture();
                }).executes(context -> onCommand(context, IntegerArgumentType.getInteger(context, "pageNumber")))
            ).executes(
                context -> onCommand(context, 1)
            ).build());
    }

    @Override
    @NotNull
    public Set<@NotNull String> getAliases() {
        return Set.of(LIST);
    }

    @Override
    @NotNull
    public Component getHelpText() { // todo how to integrate into brigadier?
        return Component.text("TODO");
    }

    private int getNumberOfPages() {
        //get all currently active quotes
        final @NotNull SortedMap<@NotNull Integer, @NotNull Component> quotes = quoteConfig.getQuotes();
        //how many quotes are known. Needed to calculate how many pages there are and
        //how many there should be on the given page (if the page is not full)
        final int numOfQuotes = quotes.size();
        //how many pages of quotes are there? - needed in header and limit page to how many exits
        return (int) Math.ceil((double) numOfQuotes / (double) QUOTES_PER_PAGE);
    }

    private int onCommand(@NotNull CommandContext<CommandSourceStack> context, int page) {
        if (checkPermission(context.getSource().getSender())) { // todo do we need this check?
            //get all currently active quotes
            final @NotNull Int2ObjectSortedMap<@NotNull Component> quotes = (Int2ObjectSortedMap<@NotNull Component>)quoteConfig.getQuotes(); // todo check type casting
            //how many quotes are known. Needed to calculate how many pages there are and
            //how many there should be on the given page (if the page is not full)
            final int numOfQuotes = quotes.size();
            //how many pages of quotes are there? - needed in header and limit page to how many exits
            final int numOfPages = (int) Math.ceil((double) numOfQuotes / (double) QUOTES_PER_PAGE);

            page = Math.clamp(page, 1, numOfPages);

            //don't try to access more quotes than existing
            final int maxQuotesThisPage = Math.min(numOfQuotes, page * QUOTES_PER_PAGE);

            MessageManager messageManager = plugin.getMessageManager();

            ListBuilder listBuilder = new ListBuilder(plugin, messageManager.getLang(QuotesLangPath.CMD_SUB_LIST_HEADER));
            listBuilder.paged(page, numOfPages);
            listBuilder.pageBackCommand("/" + GreenBookCmd.getCommandName() + " " + QuoteSubCmd.getSubcommandName() + " " + LIST + " " + (page - 1));
            listBuilder.pageNextCommand("/" + GreenBookCmd.getCommandName() + " " + QuoteSubCmd.getSubcommandName() + " " + LIST + " " + (page + 1));
            listBuilder.pageLastCommand("/" + GreenBookCmd.getCommandName() + " " + QuoteSubCmd.getSubcommandName() + " " + LIST + " " + numOfPages);

            //add the quotes for the page
            final @NotNull ObjectBidirectionalIterator<Int2ObjectMap.@NotNull Entry<@NotNull Component>> iterator = quotes.int2ObjectEntrySet().iterator();
            iterator.skip(Math.max(0, (page - 1) * QUOTES_PER_PAGE - 1));
            for (int i = (page - 1) * QUOTES_PER_PAGE; i < maxQuotesThisPage; i++) {
                final @NotNull Int2ObjectMap.Entry<@NotNull Component> entry = iterator.next();

                listBuilder.addEntry(messageManager.getLang(QuotesLangPath.CMD_SUB_LIST_ENTRY,
                    Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(entry.getIntKey())),
                    Placeholder.component(StandartPlaceHolders.TEXT.getPlaceholder(), entry.getValue())));
            }

            context.getSource().getSender().sendMessage(listBuilder.build());
        } else {
            plugin.getMessageManager().sendLang(context.getSource().getSender(), StandardLangPath.NO_PERMISSION);
        }

        return Command.SINGLE_SUCCESS;
    }
}
