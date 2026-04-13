package de.greensurvivors.greenbook.features.lift;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.features.AFeature;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.language.StandardLangPath;
import de.greensurvivors.greenbook.language.StandartPlaceHolders;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Entity;
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

/**
 * teleports a player up / down depending where on a lift sign they click to another lift sign
 */
public class LiftFeature extends AFeature<LiftConfigManager> implements Listener {
    public LiftFeature(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.LIFT, new LiftConfigManager(plugin));

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void registerCommands(@NotNull Commands commandsRegistrar, @NotNull GreenBookCmd mainCommand) {
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onEnable() {

    }

    /**
     * checks for permission, set case correctly and gives feedback if a lift sign was placed
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onSignChange(@NotNull SignChangeEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        Component line1 = event.line(1);
        Player ePlayer = event.getPlayer();

        //if the 2nd line exists
        if (line1 != null) {
            //and is a lift label
            LiftType type = getFeatureConfig().fromLabel(line1);
            if (type != null) {
                // only handle front sides
                if (event.getSide() == Side.FRONT) {
                    // check permission
                    if (ePlayer.hasPermission(LiftPermissions.LIFT_CREATE.getPermission())) {
                        //set the line with the right casing
                        event.line(1, getFeatureConfig().getLabel(type));

                        plugin.getMessageManager().sendLang(ePlayer, LiftLangPath.LIFT_CREATE_SUCCESS);

                        // auto wax to don't annoy users opening editing screen every time they try to use a lift
                        if (event.getBlock().getState(false) instanceof Sign sign) { // danger no snapshot
                            sign.setWaxed(true);
                            sign.update(); //todo test if we have to call update if we don't use a snapshot
                        }
                    } else { //no permission
                        plugin.getMessageManager().sendLang(ePlayer, StandardLangPath.NO_PERMISSION);

                        event.setCancelled(true);
                        event.getBlock().breakNaturally();
                    }
                } else { //backside
                    plugin.getMessageManager().sendLang(ePlayer, StandardLangPath.ERROR_SIGN_BACKSIDE);
                    event.line(1, Component.empty());
                    event.setCancelled(true);
                }
            } //no lift
        } //no 2nd line
    }

    /**
     * get the destination sign, from an origin sign
     *
     * @param originSign gives location and matches supposed destination against a found sign
     * @param type       type of the origin sign
     * @return the found destination or null if non was found
     */
    private @Nullable Sign getDestination(@NotNull Sign originSign, @NotNull LiftType type, boolean isUpIfBoth) {
        //1 means 1 block up, -1 means 1 block down
        int step;
        switch (type) {
            case UP -> step = 1;
            case DOWN -> step = -1;
            case BOTH -> step = isUpIfBoth ? 1 : -1;
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
        final int maxSearchCoord = step > 0 ? world.getMaxHeight() - 1 : world.getMinHeight();

        //try to extract the floor from "to:<floor name>"
        final LiftConfigManager.DestinationMatcher destinationMatcher = getFeatureConfig().getDeDestinationMatcher(originSign.getSide(Side.FRONT).line(2));

        //loop through the blocks in the world, trying to find a sign
        for (int y = starty; y != maxSearchCoord; y += step) {
            if (world.getBlockState(x, y, z) instanceof Sign destinationSign) {
                //is the found sign a lift?
                if (getFeatureConfig().fromLabel(destinationSign.getSide(Side.FRONT).line(1)) != null &&
                    //no destination string was given or it matches
                    destinationMatcher.isDestination(destinationSign.getSide(Side.FRONT).line(0))) {

                    //found the sign
                    return destinationSign;
                }
            }
        }

        return null;
    }   //might be null if no location was found

    /**
     * try to get a safe location to teleport to, up to 5 blocks difference
     *
     * @param toTest player to try to get the Destination for
     * @param dy     the vertical distance counting form the postion of the Player toTest. negative means downwards.
     * @return safe (no damage will be taken) location to teleport to, will be null, if no location was found
     */
    private @Nullable Location getSafeLiftDestination(final @NotNull Entity toTest, double dy) {
        //the end location we try to get a safe location around
        final Location toLoc = toTest.getLocation().add(0, dy, 0);

        if (toLoc.getWorld() == null) {
            plugin.getComponentLogger().warn("Couldn't find safe destination for entity" + toTest + " at world null.");
            return null;
        }

        final World world = toLoc.getWorld();
        final double x = toLoc.getX();
        int y = toLoc.getBlockY();
        //y to start from, is imported to not teleport to the same y level
        int originY = toTest.getLocation().getBlockY();
        final double z = toLoc.getZ();

        int maxY = Math.min(toLoc.getBlockY() + 5, world.getMaxHeight());
        int minY = Math.max(toLoc.getBlockY() - 5, world.getMinHeight() + 1);

        //if the from-location gets over or under the end-location into play
        if (dy > 0) { //never teleport to the same floor the origin lift is on
            minY = Math.max(minY, originY + 1);
        } else {
            maxY = Math.min(maxY, originY - 1);
        }

        boolean foundSafeLoc = false;

        // todo optimize the code below since we are now in control of #isEntitySafeAt()
        // todo use cache to reduce expensive lookups
        if (plugin.getConfigManager().isEntitySafeAt(toTest, world, x, y, z)) { //this is purely optimizing, starting with i = 0 would have the same effect, but we would check the same location twice.
            foundSafeLoc = true;
        } else {
            for (int i = 1; i <= 5; i++) { //todo there is probably optimizing to be had here, since we are checking if a block was safe at least double.
                if (y + i <= maxY && //never teleport to the same floor the origin lift is on
                    plugin.getConfigManager().isEntitySafeAt(toTest, world, x, y + i, z)) { //check if the new location + i blocks is safe to stand on
                    foundSafeLoc = true;
                    y += i;
                    break;
                }

                if (y - i >= minY && //never teleport to the same floor the origin lift is on
                    plugin.getConfigManager().isEntitySafeAt(toTest, world, x, y - i, z)) { //check if the new location - i blocks is safe to stand on
                    foundSafeLoc = true;
                    y -= i;
                    break;
                }
            }
        }

        //no location where found.
        if (!foundSafeLoc) {
            return null;
        }

        return new Location(world, x, y, z, toLoc.getYaw(), toLoc.getPitch());
    }

    /**
     * trys to teleports a player relative in y direction, with passager / vehicle
     * will try to go up/down 5 blocks if the destination was not safe, but never to the height the player is already on
     *
     * @param player
     * @param dy        distance in y relative to the player coordinates
     * @param floorName the name of the floor the player will be teleported to, used in feedback message
     */
    private void liftTeleport(@NotNull Player player, double dy, @NotNull Component floorName) {
        //try to get a safe location to teleport to, up to 5 blocks difference,
        //might be null if no destination was found
        Location destination = getSafeLiftDestination(player, dy);

        if (destination != null) {
            boolean teleported;

            //try to teleport
            if (!player.isInsideVehicle()) {
                teleported = player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
            } else {
                teleported = player.getVehicle().teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }

            if (teleported) {
                //success! give feedback to the player
                if (PlainTextComponentSerializer.plainText().serialize(floorName).isBlank()) {
                    if (dy > 0) {
                        plugin.getMessageManager().sendLang(player, LiftLangPath.LIFT_USED_UP);
                    } else {
                        plugin.getMessageManager().sendLang(player, LiftLangPath.LIFT_USED_DOWN);
                    }
                } else {
                    plugin.getMessageManager().sendLang(player, LiftLangPath.LIFT_USED_FLOOR,
                        Placeholder.component(StandartPlaceHolders.TEXT.getPlaceholder(), floorName));
                }
            } else { //what?
                plugin.getMessageManager().sendLang(player, StandardLangPath.ERROR_WHAT);
            }
        } else {
            plugin.getMessageManager().sendLang(player, LiftLangPath.LIFT_DESTINATION_OBSTRUCTED);
        }
    }

    /**
     * common part regardless if the lift was directly clicked or a button was used
     *
     * @param originSign the sign that was activated
     * @param player     the player who was activating a sign
     * @param isUpIfBoth if the player should teleported up or down, if the lift is a bidirectional lift
     */
    private void useLift(@NotNull Sign originSign, Player player, boolean isUpIfBoth) {
        LiftType type = getFeatureConfig().fromLabel(originSign.getSide(Side.FRONT).line(1));

        //is it a lift?
        if (type != null) {
            if (type == LiftType.STOP) {
                //you can only arrive at stops
                plugin.getMessageManager().sendLang(player, LiftLangPath.LIFT_USED_STOP);
                return;
            }
            //check permission
            if (player.hasPermission(LiftPermissions.LIFT_USE.getPermission())) {
                //get destination
                Sign destinationSign = getDestination(originSign, type, isUpIfBoth);
                if (destinationSign != null) {
                    double dy = destinationSign.getLocation().getY() - originSign.getLocation().getY();

                    //finally teleport the player
                    liftTeleport(player, dy, destinationSign.getSide(Side.FRONT).line(0));
                } else {
                    plugin.getMessageManager().sendLang(player, LiftLangPath.LIFT_DESTINATION_UNKNOWN);
                } //no destination
            } else {
                plugin.getMessageManager().sendLang(player, StandardLangPath.NO_PERMISSION);
            }
        } //not interacted with a lift
    }

    /**
     * a lift can be indirectly activated via a button
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onRightUseButton(final @NotNull PlayerInteractEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        //don't fire for offhand
        if (event.getHand() == EquipmentSlot.HAND &&
            //right-clicked a block. Should ensure the getBlock() is not null
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            //cache clicked block
            Block eBlock = event.getClickedBlock();

            //is the block a button?
            if (eBlock != null && Tag.BUTTONS.isTagged(eBlock.getType())) {
                //and the block 2 behind a sign?
                Block block = eBlock.getRelative(((Directional) eBlock.getBlockData()).getFacing().getOppositeFace(), 2);
                if (block.getState(false) instanceof Sign origionSign) { // danger not a snapshot
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
    private void onRightClickSign(final @NotNull PlayerInteractEvent event) {
        //don't fire for offhand
        if (event.getHand() == EquipmentSlot.HAND &&
            //right-clicked a block. Should ensure the getBlock() is not null
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            //cache the clicked block
            Block eBlock = event.getClickedBlock();

            //is the clicked block a sign?
            if (eBlock != null && eBlock.getState(false) instanceof Sign origionSign) { // danger no snapshot
                // let player edit signs if shifting
                if (origionSign.isWaxed() || !event.getPlayer().isSneaking()) {
                    //determine if teleport should go up or down, in case of a bidirectional lift
                    boolean isUpIfBoth;
                    if (event.getInteractionPoint() == null) { //todo the interaction point might be faulty
                        isUpIfBoth = false;
                    } else {
                        double relativeHeight = event.getInteractionPoint().getY();
                        isUpIfBoth = (relativeHeight - (int) relativeHeight) >= 0.5;
                    }

                    //try to handle the sign as a lift
                    useLift(origionSign, event.getPlayer(), isUpIfBoth);
                } //player will edit sign instead of using the lift
            } //not interacted with a sign
        } //wrong hand or not a block
    }
}
