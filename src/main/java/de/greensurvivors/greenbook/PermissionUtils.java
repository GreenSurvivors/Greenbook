package de.greensurvivors.greenbook;

import org.bukkit.command.CommandSender;


public enum PermissionUtils {
    GREENBOOK_ROOT("greenbook"),
    GREENBOOK_COIN_PLAYER(GREENBOOK_ROOT.get() + ".coin.player"),
    GREENBOOK_COIN_ADMIN(GREENBOOK_ROOT.get() + ".coin.admin"),


    FINDME_REMOVE("findme.remove"),
    FINDME_REMOVE_GAME("findme.remove.game"),
    FINDME_REMOVE_HIDEAWAY("findme.remove.hideaway"),

    FINDME_CREATE("findme.create"),
    FINDME_CREATE_GAME("findme.create.game"),
    FINDME_CREATE_HIDEAWAY("findme.create.hideaway"),
    FINDME_CREATE_SIGN("findme.create.sign"),

    FINDME_SET("findme.set"),
    FINDME_SET_HEADS("findme.set.heads"),
    FINDME_SET_GAMELENGTH("findme.set.gamelength"),
    FINDME_SET_LATEJOIN("findme.set.latejoin"),
    FINDME_SET_LOCATIONS("findme.set.locations"),
    FINDME_SET_AVERAGE_TICKS_UNTIL_HIDE("findme.set.hide_ticks"),
    FINDME_SET_STARTING_PERCENT("findme.set.stating_percent"),
    FINDME_SET_HIDING_COOLDOWN("findme.set.cooldown"),

    FINDME_START("findme.start"),
    FINDME_END("findme.end"),

    FINDME_OTHER_PLAYERS("findme.otherplayers"),

    FINDME_SHOW("findme.show"),

    FINDME_LIST("findme.list"),

    FINDME_ADMIN("findme.admin"),
    FINDME_PLAYER("findme.player"),

    FINDME_PLUGIN("findme.plugin"),
    FINDME_RELOAD("findme.reload");

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
     * @param cs         CommandSender to check for
     * @param permission permissions to check
     * @return true if cs has at least one of the given permissions.
     */
    public static boolean hasPermission(CommandSender cs, PermissionUtils... permission) {
        for (PermissionUtils p : permission) {
            if (cs.hasPermission(p.get()))
                return true;
        }

        return false;
    }

    /**
     * Check if CommandSender has any permission from this plugin.
     *
     * @param cs CommandSender to check for
     * @return true if cs has at least one permission from this plugin.
     */
    public static boolean hasAnyPermission(CommandSender cs) {
        for (PermissionUtils p : values()) {
            if (cs.hasPermission(p.get()))
                return true;
        }
        return false;
    }
}
