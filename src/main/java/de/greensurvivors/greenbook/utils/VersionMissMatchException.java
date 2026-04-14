package de.greensurvivors.greenbook.utils;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.serialize.SerializationException;

public class VersionMissMatchException extends SerializationException {
    public VersionMissMatchException(final @NotNull String message) {
        super(message);
    }
}
