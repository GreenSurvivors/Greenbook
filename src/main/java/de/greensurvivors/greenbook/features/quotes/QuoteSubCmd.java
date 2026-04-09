package de.greensurvivors.greenbook.features.quotes;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.ASubCommand;
import de.greensurvivors.greenbook.features.quotes.quotesubcmds.*;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuoteSubCmd extends ASubCommand {
    private static final @NotNull String SUBCOMMAND = "quote";
    private static final @NotNull String ALIAS1 = "shelf", ALIAS2 = "book";
    private final QuoteConfig quoteConfig;

    /**
     * contains all the registered subcommands
     * (they get registered when a new instance get created)
     */
    private final Set<ASubCommand> subCommands = new HashSet<>();

    public QuoteSubCmd(@NotNull GreenBook plugin,
                       @NotNull QuoteConfig config,
                       @NotNull Permission parentPerm) {
        super(plugin);
        quoteConfig = config;

        subCommands.add(new ListSubQuoteSubCmd(plugin, config, parentPerm));
        subCommands.add(new SneakSubQuoteSubCmd(plugin, config, parentPerm));
        subCommands.add(new HandSubQuoteSubCommand(plugin, config, parentPerm));
        subCommands.add(new RemoveSubQuoteSubCommand(plugin, config, parentPerm));
        subCommands.add(new GetSubQuoteSubCommand(plugin, config, parentPerm));
        subCommands.add(new AddSubQuoteSubCommand(plugin, config, parentPerm));
        /* subCommands.add(new EditSubSubCommand(plugin, config, parentPerm));*/ // todo

        // register permissions
        Map<String, Boolean> permChildren = parentPerm.getChildren();
        permChildren.put(QuotePermissions.ALL_CMDS.getPermission().getName(), true);
        parentPerm.recalculatePermissibles();
    }

    public static @NotNull String getSubcommandName() {
        return SUBCOMMAND;
    }

    @NotNull
    public List<LiteralCommandNode<CommandSourceStack>> getCmdNodes() {
        LiteralArgumentBuilder<CommandSourceStack> cmdBuilder = Commands.literal(SUBCOMMAND).
            requires(s -> quoteConfig.isEnabled())
            .requires(cmdSourceStack -> checkPermission(cmdSourceStack.getSender()));

        for (ASubCommand subCommand : subCommands) {
            for (LiteralCommandNode<CommandSourceStack> node : subCommand.getCmdNodes()) {
                cmdBuilder.then(node);
            }
        }

        return List.of(cmdBuilder.build());
    }

    @Override
    public boolean checkPermission(@NotNull Permissible permissible) {
        for (ASubCommand subCommand : subCommands) {
            if (subCommand.checkPermission(permissible)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @NotNull
    public Set<@NotNull String> getAliases() {
        return Set.of(SUBCOMMAND, ALIAS1, ALIAS2);
    }

    @Override
    @NotNull
    public Component getHelpText() { // todo all help text
        return Component.text("TODO");
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
}
