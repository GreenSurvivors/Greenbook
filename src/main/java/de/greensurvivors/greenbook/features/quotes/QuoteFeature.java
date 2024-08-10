package de.greensurvivors.greenbook.features.quotes;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.features.AFeature;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.language.StandartPlaceHolders;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

public class QuoteFeature extends AFeature<QuoteConfig> implements Listener { // todo Add Parent permissions per feature, so whoever has that has every cmd and normal permission of that feature.
    public QuoteFeature(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.QUOTES, new QuoteConfig(plugin));

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    @SuppressWarnings("UnstableApiUsage") // brigadier api
    public void registerCommands(@NotNull Commands commandsRegistrar, @NotNull GreenBookCmd mainCommand) {
        QuoteSubCmd subCmd = new QuoteSubCmd(plugin, getFeatureConfig(), mainCommand.getPermission());

        mainCommand.registerSubcommand(subCmd, subCmd.getCmdNodes());
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onEnable() {

    }

    /**
     * sends a random quote (book) if a player right-clicks a configurated material (default bookshelf)
     * checks if requirements (sneak / empty hand) are meet and if the player has permission
     */
    @SuppressWarnings("UnstableApiUsage") // block type
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onInteract(@NotNull PlayerInteractEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        //don't fire for offhand
        if (event.getHand() == EquipmentSlot.HAND &&
            //right-clicked a block. Should ensure the getBlock() is not null
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            //is the block a quote block?
            Block eBlock = event.getClickedBlock();
            if (eBlock != null && getFeatureConfig().isQuoteBlockType(eBlock.getType().asBlockType())) {

                //check requirements
                Player ePlayer = event.getPlayer();
                if ((getFeatureConfig().isEmptyHandRequired() && !ePlayer.getInventory().getItemInMainHand().getType().isAir()) ||
                    (getFeatureConfig().isSneakingRequired() && !ePlayer.isSneaking())) {
                    return;
                }

                //check permission
                if (ePlayer.hasPermission(QuotePermissions.GET_QUOTE.getPermission())) {
                    Component quote = getFeatureConfig().getRandomQuote();

                    //do we have books?
                    if (quote != null) {
                        //send a random quote
                        plugin.getMessageManager().sendLang(ePlayer, QuotesLangPath.GET_QUOTE,
                            Placeholder.component(StandartPlaceHolders.TEXT.getPlaceholder(), quote));
                    }
                }
            }
        }
    }
}
