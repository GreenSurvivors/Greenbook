package de.greensurvivors.greenbook.listener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.WireLessConfig;
import de.greensurvivors.greenbook.language.Lang;
import de.greensurvivors.greenbook.utils.PermissionUtils;
import de.greensurvivors.greenbook.wireless.Network;
import de.greensurvivors.greenbook.wireless.WirelessReceiver;
import de.greensurvivors.greenbook.wireless.WirelessSign;
import de.greensurvivors.greenbook.wireless.WirelessTransmitter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WirelessListener implements Listener {
    //pattern to identify transmitters and receivers
    private static final Pattern SIGN_PATTERN = Pattern.compile("\\[(.*?)\\]S?");
    //key to save/load uuids of players owning a wireless transmitter/receiver
    //used for player specific channels
    private static final NamespacedKey OWNER_UUID_KEY = new NamespacedKey(GreenBook.inst(), "ownerUUID");
    //this class keeps track of its own instance, so it's basically static
    private static WirelessListener instance;
    //chaches are thread safe, the keys/values in it not.
    private final @NotNull MultiKeyMap<String, @NotNull Network> networks = new MultiKeyMap<>();
    private final @NotNull Cache<@NotNull Location, @NotNull WirelessTransmitter> transmitterLocations = Caffeine.newBuilder().
            expireAfterAccess(5, TimeUnit.MINUTES).
            maximumSize(500).
            build();
    private final @NotNull Cache<@NotNull Network, @NotNull Set<@NotNull WirelessTransmitter>> transmittersInNetwork = Caffeine.newBuilder().
            expireAfterAccess(40, TimeUnit.MINUTES).
            maximumSize(1000).
            build();
    private final @NotNull LoadingCache<@NotNull Network, @NotNull Set<@NotNull WirelessReceiver>> receiversInNetwork = Caffeine.newBuilder().
            expireAfterAccess(40, TimeUnit.MINUTES).
            maximumSize(1000).
            build(network -> WireLessConfig.inst().loadReceivers(network));
    //these settings are only accessible via config file
    //configurates if every player should have their own channel based on their uuid,
    //or if everyone should use the global one
    //also if every freshly loaded chunk should get scanned for receiver channels
    private boolean
            usePlayerSpecificChannels = true,
            compatibilityMode = false;

    private WirelessListener() {
    }

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
     * may return -1 if no highest level was found
     *
     * @param network
     * @return
     */
    private byte getHighestPowerStateNetwork(@NotNull Network network) {
        network.getLock().readLock().lock();
        @Nullable Set<@NotNull WirelessTransmitter> transmitters = transmittersInNetwork.getIfPresent(network);
        byte highestValue = -1;

        if (transmitters != null) {
            for (WirelessTransmitter transmitter : transmitters) {
                if (transmitter.getLastPowerState() > highestValue) {
                    highestValue = transmitter.getLastPowerState();
                }

                if (highestValue >= 15) { // there is no higher value. return early
                    network.getLock().readLock().unlock();
                    return 15;
                }
            }
        }

        network.getLock().readLock().unlock();
        return highestValue;
    }

    /**
     * clears list of cached receiver locations
     */
    public void clear() {
        transmitterLocations.invalidateAll();
        transmittersInNetwork.invalidateAll();
        receiversInNetwork.invalidateAll();
    }

    /**
     * set if player specific channels should be used.
     * A channel is the string a transmitter and a receiver share to mark what receiver should activate
     *
     * @param usePlayerSpecificChannels true, if player specific channels should be used.
     */
    public void setUsePlayerSpecificChannels(boolean usePlayerSpecificChannels) {
        this.usePlayerSpecificChannels = usePlayerSpecificChannels;
    }

    /**
     * set if compatibilityMode should be used. If enabled the plugin will scan every loaded chunk for receiver signs
     * and save them to config files. so you can turn the mode on, load the whole world once and turn it back off
     * to save cpu circles
     *
     * @param compatibilityMode true if the mode should turn on
     */
    public void setCompatibilityMode(boolean compatibilityMode) {
        this.compatibilityMode = compatibilityMode;
    }

    /**
     * add a receiver to the cached ones
     *
     * @param receiverLocation the location of the receiver sign
     * @param network          the receiver belongs to
     */
    private void addReceiver(@NotNull Location receiverLocation, @NotNull Network network) {
        network.getLock().writeLock().lock();
        synchronized (networks) {
            networks.putIfAbsent(new MultiKey<>(network.getId(), network.getOwnerUUIDStr()), network);
        }
        receiversInNetwork.get(network).add(new WirelessReceiver(receiverLocation, network));

        network.getLock().writeLock().unlock();
    }

    private @NotNull WirelessTransmitter getTransmitterAt(@NotNull Location location) {
        location = location.toBlockLocation();
        PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
        WirelessTransmitter wirelessTransmitter;

        if (location.getBlock().getState() instanceof Sign transmitterSign) {
            String line2 = plainSerializer.serialize(transmitterSign.getSide(Side.FRONT).line(1)).trim();
            Matcher matcher = SIGN_PATTERN.matcher(line2);
            // clear line 2 of square brackets []

            if (matcher.matches()) {
                line2 = matcher.group(1);

                if (line2.equalsIgnoreCase(Lang.SIGN_TRANSMITTER_ID.get())) {
                    String transmitterChannel = plainSerializer.serialize(transmitterSign.getSide(Side.FRONT).line(2));
                    String transmitterUUIDStr = transmitterSign.getPersistentDataContainer().get(OWNER_UUID_KEY, PersistentDataType.STRING);
                    //todo if uuid null get from name on sign

                    synchronized (networks) {
                        Network network = networks.computeIfAbsent(new MultiKey<>(transmitterChannel, transmitterUUIDStr), k -> new Network(transmitterChannel, transmitterUUIDStr));
                        network.getLock().writeLock().lock();

                        wirelessTransmitter = new WirelessTransmitter(location, network);

                        transmittersInNetwork.asMap().computeIfAbsent(network, k -> new HashSet<>()).add(wirelessTransmitter);
                        network.getLock().writeLock().unlock();
                    }
                } else { // not a transmitter, but that also gets cached, so we don't check it again.
                    wirelessTransmitter = new WirelessTransmitter(location, null);
                }
            } else { // not matching second line, therefor not a transmitter
                wirelessTransmitter = new WirelessTransmitter(location, null);
            }
        } else {
            wirelessTransmitter = new WirelessTransmitter(location, null);
        }

        return wirelessTransmitter;
    }

    private void setSignal(Location receiverLocation, byte powerState, BlockFace facingDirection) {
        Bukkit.getScheduler().runTask(GreenBook.inst(), () -> {
            Location lecternLoc = receiverLocation.clone().add(facingDirection.getDirection().multiply(-1));

            if (lecternLoc.getBlock().getState() instanceof Lectern lectern) {
                ItemStack bookItem = lectern.getInventory().getContents()[0];

                if (bookItem != null && bookItem.getItemMeta() instanceof BookMeta bookMeta) {
                    int pageCount = bookMeta.getPageCount();

                    if (powerState == 0) {
                        // todo power 0
                    } else {
                        if (pageCount > 1) {
                            int page = (int) Math.ceil(((double) powerState - 1D) / 14D * ((double) pageCount - 1D));

                            lectern.setPage(page);
                            lectern.update();
                        } // page count == 1
                    }
                } else { // no book
                    //todo maybe last state was power == 0
                }
            } else { // update lever
                Location leverLoc = receiverLocation.clone().add(facingDirection.getDirection().multiply(-2));

                if (leverLoc.getBlock().getBlockData() instanceof Switch leverData) {
                    leverData.setPowered(powerState > 0);
                    leverLoc.getBlock().setBlockData(leverData);
                }
            }
        });
    }

    /**
     * if a transmitter sign gets powered it turns all the receiver signs of the same channel on (or off if unpowered)
     */
    @EventHandler(ignoreCancelled = true)
    private void onSignPowered(BlockPhysicsEvent event) { //todo async
        PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
        Block eBlock = event.getBlock();
        if (Tag.WALL_SIGNS.isTagged(event.getChangedType())) { // fast check to "fail" fast for non-relevant changes
            WirelessTransmitter wirelessTransmitter = transmitterLocations.get(eBlock.getLocation().toBlockLocation(), this::getTransmitterAt);

            if (wirelessTransmitter.isValid()) {
                Network transmitterNetwork = wirelessTransmitter.getNetwork();
                transmitterNetwork.getLock().writeLock().lock();

                Byte lastPowerState = wirelessTransmitter.getLastPowerState();
                byte nowPowerState = (byte) eBlock.getBlockPower();

                if (lastPowerState == null || (lastPowerState != nowPowerState)) {
                    wirelessTransmitter.setLastPowerState(nowPowerState);
                }

                byte highestPowerState = getHighestPowerStateNetwork(transmitterNetwork);

                if (nowPowerState >= highestPowerState) {
                    Set<WirelessReceiver> receivers = receiversInNetwork.get(transmitterNetwork);

                    // for loop receivers
                    for (Iterator<WirelessReceiver> iterator = receivers.iterator(); iterator.hasNext(); ) {
                        WirelessReceiver receiver = iterator.next();
                        Location receiverLocation = receiver.getLocation();

                        // -- update power of the receiver --
                        // test if the receiver is loaded
                        if (receiverLocation.getChunk().isLoaded()) {
                            Block receiverBlock = receiverLocation.getBlock();

                            // test if receiver is a wall sign
                            if (receiverBlock.getBlockData() instanceof WallSign wallSign) {
                                Sign receiverSign = (Sign) receiverBlock.getState();

                                if (receiver.isExpired()) {
                                    // test if the second line is stating the sign is a receiver
                                    String line2 = plainSerializer.serialize(receiverSign.getSide(Side.FRONT).line(1)).trim();
                                    Matcher matcher = SIGN_PATTERN.matcher(line2);

                                    if (matcher.matches()) {
                                        line2 = matcher.group(1);
                                        if (line2.equalsIgnoreCase(Lang.SIGN_RECEIVER_ID.get())) {
                                            // sign is a receiver, check the channel
                                            String receiverChannel = plainSerializer.serialize(receiverSign.getSide(Side.FRONT).line(2));

                                            // if user specific channels is turned on, compare the two uuid lines
                                            String receiverPlayerUUIDStr = receiverSign.getPersistentDataContainer().get(OWNER_UUID_KEY, PersistentDataType.STRING);
                                            //todo if uuid null get from name on sign

                                            synchronized (networks) {
                                                Network reciverNetwork = networks.computeIfAbsent(new MultiKey<>(receiverChannel, receiverPlayerUUIDStr), multiKey -> new Network(receiverChannel, receiverPlayerUUIDStr));

                                                reciverNetwork.getLock().readLock().lock();
                                                if (reciverNetwork.equals(receiver.getNetwork())) {
                                                    receiver.refresh();
                                                } else { // is a receiver but wrong network
                                                    iterator.remove();
                                                    reciverNetwork.getLock().writeLock().lock();

                                                    this.addReceiver(receiverLocation, reciverNetwork);
                                                    WireLessConfig.inst().saveReceiverLocations(reciverNetwork, receiversInNetwork.get(reciverNetwork).stream().map(WirelessReceiver::getLocation).collect(Collectors.toSet()));
                                                    WireLessConfig.inst().saveReceiverLocations(transmitterNetwork, receiversInNetwork.get(transmitterNetwork).stream().map(WirelessReceiver::getLocation).collect(Collectors.toSet()));

                                                    reciverNetwork.getLock().writeLock().lock();
                                                    break;
                                                }

                                                reciverNetwork.getLock().readLock().unlock();
                                            }
                                        } else { // no longer a receiver sign here
                                            iterator.remove();
                                            WireLessConfig.inst().saveReceiverLocations(transmitterNetwork, receiversInNetwork.get(transmitterNetwork).stream().map(WirelessReceiver::getLocation).collect(Collectors.toSet()));

                                            break;
                                        }
                                    } else { // no longer a receiver sign here
                                        iterator.remove();

                                        WireLessConfig.inst().saveReceiverLocations(transmitterNetwork, receiversInNetwork.get(transmitterNetwork).stream().map(WirelessReceiver::getLocation).collect(Collectors.toSet()));
                                        break;
                                    }
                                } // receiver not expired

                                receiver.getNetwork().getLock().readLock().lock();
                                if (receiver.getNetwork().getId().equals(transmitterNetwork.getId())) {
                                    if (!usePlayerSpecificChannels || receiver.getNetwork().getOwnerUUIDStr().equals(transmitterNetwork.getOwnerUUIDStr())) {
                                        setSignal(receiverLocation, highestPowerState, wallSign.getFacing());
                                    } else {// uuids doesn't match
                                        receiver.getNetwork().getLock().writeLock().lock();
                                        iterator.remove();

                                        Set<WirelessReceiver> newReciverSet = receiversInNetwork.get(receiver.getNetwork());
                                        newReciverSet.add(receiver);

                                        WireLessConfig.inst().saveReceiverLocations(receiver.getNetwork(), receiversInNetwork.get(receiver.getNetwork()).stream().map(WirelessReceiver::getLocation).collect(Collectors.toSet()));
                                        WireLessConfig.inst().saveReceiverLocations(transmitterNetwork, receiversInNetwork.get(transmitterNetwork).stream().map(WirelessReceiver::getLocation).collect(Collectors.toSet()));

                                        receiver.getNetwork().getLock().writeLock().unlock();
                                    }
                                } else { // network ids doesn't match
                                    receiver.getNetwork().getLock().writeLock().lock();
                                    iterator.remove();

                                    Set<WirelessReceiver> newReciverSet = receiversInNetwork.get(receiver.getNetwork());
                                    newReciverSet.add(receiver);

                                    WireLessConfig.inst().saveReceiverLocations(receiver.getNetwork(), receiversInNetwork.get(receiver.getNetwork()).stream().map(WirelessReceiver::getLocation).collect(Collectors.toSet()));
                                    WireLessConfig.inst().saveReceiverLocations(transmitterNetwork, receiversInNetwork.get(transmitterNetwork).stream().map(WirelessReceiver::getLocation).collect(Collectors.toSet()));

                                    receiver.getNetwork().getLock().writeLock().unlock();
                                }

                                receiver.getNetwork().getLock().readLock().unlock();
                            } else { // receiver block is not a wall sign
                                iterator.remove();

                                WireLessConfig.inst().saveReceiverLocations(transmitterNetwork, receiversInNetwork.get(transmitterNetwork).stream().map(WirelessReceiver::getLocation).collect(Collectors.toSet()));
                            }
                        } // chunk of receiver not loaded
                    }

                    transmitterNetwork.getLock().writeLock().unlock();
                } // new power state is lower than network

            } // not a transmitter at this location
        } // wasn't a wall sign. ignore.
    }


    /**
     * if a receiver gets placed, safe its location to file and cache it
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onSignPlace(SignChangeEvent event) { //todo remove from locations; also on sign break
        PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
        Component line2Comp = event.line(1);

        if (line2Comp != null) {
            String line2Str = plainSerializer.serialize(line2Comp).trim();
            Sign changedSign = (Sign) event.getBlock().getState();
            Matcher matcher = SIGN_PATTERN.matcher(line2Str);

            //clear line 2 of square brackets []
            if (matcher.matches()) {
                line2Str = matcher.group(1);

                if (line2Str.equalsIgnoreCase(Lang.SIGN_RECEIVER_ID.get())) {
                    //check permission
                    if (PermissionUtils.hasPermission(event.getPlayer(), PermissionUtils.GREENBOOK_WIRELESS_CREATE_SIGN)) {
                        // only handle front sides
                        if (event.getSide() == Side.FRONT) {
                            if (Tag.WALL_SIGNS.isTagged(event.getBlock().getType())) {
                                //set the name and getId of the receiver
                                event.line(0, Lang.build(Lang.SIGN_RECEIVER_NAME.get()));
                                event.line(1, Lang.build("[" + Lang.SIGN_RECEIVER_ID.get() + "]"));

                                //set the uuid of the player if needed
                                String playerUUIDStr;
                                if (usePlayerSpecificChannels) {
                                    playerUUIDStr = event.getPlayer().getUniqueId().toString();

                                    changedSign.getPersistentDataContainer().set(OWNER_UUID_KEY, PersistentDataType.STRING, playerUUIDStr);
                                    changedSign.update();
                                    event.line(3, event.getPlayer().name());
                                } else {
                                    playerUUIDStr = null;
                                }

                                Component channel = event.line(2);
                                String channelStr;

                                if (channel == null) {
                                    channelStr = "";
                                } else {
                                    channelStr = plainSerializer.serialize(channel);
                                }

                                synchronized (networks) {
                                    Network network = networks.computeIfAbsent(new MultiKey<>(channelStr, playerUUIDStr), k -> new Network(channelStr, playerUUIDStr));

                                    network.getLock().writeLock().lock();
                                    //cache the new receiver
                                    this.addReceiver(event.getBlock().getLocation(), network);

                                    WireLessConfig.inst().saveReceiverLocations(network, receiversInNetwork.get(network).stream().map(WirelessSign::getLocation).collect(Collectors.toSet()));

                                    network.getLock().writeLock().unlock();
                                }
                            } else {
                                event.getPlayer().sendMessage(Lang.build(Lang.NO_WALLSIGN.get()));
                                event.getBlock().breakNaturally();
                            }
                        } else { // backside
                            event.getPlayer().sendMessage(Lang.build(Lang.NO_FRONTSIDE.get()));
                            event.line(1, Component.empty());
                            event.setCancelled(true);
                        }
                    } else { //no permission
                        event.getPlayer().sendMessage(Lang.build(Lang.NO_PERMISSION_SOMETHING.get()));

                        event.setCancelled(true);
                        event.getBlock().breakNaturally();
                    }
                } else if (line2Str.equalsIgnoreCase(Lang.SIGN_TRANSMITTER_ID.get())) {
                    //check permission
                    if (PermissionUtils.hasPermission(event.getPlayer(), PermissionUtils.GREENBOOK_WIRELESS_CREATE_SIGN)) {
                        //set the name and getId of the transmitter
                        event.line(0, Lang.build(Lang.SIGN_TRANSMITTER_NAME.get()));
                        event.line(1, Lang.build("[" + Lang.SIGN_TRANSMITTER_ID.get() + "]"));

                        Component channel = event.line(2);
                        String channelStr;

                        if (channel == null) {
                            channelStr = "";
                        } else {
                            channelStr = plainSerializer.serialize(channel);
                        }

                        String playerUUIDStr;
                        //set the uuid of the player if needed
                        if (usePlayerSpecificChannels) {
                            playerUUIDStr = event.getPlayer().getUniqueId().toString();
                            changedSign.getPersistentDataContainer().set(OWNER_UUID_KEY, PersistentDataType.STRING, playerUUIDStr);
                            changedSign.update();
                            event.line(3, event.getPlayer().name());
                        } else {
                            playerUUIDStr = null;
                        }

                        synchronized (networks) {
                            Network network = networks.computeIfAbsent(new MultiKey<>(channelStr, playerUUIDStr), k -> new Network(channelStr, playerUUIDStr));
                            Location location = event.getBlock().getLocation().toBlockLocation();
                            WirelessTransmitter wirelessTransmitter = new WirelessTransmitter(location, network);
                            transmittersInNetwork.asMap().computeIfAbsent(network, k -> new HashSet<>()).add(wirelessTransmitter);
                            transmitterLocations.put(location, wirelessTransmitter);
                        }
                    }
                } else { //no permission
                    event.getPlayer().sendMessage(Lang.build(Lang.NO_PERMISSION_SOMETHING.get()));

                    event.setCancelled(true);
                    event.getBlock().breakNaturally();
                }
            }
        }
    }

    /**
     * iterates through all blocks in a chunk to find legacy signs
     * and signs that where deleted from config but not from world
     * called, if a chunk loads and compatibility mode is on or the update sign command was called
     *
     * @param chunk the chunk to index all receiver signs in
     */
    public void parseThroughChunk(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities(block -> Tag.SIGNS.isTagged(block.getType()), false)) {
            if ((state instanceof Sign sign)) {
                PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
                String line2 = plainSerializer.serialize(sign.getSide(Side.FRONT).line(1)).trim();
                Matcher matcher = SIGN_PATTERN.matcher(line2);

                //clear line 2 of square brackets []
                if (matcher.matches()) {
                    line2 = matcher.group(1);

                    if (line2.equalsIgnoreCase(Lang.SIGN_RECEIVER_ID.get())) {
                        if (Tag.WALL_SIGNS.isTagged(state.getType())) {
                            Component channel = sign.getSide(Side.FRONT).line(2);

                            String channelStr = plainSerializer.serialize(channel);

                            String playerUUIDStr = sign.getPersistentDataContainer().get(OWNER_UUID_KEY, PersistentDataType.STRING);
                            //if no uuid was in the data container try to get it from the third line
                            playerUUIDStr = playerUUIDStr == null ? plainSerializer.serialize(sign.getSide(Side.FRONT).line(3)) : playerUUIDStr; //todo this may be a name not a uuid

                            Network network;
                            synchronized (networks) {
                                String finalPlayerUUIDStr = playerUUIDStr; // just a java quirk that this has to be final...
                                network = networks.computeIfAbsent(new MultiKey<>(channelStr, playerUUIDStr), k -> new Network(channelStr, finalPlayerUUIDStr));
                            }

                            network.getLock().readLock().lock();

                            //cache the new receiver
                            this.addReceiver(state.getLocation(), network);

                            WireLessConfig.inst().saveReceiverLocations(network, receiversInNetwork.get(network).stream().map(WirelessReceiver::getLocation).collect(Collectors.toSet()));
                            network.getLock().readLock().unlock();
                        }
                    }
                }
            }
        }
    }

    /**
     * iterates through all blocks in a freshly loaded chunk to find legacy signs
     * and signs that where deleted from config but not from world
     * To work the compatibilityMode have to get turned on
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

        transmitterLocations.asMap().keySet().removeIf(transmitterLoc -> transmitterLoc.getChunk().equals(event.getChunk()));
    }
}
