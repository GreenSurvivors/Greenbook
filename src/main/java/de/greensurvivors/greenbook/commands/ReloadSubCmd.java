package de.greensurvivors.greenbook.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.language.StandardLangPath;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage") // brigadier api
public class ReloadSubCmd extends ASubCommand {
    private static final Permission RELOAD_PERM = new Permission("greenbook.cmd.greenbook.reload",
        "Allows to reload the language and config files", PermissionDefault.OP);
    private static final String SUBCOMMAND = "reload";

    protected ReloadSubCmd(@NotNull GreenBook plugin, @NotNull Permission parentPerm) {
        super(plugin);

        // register permissions
        PluginManager pluginManager = Bukkit.getPluginManager();

        pluginManager.addPermission(RELOAD_PERM);

        parentPerm.getChildren().put(RELOAD_PERM.getName(), true);
        parentPerm.recalculatePermissibles();
    }

    @Override
    @NotNull
    public List<LiteralCommandNode<CommandSourceStack>> getCmdNodes() {
        return List.of(Commands.literal(SUBCOMMAND).
            requires(cmdSourceStack -> checkPermission(cmdSourceStack.getSender())).
            executes(context -> {
                    if (checkPermission(context.getSource().getSender())) {
                        plugin.getConfigManager().reload();

                        plugin.getMessageManager().sendLang(context.getSource().getSender(), StandardLangPath.CMD_SUB_RELOAD_SUCCESS);
                    } else {
                        plugin.getMessageManager().sendLang(context.getSource().getSender(), StandardLangPath.NO_PERMISSION);
                    }

                    return Command.SINGLE_SUCCESS;
                }

            ).build());
    }

    @Override
    public boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(RELOAD_PERM);
    }

    @Override
    @NotNull
    public Set<@NotNull String> getAliases() {
        return Set.of(SUBCOMMAND);
    }

    @Override
    @NotNull
    public Component getHelpText() {
        return plugin.getMessageManager().getLang(StandardLangPath.CMD_SUB_RELOAD_HELP);
    }
}
