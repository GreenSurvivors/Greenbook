package de.greensurvivors.greenbook.features.wireless;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfigData;
import de.greensurvivors.greenbook.config.AYamlFeatureConfigManager;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.features.wireless.network.WirelessNodeType;
import de.greensurvivors.greenbook.utils.VersionMissMatchException;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.PostProcess;

import java.util.regex.Pattern;

public class WirelessConfigManager extends AYamlFeatureConfigManager<WirelessConfigManager.WirelessConfigData> {

    protected WirelessConfigManager(final @NotNull GreenBook plugin) {
        super(plugin, FeatureType.WIRELESS, TypeToken.get(WirelessConfigData.class));
    }

    /**
     * if every player should have their own channel based on their uuid,
     * or if everyone should use the global one
     */
    public boolean usePlayerSpecificChannels() {
        return config.usePlayerChannels;
    }

    /**
     * Returns the display name component for the current WirelessNodeType.
     * If the current type is {@link WirelessNodeType#NONE}, an error component is returned instead,
     * as it is an invalid value.
     *
     * @return The display name component for the current WirelessNodeType,
     * or an error component if the current type is {@link WirelessNodeType#NONE}.
     */
    public @NotNull Component getLabel(final @NotNull WirelessNodeType nodeType) {
        return switch (nodeType) {
            case RECEIVER -> config.receiver.displayLabel;
            case TRANSMITTER -> config.transmitter.displayLabel;
            case NONE -> config.invalidDisplayLabel;
        };
    }

    public @NotNull WirelessNodeType fromID(final @NotNull Component line) {
        final @NotNull String strLine = PlainTextComponentSerializer.plainText().serialize(line);

        if (config.receiver.idPattern.matcher(strLine).matches()) {
            return WirelessNodeType.RECEIVER;
        }
        if (config.transmitter.idPattern.matcher(strLine).matches()) {
            return WirelessNodeType.TRANSMITTER;
        }

        return WirelessNodeType.NONE;
    }

    public @NotNull Component getID(final @NotNull WirelessNodeType wirelessNodeType) throws IllegalArgumentException {
        return switch (wirelessNodeType) {
            case NONE ->
                throw new IllegalArgumentException("Invalid WirelessNodeType to get ID for: " + WirelessNodeType.NONE + " has no ID!");
            case RECEIVER -> config.receiver.id;
            case TRANSMITTER -> config.transmitter.id;
        };
    }

    @ConfigSerializable
    protected static class WirelessNodeTypeSettings {
        private final static @NotNull ComparableVersion EXPECTED_VERSION = new ComparableVersion("1.0.0");
        private @NotNull Component id;
        //pattern to easy match the label against a string
        private @NotNull Pattern idPattern;
        // user-friendly name
        private @NotNull Component displayLabel;
        // version in case we ever need a change in the way data is serialized, so we can do a DFU
        private final @NotNull ComparableVersion version = EXPECTED_VERSION;

        protected WirelessNodeTypeSettings(final @NotNull Component id, final @NotNull Pattern idPattern, final @NotNull Component displayLabel) {
            this.id = id;
            this.idPattern = idPattern;
            this.displayLabel =  displayLabel;
        }

        // no parameter constructor for configurate
        private WirelessNodeTypeSettings() {}

        @PostProcess
        protected void checkVersion() {
            if (version.compareTo(EXPECTED_VERSION) > 0) {
                throw new VersionMissMatchException("");
            }
        }
    }

    @ConfigSerializable
    protected static class WirelessConfigData extends AFeatureConfigData {
        protected final @NotNull Component invalidDisplayLabel = Component.text("ERROR");
        protected final boolean usePlayerChannels = true;
        protected final @NotNull WirelessNodeTypeSettings  receiver = new WirelessConfigManager.WirelessNodeTypeSettings(
            Component.text("[Mc1111]"),
            Pattern.compile("^\\s*(?i)\\[Mc1111]\\s*S?\\s*$"),
            Component.text("Repeater"));
        protected final @NotNull WirelessNodeTypeSettings transmitter = new WirelessConfigManager.WirelessNodeTypeSettings(
            Component.text("[Mc1110]"),
            Pattern.compile("^\\s*(?i)\\[Mc1110]\\s*S?\\s*$"),
            Component.text("Transmitter"));
    }
}
