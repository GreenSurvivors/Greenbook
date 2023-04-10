package de.greensurvivors.greenbook.listener;

import de.greensurvivors.greenbook.PermissionUtils;
import de.greensurvivors.greenbook.language.Lang;
import de.greensurvivors.greenbook.utils.LocationUtil;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * teleports a player up / down depending where on a lift sign they click to another lift sign
 */
public class LiftListener implements Listener {
    private enum LiftType {
        //tps up
        UP("[Lift Up]"),
        //tps down
        DOWN("[Lift Down]"),
        //tps up or down, depending on where the player looked, defaults to down
        BOTH("[Lift UpDown]"),
        //just let other signs tp to this floor
        STOP("[Lift]");

        //sting representation
        private final String label;
        //pattern to easy match the label against a string
        private final Pattern pattern;

        LiftType(String label) {
            this.label = label;
            //case-insensitive regex with the square brackets escaped
            this.pattern = Pattern.compile(String.format("(?i)%s", label.replace("[", "\\[")));
        }

        /**
         * Get the label of this lift type.
         * @return The label
         */
        public @NotNull String getLabel() {
            return this.label;
        }

        /**
         * Get the lift type from this line.
         * @param line The line
         * @return The lift type, or null if no with the given component was defined
         */
        public static @Nullable LiftType fromLabel(Component line) {
            for (LiftType liftType : values()) {
                if (liftType.pattern.matcher(PlainTextComponentSerializer.plainText().serialize(line)).matches()){
                    return liftType;
                }
            }

            return null;
        }
    }

    //pattern for signs to specify what floor they should tp to
    private static final Pattern destinationPattern = Pattern.compile("to:(.*)");

    //this class keeps track of its own instance, so it's basically static
    private static LiftListener instance;

    private LiftListener(){}

    /**
     * static to instance translator
     */
    public static LiftListener inst() {
        if (instance == null) {
            instance = new LiftListener();
        }
        return instance;
    }

    //todo maybe wax the sign automatically in 1.20

    /**
     * checks for permission, set case correctly and gives feedback if a lift sign was placed
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onSignChange(SignChangeEvent event) {
        Component line1 = event.line(1);
        Player ePlayer = event.getPlayer();

        //if the 2nd line exists
        if (line1 != null) {
            //and is a lift label
            LiftType type = LiftType.fromLabel(line1);
            if (type != null){
                //
                if (PermissionUtils.hasPermission(ePlayer, PermissionUtils.GREENBOOK_LIFT_WILDCARD, PermissionUtils.GREENBOOK_LIFT_CREATE)){
                    //set the line with the right casing, but keep the decorations
                    event.line(1, PlainTextComponentSerializer.plainText().deserialize(type.getLabel()).decorations(line1.decorations()));

                    ePlayer.sendMessage(Lang.build(Lang.LIFT_CREATE_SUCCESS.get()));
                } else {
                    ePlayer.sendMessage(Lang.build(Lang.NO_PERMISSION_SOMETHING.get()));

                    event.setCancelled(true);
                    event.getBlock().breakNaturally();
                }
            } //no lift
        } //no 2nd line
    }

    /**
     * get the destination sign, from an origin sign
     * @param originSign gives location and matches supposed destination against a found sign
     * @param type type of the origion sign
     * @param useBothUp if we search up or down if a bí directional sign was used
     * @return the found destination or null if non was found
     */
    private @Nullable Sign getDestination(@NotNull Sign originSign, @NotNull LiftType type,  boolean useBothUp){
        //1 means 1 block up, -1 means 1 block down
        int step;
        switch (type) {
            case UP -> step = 1;
            case DOWN -> step = -1;
            case BOTH -> step = useBothUp ? 1 : -1;
            default -> {//stop or unknown type
                return null;
            }
        }

        //cache world we are in
        final World world = originSign.getWorld();
        //get starting coords
        //lifts only go up / down, so x and z always stay the same
        final int x = originSign.getX(), starty = originSign.getY() + step, z = originSign.getZ();
        //is the world max or min height the point to stop for a sign?
        final int maxSearchCoord = step > 0 ? world.getMaxHeight() : world.getMinHeight();

        //try to extract the flor from "to:<floor name>"
        Matcher matcher = destinationPattern.matcher(PlainTextComponentSerializer.plainText().serialize(originSign.line(2)));
        String destinationStr = matcher.matches() ? matcher.group(1) : null;

        //loop through the blocks in the world, trying to find a sign
        for (int y = starty; y != maxSearchCoord; y += step){
            if (world.getBlockState(x, y, z) instanceof Sign destinationSign){
                //is the found sign a lift?
                if (LiftType.fromLabel(destinationSign.line(1)) != null &&
                        //no destination string was given or it matches
                        (destinationStr == null || PlainTextComponentSerializer.plainText().serialize(destinationSign.line(0)).equalsIgnoreCase(destinationStr))){

                    //found the sign
                    return destinationSign;
                }
            }
        }

        return null;
    }

