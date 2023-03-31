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

public class LiftListener implements Listener {
    private enum LiftType {
        UP("[Lift Up]"),
        DOWN("[Lift Down]"),
        BOTH("[Lift UpDown]"),
        STOP("[Lift]");

        private final String label;
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
         * @return The lift type, or null
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

    private static final Pattern destinationPattern = Pattern.compile("to:(.*)");

    private static LiftListener instance;

    public static LiftListener inst() {
        if (instance == null) {
            instance = new LiftListener();
        }
        return instance;
    }

    //todo maybe wax the sign automatically in 1.20
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
                if (PermissionUtils.hasPermission(ePlayer, PermissionUtils.GREENBOOK_LIFT_ADMIN, PermissionUtils.GREENBOOK_LIFT_CREATE)){
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

    private Sign getDestination(@NotNull Sign origionSign, @NotNull LiftType type,  boolean useBothUp){
        int direction;
        switch (type) {
            case UP -> direction = 1;
            case DOWN -> direction = -1;
            case BOTH -> direction = useBothUp ? 1 : -1;
            default -> {//stop or unknown
                return null;
            }
        }

        World world = origionSign.getWorld();
        int x = origionSign.getX(), starty = origionSign.getY() + direction, z = origionSign.getZ();
        int maxSearchCoord = direction > 0 ? world.getMaxHeight() : world.getMinHeight();

        Matcher matcher = destinationPattern.matcher(PlainTextComponentSerializer.plainText().serialize(origionSign.line(2)));
        String destinationStr = matcher.matches() ? matcher.group(1) : null;

        for (int y = starty; y != maxSearchCoord; y += direction){
            if (world.getBlockState(x, y, z) instanceof Sign destinationSign){
                //destination is lift
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
     * teleports a player relative in y direction,
     * @param player
     * @param dy
     */
    private void liftTeleport(@NotNull Player player, double dy, @NotNull String floorName){
        Location relative = player.getLocation().add(0, dy, 0);

        Location destination = LocationUtil.getSafeLiftDestination(player.getLocation(), dy > 0, relative);
        if (destination != null){
            boolean teleported;

            if (player.getVehicle() == null){
                teleported = player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN,
                        TeleportFlag.Relative.PITCH, TeleportFlag.Relative.YAW,
                        TeleportFlag.EntityState.RETAIN_PASSENGERS);
            } else {
                teleported = player.getVehicle().teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN,
                        TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
            }

            if (teleported){
                if (floorName.isBlank()){
                    if (dy > 0){
                        player.sendMessage(Lang.build(Lang.LIFT_USED_UP.get()));
                    } else {
                        player.sendMessage(Lang.build(Lang.LIFT_USED_DOWN.get()));
                    }
                } else {
                    player.sendMessage(Lang.build(Lang.LIFT_USED_FLOOR.get().replace(Lang.VALUE, floorName)));
                }
            } else {
                player.sendMessage(Lang.build(Lang.UNKNOWN_ERROR.get()));
            }
        } else {
            player.sendMessage(Lang.build(Lang.LIFT_DESTINATION_OBSTRUCTED.get()));
        }
    }

    private void useLift(Sign origionSign, Player player, boolean useBothUp){
            LiftType type = LiftType.fromLabel(origionSign.line(1));

            if (type != null){
                //user clicked on a lift.
                //event.setCancelled(true); //we are monitoring the event, we shouldn't cancel it. it's probably fine anyway.

                if (type == LiftType.STOP){
                    //you can only arrive at stops
                    player.sendMessage(Lang.build(Lang.LIFT_USED_STOP.get()));
                    return;
                }

                //check permission
                if (PermissionUtils.hasPermission(player, PermissionUtils.GREENBOOK_LIFT_ADMIN, PermissionUtils.GREENBOOK_LIFT_USE)) {
                    Sign destinationSign = getDestination(origionSign, type, useBothUp);

                    if (destinationSign != null) {
                        double dy = destinationSign.getLocation().getY() - origionSign.getLocation().getY();

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onRightUseButton(PlayerInteractEvent event) {
        Block eBlock = event.getClickedBlock();
        Player ePlayer = event.getPlayer();

        //don't fire for offhand
        if (event.getHand() == EquipmentSlot.HAND &&
                //right-clicked a block. Should ensure the getBlock() is not null
                event.getAction() == Action.RIGHT_CLICK_BLOCK){

            if (eBlock != null && Tag.BUTTONS.isTagged(eBlock.getType())){
                Block block = eBlock.getRelative(((Directional)eBlock.getBlockData()).getFacing().getOppositeFace(), 2);

                if (block.getState() instanceof Sign origionSign){
                    useLift(origionSign, ePlayer, false); //default: teleport down if a bidirectional sign is powert
                }
            } //not interacted with a button
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onRightClickSign(PlayerInteractEvent event) {
        Block eBlock = event.getClickedBlock();
        Player ePlayer = event.getPlayer();

        //don't fire for offhand
        if (event.getHand() == EquipmentSlot.HAND &&
                //right-clicked a block. Should ensure the getBlock() is not null
                event.getAction() == Action.RIGHT_CLICK_BLOCK){

            if (eBlock != null && eBlock.getState() instanceof Sign origionSign){

                //determine if teleport should go up or down, in case of a bidirectional lift
                boolean useBothUp;
                if (event.getInteractionPoint() == null){
                    useBothUp = false;
                } else {
                    double relativeHeight = event.getInteractionPoint().getY();
                    useBothUp = (relativeHeight - (int) relativeHeight) >= 0.5;
                }

                useLift(origionSign, ePlayer, useBothUp);
            } //not interacted with a sign
        }
    }
}
