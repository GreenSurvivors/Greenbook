package de.greensurvivors.greenbook.features;

import de.greensurvivors.greenbook.GreenBook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FeatureRegistry {
    private final @NotNull Map<@NotNull FeatureType, @NotNull AFeature<?>> registeredFeatures = new HashMap<>(FeatureType.getStandardTypes().size());

    /**
     * Creates a new FeatureRegistry.
     * The standard features are registered automatically.
     *
     * @param plugin the plugin to be used
     */
    public FeatureRegistry(final @NotNull GreenBook plugin) {
        for (FeatureType standardType : FeatureType.getStandardTypes()) {
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
        newFeature.onEnable(); // todo onEnable() should only be called, whenever <-- thanks for never writing the end of this. Now I forgot.
    }

    /**
     * Unregisters a feature of the specified type.
     *
     * @param type the type of the feature to be unregistered
     */
    public void unregisterFeature(final @NotNull FeatureType type) {
        AFeature<?> feature = registeredFeatures.get(type);

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
    public @Nullable AFeature<?> getFeature(final @NotNull FeatureType type) {
        return registeredFeatures.get(type);
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
