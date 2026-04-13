package de.greensurvivors.greenbook.features.painting;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.features.AFeature;
import de.greensurvivors.greenbook.features.FeatureType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.bukkit.Art;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

public class PaintingFeature extends AFeature<PaintingConfigManager> implements Listener {
    //map holding every player uuid who's currently editing a painting
    //every player edits only one painting and every painting gets only edited by one player
    private final @NotNull BidiMap<@NotNull UUID, @NotNull UUID> modifyingMap = new DualHashBidiMap<>(); //uuid player, uuid painting

    public PaintingFeature(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.PAINTING, new PaintingConfigManager(plugin));
    }

    @Override
    public void registerCommands(final @NotNull Commands commandsRegistrar, final @NotNull GreenBookCmd mainCommand) {
        PaintingSubCommand subCommand = new PaintingSubCommand(plugin, this, mainCommand.getPermission());

        mainCommand.registerSubcommand(subCommand, subCommand.getCmdNodes());
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);

        //remove all players who are currently editing a painting
        for (final @NotNull UUID uuid : modifyingMap.values()) {
            modifyingMap.remove(uuid);

            final @Nullable Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                //message the player

                plugin.getMessageManager().sendLang(player, PaintingLangPath.PAINTING_EDIT_STOPPED);
            }
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * check if a player and a player are still in range of each other, for the player to modify the painting
     *
     * @param player   player to ReflectHelper for
     * @param painting painting to ReflectHelper for
     * @return true if player and painting are in the same world and in modifying range
     */
    private boolean isInEditingRange(final @NotNull Player player, final @NotNull Painting painting) {
        if (player.getWorld() == painting.getWorld()) { //check world
            //check distance. Note: we compare the distance squared with the range squared, since it's faster than the root
            return player.getLocation().distanceSquared(painting.getLocation()) <= getFeatureConfig().getModifyRangeSqr();
        }

        return false;
    }


    /**
     * get the player who is editing a given painting or null if there is none
     * note: if a player was found, it checks also if the player is still in editing range
     *
     * @param painting the painting in question
     * @return null if no player was found or the player is not in range any more
     */
    private @Nullable Player getEditingPlayer(final @NotNull Painting painting) {
        final @Nullable UUID uuidPlayer = modifyingMap.inverseBidiMap().get(painting.getUniqueId());

        if (uuidPlayer != null) {
            final @Nullable Player player = Bukkit.getPlayer(uuidPlayer);

            //is still in editing range?
            if (player != null && isInEditingRange(player, painting)) {
                return player;
            } else { //nope is not in range
                //removes the player from the tracked ones
                modifyingMap.remove(uuidPlayer);

                if (player != null) { //message player if online
                    plugin.getMessageManager().sendLang(player, PaintingLangPath.PAINTING_EDIT_OUTSIDE_RANGE);
                }
            }
        }

        return null;
    }

    /**
     * stop editing a painting if it dies
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPaintingDestroy(final @NotNull HangingBreakByEntityEvent event) {
        if (event.getEntity() instanceof final @NotNull Painting painting) {
            Player player = getEditingPlayer(painting);

            //removes the painting from the tracked ones
            modifyingMap.inverseBidiMap().remove(painting.getUniqueId());

            //message the player
            if (player != null) {
                plugin.getMessageManager().sendLang(player, PaintingLangPath.PAINTING_EDIT_STOPPED);
            }
        }
    }


    /**
     * stop editing a painting if the editing player changes worlds
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onWorldChange(final @NotNull PlayerChangedWorldEvent event) {
        final @NotNull UUID uuidPlayer = event.getPlayer().getUniqueId();

        //if the player is currently editing a painting remove them from beeing tracked
        if (modifyingMap.get(uuidPlayer) != null) {
            modifyingMap.remove(uuidPlayer);

            //player feedback
            plugin.getMessageManager().sendLang(event.getPlayer(), PaintingLangPath.PAINTING_EDIT_STOPPED);
        }
    }

    /**
     * stop editing a painting if the player quits
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onQuit(final @NotNull PlayerQuitEvent event) {
        modifyingMap.remove(event.getPlayer().getUniqueId());
    }

    /**
     * stop editing a painting if the player gets kicked
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onKick(final @NotNull PlayerKickEvent event) {
        modifyingMap.remove(event.getPlayer().getUniqueId());
    }

    /**
     * if a player right-clicks a painting, they get linked and the player can switch the motives,
     * if they have the permission to do so
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPaintingInteract(final @NotNull PlayerInteractEntityEvent event) {
        //just listen to main hand and check if the right-clicked entity is a painting
        if (event.getHand() == EquipmentSlot.HAND &&
            event.getRightClicked() instanceof final @NotNull Painting paint) {
            //cache the player
            final @NotNull Player ePlayer = event.getPlayer();

            //check permission
            if (ePlayer.hasPermission(PaintingPermissions.CHANGE_PAINTING.getPermission())) {
                final @Nullable Player other = getEditingPlayer(paint);

                if (other == null) { //no one is currently editing this painting
                    modifyingMap.put(ePlayer.getUniqueId(), paint.getUniqueId());
                    plugin.getMessageManager().sendLang(ePlayer, PaintingLangPath.PAINTING_EDIT_START);

                    //event.setCancelled(true);
                    // the player already was editing this painting and now wants to stop
                } else if (other.getUniqueId() == ePlayer.getUniqueId()) {
                    modifyingMap.remove(ePlayer.getUniqueId());
                    plugin.getMessageManager().sendLang(ePlayer, PaintingLangPath.PAINTING_EDIT_STOPPED);

                    //event.setCancelled(true);
                    //another player is currently editing the painting, so another one can't
                    //that's a technical requirement, because we use a bimap to easy access both sides easily
                } else {
                    plugin.getMessageManager().sendLang(ePlayer, PaintingLangPath.PAINTING_EDIT_IN_USE);
                }
            } else {
                //no permission
                // this is silent to not annoy
                //plugin.getMessageManager().sendLang(ePlayer, LangPath.NO_PERMISSION);
            }
        }
    }

    /**
     * switch between motives of the linked painting when scrolling (changing hotbar slot)
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onItemHeldChange(final @NotNull PlayerItemHeldEvent event) {
        //the player who changed the item in their hand
        final @NotNull Player ePlayer = event.getPlayer();

        //try to get the uuid of the painting a player might be linked to
        final @Nullable UUID paintingUUID = modifyingMap.get(ePlayer.getUniqueId());
        if (paintingUUID != null) {
            //we got an entity. Just to be sure is it a painting or did bukkit spawned a new entity with the same uuid?
            //also is the player still in range?
            final @Nullable Entity entity = Bukkit.getEntity(paintingUUID);
            if (entity instanceof final @NotNull Painting painting && painting.isValid() &&
                isInEditingRange(ePlayer, painting)) {
                //only do something if the selected hotbar slot changed
                if (event.getNewSlot() != event.getPreviousSlot()) {
                    //get the direction of changing the motive, scrolling to the right is always forward,
                    //scrolling to the left always backward, even if the slot id jumps back to the other end
                    //8 -> 0 forward
                    //0 -> 8 backward
                    //0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8 forward
                    final boolean forward = ((event.getPreviousSlot() == 8) && (event.getNewSlot() == 0)) ||
                        ((event.getNewSlot() > event.getPreviousSlot()) &&
                            !(event.getPreviousSlot() == 0 && event.getNewSlot() == 8));
                    //now get the steps from the direction. 1 moves the motive forward, -1 backward
                    final int step = forward ? 1 : -1;

                    //get an array of all motives
                    final @NotNull Registry<Art> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.PAINTING_VARIANT);
                    final @NotNull Art @NotNull [] arts = registry.stream().sorted(Comparator.comparing(Art::assetId)).toArray(Art[]::new);

                    int oldOrd = 0;
                    for (final @NotNull Art art : arts) {
                        if (art.assetId().equals(painting.getArt().assetId())) {
                            break;
                        }
                        oldOrd++;
                    }

                    //get the mathematically accurate modulo, since the %-operator will be negative, if the first argument is negative
                    //this catches the ord value in range of all possible motives
                    //starting value
                    int newOrd = Math.floorMod(oldOrd + step, arts.length);

                    //try to set the motive, if it doesn't fit, try the next one.
                    //if the last was reached start over
                    while (!painting.setArt(arts[newOrd])) {
                        newOrd = Math.floorMod(newOrd + step, arts.length);
                    }
                } //slot didn't change
            } else { // painting died or out of range
                //remove from tracked and give feedback to the player
                modifyingMap.remove(ePlayer.getUniqueId());
                plugin.getMessageManager().sendLang(ePlayer, PaintingLangPath.PAINTING_EDIT_STOPPED);
            }
        }
    }
}
