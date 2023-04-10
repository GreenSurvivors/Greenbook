package de.greensurvivors.greenbook.commands;

import de.greensurvivors.greenbook.PermissionUtils;
import de.greensurvivors.greenbook.config.CoinConfig;
import de.greensurvivors.greenbook.language.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * /coin command
 * sets coin item as well tossing one of this item to another player
 */
public class CoinCmd implements CommandExecutor, TabCompleter {
    private static final String COMMAND = "coin";
    private static final String SET = "set";

    private static final Pattern pattern = Pattern.compile(Lang.VALUE);

    // Set default itemstack, so we should not run into null exceptions.
    // However, it should never come into play, since at least the config should provide a default value itself.
    private ItemStack coinItem = new ItemStack(Material.GOLD_NUGGET);

    public static String getCommand() {
        return COMMAND;
    }

    public void setCoinItem(ItemStack newCoin) {
        this.coinItem = newCoin;
    }

    /**
     * Executes /coin, tossing a coin to another player or setting the coin item.
     * <br>
     * <br> /coin set - set the coin item to the one in main hand
     * <br> /coin [player] - toss a coin to the other player or kill self if the other player is the command sender
     *
     * @param sender  Source of the command
     * @param command Command which was executed (ignored)
     * @param label   Alias of the command which was used (ignored)
     * @param args    Passed command arguments
     * @return        default true, but false if no additional args where given
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @Nullable Command command, @NotNull String label, @Nullable String[] args) {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase(SET)) {
                //check permission
                if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_COIN_ADMIN)) {
                    //try to get the new item
                    if (sender instanceof Player player) {
                        ItemStack itemStack = player.getInventory().getItemInMainHand();

                        if (!itemStack.getType().isEmpty()) {
                            //clone so even it the held item stack changes, our copy will stay the same.
                            this.coinItem = itemStack.clone();
                            //regardless how many items where hold, just one count as coin.
                            this.coinItem.setAmount(1);

                            //save new coin to config
                            CoinConfig.inst().saveCoinItem(this.coinItem);

                            //send feedback message with the item translated by the client
                            sender.sendMessage(Lang.build(Lang.COIN_SET.get()).replaceText(builder -> {
                                builder.match(pattern);
                                builder.replacement(Component.translatable(this.coinItem));
                            }));
                        } else {
                            //holding no item would mean an empty slot is considered as a coin and that's going to mess with things.
                            sender.sendMessage(Lang.build(Lang.NO_ITEM_HOLDING.get()));
                        }
                    } else {
                        //command sender is not a player
                        sender.sendMessage(Lang.build(Lang.NOT_PLAYER_SELF.get()));
                    }
                } else {
                    //no permission to set coin item
                    sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                }
            } else if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_COIN_ADMIN, PermissionUtils.GREENBOOK_COIN_PLAYER)) {
                //only players can toss a coin
                if (sender instanceof Player player) {
                    //check other player
                    Player otherPlayer = Bukkit.getPlayer(args[0]);
                    if (otherPlayer != null) {
                        if (player.getUniqueId() != otherPlayer.getUniqueId()) {
                            if (player.getInventory().removeItemAnySlot(coinItem).isEmpty()) {
                                //drop items that didn't fit into the other players inventory
                                for (ItemStack lostCoin : otherPlayer.getInventory().addItem(coinItem).values()) {
                                    otherPlayer.getWorld().dropItemNaturally(otherPlayer.getLocation(), lostCoin);
                                }

                                //broadcast success
                                player.getServer().broadcast(Lang.build(Lang.COIN_TOSS_OTHER.get().replace(Lang.PLAYER, player.getName()).replace(Lang.PLAYER2, otherPlayer.getName())));
                            } else {
                                //player didn't have enough coins
                                sender.sendMessage(Lang.build(Lang.COIN_NOT_ENOUGH.get()));
                            }
                        } else {
                            //kill player that tried to give themselves a coin and announce it
                            player.setHealth(0.0d);
                            player.getServer().broadcast(Lang.build(Lang.COIN_STOSS_SELF.get().replace(Lang.PLAYER, player.getName()))); //todo maybe broadcast it across all servers, that share the same inventory
                        }
                    } else {
                        //unknown or offline other player
                        sender.sendMessage(Lang.build(Lang.NO_SUCH_PLAYER.get().replace(Lang.VALUE, args[0])));
                    }
                } else {
                    //command sender is not a player
                    sender.sendMessage(Lang.build(Lang.NOT_PLAYER_SELF.get()));
                }
            } else {
                //no permission to toss a coin
                sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
            }
        } else {
            //no arguments where given
            sender.sendMessage(Lang.build(Lang.NOT_ENOUGH_ARGS.get()));
            return false;
        }
        return true;
    }

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender  Source of the command.  For players tab-completing a
     *                command inside a command block, this will be the player, not
     *                the command block.
     * @param command Command which was executed (ignored)
     * @param label   Alias of the command which was used (ignored)
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed
     * @return        A List of possible completions for the final argument, or an empty list
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @Nullable Command command, @Nullable String label, @NotNull String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            //set sub command to set the coin item
            if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_COIN_ADMIN)) {
                result.add(CoinCmd.SET);
            }

            //list of all players, to toss a coin
            if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_COIN_ADMIN, PermissionUtils.GREENBOOK_COIN_PLAYER)) {
                //todo add all players from other servers that share the same inventory
                result.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }

            //filter by already typed string
            result = result.stream().filter(s -> s.startsWith(args[0])).toList();
        }

        return result;
    }
}
