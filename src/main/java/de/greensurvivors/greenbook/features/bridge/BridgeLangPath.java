package de.greensurvivors.greenbook.features.bridge;

import de.greensurvivors.greenbook.language.LangPath;
import org.jetbrains.annotations.NotNull;

public enum BridgeLangPath implements LangPath { // todo
    BRIDGE_NOT_CARDINAL("bridge.error.not_cardinal"),
    BRIDGE_CREATE_SUCCESS("bridge.create.success");

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    BridgeLangPath(@NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    BridgeLangPath(@NotNull String path, @NotNull String defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    @Override
    public @NotNull String getPath() {
        return path;
    }

    @Override
    public @NotNull String getDefaultValue() {
        return defaultValue;
    }
}
