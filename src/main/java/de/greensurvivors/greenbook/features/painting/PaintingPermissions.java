package de.greensurvivors.greenbook.features.painting;

import de.greensurvivors.greenbook.features.AFeature;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

enum PaintingPermissions implements AFeature.IPermissionHolder {
    CMD_SET_RANGE(new Permission(
        "greenbook.painting.cmd.set_range",
        "allows to set the Range of switching between paintings",
        PermissionDefault.OP
    )),
    CHANGE_PAINTING(new Permission(
        "greenbook.painting.change_painting",
        "right-click a painting to switch between all possible motives",
        PermissionDefault.OP
    )),
    ADMIN(new Permission(
        "greenbook.painting.*",
        "all painting feature permissions",
        PermissionDefault.OP,
        Map.of(
            CMD_SET_RANGE.getPermission().getName(), true,
            CHANGE_PAINTING.getPermission().getName(), true
        )));

    private final @NotNull Permission permission;

    PaintingPermissions(@NotNull Permission permission) {
        this.permission = permission;

        Bukkit.getPluginManager().addPermission(permission);
    }

    @Override
    public @NotNull Permission getPermission() {
        return permission;
    }
}
