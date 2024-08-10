package de.greensurvivors.greenbook.features.coin;

import de.greensurvivors.greenbook.features.AFeature;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

enum CoinPermissions implements AFeature.IPermissionHolder {
    CMD_USE(new Permission(
        "greenbook.coin.cmd.use",
        "Allows to give a coin to another user.",
        PermissionDefault.TRUE)),
    CMD_SET(new Permission(
        "greenbook.coin.cmd.set",
        "Allows to set the coin item.",
        PermissionDefault.OP)),
    ADMIN(new Permission(
        "greenbook.coin.*",
        "All coin feature permissions",
        PermissionDefault.OP,
        Map.of(
            CMD_USE.getPermission().getName(), true,
            CMD_SET.permission.getName(), true
        )));

    private final @NotNull Permission permission;

    CoinPermissions(@NotNull Permission permission) {
        this.permission = permission;

        Bukkit.getPluginManager().addPermission(permission);
    }

    @Override
    public @NotNull Permission getPermission() {
        return permission;
    }
}
