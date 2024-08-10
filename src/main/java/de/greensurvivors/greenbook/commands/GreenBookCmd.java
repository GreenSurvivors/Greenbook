package de.greensurvivors.greenbook.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage") // brigadier api
public class GreenBookCmd {
    private static final String COMMAND = "greenbook";
    private static final String DESCRIPTION = "Contains all main GreenBookCommands use /greenbook help <subcommand>.";
    private static final Permission PERMISSION = new Permission("greenbook.cmd.greenbook.*", DESCRIPTION, PermissionDefault.OP);
    /**
     * contains all the registered subcommands
     * (they get registered when a new instance get created)
     */
    private final Set<ASubCommand> subCommands = new HashSet<>();
    private final @NotNull GreenBook plugin;
    private LiteralArgumentBuilder<CommandSourceStack> cmdBuilder;

    public GreenBookCmd(@NotNull GreenBook plugin) {
        this.plugin = plugin;

        // register permission
        Bukkit.getPluginManager().addPermission(PERMISSION);

        cmdBuilder = Commands.literal(COMMAND).
            requires(s -> s.getSender().hasPermission(PERMISSION));

        ReloadSubCmd reloadSubCmd = new ReloadSubCmd(plugin, PERMISSION);
        HelpSubCommand helpSubCommand = new HelpSubCommand(plugin, PERMISSION);

        registerSubcommand(reloadSubCmd, reloadSubCmd.getCmdNodes());
    }

    public static @NotNull String getCommandName() {
        return COMMAND;
    }

    public @NotNull Permission getPermission() {
        return PERMISSION;
    }

    public void registerSubcommand(@NotNull ASubCommand subCommand,
                                   @NotNull List<@NotNull LiteralCommandNode<CommandSourceStack>> nodes) {
        subCommands.add(subCommand);

        for (LiteralCommandNode<CommandSourceStack> node : nodes) {
            cmdBuilder.then(node);
        }
    }

    public void finalizeSubCommands(final @NotNull Commands commandsRegistrar) {
        if (cmdBuilder != null) {
            commandsRegistrar.register(cmdBuilder.build(), DESCRIPTION, List.of());

            cmdBuilder = null;
        }
    }

    /**
     * get a subcommand by its alias, filtered by the permission check of each subcommand against the permissible
     *
     * @param permissible to check against for permission to using the subcommand
     * @param string      the string to contain an alias of a subcommand to get
     * @return the subcommand with the alias or null if no where found
     */
    protected @Nullable ASubCommand getSubCommandFromString(@NotNull Permissible permissible, @NotNull String string) {
        String subCmdStr = string.toLowerCase();

        for (ASubCommand subCommand : subCommands) {
            if (subCommand.getAliases().contains(subCmdStr) && subCommand.checkPermission(permissible)) {
                return subCommand;
            }
        }

        return null;
    }

    /**
     * get all registered Subcommands, filtered by the permission check of each subcommand against the permissible
     *
     * @param permissible to check against for permission to using the subcommand
     */
    protected Set<ASubCommand> getSubCommands(@NotNull Permissible permissible) {
        return subCommands.stream().filter(subCommand -> subCommand.checkPermission(permissible)).collect(Collectors.toSet());
    }
}
