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
import org.spongepowered.configurate.objectmapping.meta.Comment;
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
    protected static class GateConfigData extends AFeatureConfigData { // todo ponder about tags
        @Comment("""
            The blocks that are a valid part of a gate (and therefore not a valid frame block).
            Block data (spigot name) aka the block state is defined by https://minecraft.wiki/w/Block_states
            syntax is block_id[block_states]{data_tags} as defined by https://minecraft.wiki/w/Argument_types#block_state
            """)
        protected final @NotNull Set<@NotNull BlockData> allowedGateBlocks = new HashSet<>();
        @Comment("""
            The blocks a gate sees as 'empty' and can replace.
            Block data (spigot name) aka the block state is defined by https://minecraft.wiki/w/Block_states
            syntax is block_id[block_states]{data_tags} as defined by https://minecraft.wiki/w/Argument_types#block_state
            """)
        protected @NotNull Set<@NotNull BlockData> allowedReplaceableBlocks = new HashSet<>();
        @Comment("The second line of a sign that determines if a sign is part of a gate.")
        protected @NotNull Component signLabel = Component.text("[Gate]");
        protected transient @MonotonicNonNull String signLabelRaw = "[Gate]";
        @Comment("Amount of blocks a gate may span. Lower this, if gates start to cause lags!")
        protected int maxArea = 100;

        @PostProcess
        protected void deserializeLabel() {
            signLabelRaw = PlainTextComponentSerializer.plainText().serialize(signLabel);
        }
    }
}
