package de.greensurvivors.greenbook.utils;

import org.bukkit.permissions.Permissible;


public enum PermissionUtils {
    //never use this one, it's just the root
    GREENBOOK_ROOT("greenbook."),

    //technical permission for bukkit to show the coin command.
    //you need this and at least one of the following permissions to successfully use the coin command.
    //that's because bukkit doesn't allow to check for multiple subcommand permissions when deciding to show
    //a command to a player or not.
    //have a look into the plugin.yml to see its use.
    GREENBOOK_COIN_USE(GREENBOOK_ROOT.get() + "coin.use"),

    GREENBOOK_COIN_PLAYER(GREENBOOK_ROOT.get() + "coin.player"),
    GREENBOOK_COIN_ADMIN(GREENBOOK_ROOT.get() + "coin.admin"),

    //technical permission for bukkit to show the main command.
    //you need this and at least one of the following permissions for a subcommand,
    //to successfully use the main command.
    //That's because bukkit doesn't allow to check for multiple subcommand permissions when deciding to show
    //a command to a player or not.
    //have a look into the plugin.yml to see its use.
    GREENBOOK_USEMAIN(GREENBOOK_ROOT.get() + "usemaincmd"),

    GREENBOOK_LIFT_WILDCARD(GREENBOOK_ROOT.get() + "lift.*"),
    GREENBOOK_LIFT_CREATE(GREENBOOK_ROOT.get() + "lift.create"),
    GREENBOOK_LIFT_USE(GREENBOOK_ROOT.get() + "lift.use"),

    GREENBOOK_SHELF_WILDCARD(GREENBOOK_ROOT.get() + "shelf.*"),
    GREENBOOK_SHELF_USE(GREENBOOK_ROOT.get() + "shelf.use"),
    GREENBOOK_SHELF_ADD(GREENBOOK_ROOT.get() + "shelf.add"),
    GREENBOOK_SHELF_REMOVE(GREENBOOK_ROOT.get() + "shelf.remove"),
    GREENBOOK_SHELF_LIST(GREENBOOK_ROOT.get() + ".shelf.list"),
    GREENBOOK_SHELF_EMPTYHAND(GREENBOOK_ROOT.get() + "shelf.set.emptyhand"),
    GREENBOOK_SHELF_SNEAK(GREENBOOK_ROOT.get() + "shelf.set.require_sneak"),

    GREENBOOK_PAINTING_EDIT(GREENBOOK_ROOT.get() + "painting.edit"),
    GREENBOOK_PAINTING_RANGE(GREENBOOK_ROOT.get() + "painting.set.range"),

    GREENBOOK_WIRELESS_CREATE_SIGN(GREENBOOK_ROOT.get() + "wireless.create_sign"),
    GREENBOOK_WIRELESS_UPDATE_SIGNS_CMD(GREENBOOK_ROOT.get() + "wireless.update_signs_cmd"),
    GREENBOOK_WIRELESS_SET_PLAYER_SPECIFIC_CHANNELS(GREENBOOK_ROOT.get() + "wireless.set.player_specific_channels"),
    GREENBOOK_WIRELESS_SET_COMPATIBILITY_MODE(GREENBOOK_ROOT.get() + "wireless.set.compatibility_mode"),

    GREENBOOK_PLUGIN(GREENBOOK_ROOT.get() + "plugin"),
    GREENBOOK_RELOAD(GREENBOOK_ROOT.get() + "reload");

    private final String value;

    PermissionUtils(String value) {
        this.value = value;
    }

    /**
     * Check if CommandSender has any of the given permissions.
     *
     * @param permissible something with permissions to check for
     * @param permission  permissions to check
     * @return true if permissible has at least one of the given permissions.
     */
    public static boolean hasPermission(Permissible permissible, PermissionUtils... permission) {
        for (PermissionUtils p : permission) {
            if (permissible.hasPermission(p.get()))
                return true;
        }

        return false;
    }

    public String get() {
        return value;
    }
}
