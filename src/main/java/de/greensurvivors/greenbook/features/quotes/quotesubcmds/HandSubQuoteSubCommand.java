package de.greensurvivors.greenbook.features.quotes.quotesubcmds;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.ASubCommand;
import de.greensurvivors.greenbook.features.quotes.QuoteConfigManager;
import de.greensurvivors.greenbook.features.quotes.QuotePermissions;
import de.greensurvivors.greenbook.features.quotes.QuotesLangPath;
import de.greensurvivors.greenbook.language.StandardLangPath;
import de.greensurvivors.greenbook.language.StandartPlaceHolders;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class HandSubQuoteSubCommand extends ASubCommand {
    private static final String EMPTY_HAND_SHORT = "hand", EMPTY_HAND_LONG = "emptyhand";

    private final @NotNull QuoteConfigManager quoteConfig;

    public HandSubQuoteSubCommand(final @NotNull GreenBook plugin,
                                  final @NotNull QuoteConfigManager config,
                                  final @NotNull Permission parentPerm) {
        super(plugin);
        quoteConfig = config;

        QuotePermissions.CMD_HAND.getPermission().addParent(parentPerm, true);
    }

    @Override
    public boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(QuotePermissions.CMD_HAND.getPermission());
    }

    @Override
    @NotNull
    public List<LiteralCommandNode<CommandSourceStack>> getCmdNodes() {
        // create subcommand
        LiteralCommandNode<CommandSourceStack> subCmdNode = Commands.literal(EMPTY_HAND_LONG).
            requires(commandSourceStack -> checkPermission(commandSourceStack.getSender())).
            then(Commands.argument("emptyHandRequired", BoolArgumentType.bool()).
                executes(context -> onCommand(context, BoolArgumentType.getBool(context, "emptyHandRequired")))
            ).build();

        // register subcommand and alias
        return List.of(subCmdNode,
            Commands.literal(EMPTY_HAND_LONG).
                redirect(subCmdNode).
                build()
        );
    }

    @Override
    @NotNull
    public Set<@NotNull String> getAliases() {
        return Set.of(EMPTY_HAND_LONG, EMPTY_HAND_SHORT);
    }

    @Override
    @NotNull
    public Component getHelpText() {
        return Component.text("TODO");
    }

    private int onCommand(@NotNull CommandContext<CommandSourceStack> context, boolean shouldQuoteRequireEmptyHand) {
        if (checkPermission(context.getSource().getSender())) {
            quoteConfig.setRequireEmptyHand(shouldQuoteRequireEmptyHand).thenRunAsync(() -> {
                plugin.getMessageManager().sendLang(context.getSource().getSender(), QuotesLangPath.CMD_SUB_HAND_SUCCESS,
                    Placeholder.unparsed(StandartPlaceHolders.BOOL.getPlaceholder(), String.valueOf(shouldQuoteRequireEmptyHand)));
            }, plugin.getServer().getScheduler().getMainThreadExecutor(plugin));
        } else {
            plugin.getMessageManager().sendLang(context.getSource().getSender(), StandardLangPath.NO_PERMISSION);
        }

        return Command.SINGLE_SUCCESS;
    }
}
