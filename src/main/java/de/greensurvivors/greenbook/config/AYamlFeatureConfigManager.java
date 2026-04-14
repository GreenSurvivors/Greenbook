package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.transformer.ComparableVersionedTransformation;
import de.greensurvivors.greenbook.features.FeatureType;
import io.leangen.geantyref.TypeToken;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public abstract class AYamlFeatureConfigManager<AFeatureConfigType extends AFeatureConfigData> extends AFeatureConfigManager<AFeatureConfigType,
        YamlConfigurationLoader.Builder, YamlConfigurationLoader> {
    protected AYamlFeatureConfigManager(final @NotNull GreenBook plugin,
                                        final @NotNull FeatureType<? extends AYamlFeatureConfigManager<AFeatureConfigType>> featureType,
                                        final AbstractConfigurationLoader.@NotNull Builder<YamlConfigurationLoader.Builder, YamlConfigurationLoader> configLoaderBuilder,
                                        final @NotNull TypeToken<AFeatureConfigType> typeToken,
                                        final @NotNull ComparableVersion currentConfigVersion, final @Nullable ComparableVersionedTransformation updateTransformation) {
        super(plugin, featureType, configLoaderBuilder, ".yml", typeToken, currentConfigVersion, updateTransformation);
    }

    protected AYamlFeatureConfigManager(final @NotNull GreenBook plugin,
                                        final @NotNull FeatureType<? extends AYamlFeatureConfigManager<AFeatureConfigType>> featureType,
                                        final AbstractConfigurationLoader.@NotNull Builder<YamlConfigurationLoader.Builder, YamlConfigurationLoader> configLoaderBuilder,
                                        final @NotNull TypeToken<AFeatureConfigType> typeToken) {
        this(plugin, featureType, configLoaderBuilder, typeToken, new ComparableVersion("1.0.0"), null);
    }

    protected AYamlFeatureConfigManager(final @NotNull GreenBook plugin,
                                        final @NotNull FeatureType<? extends AYamlFeatureConfigManager<AFeatureConfigType>> featureType,
                                        final @NotNull TypeToken<AFeatureConfigType> typeToken,
                                        final @NotNull ComparableVersion currentConfigVersion, final @Nullable ComparableVersionedTransformation updateTransformation) {
        // not setting node style defaults to auto, that somewhy prefers flow aka json style.
        this(plugin, featureType, YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK), typeToken, currentConfigVersion, updateTransformation);
    }

    protected AYamlFeatureConfigManager(final @NotNull GreenBook plugin,
                                        final @NotNull FeatureType<? extends AYamlFeatureConfigManager<AFeatureConfigType>> featureType,
                                        final @NotNull TypeToken<AFeatureConfigType> typeToken) {
        // not setting node style defaults to auto, that somewhy prefers flow aka json style.
        this(plugin, featureType, YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK), typeToken, new ComparableVersion("1.0.0"), null);
    }
}
