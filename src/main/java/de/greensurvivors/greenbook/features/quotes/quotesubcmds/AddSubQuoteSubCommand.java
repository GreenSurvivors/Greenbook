package de.greensurvivors.greenbook.features.quotes.quotesubcmds;

import com.mojang.brigadier.Command;
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
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.SignedMessageResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class AddSubQuoteSubCommand extends ASubCommand {
    private static final String ADD = "add";
    private final @NotNull QuoteConfigManager quoteConfig;

    public AddSubQuoteSubCommand(final @NotNull GreenBook plugin,
                                 final @NotNull QuoteConfigManager config,
                                 final @NotNull Permission parentPerm) {
        super(plugin);
        quoteConfig = config;

        // add parent, since this actually adds the permission as child to the parent, and should exist before
        QuotePermissions.CMD_ADD.getPermission().addParent(parentPerm, true);
    }

    @Override
    public boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(QuotePermissions.CMD_ADD.getPermission());
    }

    @Override
    @NotNull
    public List<LiteralCommandNode<CommandSourceStack>> getCmdNodes() {
        return List.of(Commands.literal(ADD).
            requires(commandSourceStack -> checkPermission(commandSourceStack.getSender())).
            then(Commands.argument("rawQuoteText", ArgumentTypes.signedMessage()).
                executes(context -> {
                    SignedMessageResolver argumentResponse = context.getArgument("rawQuoteText", SignedMessageResolver.class); // Gets the raw argument
                    // This is a better way of getting signed messages, includes the concept of "disguised" messages.
                    argumentResponse.resolveSignedMessage("rawQuoteText", context)
                        .thenAccept((signedMsg) -> onCommand(context, signedMsg.message()));

                    return Command.SINGLE_SUCCESS; // just accept every message
                })
            ).build());
    }

    @Override
    @NotNull
    public Set<@NotNull String> getAliases() {
        return Set.of(ADD);
    }

    @Override
    @NotNull
    public Component getHelpText() {
        return Component.text("TODO");
    }

    private void onCommand(final @NotNull CommandContext<CommandSourceStack> context, final @NotNull String arg) {
        //check permission
        if (checkPermission(context.getSource().getSender())) {
            //our quote was broken into an array of strings, we have to glue it back together
            Component quoteText = MiniMessage.miniMessage().deserialize(arg);

            //save the added book to config and add it to the current active list
            int bookID = quoteConfig.addQuote(quoteText);
            // send feedback
            plugin.getMessageManager().sendLang(context.getSource().getSender(), QuotesLangPath.CMD_SUB_ADD_SUCCESS,
                Placeholder.component(StandartPlaceHolders.TEXT.getPlaceholder(), quoteText),
                Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(bookID)));

        } else { //no permission
            plugin.getMessageManager().sendLang(context.getSource().getSender(), StandardLangPath.NO_PERMISSION);
        }
    }
}
