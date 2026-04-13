package de.greensurvivors.greenbook.config.transformer;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

/**
 * A transformation that is aware of node versions.
 *
 * @see org.spongepowered.configurate.transformation.ConfigurationTransformation.Versioned
 */
interface IComparableVersioned extends ConfigurationTransformation {

    /**
     * Indicates a node with an unknown version.
     *
     * <p>This can be returned as the latest version.</p>
     *
     */
    @NotNull ComparableVersion VERSION_UNKNOWN = new ComparableVersion("-1.0.0");

    /**
     * Get the path the node's current version is located at.
     *
     * @return version path
     */
    @NotNull NodePath versionKey();

    /**
     * Get the latest version that nodes can be updated to.
     *
     * @return the most recent version
     */
    @NotNull ComparableVersion latestVersion();

    /**
     * Get the version of a node hierarchy.
     *
     * <p>Note that the node checked here must be the same node passed to
     * {@link #apply(ConfigurationNode)}, not any node in a hierarchy.
     *
     * <p>If the node value is not present or not coercible to an integer,
     * {@link #VERSION_UNKNOWN} will be returned. When the transformation is
     * executed, every version transformation will be applied.
     *
     * @param node node to check
     * @return version, or {@link #VERSION_UNKNOWN} if no value is present
     */
    default @NotNull ComparableVersion version(final ConfigurationNode node) throws SerializationException {
        return node.node(this.versionKey()).get(ComparableVersion.class, VERSION_UNKNOWN);
    }
}
