package de.greensurvivors.greenbook.features.lift;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfig;
import de.greensurvivors.greenbook.config.ConfigOption;
import de.greensurvivors.greenbook.features.FeatureType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiftConfig extends AFeatureConfig {
    private final @NotNull ConfigOption<@NotNull Pattern> DESTINATION_PATTERN = new ConfigOption<>("destinationPattern", Pattern.compile("^(?i)\\s*to\\s*:\\s*(?<floorName>.*?)\\s*$"));

    protected LiftConfig(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.LIFT, new YamlConfiguration(), ".yml", new ComparableVersion("1.0.0"));
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
     * Returns a new instance of the DestinationMatcher class with the given destination name.
     * The names does specify the expected destination name.
     *
     * @param  destinationName  the name of the destination component
     * @return                   a new instance of the DestinationMatcher class
     */
    @Contract(value = "_ -> new", pure = true)
    protected @NotNull DestinationMatcher getDeDestinationMatcher(@NotNull Component destinationName) {
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
         * @param  expectedDestination  the name of the destination component
         */
        protected DestinationMatcher (@NotNull Component expectedDestination) {
            matcher = DESTINATION_PATTERN.getValueOrFallback().matcher(PlainTextComponentSerializer.plainText().serialize(expectedDestination));
        }

        /**
         * Determines if the given line is a destination, based on the line given in the constructor
         * @param destinationName the line to check for destination name
         * @return true, if an expected destination was defined and the destinationName matches it,
         * true, if no expected destination was defined, false otherwise
         */
        protected boolean isDestination (@NotNull Component destinationName) {
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
}
