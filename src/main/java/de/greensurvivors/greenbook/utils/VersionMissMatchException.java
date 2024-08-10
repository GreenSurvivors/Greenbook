package de.greensurvivors.greenbook.utils;

import org.jetbrains.annotations.NotNull;

public class VersionMissMatchException extends IllegalArgumentException {
    public VersionMissMatchException(@NotNull String message) {
        super(message);
    }

    public VersionMissMatchException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

    public VersionMissMatchException(@NotNull Throwable cause) {
        super(cause);
    }
}
