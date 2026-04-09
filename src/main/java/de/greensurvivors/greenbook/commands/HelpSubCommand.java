package de.greensurvivors.greenbook.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class HelpSubCommand extends ASubCommand { // todo
    protected HelpSubCommand(@NotNull GreenBook plugin, @NotNull Permission parentPerm) {
        super(plugin);
    }

    @Override
    public boolean checkPermission(@NotNull Permissible permissible) {
        return false;
    }

    @Override
    public @NotNull List<LiteralCommandNode<CommandSourceStack>> getCmdNodes() {
        return List.of();
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of();
    }

    @Override
    public @NotNull Component getHelpText() {
        return null;
    }
}
