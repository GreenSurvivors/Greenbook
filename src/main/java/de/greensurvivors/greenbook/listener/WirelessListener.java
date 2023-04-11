package de.greensurvivors.greenbook.listener;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.WireLessConfig;
import de.greensurvivors.greenbook.language.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
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
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WirelessListener implements Listener {
    //pattern to identify transmitters and receivers
    private static final Pattern signPattern = Pattern.compile("\\[(.*?)\\]S?");
    //key to save/load uuids of players owning a wireless transmitter/receiver
    //used for player specific channels
    private static final NamespacedKey CHANNEL_UUID_KEY = new NamespacedKey(GreenBook.inst(), "channelUUID");

    //cache of recently used receiver location
    private final HashMap<String, HashSet<Location>> knownReceiverLocations = new HashMap<>();
    //cached power states to save time.
    //this is needed since reading the component of a sign multiple times a tick,
    //every time redstone level of a redstone line updates is too slow
    private final HashMap<Location, Boolean> lastPowerState = new HashMap<>();

    //these settings are only accessible via config file
    //configurates if every player should have their own channel based on their uuid,
    //or if everyone should use the global one
    //also if every freshly loaded chunk should get scanned for receiver channels
    private boolean
            usePlayerSpecificChannels = true,
            compatibilityMode = false;

    //this class keeps track of its own instance, so it's basically static
    private static WirelessListener instance;

    private static final Object MUTEX = new Object();

    private WirelessListener(){}

    /**
     * static to instance translator
     */
    public static WirelessListener inst() {
        if (instance == null) {
            instance = new WirelessListener();
        }
        return instance;
    }

    /**
     * clears list of cached receiver locations
     */
    public void clear() {
        synchronized (knownReceiverLocations) {
            knownReceiverLocations.clear();
        }
    }

    /**
     * set if player specific channels should be used.
     * A channel is the string a transmitter and a receiver share to mark what receiver should activate
     * @param usePlayerSpecificChannels true, if player specific channels should be used.
     */
    public void setUsePlayerSpecificChannels(boolean usePlayerSpecificChannels) {
        this.usePlayerSpecificChannels = usePlayerSpecificChannels;
    }

    /**
     * set if compatibilityMode should be used. If enabled the plugin will scan every loaded chunk for receiver signs
     * and save them to config files. so you can turn the mode on, load the whole world once and turn it back off
     * to save cpu circles
     * @param compatibilityMode true if the mode should turn on
     */
    public void setCompatibilityMode(boolean compatibilityMode) {
        this.compatibilityMode = compatibilityMode;
    }

    /**
     * add a receiver to the cached ones
     * @param receiverLocation the location of the receiver sign
     * @param channel channel the receiver belongs to
     */
    private void addReceiver(@NotNull Location receiverLocation, @NotNull String channel) {
        synchronized (knownReceiverLocations) {
            knownReceiverLocations.computeIfAbsent(channel, k -> new HashSet<>());
            knownReceiverLocations.get(channel).add(receiverLocation.toBlockLocation());
        }
    }

    /**
     * if a transmitter sign gets powered it turns all the receiver signs of the same channel on (or off if unpowered)
     */
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
                    //todo: maybe use a lectern as receiver to transmit all possible redstone signal strengths (optional)
                    //todo: might cache transmitters for this
                    //but than the reading of the components is to slow.

                    // test if the power-level has changed from off to on or reverse.
                    // this is an imported check, for not only makes it all the following checks and updates obsolete,
                    // but also reading the component lines form a sign is not fast enough to compete against a redstone wire signal
                    // and didn't include all lines anymore.
                    boolean powerNow = eBlock.getBlockPower() > 0;
                    Boolean powerLast = lastPowerState.get(eBlock.getLocation());
                    if (powerLast == null || (powerLast != powerNow)) {
                        lastPowerState.put(eBlock.getLocation(), powerNow);

                        String transmitterChannel = plainSerializer.serialize(transmitterSign.line(2));

                        HashSet<Location> receiverLocations = knownReceiverLocations.get(transmitterChannel);
                        String transmitterPlayerUUIDStr = transmitterSign.getPersistentDataContainer().get(CHANNEL_UUID_KEY, PersistentDataType.STRING);

                        if (receiverLocations == null) {
                            receiverLocations = WireLessConfig.inst().loadReceiverLocations(transmitterChannel, usePlayerSpecificChannels ? transmitterPlayerUUIDStr : null);
                            synchronized (knownReceiverLocations){
                                knownReceiverLocations.put(transmitterChannel, receiverLocations);
                            }
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

                                                String receiverPlayerUUIDStr = transmitterSign.getPersistentDataContainer().get(CHANNEL_UUID_KEY, PersistentDataType.STRING);

                                                // if user specific channels is turned on, compare the two uuid lines
                                                if (usePlayerSpecificChannels && receiverPlayerUUIDStr != null && !receiverPlayerUUIDStr.equals(transmitterPlayerUUIDStr)) {
                                                    return;
                                                }

                                                // sign is a receiver, check the channel
                                                String receiverChannel = plainSerializer.serialize(receiverSign.line(2));
                                                if (transmitterChannel.equals(receiverChannel)) {
                                                    // update lever
                                                    Location leverLoc = receiverLocation.clone().add(wallSign.getFacing().getDirection().multiply(-2));

                                                    if (leverLoc.getBlock().getBlockData() instanceof Switch leverData) {
                                                        leverData.setPowered(powerNow);
                                                        leverLoc.getBlock().setBlockData(leverData);
                                                    }

                                                } else {
                                                    // update channel, should never occur, but fixing it anyway
                                                    synchronized (knownReceiverLocations) {
                                                        knownReceiverLocations.get(transmitterChannel).remove(receiverLocation.toBlockLocation());
                                                    }
                                                    addReceiver(receiverLocation, receiverChannel);
                                                }
                                            } else {
                                                // remove from list
                                                synchronized (knownReceiverLocations){
                                                    knownReceiverLocations.get(transmitterChannel).remove(receiverLocation.toBlockLocation());
                                                }
                                            }
                                        } else {
                                            // remove from list
                                            synchronized (knownReceiverLocations) {
                                                knownReceiverLocations.get(transmitterChannel).remove(receiverLocation.toBlockLocation());
                                            }
                                        }
                                    } else {
                                        // remove from list
                                        synchronized (knownReceiverLocations){
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
    }

    //todo feedback message + sign destroy if no permission
    //todo add player name on last line to indicate owner visually

    /**
     * if a reciver gets placed, safe its location to file and cache it
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onSignPlace(SignChangeEvent event) {
        PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
        Component line2Comp = event.line(1);

        if (line2Comp != null){
            String line2Str = plainSerializer.serialize(line2Comp).trim();
            Sign changedSign = (Sign) event.getBlock().getState();
            Matcher matcher = signPattern.matcher(line2Str);

            //clear line 2 of square brackets []
            if (matcher.matches()) {
                line2Str = matcher.group(1);

                if (line2Str.equalsIgnoreCase(Lang.SIGN_RECEIVER_ID.get())) {
                    if (Tag.WALL_SIGNS.isTagged(event.getBlock().getType())) {
                        //set the name of the ic
                        event.line(0, Lang.build(Lang.SIGN_RECEIVER_NAME.get()));

                        //set the uuid of the player if needed
                        String playerUUIDStr = null;
                        if (usePlayerSpecificChannels) {
                            playerUUIDStr = event.getPlayer().getUniqueId().toString();

                            changedSign.getPersistentDataContainer().set(CHANNEL_UUID_KEY, PersistentDataType.STRING, playerUUIDStr);
                        }

                        Component channel = event.line(2);
                        String channelStr;

                        if (channel == null) {
                            channelStr = "";
                        } else {
                            channelStr = plainSerializer.serialize(channel);
                        }

                        //cache the new receiver
                        this.addReceiver(event.getBlock().getLocation(), channelStr);

                        synchronized (knownReceiverLocations){
                            WireLessConfig.inst().saveReceiverLocations(channelStr, knownReceiverLocations.get(channelStr), playerUUIDStr);
                        }
                    } else {
                        event.getPlayer().sendMessage(Lang.build(Lang.NO_WALLSIGN.get()));
                        event.getBlock().setType(Material.AIR);
                        for (ItemStack signItem : event.getBlock().getDrops()) {
                            event.getBlock().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(), signItem);
                        }
                    }
                } else if (line2Str.equalsIgnoreCase(Lang.SIGN_TRANSMITTER_ID.get())) {
                    //set the name of the ic
                    event.line(0, Lang.build(Lang.SIGN_TRANSMITTER_NAME.get()));

                    //set the uuid of the player if needed
                    if (usePlayerSpecificChannels) {
                        changedSign.getPersistentDataContainer().set(CHANNEL_UUID_KEY, PersistentDataType.STRING, event.getPlayer().getUniqueId().toString());
                    }
                }
            }
        }
    }

    /**
     *
     * @param chunk
     */
    public void parseThroughChunk(Chunk chunk){
        synchronized (MUTEX){
            for (BlockState state : chunk.getTileEntities(block -> Tag.SIGNS.isTagged(block.getType()), false)) {
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

                                String channelStr = plainSerializer.serialize(channel);

                                //cache the new receiver
                                this.addReceiver(state.getLocation(), channelStr);

                                String playerUUIDStr = plainSerializer.serialize(sign.line(3));
                                synchronized (knownReceiverLocations){
                                    WireLessConfig.inst().saveReceiverLocations(channelStr, knownReceiverLocations.get(channelStr), usePlayerSpecificChannels ? playerUUIDStr : null);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //todo load uuid from persistant data storage to restore lost receiver files
    /**
     * iterates through all blocks in a freshly loaded chunk to find legacy signs
     * and signs that where deleted from config but not from world
     * To work the compatibilityMode have to get turned on
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onChunkLoad(final ChunkLoadEvent event) {
        if (compatibilityMode) {
            Bukkit.getScheduler().runTaskAsynchronously(GreenBook.inst(), () -> parseThroughChunk(event.getChunk()));
        }
    }

    /**
     * remove cached locations, if the chunk unloads the location is in
     */
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
