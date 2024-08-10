package de.greensurvivors.greenbook.features.lift;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.regex.Pattern;

public enum LiftType { // todo move label logic to config
    //tps up
    UP("[Lift Up]"),
    //tps down
    DOWN("[Lift Down]"),
    //tps up or down, depending on where the player looked, defaults to down
    BOTH("[Lift UpDown]"),
    //just let other signs tp to this floor
    STOP("[Lift]");

    private static volatile EnumSet<LiftType> cachedValues = null; // lazy init to let every LiftType load in first
    //sting representation
    private volatile @NotNull Component label;
    //pattern to easy match the label against a string
    private volatile @NotNull Pattern pattern;

    LiftType(final @NotNull String rawLabel) {
        // we don't use setLabel here because the methode is synchronized and that could lead to some racing conditions
        this.label = MiniMessage.miniMessage().deserialize(rawLabel);
        //case-insensitive regex with all special characters escaped; nothing surrounding the label but optional whitespace
        this.pattern = Pattern.compile(String.format("^\\s*(?i)%s\\s*$",
            Pattern.quote(MiniMessage.miniMessage().stripTags(rawLabel))));
    }

    /**
     * Get the lift type from the component. Ignores text decorations
     *
     * @param label The line
     * @return The lift type, or null if no with the given component was defined
     */
    public synchronized static @Nullable LiftType fromLabel(final @NotNull Component label) {
        final String lineStr = PlainTextComponentSerializer.plainText().serialize(label);

        if (cachedValues == null) {
            cachedValues = EnumSet.allOf(LiftType.class);
        }

        for (LiftType liftType : cachedValues) {
            if (liftType.matchPattern(lineStr)) {
                return liftType;
            }
        }

        return null;
    }

    private synchronized boolean matchPattern(final @NotNull String str) {
        return pattern.matcher(str).matches();
    }

    public synchronized @NotNull Component getLabel() {
        return label;
    }

    public synchronized void setLabel(final @NotNull String rawLabel) {
        this.label = MiniMessage.miniMessage().deserialize(rawLabel);
        //case-insensitive regex with all special characters escaped; nothing surrounding the label but optional whitespace
        this.pattern = Pattern.compile(String.format("^\\s*(?i)%s\\s*$",
            Pattern.quote(MiniMessage.miniMessage().stripTags(rawLabel))));
    }
}
