package de.greensurvivors.greenbook.features.bridge;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfig;
import de.greensurvivors.greenbook.config.ConfigOption;
import de.greensurvivors.greenbook.features.FeatureType;
import net.kyori.adventure.text.Component;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BridgeConfig extends AFeatureConfig { // todo cooldown (especially for redstone activation)
    private final @NotNull ConfigOption<Set<BlockData>> ALLOWED_BRIDGE_BLOCKS = new ConfigOption<>("allowedBridgeBlocks", ConcurrentHashMap.newKeySet());

    protected BridgeConfig(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.BRIDGE, new YamlConfiguration(), ".yml", new ComparableVersion("1.0.0"));
    }

    @Override
    protected void reloadConfig() {

    }

    @Override
    protected @NotNull CompletableFuture<Void> saveConfig() {
        CompletableFuture<Void> result = new CompletableFuture<>();

        return result;
    }

    /**
     * Checks if the given block data is allowed in the bridge.
     *
     * @param blockData the block data to check
     * @return true if the block data is allowed, false otherwise
     */
    protected boolean isAllowedBlock(@NotNull BlockData blockData) {
        for (BlockData allowedBlock : ALLOWED_BRIDGE_BLOCKS.getValueOrFallback()) {
            if (blockData.matches(allowedBlock)) {
                return true;
            }
        }

        return false;
    }

    protected @Nullable BlockFace getExpectedBridgeDirectionFromComponent(@NotNull Component line) {
        return null; // todo
    }

    protected @NotNull Component getExpectedBridgeDirectionLabel(@NotNull BlockFace blockFace) {
        return null; // todo
    }
}
