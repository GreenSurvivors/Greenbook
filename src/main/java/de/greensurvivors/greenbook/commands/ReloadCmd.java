package de.greensurvivors.greenbook.commands;

import de.greensurvivors.greenbook.PermissionUtils;
import de.greensurvivors.greenbook.config.MainConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCmd {
    public static final String SUBCOMMAND = "reload";

    protected static void handleCommand(@NotNull CommandSender sender, @NotNull String[] args){
        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_RELOAD)){
            MainConfig.inst().reloadMain();
        }
    }
}
