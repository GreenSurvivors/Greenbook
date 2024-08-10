package de.greensurvivors.greenbook.features.quotes;

import de.greensurvivors.greenbook.language.LangPath;
import org.jetbrains.annotations.NotNull;

public enum QuotesLangPath implements LangPath {
    // (sub) commands
    CMD_SUB_ADD_SUCCESS("quotes.cmd.add.success"),
    CMD_SUB_GET_SUCCESS("quotes.cmd.get.success"),
    CMD_SUB_GET_UNKNOWN("quotes.cmd.get.unknown-id"),
    CMD_SUB_REMOVE_NO_ID("quotes.cmd.remove.no-id"),
    CMD_SUB_REMOVE_SUCCESS("quotes.cmd.remove.success"),
    CMD_SUB_LIST_HEADER("quotes.cmd.ist.header"),
    CMD_SUB_LIST_ENTRY("quotes.cmd.list.entry"),
    CMD_SUB_HAND_SUCCESS("quotes.cmd.hand.success"),
    CMD_SUB_SNEAK_SUCCESS("quotes.cmd.sneak.success"),
    //
    GET_QUOTE("quotes.get_quote"), // todo
    ;

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    QuotesLangPath(@NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    QuotesLangPath(@NotNull String path, @NotNull String defaultValue) {
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
