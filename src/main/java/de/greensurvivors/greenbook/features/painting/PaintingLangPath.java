package de.greensurvivors.greenbook.features.painting;

import de.greensurvivors.greenbook.language.LangPath;
import org.jetbrains.annotations.NotNull;

enum PaintingLangPath implements LangPath { // todo
    // painting subcommand
    CMD_SUB_PAINTING_RANGE_SUCCESS("cmd.sub.painting.range.success"),
    CMD_SUB_PAINTING_RANGE_HElP("cmd.sub.painting.range.help"),
    // feature
    PAINTING_EDIT_OUTSIDE_RANGE("painting.edit.outside_range"),
    PAINTING_EDIT_STOPPED("painting.edit.stop"),
    PAINTING_EDIT_IN_USE("painting.edit.in_use"),
    PAINTING_EDIT_START("painting.edit.start");

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    PaintingLangPath(@NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    PaintingLangPath(@NotNull String path, @NotNull String defaultValue) {
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
