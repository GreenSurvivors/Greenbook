package de.greensurvivors.greenbook.features.gate;

import de.greensurvivors.greenbook.language.LangPath;
import org.jetbrains.annotations.NotNull;

public enum GateLangPath implements LangPath { // todo
    GATE_CREATE_SUCCESS("gate.create.success"),
    GATE_ERROR_NO_FRAME("gate.error.no-frame");

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    GateLangPath(@NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    GateLangPath(@NotNull String path, @NotNull String defaultValue) {
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
