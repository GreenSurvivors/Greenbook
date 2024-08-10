package de.greensurvivors.greenbook.features.wireless;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfig;
import de.greensurvivors.greenbook.config.ConfigOption;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.features.wireless.network.WirelessNodeType;
import de.greensurvivors.greenbook.utils.VersionMissMatchException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class WirelessConfig extends AFeatureConfig {
    // wirelessNodeType

    private final static @NotNull String NODE_SETTINGS_KEY = "wirelessNode_settings";
    private final @NotNull ConfigOption<WirelessNodeTypeSettings> RECEIVER_SETTINGS = new ConfigOption<>("receiver",
        new WirelessConfig.WirelessNodeTypeSettings(Component.text("[Mc1111]"), Pattern.compile("^\\s*(?i)\\[Mc1111]\\s*S?\\s*$"), Component.text("Repeater")));
    private final @NotNull ConfigOption<WirelessNodeTypeSettings> TRANSMITTER_SETTINGS = new ConfigOption<>("receiver",
        new WirelessConfig.WirelessNodeTypeSettings(Component.text("[Mc1110]"), Pattern.compile("^\\s*(?i)\\[Mc1110]\\s*S?\\s*$"), Component.text("Transmitter")));
    private final @NotNull ConfigOption<@NotNull Component> INVALID_DISPLAY_NAME = new ConfigOption<>("display-name", Component.text("ERROR"));
    private final @NotNull ConfigOption<@NotNull Boolean> USE_PLAYER_CHANNELS = new ConfigOption<>("use-player-channels", Boolean.TRUE);

    protected WirelessConfig(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.WIRELESS, new YamlConfiguration(), ".yml", new ComparableVersion("1.0.0"));
    }

    @Override
    protected void reloadConfig() {



        /*
        Map<String, ?> nodesSettings;
        try {
             nodesSettings = ConfigManager.getMapFromConfigObject(config.get(NODE_SETTINGS_KEY), obj -> {
                plugin.getComponentLogger().warn("Could not read setting for wireless node: " + obj);
                return false;
            });
        } catch (NullPointerException | IllegalArgumentException e) {

        }

        for (Map.Entry<String, ?> nodeSettingsEntry : nodesSettings.entrySet()) {
            WirelessNodeType nodeType = Utils.getEnumIgnoreCase(WirelessNodeType.class, nodeSettingsEntry.getKey());

            if (nodeType != null) {
                Map<String, ?> specificNodeSettings;
                try {
                    specificNodeSettings = ConfigManager.getMapFromConfigObject(nodeSettingsEntry.getValue(), obj -> {
                        plugin.getComponentLogger().warn("Could not read setting entry for wireless node: " + nodeSettingsEntry.getKey());
                       return false;
                    });
                } catch (NullPointerException | IllegalArgumentException e) {

                    continue;
                }

                WirelessNodeType result;

                switch (specificNodeSettings.get(TYPE)) {
                    case WirelessNodeType type -> result = type;
                    case String str -> result =  Utils.getEnum(WirelessNodeType.class, str);
                    default -> throw new IllegalArgumentException("Unknown WirelessNodeType " + map.get(TYPE));
                }



            } else {
                plugin.getComponentLogger().warn("unknown wirelessNodeType : " + nodeSettingsEntry.getKey());
            }

        }*/


        // todo
    }

    @Override
    protected @NotNull CompletableFuture<Void> saveConfig() {
        final CompletableFuture<Void> result = new CompletableFuture<>();

        return result;
    }

    //configurates if every player should have their own channel based on their uuid,
    //or if everyone should use the global one
    public boolean usePlayerSpecificChannels() {
        return USE_PLAYER_CHANNELS.getValueOrFallback();
    }

    /**
     * Returns the display name component for the current WirelessNodeType.
     * If the current type is {@link WirelessNodeType#NONE}, an error component is returned instead,
     * as it is an invalid value.
     *
     * @return The display name component for the current WirelessNodeType,
     * or an error component if the current type is {@link WirelessNodeType#NONE}.
     */
    public @NotNull Component getLabel(@NotNull WirelessNodeType nodeType) {
        return switch (nodeType) {
            case RECEIVER -> RECEIVER_SETTINGS.getValueOrFallback().displayLabel.getValueOrFallback();
            case TRANSMITTER -> TRANSMITTER_SETTINGS.getValueOrFallback().displayLabel.getValueOrFallback();
            case NONE -> INVALID_DISPLAY_NAME.getValueOrFallback();
        };
    }

    public WirelessNodeType fromID(@NotNull Component line) {
        final @NotNull String strLine = PlainTextComponentSerializer.plainText().serialize(line);

        if (RECEIVER_SETTINGS.getValueOrFallback().idPattern.getValueOrFallback().matcher(strLine).matches()) {
            return WirelessNodeType.RECEIVER;
        }
        if (TRANSMITTER_SETTINGS.getValueOrFallback().idPattern.getValueOrFallback().matcher(strLine).matches()) {
            return WirelessNodeType.TRANSMITTER;
        }

        return WirelessNodeType.NONE;
    }

    public @NotNull Component getID(@NotNull WirelessNodeType wirelessNodeType) throws IllegalArgumentException {
        return switch (wirelessNodeType) {
            case NONE ->
                throw new IllegalArgumentException("Invalid WirelessNodeType to get ID for: " + WirelessNodeType.NONE + " has no ID!");
            case RECEIVER -> RECEIVER_SETTINGS.getValueOrFallback().id.getValueOrFallback();
            case TRANSMITTER -> TRANSMITTER_SETTINGS.getValueOrFallback().displayLabel.getValueOrFallback();
        };
    }

    public static class WirelessNodeTypeSettings implements ConfigurationSerializable {
        private final static @NotNull String ID_KEY = "id";
        private final @NotNull ConfigOption<@NotNull Component> id;
        //pattern to easy match the label against a string
        private final static @NotNull String ID_PATTERN_KEY = "labelPattern";
        private final@NotNull ConfigOption<@NotNull Pattern> idPattern;
        // user friendly name
        private final static @NotNull String DISPLAY_NAME_KEY = "displayName";
        private final @NotNull ConfigOption<@NotNull Component> displayLabel;
        // version in case we ever need a change in the way data is serialized, so we can do a DFU
        private final static @NotNull ConfigOption<@NotNull ComparableVersion> VERSION = new ConfigOption<>("version", new ComparableVersion("1.0.0"));

        static {
            ConfigurationSerialization.registerClass(WirelessNodeTypeSettings.class);
        }

        public WirelessNodeTypeSettings(@NotNull Component id, @NotNull Pattern idPattern, @NotNull Component displayLabel) {
            this.id = new ConfigOption<>(ID_KEY, id);
            this.idPattern = new ConfigOption<>(ID_PATTERN_KEY, idPattern);
            this.displayLabel = new ConfigOption<>(DISPLAY_NAME_KEY, displayLabel);
        }

        public static @NotNull WirelessNodeTypeSettings deserialize(Map<String, Object> rawMap) throws IllegalArgumentException {
            if (rawMap.get(VERSION.getPath()) instanceof String versionStr) {
                if (new ComparableVersion(versionStr).compareTo(VERSION.getValueOrFallback()) > 0) {
                    throw new VersionMissMatchException("");
                }
            }

            Component id;
            if (rawMap.get(ID_KEY) instanceof String rawID) {
                id = MiniMessage.miniMessage().deserialize(rawID);
            } else {
                throw new IllegalArgumentException("");
            }

            Pattern pattern;
            if (rawMap.get(ID_PATTERN_KEY) instanceof String rawPattern) {
                pattern = Pattern.compile(rawPattern);
            } else {
                throw new IllegalArgumentException("");
            }

            Component displayName;
            if (rawMap.get(ID_KEY) instanceof String rawDisplayName) {
                displayName = MiniMessage.miniMessage().deserialize(rawDisplayName);
            } else {
                throw new IllegalArgumentException("");
            }

            return new WirelessNodeTypeSettings(id, pattern, displayName);
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            return Map.of(
                ConfigurationSerialization.SERIALIZED_TYPE_KEY, WirelessNodeTypeSettings.class.getName(),
                VERSION.getPath(), VERSION.getValueOrFallback().toString(),
                id.getPath(), MiniMessage.miniMessage().serialize(id.getValueOrFallback()),
                idPattern.getPath(), idPattern.getValueOrFallback().toString(),
                displayLabel.getPath(), MiniMessage.miniMessage().serialize(displayLabel.getValueOrFallback())
            );
        }
    }
}
