package de.greensurvivors.greenbook.features.quotes.quotesubcmds;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.ASubCommand;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.commands.ListBuilder;
import de.greensurvivors.greenbook.features.quotes.QuoteConfig;
import de.greensurvivors.greenbook.features.quotes.QuotePermissions;
import de.greensurvivors.greenbook.features.quotes.QuoteSubCmd;
import de.greensurvivors.greenbook.features.quotes.QuotesLangPath;
import de.greensurvivors.greenbook.language.MessageManager;
import de.greensurvivors.greenbook.language.StandardLangPath;
import de.greensurvivors.greenbook.language.StandartPlaceHolders;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * /geenbook quote list < page > - list all known quotes (quotes) neatly arranged in pages.
 */
@SuppressWarnings("UnstableApiUsage") // brigadier api
public class ListSubQuoteSubCmd extends ASubCommand {
    private static final String LIST = "list";
    private static final int QUOTES_PER_PAGE = 5;

    private final @NotNull QuoteConfig quoteConfig;

    public ListSubQuoteSubCmd(@NotNull GreenBook plugin, @NotNull QuoteConfig config, @NotNull Permission parentPerm) {
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
            then(Commands.argument("pageNumber", new DynamicIntegerArgumentType(() -> 1, this::getNumberOfPages)).
                requires(c -> getNumberOfPages() > 0). // only allow an argument if we have quotes at all.
                    executes(context -> onCommand(context, DynamicIntegerArgumentType.getInteger(context, "pageNumber")))/*.
                    suggests((context, suggestionsBuilder) -> { // todo check for suggestions on all other arguments

                        return suggestionsBuilder.suggest().buildFuture();
                    })*/
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
        final List<QuoteConfig.Quote> quotes = quoteConfig.getQuotes();
        //how many quotes are known. Needed to calculate how many pages there are and
        //how many there should be on the given page (if the page is not full)
        final int numOfQuotes = quotes.size();
        //how many pages of quotes are there? - needed in header and limit page to how many exits
        return (int) Math.ceil((double) numOfQuotes / (double) QUOTES_PER_PAGE);
    }

    private int onCommand(@NotNull CommandContext<CommandSourceStack> context, int page) {
        if (checkPermission(context.getSource().getSender())) { // todo do we need this check?
            //get all currently active quotes
            final List<QuoteConfig.Quote> quotes = quoteConfig.getQuotes();
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
            for (int i = (page - 1) * QUOTES_PER_PAGE; i < maxQuotesThisPage; i++) {
                QuoteConfig.Quote quote = quotes.get(i);

                listBuilder.addEntry(messageManager.getLang(QuotesLangPath.CMD_SUB_LIST_ENTRY,
                    Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(quote.id())),
                    Placeholder.component(StandartPlaceHolders.TEXT.getPlaceholder(), quote.content())));
            }

            context.getSource().getSender().sendMessage(listBuilder.build());
        } else {
            plugin.getMessageManager().sendLang(context.getSource().getSender(), StandardLangPath.NO_PERMISSION);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static class DynamicIntegerArgumentType implements ArgumentType<Integer> {
        private final Supplier<Integer> minimum;
        private final Supplier<Integer> maximum;

        protected DynamicIntegerArgumentType(final @NotNull Supplier<@NotNull Integer> minimum,
                                             final @NotNull Supplier<@NotNull Integer> maximum) {
            this.minimum = minimum;
            this.maximum = maximum;
        }

        public static int getInteger(final CommandContext<?> context, final String name) {
            return context.getArgument(name, int.class);
        }

        public int getNumber(final CommandContext<?> context, final String name) {
            return context.getArgument(name, int.class);
        }

        public int getMinimum() {
            return minimum.get();
        }

        public int getMaximum() {
            return maximum.get();
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
            for (int i = getMinimum(), max = getMaximum(); i < max; i++) {
                builder.suggest(i);
            }

            return builder.buildFuture();
        }

        @Override
        public Integer parse(final StringReader reader) throws CommandSyntaxException {
            final int start = reader.getCursor();
            final int result = reader.readInt();
            if (result < minimum.get()) {
                reader.setCursor(start);
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, result, minimum);
            }
            if (result > maximum.get()) {
                reader.setCursor(start);
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().createWithContext(reader, result, maximum);
            }
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o instanceof DynamicIntegerArgumentType that) {
                return getMaximum() == that.getMaximum() && getMinimum() == that.getMinimum();
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return 31 * minimum.get() + maximum.get();
        }

        @Override
        public String toString() {
            if (minimum.get() == Integer.MIN_VALUE && maximum.hashCode() == Integer.MAX_VALUE) {
                return "dynInteger()";
            } else if (maximum.get() == Integer.MAX_VALUE) {
                return "dynInteger(" + minimum + ")";
            } else {
                return "dynInteger(" + minimum + ", " + maximum + ")";
            }
        }

        @Override
        public Collection<String> getExamples() {
            return List.of(String.valueOf(getMinimum()), String.valueOf(getMaximum()));
        }
    }
}
