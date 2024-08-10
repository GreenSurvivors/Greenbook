package de.greensurvivors.greenbook.features.gate;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfig;
import de.greensurvivors.greenbook.config.ConfigOption;
import de.greensurvivors.greenbook.features.FeatureType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GateConfig extends AFeatureConfig { // todo cooldown (especially for redstone activation)
    private final @NotNull ConfigOption<Set<BlockData>> ALLOWED_GATE_BLOCKS = new ConfigOption<>("allowedGateBlocks", ConcurrentHashMap.newKeySet());
    private final @NotNull ConfigOption<Set<BlockData>> ALLOWED_REPLACEABLE_BLOCKS = new ConfigOption<>("allowedReplaceableBlocks", ConcurrentHashMap.newKeySet());
    private final @NotNull ConfigOption<TextComponent> GATE_LABEL = new ConfigOption<>("sign_label", Component.text("[Gate]"));
    private final @NotNull ConfigOption<Integer> MAX_AREA = new ConfigOption<>("maxArea", 100);

    protected GateConfig(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.BRIDGE,  new YamlConfiguration(), ".yml", new ComparableVersion("1.0.0"));
    }

    @Override
    protected void reloadConfig() {
    }

    @Override
    protected @NotNull CompletableFuture<Void> saveConfig() {
        CompletableFuture<Void> result = new CompletableFuture<>();

        return result;
    }

    @Contract(value = "null -> false")
    public boolean isGate(@Nullable Component line) {
        if (line == null) {
            return false;
        }



        return PlainTextComponentSerializer.plainText().serialize(line). // get string from component without any format
            equalsIgnoreCase(
                GATE_LABEL.getValueOrFallback().content() // get expected string without format
            );
    }

    public @NotNull Component getLabel() {
        return GATE_LABEL.getValueOrFallback();
    }

    protected boolean isGateBlock(@NotNull BlockData blockData) {
        for (BlockData allowedBlock : ALLOWED_GATE_BLOCKS.getValueOrFallback()) {
            if (blockData.matches(allowedBlock)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isEmptyBlock(@NotNull BlockData blockData) {
        for (BlockData allowedBlock : ALLOWED_REPLACEABLE_BLOCKS.getValueOrFallback()) {
            if (blockData.matches(allowedBlock)) {
                return true;
            }
        }

        return false;
    }

    public int getMaxArea() {
        return MAX_AREA.getValueOrFallback();
    }
}
