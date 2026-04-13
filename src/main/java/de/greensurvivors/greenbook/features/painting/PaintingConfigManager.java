package de.greensurvivors.greenbook.features.painting;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfigData;
import de.greensurvivors.greenbook.config.AYamlFeatureConfigManager;
import de.greensurvivors.greenbook.features.FeatureType;
import io.leangen.geantyref.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.concurrent.CompletableFuture;

public class PaintingConfigManager extends AYamlFeatureConfigManager<PaintingConfigManager.PaintingConfigData> {

    protected PaintingConfigManager(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.PAINTING, TypeToken.get(PaintingConfigData.class));
    }

    public @NotNull CompletableFuture<Void> setPaintingModifyRange(final int newRange) {
        configData.editingRange = newRange;
        return saveAndReload();
    }

    public double getModifyRangeSqr() {
        return configData.editingRange * configData.editingRange;
    }

    @ConfigSerializable
    protected static class PaintingConfigData extends AFeatureConfigData {
        protected int editingRange = 16;
    }
}
