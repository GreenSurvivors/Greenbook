package de.greensurvivors.greenbook.features.wireless;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfigData;
import de.greensurvivors.greenbook.config.AYamlFeatureConfigManager;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.features.wireless.network.WirelessNodeType;
import de.greensurvivors.greenbook.utils.VersionMissMatchException;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
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
        return configData.usePlayerChannels;
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
            case RECEIVER -> configData.receiver.displayLabel;
            case TRANSMITTER -> configData.transmitter.displayLabel;
            case NONE -> configData.invalidDisplayLabel;
        };
    }

    public @NotNull WirelessNodeType fromID(final @NotNull Component line) {
        final @NotNull String strLine = PlainTextComponentSerializer.plainText().serialize(line);

        if (configData.receiver.idPattern.matcher(strLine).matches()) {
            return WirelessNodeType.RECEIVER;
        }
        if (configData.transmitter.idPattern.matcher(strLine).matches()) {
            return WirelessNodeType.TRANSMITTER;
        }

        return WirelessNodeType.NONE;
    }

    public @NotNull Component getID(final @NotNull WirelessNodeType wirelessNodeType) throws IllegalArgumentException {
        return switch (wirelessNodeType) {
            case NONE ->
                throw new IllegalArgumentException("Invalid WirelessNodeType to get ID for: " + WirelessNodeType.NONE + " has no ID!");
            case RECEIVER -> configData.receiver.id;
            case TRANSMITTER -> configData.transmitter.id;
        };
    }

    @ConfigSerializable
    protected static class WirelessNodeTypeSettings {
        private final static @NotNull ComparableVersion EXPECTED_VERSION = new ComparableVersion("1.0.0");
        @Comment("""
            The 2. line of the sign, the plugin corrects the user input to.
            Must match the id pattern!""")
        private @NotNull Component id;
        @Comment("Pattern to match the 2nd line of the sign. Determines the type of wireless redstone.")
        private @NotNull Pattern idPattern;
        @Comment("Unfriendly name, displayed on the first line of the sign")
        private @NotNull Component displayLabel;
        // version in case we ever need a change in the way data is serialized, so we can do a DFU
        @Comment("""
            Internal data version.
            Used to migrate changes in data structure.
            Don't touch! Or do and suffer the consequences. I'm not your real dad anyway.""")
        private final @NotNull ComparableVersion dataVersion = EXPECTED_VERSION;

        protected WirelessNodeTypeSettings(final @NotNull Component id, final @NotNull Pattern idPattern, final @NotNull Component displayLabel) {
            this.id = id;
            this.idPattern = idPattern;
            this.displayLabel = displayLabel;
        }

        // no parameter constructor for configurate
        @ApiStatus.Internal
        private WirelessNodeTypeSettings() {}

        @PostProcess
        protected void checkVersion() throws VersionMissMatchException {
            if (dataVersion.compareTo(EXPECTED_VERSION) > 0) {
                throw new VersionMissMatchException("Version higher than expected: " + EXPECTED_VERSION + " got: " + dataVersion);
            }
        }
    }

    @ConfigSerializable
    protected static class WirelessConfigData extends AFeatureConfigData {
        @Comment("What the 2. line of a sign should display, in case a receiver / transmitter is .")
        protected final @NotNull Component invalidDisplayLabel = Component.text("ERROR", NamedTextColor.DARK_RED);
        @Comment("""
            If this is true, a transmitter / receiver is 'owned' by the last player who edited the sign.
            Every player has their own network and every receiver owned by a player only outputs a signal, when triggered by a transmitter of the same player.""")
        protected final boolean usePlayerChannels = true;
        @Comment("Outputs redstone signals from the network.")
        protected final @NotNull WirelessNodeTypeSettings receiver = new WirelessConfigManager.WirelessNodeTypeSettings(
            Component.text("[Mc1111]"),
            Pattern.compile("^\\s*(?i)\\[Mc1111]\\s*S?\\s*$"),
            Component.text("Receiver"));
        @Comment("Inputs redstone signals into the network.")
        protected final @NotNull WirelessNodeTypeSettings transmitter = new WirelessConfigManager.WirelessNodeTypeSettings(
            Component.text("[Mc1110]"),
            Pattern.compile("^\\s*(?i)\\[Mc1110]\\s*S?\\s*$"),
            Component.text("Transmitter"));
    }
}
