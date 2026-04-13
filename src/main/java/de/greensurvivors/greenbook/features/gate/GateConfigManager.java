package de.greensurvivors.greenbook.features.gate;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfigData;
import de.greensurvivors.greenbook.config.AYamlFeatureConfigManager;
import de.greensurvivors.greenbook.features.FeatureType;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.PostProcess;

import java.util.HashSet;
import java.util.Set;

public class GateConfigManager extends AYamlFeatureConfigManager<GateConfigManager.GateConfigData> { // todo cooldown (especially for redstone activation)

    protected GateConfigManager(final @NotNull GreenBook plugin) {
        super(plugin, FeatureType.GATE, TypeToken.get(GateConfigData.class));
    }

    @Contract(value = "null -> false")
    public boolean isGate(final @Nullable Component line) {
        if (line == null) {
            return false;
        }

        return PlainTextComponentSerializer.plainText().serialize(line). // get string from component without any format
            equalsIgnoreCase(configData.signLabelRaw); // get expected string without format
    }

    public @NotNull Component getLabel() {
        return configData.signLabel;
    }

    protected boolean isGateBlock(final @NotNull BlockData blockData) {
        for (BlockData allowedBlock : configData.allowedGateBlocks) {
            if (blockData.matches(allowedBlock)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isEmptyBlock(final @NotNull BlockData blockData) {
        for (final @NotNull BlockData allowedBlock : configData.allowedReplaceableBlocks) {
            if (blockData.matches(allowedBlock)) {
                return true;
            }
        }

        return false;
    }

    public int getMaxArea() {
        return configData.maxArea;
    }

    @ConfigSerializable
    protected static class GateConfigData extends AFeatureConfigData {
        protected final @NotNull Set<@NotNull BlockData> allowedGateBlocks = new HashSet<>();
        protected @NotNull Set<@NotNull BlockData> allowedReplaceableBlocks = new HashSet<>();
        protected @NotNull Component signLabel = Component.text("[Gate]");
        protected transient @MonotonicNonNull String signLabelRaw = "[Gate]";
        protected int maxArea = 100;

        @PostProcess
        protected void deserializeLabel() {
            signLabelRaw = PlainTextComponentSerializer.plainText().serialize(signLabel);
        }
    }
}
