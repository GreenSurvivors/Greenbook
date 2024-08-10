package de.greensurvivors.greenbook.features.quotes;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfig;
import de.greensurvivors.greenbook.config.ConfigOption;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.utils.VersionMissMatchException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class QuoteConfig extends AFeatureConfig {
    private final @NotNull File configFile;
    private final @NotNull FileConfiguration config;

    private final @NotNull ConfigOption<List<@NotNull Quote>> QUOTES = new ConfigOption<>("quotes", new CopyOnWriteArrayList<>());
    @SuppressWarnings("UnstableApiUsage") // block type
    private final @NotNull ConfigOption<Set<@NotNull BlockType>> QUOTE_MATERIALS = new ConfigOption<>("clickableBlockTypes", ConcurrentHashMap.newKeySet());
    private final @NotNull ConfigOption<Boolean> REQUIRE_SNEAK = new ConfigOption<>("requiresSneak", false);
    private final @NotNull ConfigOption<Boolean> REQUIRE_EMPTY_HAND = new ConfigOption<>("requiresEmptyHand", true);
    private int highestId = 0;

    protected QuoteConfig(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.QUOTES, new YamlConfiguration(), ".yml", new ComparableVersion("1.0.0"));

        QUOTES.setValue(new CopyOnWriteArrayList<>());

        ConfigurationSerialization.registerClass(Quote.class/*, "GreenBook_Quote"*/); // todo get alias working to be future proof

        configFile = new File(plugin.getDataFolder(), "quotesConfig.yml");
        synchronized (configFile) { // just in case something really stupid happens
            if (!configFile.exists()) { // todo async
                // plugin.saveResource("quotesConfig.yml", false);

                InputStream in = plugin.getResource("quotesConfig.yml");

                if (in != null) {
                    if (!plugin.getDataFolder().exists()) {
                        plugin.getDataFolder().mkdirs();
                    }

                    try {
                        OutputStream out = new FileOutputStream(configFile);

                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        out.close();
                        in.close();
                    } catch (IOException ex) {
                        plugin.getComponentLogger().error("Could not save \"quotesConfig.yml\"!", ex);
                    }
                } else {
                    plugin.getComponentLogger().error("Did not found default \"quotesConfig.yml\"! Will rely on hardcoded default values.");
                }
            } else { // todo

            }

            config = YamlConfiguration.loadConfiguration(configFile);
        }

        //todo add freshly added keys to file load; save
    }

    private void checkHighestId(final int otherId) {
        if (otherId > highestId) {
            highestId = otherId;
        }
    }

    public int addQuote(@NotNull Component quoteText) { // todo something doesn't work here!
        QUOTES.getValueOrFallback().add(new Quote(++highestId, quoteText));
        saveConfig().thenRun(this::reloadConfig);

        return highestId;
    }

    public boolean hasQuote(int quoteID) {
        return getQuote(quoteID) != null;
    }

    // always call sync!
    public @Nullable Component getRandomQuote() {
        if (!Bukkit.isPrimaryThread()) {
            plugin.getComponentLogger().warn("wanted to access random quote async and failed!");
            return null;
        }

        return QUOTES.getValueOrFallback().get(ThreadLocalRandom.current().nextInt(QUOTES.getValueOrFallback().size())).content();
    }

    public @Nullable Component getQuote(int quoteID) {
        for (Quote quote : QUOTES.getValueOrFallback()) {
            if (quote.id == quoteID) {
                return quote.content;
            }
        }

        return null;
    }

    public @NotNull List<@NotNull Quote> getQuotes() {
        return QUOTES.getValueOrFallback();
    }

    public boolean removeQuote(int quoteID) {
        boolean result = QUOTES.getValueOrFallback().removeIf(quote -> quote.id == quoteID);

        saveConfig().thenRun(this::reloadConfig);

        return result;
    }

    public void setRequireEmptyHand(boolean shouldQuoteRequireEmptyHand) {
        REQUIRE_EMPTY_HAND.setValue(shouldQuoteRequireEmptyHand);

        saveConfig().thenRun(this::reloadConfig);
    }

    public boolean isEmptyHandRequired() {
        return REQUIRE_EMPTY_HAND.getValueOrFallback();
    }

    public void setRequireSneak(boolean shouldRequireSneak) {
        REQUIRE_SNEAK.setValue(shouldRequireSneak);

        saveConfig().thenRun(this::reloadConfig);
    }

    @Override
    public void reloadConfig() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (configFile) { // only one thread should access the file
                isEnabled.set(config.getBoolean(ENABLED_CFG_KEY)); // todo

                try {
                    config.load(configFile);
                } catch (InvalidQuoteException exception) {
                    plugin.getComponentLogger().warn("Could not read Quote and will ignore it.", exception);
                } catch (IOException | InvalidConfigurationException e) {
                    plugin.getComponentLogger().error("Could not read config. In order to not corrupt it further, I will abort reloading it and just work with in memory value.", e);
                    return;
                }

                List<Quote> newQuotes = new CopyOnWriteArrayList<>();
                highestId = 0;

                // sanity check
                for (@NotNull Object object : config.getList(QUOTES.getPath(), new ArrayList<>())) {
                    switch (object) {
                        case Quote newQuote -> {
                            newQuotes.add(newQuote);
                            checkHighestId(newQuote.id);
                        }
                        case ConfigurationSection configSection -> {
                            Map<String, Object> sectionMap = new HashMap<>();
                            for (String key : configSection.getKeys(false)) {
                                sectionMap.put(key, configSection.get(key));
                            }

                            try {
                                Quote newQuote = Quote.deserialize(sectionMap);
                                newQuotes.add(newQuote);
                                checkHighestId(newQuote.id);
                            } catch (InvalidQuoteException exception) {
                                plugin.getComponentLogger().warn("Could not read quote and will ignore it.", exception);
                            }
                        }
                        case Map<?, ?> objMap -> { // did not serialize properly
                            Map<String, Object> checkedMap = new HashMap<>();

                            for (Map.Entry<?, ?> entry : objMap.entrySet()) {
                                if (entry.getKey() instanceof String key) {
                                    checkedMap.put(key, entry.getValue());
                                } else {
                                    plugin.getComponentLogger().warn("Could not read key \"" + entry.getKey() + "\" of Quote! ignoring for now. But you should check your config!");
                                }
                            }

                            try {
                                Quote newQuote = Quote.deserialize(checkedMap);
                                newQuotes.add(newQuote);
                                checkHighestId(newQuote.id);
                            } catch (InvalidQuoteException exception) {
                                plugin.getComponentLogger().warn("Could not read quote and will ignore it.", exception);
                            }
                        }
                        default -> {
                            plugin.getComponentLogger().warn("Could not read quote, it is a unknown object type: " + object.getClass().getName() + ", ignoring it.");
                        }
                    }
                }
                QUOTES.setValue(newQuotes);

                /*Set<Material> newQuoteMaterials = ConcurrentHashMap.newKeySet();
                for (@NotNull Object object : config.getList(QUOTE_MATERIALS.getPath(), new ArrayList<>())) {
                    switch (object) {
                        case Material material -> newQuoteMaterials.add(material);
                        case String string -> {
                            Material material = Material.matchMaterial(string);

                            if (material != null) {
                                newQuoteMaterials.add(material);
                            } else {
                                plugin.getComponentLogger().warn(string + " was configured to be a clickable Material for quotes, but it isn't a valid Material. Ignoring.");
                            }
                        }
                        default -> plugin.getComponentLogger().warn(object + " was configured to be a clickable Material for quotes, but it is an unknown object. Ignoring.");
                    }
                }
                QUOTE_MATERIALS.setValue(newQuoteMaterials);*/
                QUOTE_MATERIALS.setValue(plugin.getConfigManager().getBlockTypesFromConfig(config, QUOTE_MATERIALS.getPath()));

                REQUIRE_SNEAK.setValue(config.getBoolean(REQUIRE_SNEAK.getPath()));
                REQUIRE_EMPTY_HAND.setValue(config.getBoolean(REQUIRE_EMPTY_HAND.getPath()));
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> saveConfig() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            config.set(QUOTES.getPath(), QUOTES.getValueOrFallback());
            config.set(REQUIRE_SNEAK.getPath(), REQUIRE_SNEAK.getValueOrFallback());
            config.set(REQUIRE_EMPTY_HAND.getPath(), REQUIRE_EMPTY_HAND.getValueOrFallback());
            config.set(QUOTE_MATERIALS.getPath(), QUOTE_MATERIALS.getValueOrFallback());

            synchronized (configFile) { // only one thread should access the file
                try {
                    config.save(configFile);
                    result.complete(null);
                } catch (IOException e) {
                    plugin.getComponentLogger().error("Could not save config to file. Config values WILL be lost on next reload!", e);
                }
            }
        });

        return result;
    }

    @SuppressWarnings("UnstableApiUsage") // block type
    public boolean isQuoteBlockType(@NotNull BlockType type) {
        return QUOTE_MATERIALS.getValueOrFallback().contains(type);
    }

    public boolean isSneakingRequired() {
        return REQUIRE_SNEAK.getValueOrFallback();
    }

    public record Quote(int id, @NotNull Component content) implements ConfigurationSerializable {
        private static final ComparableVersion VERSION = new ComparableVersion("1.0.0");
        private static final String ID_KEY = "id", CONTENT_KEY = "content", VERSION_KEY = "version";

        public static @NotNull Quote deserialize(@NotNull Map<@NotNull String, @NotNull Object> serialized) throws IllegalArgumentException {
            int id;
            Component content;

            if (serialized.get(VERSION_KEY) instanceof String strVersion) {
                if (VERSION.compareTo(new ComparableVersion(strVersion)) < 0) {
                    throw new VersionMissMatchException("Version " + strVersion + " is newer than " + VERSION);
                }
            } else {
                Bukkit.getLogger().warning("[GreenBook] Quote in config doesn't has an version attached to it. We will just assume it still works and hope for the best!");
            }

            switch (serialized.get(ID_KEY)) {
                case Number tempId -> id = tempId.intValue();
                case String tempStr -> id = Integer.parseInt(tempStr);
                case null, default -> throw new InvalidQuoteException("Invalid id");
            }

            switch (serialized.get(CONTENT_KEY)) {
                case Component tempContent -> content = tempContent;
                case String tempStr -> content = MiniMessage.miniMessage().deserialize(tempStr);
                case null, default -> throw new InvalidQuoteException("Invalid Content");
            }

            return new Quote(id, content);
        }

        @Override
        public @NotNull Map<@NotNull String, @NotNull Object> serialize() {
            return Map.of(
                ConfigurationSerialization.SERIALIZED_TYPE_KEY, Quote.class.getName(),
                VERSION_KEY, VERSION.toString(), // just in case we ever need to use a DFU
                ID_KEY, id,
                CONTENT_KEY, MiniMessage.miniMessage().serialize(content));
        }

        @Override
        public String toString() {
            return "[id:" + id + ", content: \"" + MiniMessage.miniMessage().serialize(content) + "\"]";
        }
    }

    private static class InvalidQuoteException extends IllegalArgumentException {
        public InvalidQuoteException(@NotNull String message) {
            super(message);
        }

        public InvalidQuoteException() {
            super();
        }
    }
}
