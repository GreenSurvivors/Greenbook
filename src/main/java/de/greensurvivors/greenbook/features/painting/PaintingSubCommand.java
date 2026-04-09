package de.greensurvivors.greenbook.features.painting;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.ASubCommand;
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

public class PaintingSubCommand extends ASubCommand {
    private static final String SUBCOMMAND = "painting", RANGE = "range";
    private final @NotNull PaintingFeature feature;

    protected PaintingSubCommand(@NotNull GreenBook plugin, @NotNull PaintingFeature feature, @NotNull Permission parentPerm) {
        super(plugin);
        this.feature = feature;

        parentPerm.getChildren().put(PaintingPermissions.CMD_SET_RANGE.getPermission().getName(), true);
        parentPerm.recalculatePermissibles();
    }

    @NotNull
    public List<LiteralCommandNode<CommandSourceStack>> getCmdNodes() {
        return List.of(Commands.literal(SUBCOMMAND).
            requires(s -> feature.getFeatureConfig().isEnabled()).
            requires(cmdSourceStack -> checkPermission(cmdSourceStack.getSender())).
            then(Commands.literal(RANGE).
                then(Commands.argument("rangeNumber", IntegerArgumentType.integer(1, 100)).
                    executes(context -> {
                            if (checkPermission(context.getSource().getSender())) {
                                int range = IntegerArgumentType.getInteger(context, "rangeNumber");

                                //set the working value
                                feature.getFeatureConfig().setPaintingModifyRange(range);

                                plugin.getMessageManager().sendLang(context.getSource().getSender(), PaintingLangPath.CMD_SUB_PAINTING_RANGE_SUCCESS,
                                    Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(range)));
                            } else {
                                plugin.getMessageManager().sendLang(context.getSource().getSender(), StandardLangPath.NO_PERMISSION);
                            }

                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        }
                    )
                )
            ).build());
    }

    @Override
    public boolean checkPermission(@NotNull Permissible permissible) {
        return permissible.hasPermission(PaintingPermissions.CMD_SET_RANGE.getPermission());
    }

    @Override
    @NotNull
    public Set<@NotNull String> getAliases() {
        return Set.of(SUBCOMMAND);
    }

    @Override
    @NotNull
    public Component getHelpText() {
        return plugin.getMessageManager().getLang(PaintingLangPath.CMD_SUB_PAINTING_RANGE_HElP);
    }
}
