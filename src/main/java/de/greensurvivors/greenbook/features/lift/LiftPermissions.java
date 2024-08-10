package de.greensurvivors.greenbook.features.lift;

import de.greensurvivors.greenbook.features.AFeature;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

enum LiftPermissions implements AFeature.IPermissionHolder {
    LIFT_USE(new Permission("greenbook.lift.use",
        "Allows to use lift signs to quickly transport up / down",
        PermissionDefault.TRUE)),
    LIFT_CREATE(new Permission(
        "greenbook.lift.create",
        "Allows to create and change lift signs",
        PermissionDefault.OP)),
    ADMIN(new Permission(
        "greenbook.lift.*",
        "all lift feature permissions",
        PermissionDefault.OP,
        Map.of(
            LIFT_CREATE.getPermission().getName(), true,
            LIFT_USE.getPermission().getName(), true
        )));

    private final @NotNull Permission permission;

    LiftPermissions(@NotNull Permission permission) {
        this.permission = permission;

        Bukkit.getPluginManager().addPermission(permission);
    }

    @Override
    public @NotNull Permission getPermission() {
        return permission;
    }
}
