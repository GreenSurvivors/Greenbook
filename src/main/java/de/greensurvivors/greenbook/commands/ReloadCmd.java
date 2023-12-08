package de.greensurvivors.greenbook.commands;

import de.greensurvivors.greenbook.config.MainConfig;
import de.greensurvivors.greenbook.language.Lang;
import de.greensurvivors.greenbook.utils.PermissionUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * reload subcommand, to reload config and language
 */
public class ReloadCmd {
    public static final String SUBCOMMAND = "reload";

    /**
     * /greenbook reload - reload all configs and language files
     *
     * @param sender source of command, for permission check
     * @param args   given arguments (ignored)
     * @return true, you can't mess up
     */
    protected static boolean handleCommand(@NotNull CommandSender sender, @Nullable String[] args) {
        //check permission
        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_RELOAD)) {
            //reload config + language
            MainConfig.inst().reloadMain();

            //feedback
            sender.sendMessage(Lang.build(Lang.RELOADED.get()));
        } else { //no permission
            sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
        }

        return true;
    }
}
