package de.greensurvivors.greenbook.config;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public abstract class AFeatureConfigData {
    protected boolean isEnabled = true;
    protected @NotNull ComparableVersion configVersion;
}
