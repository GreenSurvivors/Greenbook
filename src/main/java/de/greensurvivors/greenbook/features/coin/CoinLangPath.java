package de.greensurvivors.greenbook.features.coin;

import de.greensurvivors.greenbook.language.LangPath;
import org.jetbrains.annotations.NotNull;

enum CoinLangPath implements LangPath {
    //Coin Command
    CMD_COIN_SET_SUCCESS("cmd.coin.set.success"),
    CMD_COIN_SET_EMPTY("cmd.coin.set.empty"),
    CMD_COIN_NOT_ENOUGH("cmd.coin.not-enough-coins"),
    CMD_COIN_TOSS_OTHER("cmd.coin.toss.other"),
    CMD_COIN_TOSS_SELF("cmd.coin.toss.self"),
    CMD_COIN_DESCRIPTION("cmd.coin.description");

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    CoinLangPath(@NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    CoinLangPath(@NotNull String path, @NotNull String defaultValue) {
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
