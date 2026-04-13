package de.greensurvivors.greenbook.features.quotes;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfigData;
import de.greensurvivors.greenbook.config.AYamlFeatureConfigManager;
import de.greensurvivors.greenbook.features.FeatureType;
import io.leangen.geantyref.TypeToken;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class QuoteConfigManager extends AYamlFeatureConfigManager<QuoteConfigManager.QuoteConfigData> {

    protected QuoteConfigManager(final @NotNull GreenBook plugin) {
        super(plugin, FeatureType.QUOTES, TypeToken.get(QuoteConfigData.class));

        //todo add freshly added keys to file load; save
    }

    private void checkHighestId(final int otherId) {
        if (otherId > config.highestId) {
            config.highestId = otherId;
        }
    }

    public int addQuote(final @NotNull Component quoteText) { // todo something doesn't work here!
        config.highestId++;

        config.quotes.put(config.highestId, quoteText);
        saveConfig().thenRun(this::reloadConfig);

        return config.highestId;
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

        return config.quotes.get(ThreadLocalRandom.current().nextInt(config.quotes.size()));
    }

    public @Nullable Component getQuote(final int quoteID) {
        return config.quotes.get(quoteID);
    }

    public @NotNull SortedMap<@NotNull Integer, @NotNull Component> getQuotes() {
        return config.quotes;
    }

    public @NotNull IntSortedSet getIds() {
        return (IntSortedSet)config.quotes.keySet();
    }

    public @NotNull CompletableFuture<@Nullable Component> removeQuote(final int quoteID) {
        Component result = config.quotes.remove(quoteID);

        return saveConfig().thenRun(this::reloadConfig).thenApply(ignored -> result);
    }

    public @NotNull CompletableFuture<Void> setRequireEmptyHand(final boolean shouldQuoteRequireEmptyHand) {
        config.requiresEmptyHand = shouldQuoteRequireEmptyHand;

        return saveAndReload();
    }

    public boolean isEmptyHandRequired() {
        return config.requiresEmptyHand;
    }

    public @NotNull CompletableFuture<Void> setRequireSneak(final boolean shouldRequireSneak) {
        config.requiresSneak = shouldRequireSneak;

        return saveAndReload();
    }

    public boolean isSneakRequired() {
        return config.requiresSneak;
    }

    public boolean isQuoteBlockType(final @NotNull BlockType type) {
        return config.clickableBlockTypes.contains(type);
    }

    public boolean isSneakingRequired() {
        return config.requiresSneak;
    }

    @ConfigSerializable
    protected static class QuoteConfigData extends AFeatureConfigData {
        @Comment("Internal used highest id. No touchies. Or do and suffer, I'm not your real dad anyways.")
        protected int highestId = 0;
        protected boolean requiresSneak = false;
        protected boolean requiresEmptyHand = true;
        protected final @NotNull Set<@NotNull BlockType> clickableBlockTypes = new HashSet<>();
        protected final @NotNull SortedMap<@NotNull Integer, @NotNull Component> quotes = new Int2ObjectLinkedOpenHashMap<>();
    }
}
