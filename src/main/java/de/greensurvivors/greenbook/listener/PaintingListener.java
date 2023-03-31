package de.greensurvivors.greenbook.listener;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.greensurvivors.greenbook.GreenLogger;
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
import java.util.logging.Level;

//switch paitings
public class PaintingListener implements Listener {
    private final BiMap<UUID, UUID> modifyingMap = HashBiMap.create(); //uuid player, uuid painting
    private int modifyRange = 16;

    private static PaintingListener instance;

    public static PaintingListener inst() {
        if (instance == null) {
            instance = new PaintingListener();
        }
        return instance;
    }

    public void setModifyRange(int range){ //todo
        this.modifyRange = range;
    }

    public void clear(){
        modifyingMap.clear();
    }

    private boolean isInEditingRange(Player player, Painting painting){
        if (player.getWorld() == painting.getWorld()){ //check world
            return player.getLocation().distanceSquared(painting.getLocation()) <= modifyRange*modifyRange;
        }

        return false;
    }

    private @Nullable Player getEditingPlayer (@NotNull Painting painting){
        UUID uuidPlayer = modifyingMap.inverse().get(painting.getUniqueId());

        if (uuidPlayer != null){
            Player player = Bukkit.getPlayer(uuidPlayer);

            //is still in editing range?
            if (player != null && isInEditingRange(player, painting)){
                return player;
            } else {
                modifyingMap.remove(uuidPlayer);

                if (player != null){ //message player if online
                    player.sendMessage(Lang.build(Lang.PAINTING_EDITING_OUTSIDERANGE.get()));
                }
            }
        }

        return null;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPaintingDestroy(HangingBreakByEntityEvent event){
        GreenLogger.log(Level.INFO, "destroyed");
        if (event.getEntity() instanceof Painting painting){
            Player player = getEditingPlayer(painting);

            modifyingMap.inverse().remove(painting.getUniqueId());

            if (player != null){
                player.sendMessage(Lang.build(Lang.PAINTING_EDITING_STOPPED.get()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onWorldChange(PlayerChangedWorldEvent event){
        UUID uuidPlayer = event.getPlayer().getUniqueId();

        if (modifyingMap.get(uuidPlayer) != null){
            modifyingMap.remove(uuidPlayer);

            event.getPlayer().sendMessage(Lang.build(Lang.PAINTING_EDITING_STOPPED.get()));
        }
    }

    @EventHandler(ignoreCancelled = true,priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event){
        modifyingMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true,priority = EventPriority.MONITOR)
    private void onKick(PlayerKickEvent event){
        modifyingMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPaintingInteract(PlayerInteractEntityEvent event){
        if (event.getHand() == EquipmentSlot.HAND &&
                event.getRightClicked() instanceof Painting paint) {
            Player ePlayer = event.getPlayer();

            if (PermissionUtils.hasPermission(ePlayer, PermissionUtils.GREENBOOK_PAINTING_EDIT)){
                Player other = getEditingPlayer(paint);

                if (other == null){ //no one is currently editing this painting
                    modifyingMap.put(ePlayer.getUniqueId(), paint.getUniqueId());
                    ePlayer.sendMessage(Lang.build(Lang.PAINTING_EDITING_STARTED.get()));

                    //event.setCancelled(true);
                } else if (other.getUniqueId() == ePlayer.getUniqueId()) { //the player already was editing this painting and now wants to stop
                    modifyingMap.remove(ePlayer.getUniqueId());
                    ePlayer.sendMessage(Lang.build(Lang.PAINTING_EDITING_STOPPED.get()));

                    //event.setCancelled(true);
                } else { //another player is currently editing the painting
                    ePlayer.sendMessage(Lang.build(Lang.PAINTING_EDITING_INUSE.get()));
                }
            } else {
                ePlayer.sendMessage(Lang.build(Lang.NO_PERMISSION_SOMETHING.get()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onItemHeldChange(PlayerItemHeldEvent event){
        Player ePlayer = event.getPlayer();

        UUID paintingUUID = modifyingMap.get(ePlayer.getUniqueId());

        if (paintingUUID != null){
            Entity entity = Bukkit.getEntity(paintingUUID);

            if (entity instanceof Painting painting && painting.isValid() &&
                    isInEditingRange(ePlayer, painting)){
                if (event.getNewSlot() != event.getPreviousSlot()){
                    //8 -> 0 forward
                    //0 -> 8 backward
                    //0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8 forward
                    final boolean forward = ((event.getPreviousSlot() == 8) && (event.getNewSlot() == 0)) ||
                            ((event.getNewSlot() > event.getPreviousSlot()) &&
                                    !(event.getPreviousSlot() == 0 && event.getNewSlot() == 8));

                    final int direction = forward ? 1 : -1;

                    Art[] arts = Art.values();
                    int newOrd = Math.floorMod(painting.getArt().ordinal() + direction, arts.length);
                    GreenLogger.log(Level.INFO, "1ord: " + newOrd);


                    while (!painting.setArt(arts[newOrd])){
                        newOrd = Math.floorMod(newOrd + direction, arts.length);
                        GreenLogger.log(Level.INFO, "2ord: " + newOrd);
                    }
                }
            } else { // painting died or out of range
                modifyingMap.remove(ePlayer.getUniqueId());
                ePlayer.sendMessage(Lang.build(Lang.PAINTING_EDITING_STOPPED.get()));
            }
        }
    }
}
