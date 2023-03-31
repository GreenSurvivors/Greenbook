package de.greensurvivors.greenbook.commands;

import de.greensurvivors.greenbook.PermissionUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * all config subcommands except of /coin
 */
public class GreenBookCmd implements CommandExecutor, TabCompleter {
    private static final String COMMAND = "greenbook";

    public static String getCommand() {
        return COMMAND;
    }

    /**
     * Executes the given sub command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0){ //execute subcommand
            switch (args[0].toLowerCase()){
                case ReloadCmd.SUBCOMMAND -> {
                    return ReloadCmd.handleCommand(sender, args);
                }
                case BookCmd.SUBCOMMAND -> {
                    return BookCmd.handleCommand(sender, args);
                }
                case PaintingCmd.SUBCOMMAND -> {
                    return PaintingCmd.handleCommand(sender, args);
                }
                default -> {
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender  Source of the command.  For players tab-completing a
     *                command inside a command block, this will be the player, not
     *                the command block.
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed
     * @return A List of possible completions for the final argument, or null
     * to default to the command executor
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> result = new ArrayList<>();
        //add subcommands if the sender has the permission for it.
        if (args.length == 1) {
            if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_RELOAD)){
                result.add(ReloadCmd.SUBCOMMAND);
            }
            if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN,
                    PermissionUtils.GREENBOOK_SHELF_ADD, PermissionUtils.GREENBOOK_SHELF_REMOVE, PermissionUtils.GREENBOOK_SHELF_LIST,
                    PermissionUtils.GREENBOOK_SHELF_SNEAK, PermissionUtils.GREENBOOK_SHELF_EMPTYHAND)){
                result.add(BookCmd.SUBCOMMAND);
            }
            if(PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_PAINTING_RANGE)){
                result.add(PaintingCmd.SUBCOMMAND);
            }

            result = result.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        } else if (args.length > 1){
            switch (args[0]){//tab complete of subcommands
                case BookCmd.SUBCOMMAND -> {
                    return BookCmd.handleTabComplete(sender, args);
                }
                case PaintingCmd.SUBCOMMAND -> {
                    return PaintingCmd.handleTab(sender, args);
                }
            }
        }

        return result;
    }
}
