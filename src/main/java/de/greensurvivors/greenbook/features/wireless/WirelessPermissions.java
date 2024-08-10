package de.greensurvivors.greenbook.features.wireless;

import de.greensurvivors.greenbook.features.AFeature;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public enum WirelessPermissions implements AFeature.IPermissionHolder {
    CREATE_NODE(new Permission(
        "greenbook.wireless.create_node",
        "Allows the user to create and change wireless receivers / transmitters.",
        PermissionDefault.OP)),
    SET_NODE_OWNER(new Permission(
        "greenbook.wireless.set_owner",
        "Allows the user to set anther user as the owner of a wireless network.",
        PermissionDefault.OP,
        Map.of(CREATE_NODE.getPermission().getName(), true))),
    ADMIN(new Permission(
        "greenbook.wireless.*",
        "All wireless feature permissions.",
        PermissionDefault.OP,
        Map.of(
            SET_NODE_OWNER.getPermission().getName(), true
        )));

    private final @NotNull Permission permission;

    WirelessPermissions(@NotNull Permission permission) {
        this.permission = permission;

        Bukkit.getPluginManager().addPermission(permission);
    }

    @Override
    public @NotNull Permission getPermission() {
        return permission;
    }
}

