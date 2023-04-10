package de.greensurvivors.greenbook.listener;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.greensurvivors.greenbook.PermissionUtils;
import de.greensurvivors.greenbook.language.Lang;
import org.bukkit.Art;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * right-clicking a painting can enable a player to switch between all possible motives in the given space
 */
public class PaintingListener implements Listener {
    //map holding every player uuid who's currently editing a painting
    //every player edits only one painting and every painting gets only edited by one player
    private final BiMap<UUID, UUID> modifyingMap = HashBiMap.create(); //uuid player, uuid painting
    //maximum range a player can be and still editing the same painting
    private int modifyRange = 16;

    //this class keeps track of its own instance, so it's basically static
    private static PaintingListener instance;

    private PaintingListener(){}

    /**
     * static to instance translator
     */
    public static PaintingListener inst() {
        if (instance == null) {
            instance = new PaintingListener();
        }
        return instance;
    }

    /**
     * set the currently used range, a player can still switch painting motives
     * @param range new range
     */
    public void setModifyRange(int range){
        this.modifyRange = range;
    }

    /**
     * clears all data and prepares to shut down.
     */
    public void clear(){
        modifyingMap.clear();
    }

    /**
     * check if a player and a player are still in range of each other, for the player to modify the painting
     * @param player player to test for
     * @param painting painting to test for
     * @return true if player and painting are in the same world and in modifying range
     */
    private boolean isInEditingRange(@NotNull Player player, @NotNull Painting painting){
        if (player.getWorld() == painting.getWorld()){ //check world
            //check distance. Note: we compare the distance squared with the range squared, since it's faster than the root
            return player.getLocation().distanceSquared(painting.getLocation()) <= modifyRange*modifyRange;
        }

        return false;
    }

    /**
     * get the player who is editing a given painting or null if there is none
     * note: if a player was found, it checks also if the player is still in editing range
     * @param painting the painting in question
     * @return null if no player was found or the player is not in range any more
     */
    private @Nullable Player getEditingPlayer (@NotNull Painting painting){
        UUID uuidPlayer = modifyingMap.inverse().get(painting.getUniqueId());

        if (uuidPlayer != null){
            Player player = Bukkit.getPlayer(uuidPlayer);

            //is still in editing range?
            if (player != null && isInEditingRange(player, painting)){
                return player;
            } else { //nope is not in range
                //removes the player from the tracked ones
                modifyingMap.remove(uuidPlayer);

                if (player != null){ //message player if online
                    player.sendMessage(Lang.build(Lang.PAINTING_EDITING_OUTSIDERANGE.get()));
                }
            }
        }

        return null;
    }

    /**
     * stop editing a painting if it dies
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPaintingDestroy(HangingBreakByEntityEvent event){
        if (event.getEntity() instanceof Painting painting){
            Player player = getEditingPlayer(painting);

            //removes the painting from the tracked ones
            modifyingMap.inverse().remove(painting.getUniqueId());

            //message the player
            if (player != null){
                player.sendMessage(Lang.build(Lang.PAINTING_EDITING_STOPPED.get()));
            }
        }
    }

    /**
     * stop editing a painting if the editing player changes worlds
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onWorldChange(PlayerChangedWorldEvent event){
        UUID uuidPlayer = event.getPlayer().getUniqueId();

        //if the player is currently editing a painting remove them from beeing tracked
        if (modifyingMap.get(uuidPlayer) != null){
            modifyingMap.remove(uuidPlayer);

            //player feedback
            event.getPlayer().sendMessage(Lang.build(Lang.PAINTING_EDITING_STOPPED.get()));
        }
    }

    /**
     * stop editing a painting if the player quits
     */
    @EventHandler(ignoreCancelled = true,priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event){
        modifyingMap.remove(event.getPlayer().getUniqueId());
    }

    /**
     * stop editing a painting if the player gets kicked
     */
    @EventHandler(ignoreCancelled = true,priority = EventPriority.MONITOR)
    private void onKick(PlayerKickEvent event){
        modifyingMap.remove(event.getPlayer().getUniqueId());
    }

    /**
     * if a player right-clicks a painting, they get linked and the player can switch the motives,
     * if they have the permission to do so
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPaintingInteract(PlayerInteractEntityEvent event){
        //just listen to main hand and check if the right-clicked entity is a painting
        if (event.getHand() == EquipmentSlot.HAND &&
                event.getRightClicked() instanceof Painting paint) {
            //cache the player
            Player ePlayer = event.getPlayer();

            //check permission
            if (PermissionUtils.hasPermission(ePlayer, PermissionUtils.GREENBOOK_PAINTING_EDIT)){
                Player other = getEditingPlayer(paint);

                if (other == null){ //no one is currently editing this painting
                    modifyingMap.put(ePlayer.getUniqueId(), paint.getUniqueId());
                    ePlayer.sendMessage(Lang.build(Lang.PAINTING_EDITING_STARTED.get()));

                    //event.setCancelled(true);
                // the player already was editing this painting and now wants to stop
                } else if (other.getUniqueId() == ePlayer.getUniqueId()) {
                    modifyingMap.remove(ePlayer.getUniqueId());
                    ePlayer.sendMessage(Lang.build(Lang.PAINTING_EDITING_STOPPED.get()));

                    //event.setCancelled(true);
                //another player is currently editing the painting, so another one can't
                //that's a technical requirement, because we use a bimap to easy access both sides easily
                } else {
                    ePlayer.sendMessage(Lang.build(Lang.PAINTING_EDITING_INUSE.get()));
                }
            } else {
                //no permission
                ePlayer.sendMessage(Lang.build(Lang.NO_PERMISSION_SOMETHING.get()));
            }
        }
    }

    /**
     * switch between motives of the linked painting when scrolling (changing hotbar slot)
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onItemHeldChange(PlayerItemHeldEvent event){
        //the player who changed the item in their hand
        Player ePlayer = event.getPlayer();

        //try to get the uuid of the painting a player might be linked to
        UUID paintingUUID = modifyingMap.get(ePlayer.getUniqueId());
        if (paintingUUID != null){
            //we got an entity. Just to be sure is it a painting or did bukkit spawned a new entity with the same uuid?
            //also is the player still in range?
            Entity entity = Bukkit.getEntity(paintingUUID);
            if (entity instanceof Painting painting && painting.isValid() &&
                    isInEditingRange(ePlayer, painting)){
                //only do something if the selected hotbar slot changed
                if (event.getNewSlot() != event.getPreviousSlot()){
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
                    Art[] arts = Art.values();
                    //get the mathematically accurate modulo, since the %-operator will be negative, if the first argument is negative
                    //this catches the ord value in range of all possible motives
                    //starting value
                    int newOrd = Math.floorMod(painting.getArt().ordinal() + step, arts.length);

                    //try to set the motive, if it doesn't fit, try the next one.
                    //if the last was reached start over
                    while (!painting.setArt(arts[newOrd])){
                        newOrd = Math.floorMod(newOrd + step, arts.length);
                    }
                } //slot didn't change
            } else { // painting died or out of range
                //remove from tracked and give feedback to the player
                modifyingMap.remove(ePlayer.getUniqueId());
                ePlayer.sendMessage(Lang.build(Lang.PAINTING_EDITING_STOPPED.get()));
            }
        }
    }
}
