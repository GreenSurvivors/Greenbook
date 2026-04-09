package de.greensurvivors.greenbook.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public abstract class ASubCommand {
    protected final @NotNull GreenBook plugin;

    protected ASubCommand(@NotNull GreenBook plugin) {
        this.plugin = plugin;
    }

    /**
     * This checks, if the permissible has permission to see
     * (like in tab-completion/help) and use the subcommand.
     * <p>
     * Note: The subcommand can perform additional checks
     * Example: The method checks if the command sender is a player and owner of a sign.
     *
     * @param permissible to check the permission for
     * @return true if the permissible can see/use the subcommand.
     */
    abstract public boolean checkPermission(@NotNull Permissible permissible);

    abstract public @NotNull List<LiteralCommandNode<CommandSourceStack>> getCmdNodes();

    /**
     * get all the names a subcommand can get called.
     * If subcommand1 has alias "subcommand1-alias1" and "subcommand1-alias2"
     * and the command /command registers them they can be called via
     * <p>/command subcommand1-alias1
     * <p>/command subcommand1-alias2
     * If two or more subcommands register the same alias the outcome is unknown.
     * Please be aware of that. It really shouldn't be all that hard to not have
     * two subcommands called the same in one single plugins command.
     * <p> Every alias has to registered as lower case, so the casing when called
     * doesn't matter.
     */
    abstract public @NotNull Set<@NotNull String> getAliases();

    /**
     * get information how the command works, like
     * when used in a /help command wink wink
     */
    abstract public @NotNull Component getHelpText();
}
