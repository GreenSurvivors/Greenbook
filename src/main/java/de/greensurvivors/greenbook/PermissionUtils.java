package de.greensurvivors.greenbook;

import org.bukkit.permissions.Permissible;


public enum PermissionUtils {
    //never use this one, it's just the root
    GREENBOOK_ROOT("greenbook."),

    GREENBOOK_COIN_PLAYER(GREENBOOK_ROOT.get() + "coin.player"),
    GREENBOOK_COIN_ADMIN(GREENBOOK_ROOT.get() + "coin.admin"),

    GREENBOOK_LIFT_CREATE (GREENBOOK_ROOT.get() + "lift.create"),
    GREENBOOK_LIFT_USE (GREENBOOK_ROOT.get() + "lift.use"),
    GREENBOOK_LIFT_ADMIN (GREENBOOK_ROOT.get() + "lift.admin"),

    GREENBOOK_SHELF_USE (GREENBOOK_ROOT.get() + ".shelf.use"),
    GREENBOOK_SHELF_ADD (GREENBOOK_ROOT.get() +".shelf.add"),
    GREENBOOK_SHELF_REMOVE (GREENBOOK_ROOT.get() +".shelf.remove"),
    GREENBOOK_SHELF_LIST (GREENBOOK_ROOT.get() +".shelf.list"),
    GREENBOOK_SHELF_EMPTYHAND(GREENBOOK_ROOT.get() + ".shelf.set.emptyhand"),
    GREENBOOK_SHELF_SNEAK(GREENBOOK_ROOT.get() + ".shelf.set.require_sneak"),
    GREENBOOK_SHELF_ADMIN (GREENBOOK_ROOT.get() + ".shelf.admin"),

    GREENBOOK_PAINTING_EDIT(GREENBOOK_ROOT.get() + "painting.edit"),
    GREENBOOK_PAINTING_RANGE(GREENBOOK_ROOT.get() + "painting.set.range"),

    GREENBOOK_PLUGIN(GREENBOOK_ROOT.get() + "plugin"),
    GREENBOOK_RELOAD(GREENBOOK_ROOT.get() + "reload");

    private final String value;

    PermissionUtils(String value) {
        this.value = value;
    }

    public String get() {
        return value;
    }

    /**
     * Check if CommandSender has any of the given permissions.
     *
     * @param permissible something with permissions to check for
     * @param permission permissions to check
     * @return true if permissible has at least one of the given permissions.
     */
    public static boolean hasPermission(Permissible permissible, PermissionUtils... permission) {
        for (PermissionUtils p : permission) {
            if (permissible.hasPermission(p.get()))
                return true;
        }

        return false;
    }
}
