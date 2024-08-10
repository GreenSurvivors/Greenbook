package de.greensurvivors.greenbook.language;

import org.jetbrains.annotations.NotNull;

public interface LangPath {
    @NotNull
    String getPath();

    @NotNull
    String getDefaultValue();
}
