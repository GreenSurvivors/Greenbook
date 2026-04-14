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
import org.spongepowered.configurate.objectmapping.meta.Comment;

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
        for (final @NotNull BlockData allowedBlock : configData.allowedBridgeBlocks) {
            if (blockData.matches(allowedBlock)) {
                return true;
            }
        }

        return false;
    }

    protected @Nullable BlockFace getExpectedBridgeDirectionFromComponent(final @NotNull Component line) {
        return null; // todo
    }

    protected @NotNull Component getExpectedBridgeDirectionLabel(final @NotNull BlockFace blockFace) {
        return null; // todo
    }

    @ConfigSerializable
    protected static class BridgeConfigData extends AFeatureConfigData { // todo ponder about Tags
        @Comment("""
            The Blocks a bridge can created out of.
            Block data (spigot name) aka the block state is defined by https://minecraft.wiki/w/Block_states
            syntax is block_id[block_states]{data_tags} as defined by https://minecraft.wiki/w/Argument_types#block_state""")
        protected final @NotNull Set<@NotNull BlockData> allowedBridgeBlocks = new HashSet<>();
    }
}
