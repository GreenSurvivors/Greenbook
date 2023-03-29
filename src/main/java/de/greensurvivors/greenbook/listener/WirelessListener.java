package de.greensurvivors.greenbook.listener;

import de.greensurvivors.greenbook.config.WireLessConfig;
import de.greensurvivors.greenbook.language.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WirelessListener implements Listener {
    Pattern signPattern = Pattern.compile("\\[(.*?)\\]S?");

    private final HashMap<Component, HashSet<Location>> knownReceiverLocations = new HashMap<>();
    private final HashMap<Location, Boolean> lastPowerState = new HashMap<>();
    //these settings are only accessible via config file
    private boolean
            usePlayerSpecificChannels = true,
            compatibilityMode = false;

    private static WirelessListener instance;

    private WirelessListener() {
    }

    public static WirelessListener inst() {
        if (instance == null) {
            instance = new WirelessListener();
        }
        return instance;
    }

    public void clear() {
        knownReceiverLocations.clear();
    }

    public void setUsePlayerSpecificChannels(boolean usePlayerSpecificChannels) {
        this.usePlayerSpecificChannels = usePlayerSpecificChannels;
    }

    public void setCompatibilityMode(boolean compatibilityMode) {
        this.compatibilityMode = compatibilityMode;
    }

    private void addReceiver(@NotNull Location receiverLocation, @NotNull Component channel) {
        knownReceiverLocations.computeIfAbsent(channel, k -> new HashSet<>());
        knownReceiverLocations.get(channel).add(receiverLocation.toBlockLocation());
    }

    @EventHandler(ignoreCancelled = true)
    private void onSignPowered(BlockPhysicsEvent event) {
        Block eBlock = event.getBlock();

        if (eBlock.getState() instanceof Sign transmitterSign) {
            PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

            String line2 = plainSerializer.serialize(transmitterSign.line(1)).trim();

            Matcher matcher = signPattern.matcher(line2);
            // clear line 2 of square brackets []
            if (matcher.matches()) {
                line2 = matcher.group(1);

                if (line2.equalsIgnoreCase(Lang.SIGN_TRANSMITTER_ID.get())) {
                    // test if the power-level has changed from off to on or reverse.
                    // this is an imported check, for not only makes it all the following checks and updates obsolete,
                    // but also reading the component lines form a sign is not fast enough to compete against a redstone wire signal
                    // and didn't include all lines anymore.
                    boolean powerNow = eBlock.getBlockPower() > 0;
                    Boolean powerLast = lastPowerState.get(eBlock.getLocation());
                    if (powerLast == null || (powerLast != powerNow)) {
                        lastPowerState.put(eBlock.getLocation(), powerNow);

                        Component transmitterChannel = transmitterSign.line(2);

                        HashSet<Location> receiverLocations = knownReceiverLocations.get(transmitterChannel);
                        String transmitterPlayerUUIDStr = plainSerializer.serialize(transmitterSign.line(3));

                        if (receiverLocations == null) {
                            receiverLocations = WireLessConfig.inst().loadReceiverLocations(transmitterChannel, usePlayerSpecificChannels ? transmitterPlayerUUIDStr : null);
                            knownReceiverLocations.put(transmitterChannel, receiverLocations);
                        }

                        if (receiverLocations != null) {
                            for (Location receiverLocation : receiverLocations) {

                                // -- update power of the receiver --
                                // test if the receiver is loaded
                                if (receiverLocation.getChunk().isLoaded()) {
                                    Block receiverBlock = receiverLocation.getBlock();

                                    // test if receiver is a wall sign
                                    if (receiverBlock.getBlockData() instanceof WallSign wallSign) {
                                        Sign receiverSign = (Sign) receiverBlock.getState();

                                        // test if the second line is stating the sign is a receiver
                                        line2 = plainSerializer.serialize(receiverSign.line(1)).trim();
                                        matcher = signPattern.matcher(line2);

                                        if (matcher.matches()) {
                                            line2 = matcher.group(1);
                                            if (line2.equalsIgnoreCase(Lang.SIGN_RECEIVER_ID.get())) {

                                                String receiverPlayerUUIDStr = plainSerializer.serialize(receiverSign.line(3));

                                                // if user specific channels is turned on, compare the two uuid lines
                                                if (usePlayerSpecificChannels && !receiverPlayerUUIDStr.equals(transmitterPlayerUUIDStr)) {
                                                    return;
                                                }

                                                // sign is a receiver, check the channel
                                                Component receiverChannel = receiverSign.line(2);
                                                if (transmitterChannel.equals(receiverChannel)) {
                                                    // update lever
                                                    Location leverLoc = receiverLocation.clone().add(wallSign.getFacing().getDirection().multiply(-2));

                                                    if (leverLoc.getBlock().getBlockData() instanceof Switch leverData) {
                                                        leverData.setPowered(powerNow);
                                                        leverLoc.getBlock().setBlockData(leverData);
                                                    }

                                                } else {
                                                    // update channel, should never occur, but fixing it anyway
                                                    knownReceiverLocations.get(transmitterChannel).remove(receiverLocation.toBlockLocation());

                                                    addReceiver(receiverLocation, receiverChannel);
                                                }
                                            } else {
                                                // remove from list
                                                knownReceiverLocations.get(transmitterChannel).remove(receiverLocation.toBlockLocation());
                                            }
                                        } else {
                                            // remove from list
                                            knownReceiverLocations.get(transmitterChannel).remove(receiverLocation.toBlockLocation());
                                        }
                                    } else {
                                        // remove from list
                                        knownReceiverLocations.get(transmitterChannel).remove(receiverLocation.toBlockLocation());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onSignPlace(SignChangeEvent event) {
        PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
        String line2 = plainSerializer.serialize(event.line(1)).trim();
        Matcher matcher = signPattern.matcher(line2);

        //clear line 2 of square brackets []
        if (matcher.matches()) {
            line2 = matcher.group(1);

            if (line2.equalsIgnoreCase(Lang.SIGN_RECEIVER_ID.get())) {
                if (Tag.WALL_SIGNS.isTagged(event.getBlock().getType())) {
                    //set the name of the ic
                    event.line(0, Lang.build(Lang.SIGN_RECEIVER_NAME.get()));

                    //set the uuid of the player if needed
                    String playerUUIDStr = null;
                    if (usePlayerSpecificChannels) {
                        playerUUIDStr = event.getPlayer().getUniqueId().toString();
                        event.line(3, Lang.build(playerUUIDStr));
                    }

                    Component channel = event.line(2);

                    if (channel == null) {
                        channel = Component.empty();
                    }

                    //cache the new receiver
                    this.addReceiver(event.getBlock().getLocation(), channel);

                    WireLessConfig.inst().saveReceiverLocations(event.line(2), knownReceiverLocations.get(event.line(2)), playerUUIDStr);
                } else {
                    event.getPlayer().sendMessage(Lang.build(Lang.NO_WALLSIGN.get()));
                    event.getBlock().setType(Material.AIR);
                    for (ItemStack signItem : event.getBlock().getDrops()) {
                        event.getBlock().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(), signItem);
                    }
                }
            } else if (line2.equalsIgnoreCase(Lang.SIGN_TRANSMITTER_ID.get())) {
                //set the name of the ic
                event.line(0, Lang.build(Lang.SIGN_TRANSMITTER_NAME.get()));

                //set the uuid of the player if needed
                if (usePlayerSpecificChannels) {
                    event.line(3, Lang.build(event.getPlayer().getUniqueId().toString()));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onChunkLoad(final ChunkLoadEvent event) {
        if (compatibilityMode) {
            for (BlockState state : event.getChunk().getTileEntities(block -> Tag.SIGNS.isTagged(block.getType()), false)) {
                if ((state instanceof Sign sign)) {
                    PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
                    String line2 = plainSerializer.serialize(sign.line(1)).trim();
                    Matcher matcher = signPattern.matcher(line2);

                    //clear line 2 of square brackets []
                    if (matcher.matches()) {
                        line2 = matcher.group(1);

                        if (line2.equalsIgnoreCase(Lang.SIGN_RECEIVER_ID.get())) {
                            if (Tag.WALL_SIGNS.isTagged(state.getType())) {
                                Component channel = sign.line(2);

                                //cache the new receiver
                                this.addReceiver(state.getLocation(), channel);

                                String playerUUIDStr = plainSerializer.serialize(sign.line(3));
                                WireLessConfig.inst().saveReceiverLocations(sign.line(2), knownReceiverLocations.get(sign.line(2)), usePlayerSpecificChannels ? playerUUIDStr : null);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onChunkUnload(final ChunkUnloadEvent event) {
        //unload cache if not needed
        for (Location location : lastPowerState.keySet()) {
            if (location.getChunk() == event.getChunk()) {
                lastPowerState.remove(location);
            }
        }
    }
}
