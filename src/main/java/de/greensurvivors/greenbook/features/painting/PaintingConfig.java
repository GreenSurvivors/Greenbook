package de.greensurvivors.greenbook.features.painting;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfig;
import de.greensurvivors.greenbook.config.ConfigOption;
import de.greensurvivors.greenbook.features.FeatureType;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class PaintingConfig extends AFeatureConfig {
    private final @NotNull ConfigOption<@NotNull Integer> RANGE = new ConfigOption<>("editing_range", 16);

    protected PaintingConfig(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.PAINTING, new YamlConfiguration(), ".yml", new ComparableVersion("1.0.0"));
    }

    @Override
    protected void reloadConfig() {

    }

    @Override
    protected @NotNull CompletableFuture<Void> saveConfig() {
        CompletableFuture<Void> result = new CompletableFuture<>();

        return result;
    }

    public void setPaintingModifyRange(final int newRange) {
        RANGE.setValue(newRange);
        saveConfig();
        reloadConfig();
    }

    public double getModifyRangeSqr() {
        int range_ = RANGE.getValueOrFallback();
        return range_ * range_;
    }
}
