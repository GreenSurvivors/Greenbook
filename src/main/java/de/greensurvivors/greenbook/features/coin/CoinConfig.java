package de.greensurvivors.greenbook.features.coin;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfig;
import de.greensurvivors.greenbook.config.ConfigOption;
import de.greensurvivors.greenbook.features.FeatureType;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public class CoinConfig extends AFeatureConfig {
    protected static final String FILE_EXTENSION = ".yml"; // todo move before super call in constuctor one java 22 is an option
    // Set default itemstack, so we should not run into null exceptions.
    // However, it should never come into play, since at least the config should provide a default value itself.

    @SuppressWarnings("UnstableApiUsage") // item type
    private final @NotNull ConfigOption<ItemStack> COIN_ITEM = new ConfigOption<>("coinItem", ItemStack.of(ItemType.GOLD_NUGGET.asMaterial()));


    protected CoinConfig(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.COIN, new YamlConfiguration(), FILE_EXTENSION, new ComparableVersion("1.0.0"));

        try (BufferedReader bufferedReader = Files.newBufferedReader(configFilePath)) {
            // handle reader
            config.load(bufferedReader);
        } catch (IOException | InvalidConfigurationException | SecurityException e) {
            plugin.getComponentLogger().error("Could not load config file for " + getFeatureType().getFeatureName() + ". Will operate purely on default config and not safe anything config related!", e);
        }

        final InputStream defConfigStream = plugin.getResource(getFeatureType().getFeatureName() + FILE_EXTENSION); // todo fetching from resources should be done in super
        if (defConfigStream == null) {
            return;
        }

        config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));

        /*
        plugin.getConfig()


        config.load();



        config.compareAndSet()


        YamlConfiguration.loadConfiguration()

        config.set(new);

        config.get().*/
    }

    @Override
    public void reloadConfig() {

    }

    @Override
    public @NotNull CompletableFuture<Void> saveConfig() {
        CompletableFuture<Void> result = new CompletableFuture<>();

        return result;
    }

    public @NotNull ItemStack getCoinItem() {
        return COIN_ITEM.getValueOrFallback().clone();
    }

    public void setCoinItem(@NotNull ItemStack item) {
        COIN_ITEM.setValue(item);
        saveConfig().thenRun(this::reloadConfig);
    }
}
