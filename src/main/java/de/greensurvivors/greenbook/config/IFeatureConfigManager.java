package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.features.FeatureType;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface IFeatureConfigManager {
    @Contract(pure = true)
    @NotNull FeatureType getFeatureType();

    /**
     * Returns the current enabled state of the feature.
     *
     * @return the enabled state of the feature
     */
    @Contract(pure = true)
    boolean isEnabled(); // todo call for all features --> disable commands too!

    /**
     * Sets the enabled state of the feature. If the new state is different from the current state,
     * the config is saved and reloaded if the new state is enabled.
     *
     * @param isEnabled the new enabled state of the feature
     */
    @NotNull CompletableFuture<Void> setEnabled(boolean isEnabled);

    @NotNull ComparableVersion getExpectedVersion();

    @NotNull CompletableFuture<Void> reloadConfig();
}
