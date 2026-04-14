package de.greensurvivors.greenbook.features;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.IFeatureConfigManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FeatureRegistry {
    protected final @NotNull GreenBook plugin;
    private final @NotNull Map<@NotNull FeatureType<?>, @NotNull AFeature<?>> registeredFeatures = new HashMap<>(FeatureType.getStandardTypes().size());

    /**
     * Creates a new FeatureRegistry.
     *
     * @param plugin the plugin to be used
     */
    public FeatureRegistry(final @NotNull GreenBook plugin) {
        this.plugin = plugin;
    }

    public void registerStandardFeatures() {
        for (final @NotNull FeatureType<?> standardType : FeatureType.getStandardTypes()) {
            final @Nullable AFeature<?> newFeature = standardType.createNewInstance(plugin);

            if (newFeature != null) {
                registerFeature(newFeature);
            }
        }
    }

    /**
     * Registers a new feature and also calling its onEnable method.
     * If a feature with the same type is already registered,
     * the old one is unregistered before registering the new feature.
     *
     * @param newFeature the feature to be registered
     * @throws NullPointerException if newFeature is null
     */
    public void registerFeature(final @NotNull AFeature<?> newFeature) throws NullPointerException {
        if (registeredFeatures.containsKey(newFeature.getFeatureType())) {
            unregisterFeature(newFeature.getFeatureType());
        }

        registeredFeatures.put(newFeature.getFeatureType(), newFeature);
        newFeature.featureConfig.reloadConfig();
    }

    /**
     * Unregisters a feature of the specified type.
     *
     * @param type the type of the feature to be unregistered
     */
    public void unregisterFeature(final @NotNull FeatureType<?> type) {
        final @Nullable AFeature<?> feature = registeredFeatures.get(type);

        if (feature != null) {
            feature.onDisable();

            registeredFeatures.remove(type);
        }
    }

    /**
     * Disables all registered features.
     */
    public void disableAll() {
        for (AFeature<?> feature : registeredFeatures.values()) {
            feature.onDisable();
        }
    }

    /**
     * Retrieves the registered feature of the specified type.
     *
     * @param type the type of the feature to retrieve
     * @return the registered feature of the specified type, or null if not found
     */
    public <ConfigManagerType extends IFeatureConfigManager> @Nullable AFeature<ConfigManagerType> getFeature(final @NotNull FeatureType<ConfigManagerType> type) {
        return (AFeature<ConfigManagerType>) registeredFeatures.get(type);
    }

    /**
     * Retrieves all registered features.
     *
     * @return all registered features
     */
    public @NotNull Collection<@NotNull AFeature<?>> getAllFeatures() {
        return registeredFeatures.values();
    }
}
