package de.greensurvivors.greenbook.features.quotes.quotesubcmds;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.ASubCommand;
import de.greensurvivors.greenbook.features.quotes.QuoteConfig;
import de.greensurvivors.greenbook.features.quotes.QuotePermissions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * /greenbook quote add [quote] - add a new quote (aka book)
 */
public class EditSubSubCommand extends ASubCommand { // todo this
    private static final String EDIT = "edit";

    private final @NotNull QuoteConfig quoteConfig;

    public EditSubSubCommand(@NotNull GreenBook plugin, @NotNull QuoteConfig config) {
        super(plugin);
        quoteConfig = config;
    }

    @Override
    public boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(QuotePermissions.CMD_EDIT.getPermission());
    }

    @Override
    @NotNull
    public List<LiteralCommandNode<CommandSourceStack>> getCmdNodes() {
        return List.of(Commands.
            literal(EDIT).
            build()
        );
    }

    @Override
    @NotNull
    public Set<@NotNull String> getAliases() {
        return Set.of(EDIT);
    }

    @Override
    @NotNull
    public Component getHelpText() {
        return Component.text("TODO");
    }
}