    /**
     * trys to teleports a player relative in y direction, with passager / vehicle
     * will try to go up/down 5 blocks if the destination was not safe, but never to the height the player is already on
     * @param player
     * @param dy distance in y relative to the player coordinates
     * @param floorName the name of the floor the player will be teleported to, used in feedback message
     */
    private void liftTeleport(@NotNull Player player, double dy, @NotNull String floorName){
        //try to get a safe location to teleport to, up to 5 blocks difference,
        //might be null if no was found
        Location destination = LocationUtil.getSafeLiftDestination(player.getLocation(), dy > 0, player.getLocation().add(0, dy, 0));

        if (destination != null){
            boolean teleported;

            //try to teleport
            if (player.getVehicle() == null){
                teleported = player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN,
                        TeleportFlag.Relative.PITCH, TeleportFlag.Relative.YAW,
                        TeleportFlag.EntityState.RETAIN_PASSENGERS);
            } else {
                teleported = player.getVehicle().teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN,
                        TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
            }

            if (teleported){
                //success! give feedback to the player
                if (floorName.isBlank()){
                    if (dy > 0){
                        player.sendMessage(Lang.build(Lang.LIFT_USED_UP.get()));
                    } else {
                        player.sendMessage(Lang.build(Lang.LIFT_USED_DOWN.get()));
                    }
                } else {
                    player.sendMessage(Lang.build(Lang.LIFT_USED_FLOOR.get().replace(Lang.VALUE, floorName)));
                }
            } else { //what?
                player.sendMessage(Lang.build(Lang.UNKNOWN_ERROR.get()));
            }
        } else {
            player.sendMessage(Lang.build(Lang.LIFT_DESTINATION_OBSTRUCTED.get()));
        }
    }

    /**
     * common part regardless if the lift was directly clicked or a button was used
     * @param originSign the sign that was activated
     * @param player the player who was activating a sign
     * @param useBothUp if the player should teleported up or down, if the lift is a bidirectional lift
     */
    private void useLift(Sign originSign, Player player, boolean useBothUp){
        LiftType type = LiftType.fromLabel(originSign.line(1));

        //is it a lift?
        if (type != null){
            if (type == LiftType.STOP){
                //you can only arrive at stops
                player.sendMessage(Lang.build(Lang.LIFT_USED_STOP.get()));
                return;
            }

            //check permission
            if (PermissionUtils.hasPermission(player, PermissionUtils.GREENBOOK_LIFT_WILDCARD, PermissionUtils.GREENBOOK_LIFT_USE)) {
                //get destination
                Sign destinationSign = getDestination(originSign, type, useBothUp);
                if (destinationSign != null) {
                    double dy = destinationSign.getLocation().getY() - originSign.getLocation().getY();

                    //finally teleport the player
                    liftTeleport(player, dy, LegacyComponentSerializer.legacyAmpersand().serialize(destinationSign.line(0)));
                } else {
                    player.sendMessage(Lang.build(Lang.LIFT_DESTINATION_UNKNOWN.get()));
                    return;
                } //no destination
            } else {
                player.sendMessage(Lang.build(Lang.NO_PERMISSION_SOMETHING.get()));
                return;
            }
        } //not interacted with a lift
    }

    /**
     * a lift can be indirectly activated via a button
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onRightUseButton(PlayerInteractEvent event) {
        //don't fire for offhand
        if (event.getHand() == EquipmentSlot.HAND &&
                //right-clicked a block. Should ensure the getBlock() is not null
                event.getAction() == Action.RIGHT_CLICK_BLOCK){
            //cache clicked block
            Block eBlock = event.getClickedBlock();

            //is the block a button?
            if (eBlock != null && Tag.BUTTONS.isTagged(eBlock.getType())){
                //and the block 2 behind a sign?
                Block block = eBlock.getRelative(((Directional)eBlock.getBlockData()).getFacing().getOppositeFace(), 2);
                if (block.getState() instanceof Sign origionSign){
                    //Then try to handle the sign as a lift
                    useLift(origionSign, event.getPlayer(), false); //default: teleport down if a bidirectional sign is powert
                }
            } //not interacted with a button
        }
    }

    /**
     * a lift can be used by directly clicking a sign
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onRightClickSign(PlayerInteractEvent event) {
        //don't fire for offhand
        if (event.getHand() == EquipmentSlot.HAND &&
                //right-clicked a block. Should ensure the getBlock() is not null
                event.getAction() == Action.RIGHT_CLICK_BLOCK){
            //cache the clicked block
            Block eBlock = event.getClickedBlock();

            //is the clicked block a sign?
            if (eBlock != null && eBlock.getState() instanceof Sign origionSign){

                //determine if teleport should go up or down, in case of a bidirectional lift
                boolean useBothUp;
                if (event.getInteractionPoint() == null){
                    useBothUp = false;
                } else {
                    double relativeHeight = event.getInteractionPoint().getY();
                    useBothUp = (relativeHeight - (int) relativeHeight) >= 0.5;
                }

                //try to handle the sign as a lift
                useLift(origionSign, event.getPlayer(), useBothUp);
            } //not interacted with a sign
        }
    }
}
