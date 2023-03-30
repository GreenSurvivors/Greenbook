package de.greensurvivors.greenbook.commands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShelfCmd{
    public static final String SUBCOMMAND = "shelf";

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    public static boolean handleCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        //todo add and remove books
        return false;
    }

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender  Source of the command.  For players tab-completing a
     *                command inside of a command block, this will be the player, not
     *                the command block.
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed
     * @return A List of possible completions for the final argument, or null
     * to default to the command executor
     */
    public static @Nullable List<String> handleTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        //todo add and remove books
        return null;
    }
}
