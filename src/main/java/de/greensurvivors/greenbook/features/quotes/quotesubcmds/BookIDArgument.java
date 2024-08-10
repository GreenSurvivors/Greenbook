package de.greensurvivors.greenbook.features.quotes.quotesubcmds;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.features.quotes.QuoteConfig;
import de.greensurvivors.greenbook.features.quotes.QuoteFeature;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage") // brigadier api
class BookIDArgument implements CustomArgumentType.Converted<Integer, Integer> {
    private static final DynamicCommandExceptionType UNKNOWN_ID = new DynamicCommandExceptionType(
        (found) -> new LiteralMessage("Integer must be a valid quote ID, found: " + found)); // todo translation!

    private final @NotNull QuoteConfig quoteConfig;

    public BookIDArgument(@NotNull GreenBook plugin) {
        quoteConfig = ((QuoteFeature) plugin.getFeatureRegistry().getFeature(FeatureType.QUOTES)).getFeatureConfig();
    }

    protected static int getID(final @NotNull CommandContext<?> context, final @NotNull String name) throws IllegalArgumentException {
        return context.getArgument(name, Integer.class);
    }

    @Override
    public @NotNull Integer convert(@NotNull Integer integer) throws CommandSyntaxException {
        Component quote = quoteConfig.getQuote(integer);

        if (quote != null) {
            return integer;
        } else {
            throw UNKNOWN_ID.create(integer);
        }
    }

    @Override
    public @NotNull ArgumentType<Integer> getNativeType() {
        return IntegerArgumentType.integer();
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) { // todo is that right or does it needs to get filtered?
        List<QuoteConfig.Quote> quotes = quoteConfig.getQuotes();

        for (QuoteConfig.Quote quote : quotes) {
            builder.suggest(quote.id(), MessageComponentSerializer.message().serialize(Component.text("COOL! TOOLTIP!", NamedTextColor.GREEN))); // todo
        }

        return CompletableFuture.completedFuture(
            builder.build()
        );
    }
}
