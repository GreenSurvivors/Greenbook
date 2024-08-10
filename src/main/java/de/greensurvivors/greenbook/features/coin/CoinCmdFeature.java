package de.greensurvivors.greenbook.features.coin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.features.AFeature;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.language.StandardLangPath;
import de.greensurvivors.greenbook.language.StandartPlaceHolders;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CoinCmdFeature extends AFeature<CoinConfig> {
    private static final String COMMAND = "coin";
    private static final String SET = "set";

    public CoinCmdFeature(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.COIN, new CoinConfig(plugin));
    }

    @SuppressWarnings("UnstableApiUsage") // brigadier api
    @Override
    public void registerCommands(@NotNull Commands commandsRegistrar, @NotNull GreenBookCmd mainCommand) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.
            literal(COMMAND).

            requires(s ->
                getFeatureConfig().isEnabled() &&
                s.getSender().hasPermission(CoinPermissions.CMD_USE.getPermission())).
            then(Commands.
                literal(SET).
                requires(s -> s.getSender().hasPermission(CoinPermissions.CMD_SET.getPermission())).
                executes(context -> {
                    CommandSender sender = context.getSource().getSender();

                    // check admin permission
                    if (sender.hasPermission(CoinPermissions.CMD_SET.getPermission())) {
                        if (sender instanceof Player player) {
                            //clone so even it the held item stack changes, our copy will stay the same.
                            ItemStack itemStack = player.getInventory().getItemInMainHand().clone();

                            if (!itemStack.getType().isEmpty()) {
                                //regardless how many items where hold, just one count as coin.
                                itemStack.setAmount(1);

                                //save new coin to config
                                getFeatureConfig().setCoinItem(itemStack);

                                //send feedback message with the item translated by the client

                                plugin.getMessageManager().sendLang(sender, CoinLangPath.CMD_COIN_SET_SUCCESS,
                                    Placeholder.component(StandartPlaceHolders.ITEM.getPlaceholder(), Component.translatable(itemStack)));
                            } else {
                                //holding no item would mean an empty slot is considered as a coin and that's going to mess with things.
                                plugin.getMessageManager().sendLang(sender, CoinLangPath.CMD_COIN_SET_EMPTY);
                            }
                        } else {
                            plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_A_PLAYER);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, StandardLangPath.NO_PERMISSION);
                    }

                    return Command.SINGLE_SUCCESS;
                })
            ).
            then(Commands.
                argument("player", ArgumentTypes.player()).
                executes(context -> {
                    CommandSender sender = context.getSource().getSender();

                    //only players can toss a coin
                    if (sender instanceof Player player) {
                        //check other player
                        Player otherPlayer = context.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst(); // todo get all servers with same inventory

                        if (otherPlayer != null) {
                            if (player.getUniqueId() != otherPlayer.getUniqueId()) {
                                if (player.getInventory().removeItemAnySlot(getFeatureConfig().getCoinItem()).isEmpty()) {
                                    //drop items that didn't fit into the other players inventory
                                    for (ItemStack lostCoin : otherPlayer.getInventory().addItem(getFeatureConfig().getCoinItem()).values()) {
                                        otherPlayer.getWorld().dropItemNaturally(otherPlayer.getLocation(), lostCoin);
                                    }

                                    //broadcast success
                                    plugin.getMessageManager().broadcastLang(CoinLangPath.CMD_COIN_TOSS_OTHER,
                                        Placeholder.component(StandartPlaceHolders.PLAYER.getPlaceholder(), player.displayName()),
                                        Placeholder.component(StandartPlaceHolders.PLAYER2.getPlaceholder(), otherPlayer.displayName()));
                                } else {
                                    //player didn't have enough coins
                                    plugin.getMessageManager().sendLang(sender, CoinLangPath.CMD_COIN_NOT_ENOUGH);
                                }
                            } else {
                                //kill player that tried to give themselves a coin and announce it
                                player.setHealth(0.0d);
                                plugin.getMessageManager().broadcastLang(CoinLangPath.CMD_COIN_TOSS_SELF,
                                    Placeholder.component(StandartPlaceHolders.PLAYER.getPlaceholder(), player.displayName())); //todo maybe broadcast it across all servers, that share the same inventory
                            }
                        } else {
                            //unknown or offline other player
                            plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NOT_A_PLAYER, Placeholder.unparsed(StandartPlaceHolders.TEXT.getPlaceholder(), "?")); // todo

                            return 0;
                        }
                    } else {
                        //command sender is not a player
                        plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_A_PLAYER);
                    }

                    return Command.SINGLE_SUCCESS;
                })
            );

        commandsRegistrar.register(builder.build(), "", List.of());
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onEnable() {

    }
}
