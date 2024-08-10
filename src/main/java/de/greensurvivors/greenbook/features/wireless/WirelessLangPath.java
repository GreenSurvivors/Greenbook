package de.greensurvivors.greenbook.features.wireless;

import de.greensurvivors.greenbook.language.LangPath;
import org.jetbrains.annotations.NotNull;

public enum WirelessLangPath implements LangPath {
    ERROR_RECEIVER_NOT_WALL("wireless.error.receiver.not_wall"),
    ERROR_NO_NETWORK("wireless.error.no_network"),
    RECEIVER_CREATED("wireless.receiver.created"),
    TRANSMITTER_CREATED("wireless.transmitter.created"),
    TRANSMITTER_REMOVED("wireless.transmitter.removed"),
    RECEIVER_REMOVED("wireless.receiver.removed");

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    WirelessLangPath(@NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    WirelessLangPath(@NotNull String path, @NotNull String defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getDefaultValue() {
        return defaultValue;
    }
}
