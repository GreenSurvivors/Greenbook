package de.greensurvivors.greenbook.config;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public abstract class AFeatureConfigData {
    @Comment("""
        Internal data version.
        Used to migrate changes in data structure.
        If you are so touch starved to change this, please go and find some grass.
        """)
    protected @NotNull ComparableVersion configVersion = new ComparableVersion("1.0.0");
    @Comment("Whenever this feature is enabled.")
    protected boolean isEnabled = true;
}
