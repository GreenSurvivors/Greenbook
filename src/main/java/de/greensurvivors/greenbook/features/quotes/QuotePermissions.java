package de.greensurvivors.greenbook.features.quotes;

import de.greensurvivors.greenbook.features.AFeature;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public enum QuotePermissions implements AFeature.IPermissionHolder {
    CMD_ADD(new Permission(
        "greenbook.quotes.cmd.add",
        "Allows to add quotes to shelf's.",
        PermissionDefault.OP)),
    CMD_GET(new Permission(
        "greenbook.quotes.cmd.get",
        "Allows to get quotes by id.",
        PermissionDefault.OP)),
    CMD_REMOVE(new Permission(
        "greenbook.quotes.cmd.remove",
        "Allows to remove quotes from shelf's.",
        PermissionDefault.OP)),
    CMD_EDIT(new Permission(
        "greenbook.quotes.cmd.edit",
        "Allows to edit shelf's quotes.",
        PermissionDefault.OP,
        Map.of(
            CMD_GET.getPermission().getName(), true,
            CMD_ADD.getPermission().getName(), true,
            CMD_REMOVE.getPermission().getName(), true
        ))),
    CMD_HAND(new Permission(
        "greenbook.quotes.cmd.set_hand",
        "Allows to set if a empty hand is required to read quotes.",
        PermissionDefault.OP)),
    CMD_LIST(new Permission(
        "greenbook.quotes.cmd.list",
        "Allows to list shelf quotes.",
        PermissionDefault.OP)),
    CMD_SNEAK(new Permission(
        "greenbook.quotes.cmd.set_sneak",
        "Allows to set if sneaking status for reading quotes.",
        PermissionDefault.OP)),
    ALL_CMDS(new Permission(
        "greenbook.quotes.cmd.*",
        "All quote command permissions.",
        PermissionDefault.OP,
        Map.of(
            CMD_EDIT.getPermission().getName(), true,
            CMD_HAND.getPermission().getName(), true,
            CMD_LIST.getPermission().getName(), true,
            CMD_SNEAK.getPermission().getName(), true
        ))),
    GET_QUOTE(new Permission(
        "greenbook.quotes.use",
        "Click and receive a quote!",
        PermissionDefault.OP)),
    ADMIN(new Permission(
        "greenbook.quotes.*",
        "All quote feature permissions.",
        PermissionDefault.OP,
        Map.of(
            ALL_CMDS.getPermission().getName(), true,
            GET_QUOTE.getPermission().getName(), true
        )));

    private final @NotNull Permission permission;

    QuotePermissions(@NotNull Permission permission) {
        this.permission = permission;

        Bukkit.getPluginManager().addPermission(permission);
    }

    @Override
    public @NotNull Permission getPermission() {
        return permission;
    }
}