package de.greensurvivors.greenbook.features.lift;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfigData;
import de.greensurvivors.greenbook.config.AYamlFeatureConfigManager;
import de.greensurvivors.greenbook.features.FeatureType;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.PostProcess;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiftConfigManager extends AYamlFeatureConfigManager<LiftConfigManager.LiftConfigData> {

    protected LiftConfigManager(final @NotNull GreenBook plugin) {
        super(plugin, FeatureType.LIFT, TypeToken.get(LiftConfigData.class));
    }

    /**
     * Get the lift type from the component. Ignores text decorations
     *
     * @param label The line
     * @return The lift type, or null if no with the given component was defined
     */
    public @Nullable LiftType fromLabel(final @NotNull Component label) {
        final String lineStr = PlainTextComponentSerializer.plainText().serialize(label);

        if (configData.up.pattern.matcher(lineStr).matches()) {
            return LiftType.UP;
        } else if (configData.down.pattern.matcher(lineStr).matches()) {
            return LiftType.DOWN;
        } else if (configData.both.pattern.matcher(lineStr).matches()) {
            return LiftType.BOTH;
        } else if (configData.stop.pattern.matcher(lineStr).matches()) {
            return LiftType.STOP;
        }

        return null;
    }

    public @NotNull Component getLabel(final @NotNull LiftType liftType) {
        return switch (liftType) {
            case UP -> configData.up.label;
            case DOWN -> configData.down.label;
            case BOTH -> configData.both.label;
            case STOP -> configData.stop.label;
        };
    }

    /**
     * Returns a new instance of the DestinationMatcher class with the given destination name.
     * The names does specify the expected destination name.
     *
     * @param destinationName the name of the destination component
     * @return a new instance of the DestinationMatcher class
     */
    @Contract(value = "_ -> new", pure = true)
    protected @NotNull DestinationMatcher getDeDestinationMatcher(final @NotNull Component destinationName) {
        return new DestinationMatcher(destinationName);
    }

    /**
     * This class is used to check if a line is a destination.
     */
    protected class DestinationMatcher {
        private final @NotNull Matcher matcher;

        /**
         * Creates a new instance of the DestinationMatcher class with the given destination name.
         * The names does specify the expected destination name.
         *
         * @param expectedDestination the name of the destination component
         */
        protected DestinationMatcher(final @NotNull Component expectedDestination) {
            matcher = configData.destinationPattern.matcher(PlainTextComponentSerializer.plainText().serialize(expectedDestination));
        }

        /**
         * Determines if the given line is a destination, based on the line given in the constructor
         *
         * @param destinationName the line to check for destination name
         * @return true, if an expected destination was defined and the destinationName matches it,
         * true, if no expected destination was defined, false otherwise
         */
        protected boolean isDestination(final @NotNull Component destinationName) {
            if (matcher.matches()) {
                final @Nullable String floorName = matcher.group("floorName");

                if (floorName != null) {
                    return floorName.equalsIgnoreCase(PlainTextComponentSerializer.plainText().serialize(destinationName).trim());
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }
    }

    @ConfigSerializable
    protected static class LiftConfigData extends AFeatureConfigData {
        @Comment("How a valid floor name, a player will be  is defined. Has to contain a group named 'floorName'.")
        protected final @NotNull Pattern destinationPattern = Pattern.compile("^(?i)\\s*to\\s*:\\s*(?<floorName>.*?)\\s*$");
        protected final @NotNull LiftTypeLabel up = new LiftTypeLabel(Component.text("[Lift Up]"));
        protected final @NotNull LiftTypeLabel down = new LiftTypeLabel(Component.text("[Lift Down]"));
        protected final @NotNull LiftTypeLabel both = new LiftTypeLabel(Component.text("[Lift UpDown]"));
        protected final @NotNull LiftTypeLabel stop = new LiftTypeLabel(Component.text("[Lift]"));
    }

    // no version associated here, since this is just a string from the config file point of view.
    @ConfigSerializable
    protected static class LiftTypeLabel {
        @Setting(nodeFromParent = true) // since this is the only exposed option, no need to write a 'label' node to config.
        protected @MonotonicNonNull Component label;
        protected transient @MonotonicNonNull Pattern pattern;

        protected LiftTypeLabel(final @NotNull Component label) {
            this.label = label;
            unpack();
        }

        // private constructor for configurate
        @ApiStatus.Internal
        private LiftTypeLabel() {}

        protected static @NotNull Pattern buildLabelPattern(@NotNull String rawPatternStr) {
            //case-insensitive regex with all special characters escaped; nothing surrounding the label but optional whitespace
            return Pattern.compile(String.format("^\\s*(?i)%s\\s*$",
                Pattern.quote(MiniMessage.miniMessage().stripTags(rawPatternStr))));
        }

        @PostProcess
        protected void unpack() {
            final @NotNull PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

            pattern = buildLabelPattern(serializer.serialize(label));
        }
    }
}
