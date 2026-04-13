package de.greensurvivors.greenbook.features.bridge;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfigData;
import de.greensurvivors.greenbook.config.AYamlFeatureConfigManager;
import de.greensurvivors.greenbook.features.FeatureType;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashSet;
import java.util.Set;

public class BridgeConfigManager extends AYamlFeatureConfigManager<BridgeConfigManager.BridgeConfigData> { // todo cooldown (especially for redstone activation)

    protected BridgeConfigManager(final @NotNull GreenBook plugin) {
        super(plugin, FeatureType.BRIDGE, TypeToken.get(BridgeConfigData.class));
    }

    /**
     * Checks if the given block data is allowed in the bridge.
     *
     * @param blockData the block data to check
     * @return true if the block data is allowed, false otherwise
     */
    protected boolean isAllowedBlock(@NotNull BlockData blockData) {
        for (final @NotNull BlockData allowedBlock : config.allowedBridgeBlocks) {
            if (blockData.matches(allowedBlock)) {
                return true;
            }
        }

        return false;
    }

    protected @Nullable BlockFace getExpectedBridgeDirectionFromComponent(@NotNull Component line) {
        return null; // todo
    }

    protected @NotNull Component getExpectedBridgeDirectionLabel(final @NotNull BlockFace blockFace) {
        return null; // todo
    }

    @ConfigSerializable
    protected static class BridgeConfigData extends AFeatureConfigData {
        protected final @NotNull Set<@NotNull BlockData> allowedBridgeBlocks = new HashSet<>();
    }
}
