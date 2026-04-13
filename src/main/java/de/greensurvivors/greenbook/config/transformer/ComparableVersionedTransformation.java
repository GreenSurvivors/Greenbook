package de.greensurvivors.greenbook.config.transformer;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Implements a number of child ConfigurationTransformations which are only applied if required,
 * according to the configurations current version.
 *
 * @see org.spongepowered.configurate.transformation.VersionedTransformation
 */
public class ComparableVersionedTransformation implements IComparableVersioned {
    private final @NotNull NodePath versionPath;
    private final @NotNull NavigableMap<ComparableVersion, ConfigurationTransformation> versionTransformations;

    ComparableVersionedTransformation(final @NotNull NodePath versionPath,
                                      final @NotNull NavigableMap<@NotNull ComparableVersion, @NotNull ConfigurationTransformation> versionTransformations) {
        this.versionPath = versionPath;
        this.versionTransformations = versionTransformations;
    }

    @Override
    public void apply(final ConfigurationNode node) throws ConfigurateException {
        @Nullable ConfigurateException thrown = null;
        final ConfigurationNode versionNode = node.node(this.versionPath);
        @NotNull ComparableVersion currentVersion = versionNode.get(ComparableVersion.class, VERSION_UNKNOWN);

        for (Map.Entry<ComparableVersion, ConfigurationTransformation> entry : this.versionTransformations.entrySet()) {
            if (entry.getKey().compareTo(currentVersion) <= 0) {
                continue;
            }
            try {
                entry.getValue().apply(node);
            } catch (final ConfigurateException ex) {
                if (thrown == null) {
                    thrown = ex;
                } else {
                    thrown.addSuppressed(ex);
                }
            }
            currentVersion = entry.getKey();
        }

        if (thrown != null) {
            throw thrown;
        }

        versionNode.set(currentVersion);
    }

    @Override
    public @NotNull NodePath versionKey() {
        return this.versionPath;
    }

    @Override
    public @NotNull ComparableVersion latestVersion() {
        return this.versionTransformations.lastKey();
    }

    static final class ComparableVersionedBuilder {
        private @NotNull NodePath versionKey = NodePath.path("version");
        private final @NotNull NavigableMap<@NotNull ComparableVersion, @NotNull ConfigurationTransformation> versions = new TreeMap<>();

        ComparableVersionedBuilder() {
        }

        /**
         * Sets the path of the version key within the configuration.
         *
         * @param versionKey the path to the version key
         * @return this builder (for chaining)
         */
        public ComparableVersionedBuilder versionKey(final Object... versionKey) {
            this.versionKey = NodePath.of(versionKey);
            return this;
        }

        /**
         * Adds a transformation to this builder for the given version.
         *
         * <p>The version must be between 0 and {@link Integer#MAX_VALUE}, and a version cannot be specified multiple times.
         *
         * @param version        the version
         * @param transformation the transformation
         * @return this builder (for chaining)
         */
        public @NotNull ComparableVersionedBuilder addVersion(final @NotNull ComparableVersion version,
                                                              final @NotNull ConfigurationTransformation transformation) {
            if (version.compareTo(IComparableVersioned.VERSION_UNKNOWN) <= 0) {
                throw new IllegalArgumentException("Version must be at greater than " + IComparableVersioned.VERSION_UNKNOWN + ".");
            }
            if (this.versions.putIfAbsent(version, requireNonNull(transformation, "transformation")) != null) {
                throw new IllegalArgumentException("Version '" + version + "' has been specified multiple times.");
            }
            return this;
        }

        /**
         * Adds a new series of transformations for a version.
         *
         * <p>The version must be between 0 and {@link Integer#MAX_VALUE}.
         *
         * @param version         the version
         * @param transformations the transformations. To perform a version
         *                        upgrade, these transformations will be
         *                        executed in order.
         * @return this builder
         */
        public @NotNull ComparableVersionedBuilder addVersion(final @NotNull ComparableVersion version,
                                                              final @NotNull ConfigurationTransformation... transformations) {
            return this.addVersion(version, ConfigurationTransformation.chain(transformations));
        }

        /**
         * Create and add a new transformation to this builder.
         *
         * <p>The transformation will be created from the builder passed to
         * the callback function</p>
         *
         * <p>The version must be between 0 and {@link Integer#MAX_VALUE}
         *
         * @param version the version
         * @param maker   the transformation
         * @return this builder
         */
        public @NotNull ComparableVersionedBuilder makeVersion(final @NotNull ComparableVersion version,
                                                               final @NotNull Consumer<? super Builder> maker) {
            final ConfigurationTransformation.Builder builder = ConfigurationTransformation.builder();
            maker.accept(builder);
            return this.addVersion(version, builder.build());
        }

        /**
         * Builds the transformation.
         *
         * @return the transformation
         */
        public @NotNull ComparableVersionedTransformation build() {
            if (this.versions.isEmpty()) {
                throw new IllegalArgumentException("At least one version must be specified to build a transformation");
            }
            return new ComparableVersionedTransformation(this.versionKey, this.versions);
        }
    }
}
