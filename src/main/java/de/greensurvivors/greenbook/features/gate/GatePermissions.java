package de.greensurvivors.greenbook.features.gate;

import de.greensurvivors.greenbook.features.AFeature;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public enum GatePermissions implements AFeature.IPermissionHolder {
    GATE_USE(new Permission("greenbook.gate.use",
        "Allows to use gates.",
        PermissionDefault.TRUE)),
    GATE_CREATE(new Permission(
        "greenbook.gate.create",
        "Allows to create new gates.",
        PermissionDefault.FALSE)),
    GATE_ADMIN(new Permission(
        "greenbook.gate.*",
        "Contains all gate permissions",
        PermissionDefault.OP,
        Map.of(
            GATE_USE.getPermission().getName(), true,
            GATE_CREATE.getPermission().getName(), true)));


    private final @NotNull Permission permission;

    GatePermissions(@NotNull Permission permission) {
        this.permission = permission;

        Bukkit.getPluginManager().addPermission(permission);
    }

    @Override
    public @NotNull Permission getPermission() {
        return permission;
    }
}
