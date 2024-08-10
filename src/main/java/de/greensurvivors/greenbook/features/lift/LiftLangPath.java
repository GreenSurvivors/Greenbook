package de.greensurvivors.greenbook.features.lift;

import de.greensurvivors.greenbook.language.LangPath;
import org.jetbrains.annotations.NotNull;

enum LiftLangPath implements LangPath {// todo
    LIFT_USED_STOP("lift.used.stop"),
    LIFT_USED_UP("lift.used.up"),
    LIFT_USED_DOWN("lift.used.down"),
    LIFT_USED_FLOOR("lift.used.floor"),
    LIFT_CREATE_SUCCESS("lift.create.success"),
    LIFT_DESTINATION_UNKNOWN("lift.destination.unknown"),
    LIFT_DESTINATION_OBSTRUCTED("lift.destination.obstructed");

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    LiftLangPath(@NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    LiftLangPath(@NotNull String path, @NotNull String defaultValue) {
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
