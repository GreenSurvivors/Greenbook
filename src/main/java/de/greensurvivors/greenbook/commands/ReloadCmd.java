package de.greensurvivors.greenbook.commands;

import de.greensurvivors.greenbook.PermissionUtils;
import de.greensurvivors.greenbook.config.MainConfig;
import de.greensurvivors.greenbook.language.Lang;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCmd {
    public static final String SUBCOMMAND = "reload";

    protected static boolean handleCommand(@NotNull CommandSender sender, @NotNull String[] args){
        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_RELOAD)){
            MainConfig.inst().reloadMain();

            sender.sendMessage(Lang.build(Lang.RELOAD.get()));
        } else {
            sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
        }

        return true;
    }
}
