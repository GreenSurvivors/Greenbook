package de.greensurvivors.greenbook.features.bridge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.regex.Pattern;

public enum BridgeType {
    BRIDGE("[Bridge]"),
    BRIDGE_END("[Bridge End]");

    //sting representation
    private volatile @NotNull Component label;
    //pattern to easy match the label against a string
    private volatile @NotNull Pattern pattern;
    private static volatile EnumSet<BridgeType> cachedValues = null; // lazy init to let every BridgeType load in first

    BridgeType(final @NotNull String rawLabel) {
        // we don't use setLabel here because the methode is synchronized and that could lead to some racing conditions
        this.label = MiniMessage.miniMessage().deserialize(rawLabel);
        //case-insensitive regex with all special characters escaped; nothing surrounding the label but optional whitespace
        this.pattern = Pattern.compile(String.format("^\\s*(?i)%s\\s*$",
            Pattern.quote(MiniMessage.miniMessage().stripTags(rawLabel))));
    }

    public synchronized void setLabel(final @NotNull String rawLabel) {
        this.label = MiniMessage.miniMessage().deserialize(rawLabel);
        //case-insensitive regex with all special characters escaped; nothing surrounding the label but optional whitespace
        this.pattern = Pattern.compile(String.format("^\\s*(?i)%s\\s*$",
            Pattern.quote(MiniMessage.miniMessage().stripTags(rawLabel))));
    }

    private synchronized boolean matchPattern(final @NotNull String str) {
        return pattern.matcher(str).matches();
    }

    public synchronized @NotNull Component getLabel() {
        return label;
    }

    /**
     * Get the bridge type from the component. Ignores text decorations
     *
     * @param label The line
     * @return The bridge type, or null if no with the given component was defined
     */
    public synchronized static @Nullable BridgeType fromLabel(final @NotNull Component label) {
        final String lineStr = PlainTextComponentSerializer.plainText().serialize(label);

        if (cachedValues == null) {
            cachedValues = EnumSet.allOf(BridgeType.class);
        }

        for (BridgeType bridgeType : cachedValues) {
            if (bridgeType.matchPattern(lineStr)) {
                return bridgeType;
            }
        }

        return null;
    }
}
