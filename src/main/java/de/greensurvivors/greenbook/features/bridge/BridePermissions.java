package de.greensurvivors.greenbook.features.bridge;

import de.greensurvivors.greenbook.features.AFeature;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public enum BridePermissions implements AFeature.IPermissionHolder {
    BRIDGE_USE(new Permission(
        "greenbook.bridge.use",
        "",
        PermissionDefault.TRUE)),
    BRIDGE_CREATE(new Permission(
        "greenbook.bridge.create",
        "",
        PermissionDefault.OP)),
    BRIDGE_ADMIN(new Permission(
        "greenbook.bridge.*",
        "",
        PermissionDefault.OP,
        Map.of(
            BRIDGE_USE.getPermission().getName(), true,
            BRIDGE_CREATE.getPermission().getName(), true
        )));

    private final @NotNull Permission permission;

    BridePermissions(@NotNull Permission permission) {
        this.permission = permission;

        Bukkit.getPluginManager().addPermission(permission);
    }

    @Override
    public @NotNull Permission getPermission() {
        return permission;
    }
}
