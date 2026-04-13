package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.transformer.ComparableVersionedTransformation;
import de.greensurvivors.greenbook.features.AFeature;
import de.greensurvivors.greenbook.features.FeatureType;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ScopedConfigurationNode;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public abstract class AFeatureConfigManager<
        AFeatureConfigType extends AFeatureConfigData,
        BuilderType extends AbstractConfigurationLoader.Builder<BuilderType, LoaderType>,
        LoaderType extends AbstractConfigurationLoader<?>> implements IFeatureConfigManager {

    private static final String CONFIG_FOLDER_NAME = "features";
    protected final @NotNull GreenBook plugin;
    private final @NotNull FeatureType featureType;
    private final @NotNull String jarPath;
    private final @NotNull Path featureConfigPath;
    private final @NotNull LoaderType loader;
    private final @NotNull ComparableVersion expectedVersion;
    private final @Nullable ComparableVersionedTransformation updateTransformation;
    private final @NotNull TypeToken<AFeatureConfigType> typeToken;
    protected @MonotonicNonNull AFeatureConfigType config = null;

    protected AFeatureConfigManager(final @NotNull GreenBook plugin, final @NotNull FeatureType featureType,
                                    final @NotNull AbstractConfigurationLoader.Builder<BuilderType, LoaderType> configLoaderBuilder, final @NotNull String configFileExtension,
                                    final @NotNull TypeToken<AFeatureConfigType> typeToken,
                                    final @NotNull ComparableVersion currentConfigVersion, final @Nullable ComparableVersionedTransformation updateTransformation) {
        this(plugin, featureType, configLoaderBuilder, Path.of(CONFIG_FOLDER_NAME, featureType.getFeatureName() + configFileExtension), typeToken, currentConfigVersion, updateTransformation);
    }

    protected AFeatureConfigManager(final @NotNull GreenBook plugin, final @NotNull FeatureType featureType,
                                    final @NotNull AbstractConfigurationLoader.Builder<BuilderType, LoaderType> configLoaderBuilder, final @NotNull Path subPath,
                                    final @NotNull TypeToken<AFeatureConfigType> typeToken,
                                    final @NotNull ComparableVersion currentConfigVersion, final @Nullable ComparableVersionedTransformation updateTransformation) {
        this.plugin = plugin;
        this.featureType = featureType;
        this.expectedVersion = currentConfigVersion;
        this.updateTransformation = updateTransformation;
        this.typeToken = typeToken;

        // jar always uses '/' as separator, no matter what the os prefers.
        jarPath = subPath.toString().replace(File.separatorChar, '/');
        featureConfigPath = plugin.getDataPath().resolve(subPath);

        configLoaderBuilder.path(featureConfigPath);
        configLoaderBuilder.defaultOptions(configOptions ->
            configOptions.serializers(serializerBuilder -> {
                serializerBuilder.register(ComparableVersion.class, Serializers.ComparableVersionSerializer.INSTANCE);
                serializerBuilder.register(Component.class, Serializers.ComponentSerializer.INSTANCE);
                serializerBuilder.register(Pattern.class, Serializers.PatternSerializer.INSTANCE);
                serializerBuilder.register(BlockType.class, Serializers.BlockTypeSerializer.INSTANCE);
                serializerBuilder.register(BlockData.class, Serializers.BlockDataSerializer.INSTANCE);
                serializerBuilder.register(ItemStack.class, Serializers.ItemStackSerializer.INSTANCE);
            })
        );

        loader = configLoaderBuilder.build();

        reloadConfig();
    }

    @Contract(pure = true)
    @Override
    public @NotNull FeatureType getFeatureType() {
        return featureType;
    }

    @Contract(pure = true)
    @Override
    public boolean isEnabled() {
        return config.isEnabled;
    } // todo call for all features --> disable commands too!

    @Override
    public @NotNull CompletableFuture<Void> setEnabled(final boolean isEnabled) {
        config.isEnabled = isEnabled;
        return saveAndReload();
    }

    protected @NonNull CompletableFuture<Void> saveAndReload() {
        return saveConfig().thenCompose(ignored -> reloadConfig());
    }

    @Override
    @MustBeInvokedByOverriders
    public @NotNull CompletableFuture<Void> reloadConfig() {
        final @NotNull CompletableFuture<Void> result = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        //ForkJoinPool.commonPool().execute(() -> { // in case the features need to get loaded before the commands
            synchronized (this) {
                if (!Files.exists(featureConfigPath)) {
                    try (final InputStream inputStream = plugin.getResource(jarPath)) {
                        Files.createDirectories(featureConfigPath.getParent());
                        Files.copy(inputStream, featureConfigPath);
                    } catch (final @NotNull IOException e) {
                        result.completeExceptionally(e);
                        return;
                    }
                }

                final boolean wasEnabled = config != null && config.isEnabled;

                try {
                    config = updateNode(loader.load()).get(typeToken);
                } catch (final @NotNull ConfigurateException e) {
                    result.completeExceptionally(e);
                    return;
                }

                 if (config.configVersion.compareTo(expectedVersion) > 0) {
                    result.completeExceptionally(new ConfigurateException("Version higher than expected: " + expectedVersion + " got: " + config.configVersion));
                    return;
                }

                 if (config.isEnabled != wasEnabled) {
                     Bukkit.getScheduler().runTask(plugin, () -> {
                         final @Nullable AFeature<?> feature = plugin.getFeatureRegistry().getFeature(featureType);

                         if (feature != null) {
                             if (config.isEnabled) {
                                 feature.onEnable();
                             } else {
                                 feature.onDisable();
                             }
                         } else {
                             plugin.getComponentLogger().error("Feature Config for type " + featureType.getFeatureName() + " Tried to toggle enabled status, but the feature wasn't registered! Was Async reloading behind on time?");
                         }
                     });
                 }

                result.complete(null);
            }
        });

        return result;
    }

    /**
     * Apply the update transformations to a node.
     *
     * @param node the node to transform
     * @param <N> node type
     * @return provided node, after transformation
     */
    private <N extends @NotNull ScopedConfigurationNode<?>> N updateNode(final N node) throws ConfigurateException {
        if (updateTransformation != null && !node.virtual()) { // we only want to migrate existing data
            final @NotNull ComparableVersion startVersion = updateTransformation.version(node);
            updateTransformation.apply(node);
            final @NotNull ComparableVersion endVersion = updateTransformation.version(node);
            if (startVersion != endVersion) { // we might not have made any changes
                plugin.getComponentLogger().debug("Updated config schema for {} from {} to {}", featureType.getFeatureName(), startVersion, endVersion);
            }
        }
        return node;
    }

    /**
     * Accessing the config should happen async.
     * In most cases after saving reloading the memory values is in order,
     * and the returned CompletableFuture should be completed right after saving was done.
     */
    protected @NotNull CompletableFuture<Void> saveConfig() {
        final @NotNull CompletableFuture<Void> result = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized(this) {
                try {
                    loader.save(loader.createNode().set(typeToken, config));
                } catch (final @NotNull ConfigurateException e) {
                    plugin.getComponentLogger().error("Could not set config for feature {}", featureType.getFeatureName(), e);

                    throw new RuntimeException(e);
                }
                result.complete(null);
            }
        });

        return result;
    }

    @Override
    public @NotNull ComparableVersion getExpectedVersion() {
        return expectedVersion;
    }
}
