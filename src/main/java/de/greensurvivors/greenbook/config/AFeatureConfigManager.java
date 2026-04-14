package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.transformer.ComparableVersionedTransformation;
import de.greensurvivors.greenbook.features.AFeature;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.utils.VersionMissMatchException;
import io.leangen.geantyref.TypeToken;
import io.papermc.paper.configuration.serializer.ComponentSerializer;
import io.papermc.paper.configuration.serializer.collection.map.FastutilMapSerializer;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
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
        AFeatureConfigDataType extends AFeatureConfigData,
        BuilderType extends AbstractConfigurationLoader.Builder<BuilderType, LoaderType>,
        LoaderType extends AbstractConfigurationLoader<?>> implements IFeatureConfigManager {

    private static final @NotNull String CONFIG_DIRECTORY_NAME = "features";
    protected final @NotNull GreenBook plugin;
    private final @NotNull FeatureType<? extends AFeatureConfigManager<AFeatureConfigDataType, BuilderType, LoaderType>> featureType; // the generic is the child of this class
    private final @NotNull String jarPath;
    private final @NotNull Path featureConfigPath;
    private final @NotNull LoaderType loader;
    private final @NotNull ComparableVersion expectedVersion;
    private final @Nullable ComparableVersionedTransformation updateTransformation;
    private final @NotNull TypeToken<AFeatureConfigDataType> typeToken;
    protected volatile @MonotonicNonNull AFeatureConfigDataType configData = null;

    protected AFeatureConfigManager(final @NotNull GreenBook plugin,
                                    final @NotNull FeatureType<? extends AFeatureConfigManager<AFeatureConfigDataType, BuilderType, LoaderType>> featureType,
                                    final @NotNull AbstractConfigurationLoader.Builder<BuilderType, LoaderType> configLoaderBuilder, final @NotNull String configFileExtension,
                                    final @NotNull TypeToken<AFeatureConfigDataType> typeToken,
                                    final @NotNull ComparableVersion currentConfigVersion, final @Nullable ComparableVersionedTransformation updateTransformation) {
        this(plugin, featureType, configLoaderBuilder, createSubPath(plugin, featureType, configFileExtension), typeToken, currentConfigVersion, updateTransformation);
    }

    protected AFeatureConfigManager(final @NotNull GreenBook plugin,
                                    final @NotNull FeatureType<? extends AFeatureConfigManager<AFeatureConfigDataType, BuilderType, LoaderType>> featureType,
                                    final @NotNull AbstractConfigurationLoader.Builder<BuilderType, LoaderType> configLoaderBuilder, final @NotNull Path subPath,
                                    final @NotNull TypeToken<AFeatureConfigDataType> typeToken,
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
            configOptions.serializers(serializerBuilder -> serializerBuilder
                .register(ComparableVersion.class, Serializers.ComparableVersionSerializer.INSTANCE)
                .register(new ComponentSerializer())
                .register(Pattern.class, Serializers.PatternSerializer.INSTANCE)
                .register(BlockType.class, Serializers.BlockTypeSerializer.INSTANCE)
                .register(BlockData.class, Serializers.BlockDataSerializer.INSTANCE)
                .register(ItemStack.class, Serializers.ItemStackSerializer.INSTANCE)
                .register(new TypeToken<>() {}, new FastutilMapSerializer.PrimitiveToSomething<Int2ObjectSortedMap<?>>(Int2ObjectLinkedOpenHashMap::new, Integer.TYPE))
            ).shouldCopyDefaults(true)
        );

        loader = configLoaderBuilder.build();

        reloadConfig();
    }

    protected static @NotNull Path createSubPath (final @NotNull GreenBook plugin, final @NotNull FeatureType<?> featureType, final @NotNull String configFileExtension) {
        if (featureType.getFeatureKey().namespace().equals(plugin.namespace())) {
            return Path.of(CONFIG_DIRECTORY_NAME, featureType.getFeatureKey().value() + configFileExtension);
        } else {
            return Path.of(CONFIG_DIRECTORY_NAME, featureType.getFeatureKey().namespace(), featureType.getFeatureKey().value() + configFileExtension);
        }
    }

    @Contract(pure = true)
    @Override
    public @NotNull FeatureType<? extends AFeatureConfigManager<AFeatureConfigDataType, BuilderType, LoaderType>> getFeatureType() {
        return featureType;
    }

    @Contract(pure = true)
    @Override
    public boolean isEnabled() {
        return configData != null && configData.isEnabled;
    } // todo call for all features --> disable commands too!

    @Override
    public @NotNull CompletableFuture<Void> setEnabled(final boolean isEnabled) {
        configData.isEnabled = isEnabled;
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
                boolean wasModified = false;

                if (!Files.exists(featureConfigPath)) {
                    try (final InputStream inputStream = plugin.getResource(jarPath)) {
                        Files.createDirectories(featureConfigPath.getParent());
                        Files.copy(inputStream, featureConfigPath);
                        wasModified = true;
                    } catch (final @NotNull IOException e) {
                        plugin.getComponentLogger().warn("failed to save default data for {}", getFeatureType().getFeatureKey(), e);
                        result.completeExceptionally(e);
                        return;
                    }
                }

                final boolean wasEnabled = configData != null && configData.isEnabled;

                // Apply the update transformations to our node
                try {
                    final @NotNull ScopedConfigurationNode<? extends ScopedConfigurationNode<?>> node = loader.load();

                    if (updateTransformation != null && !node.virtual()) { // we only want to migrate existing data
                        final @NotNull ComparableVersion startVersion = updateTransformation.version(node);
                        updateTransformation.apply(node);
                        final @NotNull ComparableVersion endVersion = updateTransformation.version(node);
                        if (startVersion != endVersion) { // we might not have made any changes
                            plugin.getComponentLogger().debug("Updated config schema for {} from {} to {}", featureType.getFeatureKey(), startVersion, endVersion);
                            wasModified = true;
                        }
                    }

                    configData = node.get(typeToken);
                } catch (final @NotNull ConfigurateException e) {
                    plugin.getComponentLogger().warn("failed to load config for {}", getFeatureType().getFeatureKey(), e);
                    result.completeExceptionally(e);
                    return;
                }

                if (configData.configVersion.compareTo(expectedVersion) > 0) {
                    plugin.getComponentLogger().warn("Version for feature " + featureType.getFeatureKey() + " higher than expected: " + expectedVersion + " got: " + configData.configVersion);
                    result.completeExceptionally(new VersionMissMatchException("Version higher than expected: " + expectedVersion + " got: " + configData.configVersion));
                    return;
                }

                // save our potentially updated or missing config to disk after we are done
                if (wasModified) {
                    result.thenRun(this::saveConfig);
                }

                 if (configData.isEnabled != wasEnabled) {
                     Bukkit.getScheduler().runTask(plugin, () -> {
                         final @Nullable AFeature<?> feature = plugin.getFeatureRegistry().getFeature(featureType);

                         if (feature != null) {
                             if (configData.isEnabled) {
                                 feature.onEnable();
                             } else {
                                 feature.onDisable();
                             }
                             result.complete(null);
                         } else {
                             plugin.getComponentLogger().error("Feature Config for type " + featureType.getFeatureKey() + " Tried to toggle enabled status, but the feature wasn't registered! Was Async reloading behind on time?");
                             result.completeExceptionally(new IllegalStateException("Couldn't toggle enabled status because the feature wasn't registered!"));
                         }
                     });
                 } else {
                     result.complete(null);
                 }
            }
        });

        // don't try to work in a broken state
        return result.whenComplete((ignored, ex) -> {
            if (ex != null) {
                plugin.getFeatureRegistry().unregisterFeature(getFeatureType());
                plugin.getComponentLogger().warn("Disabling feature " + getFeatureType().getFeatureKey() + " because reloading it failed!", ex);
            }
        });
    }

    /**
     * Accessing the config should happen async.
     * In most cases after saving reloading the memory values is in order,
     * and the returned CompletableFuture should be completed right after saving was done.
     */
    protected @NotNull CompletableFuture<Void> saveConfig() {
        final @NotNull CompletableFuture<Void> result = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                try {
                    loader.save(loader.createNode().set(typeToken, configData));
                } catch (final @NotNull ConfigurateException e) {
                    plugin.getComponentLogger().error("Could not save config for feature {}. Data may be lost!", featureType.getFeatureKey(), e);

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
