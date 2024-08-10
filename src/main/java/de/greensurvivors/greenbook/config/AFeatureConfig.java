package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.features.FeatureType;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AFeatureConfig {
    private static Path featureConfigPath;
    protected final static @NotNull String CONFIG_FOLDER_NAME = "features";
    protected final static @NotNull String ENABLED_CFG_KEY = "isEnabled";
    protected final @NotNull GreenBook plugin;
    private final @NotNull FeatureType featureType;
    protected final @NotNull FileConfiguration config;
    protected final @NotNull Path configFilePath;
    protected final @NotNull AtomicBoolean isEnabled = new AtomicBoolean(true);
    protected final ConfigOption<@NotNull ComparableVersion> configVersion;

    protected AFeatureConfig(final @NotNull GreenBook plugin, final @NotNull FeatureType featureType,
                             final @NotNull FileConfiguration config, final @NotNull String configFileExtension, @NotNull ComparableVersion configVersion) {
        this.plugin = plugin;
        this.featureType = featureType;
        this.config = config;
        this.configVersion = new ConfigOption<>("version", configVersion);

        if (featureConfigPath == null) {
            featureConfigPath = Path.of(plugin.getDataFolder().getPath(), CONFIG_FOLDER_NAME);
        }

        try {
            Files.createDirectories(featureConfigPath);
        } catch (UnsupportedOperationException | IOException e) {
            plugin.getComponentLogger().error("Could not create feature config folder. Will operate purely on default config and not safe anything config related!", e);
        }

        this.configFilePath = featureConfigPath.resolve(featureType.getFeatureName() + configFileExtension);
    }

    @Contract(pure = true)
    public @NotNull FeatureType getFeatureType() {
        return featureType;
    }

    /**
     * Returns the current enabled state of the feature.
     *
     * @return the enabled state of the feature
     */
    @Contract(pure = true)
    public boolean isEnabled() {
        return isEnabled.get();
    } // todo call for all features --> disable commands too!

    /**
     * Sets the enabled state of the feature. If the new state is different from the current state,
     * the config is saved and reloaded if the new state is enabled.
     *
     * @param  isEnabled  the new enabled state of the feature
     */
    @Contract(mutates = "this")
    public void setEnabled(boolean isEnabled) {
        if (this.isEnabled.compareAndSet(!isEnabled, isEnabled)) {
            if (isEnabled) {
                saveConfig().thenRun(this::reloadConfig);
            }
        }
    }

    protected abstract void reloadConfig();

    /**
     * Accessing the config should happen async.
     * In most cases after saving reloading the memory values is in order,
     * and the returned CompletableFuture should be completed right after saving was done.
     *
     * @return
     */
    protected abstract @NotNull CompletableFuture<Void> saveConfig();
}
