package de.greensurvivors.greenbook.features.wireless;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.features.AFeature;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.features.wireless.network.*;
import de.greensurvivors.greenbook.language.StandardLangPath;
import de.greensurvivors.greenbook.language.StandartPlaceHolders;
import de.greensurvivors.greenbook.persistenddata.EnumPersistentDataType;
import de.greensurvivors.greenbook.utils.ChunkChecker;
import io.papermc.paper.command.brigadier.Commands;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.core.BlockPos;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WirelessRedstoneFeature extends AFeature<WirelessConfigManager> implements Listener {
    private static final @NotNull EnumPersistentDataType<WirelessNodeType> NODE_DATA_TYPE = new EnumPersistentDataType<>(WirelessNodeType.class);
    private final @NotNull NamespacedKey nodeTypeKey;
    private final @NotNull NamespacedKey networkChannelKey;
    private final @NotNull NamespacedKey ownerUUIDKey;
    // only care about chunks holding nodes at all
    private final @NotNull LongSet chunkOfInterest = new LongOpenHashSet();
    private final @NotNull Map<@NotNull Location, @NotNull AWirelessNode> loadedNodes = new HashMap<>();
    private final LoadingCache<@NotNull NetworkKey, @NotNull WirelessNetwork> networks = Caffeine.newBuilder().
        build(doubleStrKey -> new WirelessNetwork(plugin, doubleStrKey.networkChannel(), doubleStrKey.ownerUUIDStr()));
    /**
     * We only care for chunks that are completely loaded and are surrounded by also loaded chunks.
     * Chunks without this are never be able to process wireless redstone and can be safely ignored.
     * this saves us processing time when chunks full of block entities (especially signs) get loaded frequently at the edge of the simulation distance.
     */
    private final @NotNull ChunkChecker provider;

    public WirelessRedstoneFeature(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.WIRELESS, new WirelessConfigManager(plugin));
        this.provider = new ChunkChecker(plugin);
        this.nodeTypeKey = new NamespacedKey(plugin, "wirelessNodeType");
        this.networkChannelKey = new NamespacedKey(plugin, "wirelessChannel");
        this.ownerUUIDKey = new NamespacedKey(plugin, "ownerUUID");

        // register listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getDependencyManager().registerEditSessionEvent(this);
    }

    @Override
    public void registerCommands(@NotNull Commands commandsRegistrar, @NotNull GreenBookCmd mainCommand) {
    }

    @Override
    public void onDisable() {
        getFeatureConfig().setEnabled(false);
        loadedNodes.clear();
        networks.invalidateAll();
        networks.cleanUp();
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        getFeatureConfig().setEnabled(true);
    }

    @EventHandler(ignoreCancelled = true)
    private void onSignChange(@NotNull SignChangeEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        Component line1Comp = event.line(1);
        Component line2Comp = event.line(2);
        Component line3Comp = event.line(3);

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        AWirelessNode oldNode = loadedNodes.get(location);

        if (line1Comp != null && line2Comp != null) {
            WirelessNodeType newNodeType = getFeatureConfig().fromID(line1Comp);

            WirelessNetwork newNetwork = null;

            if (newNodeType != WirelessNodeType.NONE) {
                if (event.getSide() != Side.FRONT) { // trying to set new node on backside - warn and end
                    plugin.getMessageManager().sendLang(event.getPlayer(), StandardLangPath.ERROR_SIGN_BACKSIDE);
                    event.line(1, Component.empty());
                    event.setCancelled(true);
                }

                if (!player.hasPermission(WirelessPermissions.CREATE_NODE.getPermission())) {
                    plugin.getMessageManager().sendLang(player, StandardLangPath.NO_PERMISSION);
                    return;
                }

                if (newNodeType == WirelessNodeType.RECEIVER && !(event.getBlock().getBlockData() instanceof WallSign)) {
                    plugin.getMessageManager().sendLang(player, WirelessLangPath.ERROR_RECEIVER_NOT_WALL);
                    event.setCancelled(true);
                    return;
                }

                String networkChannel = PlainTextComponentSerializer.plainText().serialize(line2Comp);

                if (networkChannel.isBlank()) {
                    plugin.getMessageManager().sendLang(player, WirelessLangPath.ERROR_NO_NETWORK);

                    event.setCancelled(true);
                    return;
                } else {
                    @Nullable String ownerUUIDStr;

                    if (line3Comp == null) {
                        ownerUUIDStr = null;
                    } else {
                        ownerUUIDStr = PlainTextComponentSerializer.plainText().serialize(line3Comp);

                        if (ownerUUIDStr.isBlank()) {
                            ownerUUIDStr = player.getUniqueId().toString();
                        } else {
                            OfflinePlayer playerOnSign = plugin.getConfigManager().getPlayerFromString(ownerUUIDStr);

                            if (playerOnSign == null) {
                                plugin.getMessageManager().sendLang(player, StandardLangPath.ARG_NOT_A_PLAYER, Placeholder.unparsed(StandartPlaceHolders.TEXT.getPlaceholder(), ownerUUIDStr));
                                event.setCancelled(true);
                                return;
                            } else if (!player.getUniqueId().equals(playerOnSign.getUniqueId()) && !player.hasPermission(WirelessPermissions.SET_NODE_OWNER.getPermission())) {
                                plugin.getMessageManager().sendLang(player, StandardLangPath.NO_PERMISSION);
                                return;
                            }
                        }
                    }

                    newNetwork = networks.get(new NetworkKey(networkChannel, ownerUUIDStr));
                }
            }

            if (event.getSide() != Side.FRONT) {
                if ((oldNode == null && newNetwork != null) ||
                    (oldNode != null && (oldNode.getNodeType() != newNodeType || !oldNode.getNetwork().equals(newNetwork)))) { // something changed
                    if (oldNode instanceof WirelessTransmitter transmitter) {
                        transmitter.getNetwork().removeTransmitter(transmitter);

                        if (newNodeType == WirelessNodeType.NONE) {
                            plugin.getMessageManager().sendLang(player, WirelessLangPath.TRANSMITTER_REMOVED);
                        }
                    } else if (oldNode instanceof AWirelessReceiver receiver) {
                        receiver.getNetwork().removeReceiver(receiver);

                        if (newNodeType == WirelessNodeType.NONE) {
                            plugin.getMessageManager().sendLang(player, WirelessLangPath.RECEIVER_REMOVED);
                        }
                    }

                    loadedNodes.remove(location);

                    // we explicitly don't use a snapshot here since it WILL be overwritten by this event and the PDC values will not get saved!
                    // this will still use the internal snapshot of the tile entity witch will get updated by the event, so no need to call .update() on the block state.
                    PersistentDataContainer container = ((Sign) event.getBlock().getState(false)).getPersistentDataContainer(); // danger no snapshot!
                    if (newNetwork == null) {
                        // clear PDC
                        container.remove(nodeTypeKey);
                        container.remove(networkChannelKey);
                        container.remove(ownerUUIDKey);
                    } else { // new valid network
                        // register in network / cache
                        if (newNodeType == WirelessNodeType.RECEIVER) {
                            event.line(0, getFeatureConfig().getLabel(WirelessNodeType.RECEIVER));
                            event.line(1, getFeatureConfig().getID(WirelessNodeType.RECEIVER));

                            AWirelessReceiver receiver = AWirelessReceiver.createReceiver(plugin, location, newNetwork);
                            loadedNodes.put(location, receiver);
                        } else {
                            event.line(0, getFeatureConfig().getLabel(WirelessNodeType.TRANSMITTER));
                            event.line(1, getFeatureConfig().getID(WirelessNodeType.TRANSMITTER));

                            WirelessTransmitter transmitter = new WirelessTransmitter(plugin, location, newNetwork);
                            loadedNodes.put(location, transmitter);
                        }

                        // set in PDC
                        container.set(nodeTypeKey, NODE_DATA_TYPE, newNodeType);
                        container.set(networkChannelKey, PersistentDataType.STRING, newNetwork.getChannel());
                        container.set(ownerUUIDKey, PersistentDataType.STRING, newNetwork.getOwnerUUIDStr());

                        if (newNodeType == WirelessNodeType.RECEIVER) {
                            plugin.getMessageManager().sendLang(player, WirelessLangPath.RECEIVER_CREATED);
                        } else if (newNodeType == WirelessNodeType.TRANSMITTER) {
                            plugin.getMessageManager().sendLang(player, WirelessLangPath.TRANSMITTER_CREATED);
                        }
                    }
                } // something changed
            }
        } else { // empty lines
            if (event.getSide() == Side.FRONT) {
                // clear PDC
                PersistentDataContainer container = ((Sign) event.getBlock().getState(false)).getPersistentDataContainer(); // danger no snapshot!
                container.remove(nodeTypeKey);
                container.remove(networkChannelKey);
                container.remove(ownerUUIDKey);

                if (oldNode instanceof WirelessTransmitter transmitter) {
                    transmitter.getNetwork().removeTransmitter(transmitter);
                    plugin.getMessageManager().sendLang(player, WirelessLangPath.TRANSMITTER_REMOVED);
                } else if (oldNode instanceof AWirelessReceiver receiver) {
                    receiver.getNetwork().removeReceiver(receiver);
                    plugin.getMessageManager().sendLang(player, WirelessLangPath.RECEIVER_REMOVED);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onSignPowerChange(@NotNull BlockPhysicsEvent event) {
        if (!getFeatureConfig().isEnabled()) { // todo (de) register listener just with dis-/ enable methods
            return;
        }

        Block eBlock = event.getBlock();
        if (Tag.ALL_SIGNS.isTagged(event.getChangedType())) { // fast check to "fail" fast for non-relevant changes
            AWirelessNode node = loadedNodes.get(eBlock.getLocation());

            if (node instanceof WirelessTransmitter transmitter) {
                /*
                 * eBlock.getBlockPower() does NOT work here!
                 * It will fail to get indirect power whenever a "direct" power source (just redstone dust, really) is next to it
                 */
                int newPowerState = ((CraftWorld) event.getBlock().getWorld()).getHandle().getBestNeighborSignal(new BlockPos(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()));

                transmitter.setLastPowerState((byte) newPowerState);
            }  // is transmitter
        } // is sign
    }

    @Subscribe(priority = com.sk89q.worldedit.util.eventbus.EventHandler.Priority.VERY_LATE)
    @SuppressWarnings("unused")
    private void onEditSessionEvent(@NotNull EditSessionEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        if (event.getStage() == EditSession.Stage.BEFORE_CHANGE) {

            final World world;
            try {
                world = BukkitAdapter.adapt(event.getWorld());
            } catch (RuntimeException ex) {
                plugin.getComponentLogger().warn("Failed to register logging for WorldEdit!", ex);
                return;
            }

            event.setExtent(new SignCheckExtend(event.getExtent(), world));
        }
    }

    /**
     * This event gets fired, if the sign gets broken indirectly
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onSignDestroy(@NotNull BlockDestroyEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        if (Tag.ALL_SIGNS.isTagged(event.getBlock().getType())) {
            AWirelessNode node = loadedNodes.get(event.getBlock().getLocation());

            if (node instanceof WirelessTransmitter transmitter) {
                transmitter.getNetwork().removeTransmitter(transmitter);
                loadedNodes.remove(event.getBlock().getLocation());
            } else if (node instanceof AWirelessReceiver receiver) {
                receiver.getNetwork().removeReceiver(receiver);
                loadedNodes.remove(event.getBlock().getLocation());
            }
        }
    }

    /**
     * This gets fired when the sign gets broken directly.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerSignBreak(@NotNull BlockBreakEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        if (Tag.ALL_SIGNS.isTagged(event.getBlock().getType())) {
            removeNodeAt(event.getBlock().getLocation());
        }
    }

    private void removeNodeAt(final @NotNull Location location) {
        AWirelessNode node = loadedNodes.get(location);

        if (node instanceof WirelessTransmitter transmitter) {
            transmitter.getNetwork().removeTransmitter(transmitter);
            loadedNodes.remove(location);
        } else if (node instanceof AWirelessReceiver receiver) {
            receiver.getNetwork().removeReceiver(receiver);
            loadedNodes.remove(location);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onChunkLoad(@NotNull ChunkLoadEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        if (!event.isNewChunk()) { // ignore fresh chunks
            provider.whenAllNeighboursLoaded(event.getChunk()).
                thenAccept(this::onChunkCompletelyLoaded);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onChunkUnload(@NotNull ChunkUnloadEvent event) { // this should also be called when a world unloads
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        if (Bukkit.isPrimaryThread()) { // only ever call on main thread!
            if (chunkOfInterest.remove(event.getChunk().getChunkKey())) {
                Collection<BlockState> signs = event.getChunk().getTileEntities(tileEntity -> Tag.ALL_SIGNS.isTagged(tileEntity.getType()), false); // danger, not a snapshot!

                for (BlockState state : signs) {
                    loadedNodes.remove(state.getLocation().toBlockLocation());
                }
            }
        } else {
            throw new IllegalStateException("chunk load was called async!");
        }
    }

    private void onChunkCompletelyLoaded(@NotNull Chunk chunk) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        if (Bukkit.isPrimaryThread()) { // only ever call on main thread!
            chunkOfInterest.add(chunk.getChunkKey());

            Collection<BlockState> signs = chunk.getTileEntities(tileEntity -> Tag.ALL_SIGNS.isTagged(tileEntity.getType()), false); // danger, not a snapshot!

            for (BlockState sign : signs) {
                // since they are in ALL_SIGNS tag we can safely cast
                PersistentDataContainer container = ((Sign)sign).getPersistentDataContainer();

                sign.update(true, false);

                WirelessNodeType nodeType = container.get(nodeTypeKey, NODE_DATA_TYPE);

                if (nodeType == null) {
                    nodeType = getFeatureConfig().fromID(((Sign) sign).getSide(Side.FRONT).line(1));

                    if (nodeType != WirelessNodeType.NONE) {
                        container.set(nodeTypeKey, NODE_DATA_TYPE, nodeType);
                    }
                }

                if (nodeType != WirelessNodeType.NONE) { // todo everything behind this might get too heavy
                    String networkChannel = container.get(networkChannelKey, PersistentDataType.STRING);

                    if (networkChannel == null) {
                        networkChannel = PlainTextComponentSerializer.plainText().serialize(((Sign) sign).getSide(Side.FRONT).line(2));

                        if (!networkChannel.isBlank()) {
                            container.set(networkChannelKey, PersistentDataType.STRING, networkChannel);
                        }
                    }

                    String ownerUUIDStr = container.get(ownerUUIDKey, PersistentDataType.STRING);
                    if (ownerUUIDStr == null) {
                        ownerUUIDStr = PlainTextComponentSerializer.plainText().serialize(((Sign) sign).getSide(Side.FRONT).line(3));

                        if (!ownerUUIDStr.isBlank()) {
                            container.set(networkChannelKey, PersistentDataType.STRING, ownerUUIDStr);
                        }
                    }

                    if (!networkChannel.isBlank()) {
                        WirelessNetwork network = networks.get(new NetworkKey(networkChannel, ownerUUIDStr.isBlank() ? null : ownerUUIDStr));

                        if (nodeType == WirelessNodeType.TRANSMITTER) {
                            WirelessTransmitter newTransmitter = new WirelessTransmitter(plugin, sign.getLocation(), network);
                            loadedNodes.put(sign.getLocation().toBlockLocation(), newTransmitter);
                        } else if (nodeType == WirelessNodeType.RECEIVER) {
                            final AWirelessReceiver newReceiver = AWirelessReceiver.createReceiver(plugin, sign.getLocation(), network);
                            loadedNodes.put(sign.getLocation().toBlockLocation(), newReceiver);
                        }
                    }
                }
            }
        } else {
            throw new IllegalStateException("chunk load was called async!");
        }
    }

    private @NotNull WirelessNetwork getNetworkFromComponent(@NotNull String channel, @Nullable Component component) {
        // solve a name or uuid
        OfflinePlayer player;
        if (component != null) {
            player = plugin.getConfigManager().getPlayerFromString(PlainTextComponentSerializer.plainText().serialize(component));
        } else {
            player = null;
        }

        return networks.get(new NetworkKey(channel, player == null ? null : player.getUniqueId().toString()));
    }

    private @NotNull WirelessNodeType getNodeType(@NotNull Sign sign) {
        WirelessNodeType nodeType = sign.getPersistentDataContainer().get(nodeTypeKey, NODE_DATA_TYPE);

        if (nodeType == null) {
            nodeType = getFeatureConfig().fromID(sign.getSide(Side.FRONT).line(1));

            // validity check
            if (nodeType == WirelessNodeType.RECEIVER && !(sign.getBlockData() instanceof WallSign)) {
                return WirelessNodeType.NONE;
            }
        }

        return nodeType;
    }

    private record NetworkKey(@NotNull String networkChannel, @Nullable String ownerUUIDStr) {
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof NetworkKey(final @NotNull String channel, final @NotNull String uuidStr)) {
                if (this.networkChannel().equalsIgnoreCase(channel)) {
                    if (this.ownerUUIDStr() != null) {
                        return this.ownerUUIDStr().equalsIgnoreCase(uuidStr);
                    } else {
                        return uuidStr == null;
                    }
                }
            }

            return false;
        }
    }

    private class SignCheckExtend extends AbstractDelegateExtent {
        private final @NotNull World world;

        public SignCheckExtend(@NotNull Extent extent, @NotNull World world) {
            super(extent);
            this.world = world;
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(@NotNull BlockVector3 position, @NotNull T block) throws WorldEditException {
            if (!getFeatureConfig().isEnabled()) {
                return super.setBlock(position, block);
            }

            Material oldType = BukkitAdapter.adapt(super.getBlock(position).getBlockType());
            Material newType = BukkitAdapter.adapt(block.getBlockType());

            if (super.setBlock(position, block)) {
                if (Bukkit.isPrimaryThread()) {
                    if (Tag.ALL_SIGNS.isTagged(oldType)) { // removed
                        removeNodeAt(new Location(world, position.x(), position.y(), position.z()));
                    }
                } else {
                    // async is dangerous
                    Bukkit.getScheduler().runTask(plugin, () -> removeNodeAt(new Location(world, position.x(), position.y(), position.z())));
                }

                if (Tag.ALL_SIGNS.isTagged(newType)) { // set / replaced
                    final Location location = new Location(world, position.x(), position.y(), position.z());

                    // since world edit doesn't care about block changes, we can't access the block state
                    // within the Extend and have to get it from the world
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (location.isChunkLoaded()) { // sanity check if the chunk is still loaded, else wise the chunk load event will take care
                            Block newBlock = location.getBlock();

                            if (newBlock.getState(false) instanceof Sign sign) { // danger no snapshot
                                SignSide frontSide = sign.getSide(Side.FRONT);
                                WirelessNodeType nodeType = getNodeType(sign);
                                PersistentDataContainer container = sign.getPersistentDataContainer();

                                if (nodeType != WirelessNodeType.NONE) {
                                    if (nodeType == WirelessNodeType.RECEIVER && !(sign.getBlockData() instanceof WallSign)) {
                                        // invalid
                                        sign.getPersistentDataContainer().remove(nodeTypeKey);
                                        frontSide.line(1, getFeatureConfig().getLabel(WirelessNodeType.NONE));
                                        sign.update(false, false);
                                        return;
                                    }
                                    String networkChannel = container.get(networkChannelKey, PersistentDataType.STRING);

                                    if (networkChannel == null) {
                                        networkChannel = PlainTextComponentSerializer.plainText().serialize(frontSide.line(2));
                                    }

                                    if (networkChannel.isBlank()) {
                                        // invalid
                                        sign.getPersistentDataContainer().remove(nodeTypeKey);
                                        frontSide.line(1, getFeatureConfig().getLabel(WirelessNodeType.NONE));
                                        sign.update(false, false);
                                    } else {
                                        @Nullable String ownerUUIDStr = container.get(ownerUUIDKey, PersistentDataType.STRING);
                                        if (ownerUUIDStr == null) {
                                            ownerUUIDStr = PlainTextComponentSerializer.plainText().serialize(frontSide.line(3));
                                        }

                                        WirelessNetwork newNetwork = networks.get(new NetworkKey(networkChannel, ownerUUIDStr.isBlank() ? null : ownerUUIDStr));

                                        // register in network / cache
                                        if (nodeType == WirelessNodeType.RECEIVER) {
                                            AWirelessReceiver receiver = AWirelessReceiver.createReceiver(plugin, location, newNetwork);
                                            loadedNodes.put(location, receiver);
                                        } else {
                                            WirelessTransmitter transmitter = new WirelessTransmitter(plugin, location, newNetwork);
                                            loadedNodes.put(location, transmitter);
                                        }
                                    } // valid network channel
                                } // node type check
                            } // block check sign
                        } // check if world is still loaded

                    }, 20);
                }

                return true;
            }

            return false;
        }
    }
}
