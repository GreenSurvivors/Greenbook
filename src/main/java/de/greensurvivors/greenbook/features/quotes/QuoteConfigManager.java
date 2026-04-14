package de.greensurvivors.greenbook.features.quotes;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfigData;
import de.greensurvivors.greenbook.config.AYamlFeatureConfigManager;
import de.greensurvivors.greenbook.features.FeatureType;
import io.leangen.geantyref.TypeToken;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.PostProcess;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class QuoteConfigManager extends AYamlFeatureConfigManager<QuoteConfigManager.QuoteConfigData> {

    protected QuoteConfigManager(final @NotNull GreenBook plugin) {
        super(plugin, FeatureType.QUOTES, TypeToken.get(QuoteConfigData.class));
    }

    /**
     *
     * @param quoteText
     * @return the id of the freshly added quote.
     */
    public @NotNull CompletableFuture<@NotNull Integer> addQuote(final @NotNull Component quoteText) {
        final int id = ++configData.highestId;

        configData.quotes.put(id, quoteText);
        return saveAndReload().thenApply(ignored -> id);
    }

    public boolean hasQuote(final int quoteID) {
        return getQuote(quoteID) != null;
    }

    // always call sync!
    public @Nullable Component getRandomQuote() {
        if (!Bukkit.isPrimaryThread()) {
            plugin.getComponentLogger().warn("wanted to access random quote async and failed!");
            return null;
        }

        return configData.quotes.get(ThreadLocalRandom.current().nextInt(configData.quotes.size()));
    }

    public @Nullable Component getQuote(final int quoteID) {
        return configData.quotes.get(quoteID);
    }

    public @NotNull SortedMap<@NotNull Integer, @NotNull Component> getQuotes() {
        return configData.quotes;
    }

    public @NotNull IntSortedSet getIds() {
        return configData.quotes.keySet();
    }

    public @NotNull CompletableFuture<@Nullable Component> removeQuote(final int quoteID) {
        Component result = configData.quotes.remove(quoteID);

        return saveAndReload().thenApply(ignored -> result);
    }

    public @NotNull CompletableFuture<Void> setRequireEmptyHand(final boolean shouldQuoteRequireEmptyHand) {
        configData.requiresEmptyHand = shouldQuoteRequireEmptyHand;

        return saveAndReload();
    }

    public boolean isEmptyHandRequired() {
        return configData.requiresEmptyHand;
    }

    public @NotNull CompletableFuture<Void> setRequireSneak(final boolean shouldRequireSneak) {
        configData.requiresSneak = shouldRequireSneak;

        return saveAndReload();
    }

    public boolean isSneakingRequired() {
        return configData.requiresSneak;
    }

    public boolean isQuoteBlockType(final @NotNull BlockType type) { // todo setter
        return configData.clickableBlockTypes.contains(type);
    }

    @ConfigSerializable
    protected static class QuoteConfigData extends AFeatureConfigData {
        @Comment("Internal used highest id. If you really have to touch it (trust me you don't), set it to the highest number of the quotes option.")
        protected int highestId = 0;
        @Comment("If players need to sneak in order to get a quote.")
        protected boolean requiresSneak = false;
        @Comment("If players need to click with an empty hand to get a quote.")
        protected boolean requiresEmptyHand = true;
        @Comment("The block types a player may click in order to get a quote.")
        protected final @NotNull Set<@NotNull BlockType> clickableBlockTypes = new HashSet<>();
        @Comment("All the quotes with their id in Minimessage format docs.papermc.io/adventure/minimessage/format/")
        protected final @NotNull Int2ObjectSortedMap<@NotNull Component> quotes = new Int2ObjectLinkedOpenHashMap<>();

        @PostProcess
        protected void checkHighestId() {
            // note: even though Int2ObjectLinkedOpenHashMap is a sorted map, it doesn't have any comparator associated with it, meaning that integer ordering by size is not guaranteed.
            // we have to check all qoutes if any of them have a higher id than we expect!
            for (int id : quotes.sequencedKeySet()) {
                if (id > highestId) {
                    highestId = id;
                }
            }
        }
    }
}
