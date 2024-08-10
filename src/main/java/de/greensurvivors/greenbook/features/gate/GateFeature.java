package de.greensurvivors.greenbook.features.gate;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.features.AFeature;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.language.StandardLangPath;
import de.greensurvivors.greenbook.persistenddata.EnumPersistentDataType;
import de.greensurvivors.greenbook.utils.LinkedDoubleIntHashSet;
import de.greensurvivors.greenbook.utils.Utils;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.HangingSign;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({
    "UnstableApiUsage", // position
    // I don't like the way of constructing strings by using {} to insert variables.
    // It's just so much harder to read.
    // If logging ever becomes a performance problem you have bigger problems to solve.
    // Also, I wouldn't recommend to disable any Logging levels lower or equal to info for a minecraft server.
    "StringConcatenationArgumentToLogCall"
})
// todo create a queque / cooldown system; also link multible signs for the same gate together.
public class GateFeature extends AFeature<GateConfig> implements Listener { // todo work with world edit if available
    private final static @NotNull EnumPersistentDataType<BlockFace> DIRECTION_DATA_TYPE = new EnumPersistentDataType<>(BlockFace.class);
    private final static @NotNull EnumPersistentDataType<Axis> AXIS_DATA_TYPE = new EnumPersistentDataType<>(Axis.class);
    private final static @NotNull Map<BlockFace, Axis> cartesianDirections;
    private final static @NotNull Map<BlockFace, Axis> cardinalDirections;

    static {
        EnumMap<BlockFace, Axis> tempCartesian = new EnumMap<>(BlockFace.class);
        EnumMap<BlockFace, Axis> tempCardinal = new EnumMap<>(BlockFace.class);

        tempCartesian.put(BlockFace.UP, Axis.Y);
        tempCartesian.put(BlockFace.NORTH, Axis.Z);
        tempCartesian.put(BlockFace.EAST, Axis.X);
        tempCartesian.put(BlockFace.SOUTH, Axis.Z);
        tempCartesian.put(BlockFace.WEST, Axis.X);
        tempCartesian.put(BlockFace.DOWN, Axis.Y);

        tempCardinal.put(BlockFace.NORTH, Axis.Z);
        tempCardinal.put(BlockFace.EAST, Axis.X);
        tempCardinal.put(BlockFace.SOUTH, Axis.Z);
        tempCardinal.put(BlockFace.WEST, Axis.X);

        cartesianDirections = Collections.unmodifiableMap(tempCartesian);
        cardinalDirections = Collections.unmodifiableMap(tempCardinal);
    }

    private final @NotNull NamespacedKey blockTypeKey, directionKey, axisKey, amountKey, lastPowerStateKey;
    private final @NotNull LoadingCache<@NotNull Location, @NotNull Integer> gateLastPowerStateSignCache;

    public GateFeature(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.BRIDGE, new GateConfig(plugin));

        Bukkit.getPluginManager().registerEvents(this, plugin);

        blockTypeKey = new NamespacedKey(plugin, "gateBlockType");
        axisKey = new NamespacedKey(plugin, "gateAxis");
        directionKey = new NamespacedKey(plugin, "gateDirection");
        amountKey = new NamespacedKey(plugin, "gateBlockAmount");

        lastPowerStateKey = new NamespacedKey(plugin, "gateLastPowerStateKey");

        gateLastPowerStateSignCache = Caffeine.newBuilder().
            expireAfterWrite(2, TimeUnit.MINUTES).
            build(location -> {
                BlockState state = location.getBlock().getState(false); // danger no snapshot

                if (state instanceof Sign sign && getFeatureConfig().isGate(sign.getSide(Side.FRONT).line(1))) {
                    Integer lastPowerState = sign.getPersistentDataContainer().get(lastPowerStateKey, PersistentDataType.INTEGER);

                    return Objects.requireNonNullElse(lastPowerState, -1);
                } else {
                    return Integer.MIN_VALUE;
                }
            });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onSignChange(@NotNull SignChangeEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        Player ePlayer = event.getPlayer();

        //if the 2nd line exists
        if (getFeatureConfig().isGate(event.line(1))) {
            // only handle front sides
            if (event.getSide() == Side.FRONT) {
                // check permission
                if (ePlayer.hasPermission(GatePermissions.GATE_CREATE.getPermission())) {
                    event.line(1, getFeatureConfig().getLabel());

                    plugin.getMessageManager().sendLang(ePlayer, GateLangPath.GATE_CREATE_SUCCESS);
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
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onRightClickSign(@NotNull PlayerInteractEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        //don't fire for offhand
        if (event.getHand() == EquipmentSlot.HAND &&
            //right-clicked a block. Should ensure the getBlock() is not null
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            //cache the clicked block
            Block eBlock = event.getClickedBlock();

            if (eBlock != null && eBlock.getState(false) instanceof Sign sign) { // danger no snapshot!
                if (getFeatureConfig().isGate(sign.getSide(Side.FRONT).line(1))) {
                    if (event.getPlayer().hasPermission(GatePermissions.GATE_USE.getPermission())) {
                        initToggleGate(sign, event.getPlayer());
                    } else {
                        plugin.getMessageManager().sendLang(event.getPlayer(), StandardLangPath.NO_PERMISSION);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onSignPowerChange(@NotNull BlockPhysicsEvent event) {

        Block eBlock = event.getBlock();
        if (Tag.ALL_SIGNS.isTagged(event.getChangedType()) && // fast check to "fail" fast for non-relevant changes
            eBlock.getState(false) instanceof Sign sign) { // danger no snapshot
            final int lastSavedState = gateLastPowerStateSignCache.get(eBlock.getLocation());

            if (lastSavedState > Integer.MIN_VALUE) {
                /*
                 * eBlock.getBlockPower() does NOT work here!
                 * It will fail to get indirect power whenever a "direct" power source (just redstone dust, really) is next to it
                 */
                int newPowerState = ((CraftWorld) event.getBlock().getWorld()).getHandle().getBestNeighborSignal(new BlockPos(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()));

                if (newPowerState != lastSavedState) {
                    sign.getPersistentDataContainer().set(lastPowerStateKey, PersistentDataType.INTEGER, newPowerState);
                }

                if (newPowerState > 0 && lastSavedState <= 0) {
                    initToggleGate(sign, null);
                }
            } // not a gate
        } // is sign and gate
    }

    @Override
    public void registerCommands(@NotNull Commands commandsRegistrar, @NotNull GreenBookCmd mainCommand) {

    }

    @Override
    public void onDisable() { // todo

    }

    @Override
    public void onEnable() { // todo

    }

    private void doToggleWork(final @NotNull Sign sign, final @NotNull FrameFloodFill frameFloodFill, @Nullable Audience audience) { // todo optional open / close message
        final @NotNull PersistentDataContainer container = sign.getPersistentDataContainer();

        @Nullable BlockType blockType = getBlockType(container);
        @Nullable Integer amount = container.get(amountKey, PersistentDataType.INTEGER);

        if (amount == null) {
            amount = 0;
        }

        if (frameFloodFill.isOpen() && frameFloodFill.getFoundEmptyPositions() != null) { // do close
            final @NotNull World world = sign.getWorld();
            final @NotNull Iterator<Position> foundPositionsIterator = frameFloodFill.getFoundEmptyPositions().iterator();

            int i = 0;
            while (i < amount && foundPositionsIterator.hasNext()) {
                world.setType(foundPositionsIterator.next().toLocation(world), blockType.asMaterial());
                i++;
            }

            // remove placed blocks
            container.set(amountKey, PersistentDataType.INTEGER, amount - i);
        } else if (frameFloodFill.getFoundFullPositions() != null) { // do open
            final @NotNull World world = sign.getWorld();

            for (Position position : frameFloodFill.getFoundFullPositions()) {
                Location location = position.toLocation(world);

                // respect water logging
                if (world.getBlockData(location) instanceof Waterlogged waterlogged && waterlogged.isWaterlogged()) {
                    world.setType(location, BlockType.WATER.asMaterial());
                } else {
                    world.setType(location, BlockType.AIR.asMaterial());
                }

                amount++;
            }

            // add removed blocks
            container.set(amountKey, PersistentDataType.INTEGER, amount);
        } else {
            plugin.getComponentLogger().warn("Gate toggled at " + sign.getLocation() + ", but was nighter full nor empty!");
        }
    }

    private @Nullable BlockType getBlockType(final PersistentDataContainer container) {
        final @Nullable String blockTypeKeyStr = container.get(blockTypeKey, PersistentDataType.STRING);
        if (blockTypeKeyStr != null) {
            final @Nullable NamespacedKey blockTypeKey = NamespacedKey.fromString(blockTypeKeyStr);

            if (blockTypeKey != null) {
                final @Nullable BlockType blockType = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK).get(blockTypeKey);

                if (blockType != null) {
                    return blockType;
                } else {
                    plugin.getComponentLogger().warn("Could not get blocktype form registry for key \"" + blockTypeKey.asString() + "\".");
                }
            } else {
                plugin.getComponentLogger().warn("Could not get name spaced key from String \"" + blockTypeKeyStr + "\".");
            }
        } else if (plugin.getComponentLogger().isDebugEnabled()) { // everything is fine. Maybe it was not set yet.
            plugin.getComponentLogger().debug("Could not get key string from persistent data container.");
        }

        return null;
    }

    private void savePathToFrame(final @NotNull Sign sign, final @NotNull BlockFace direction, final @NotNull Axis axis) {
        sign.getPersistentDataContainer().set(directionKey, DIRECTION_DATA_TYPE, direction);
        sign.getPersistentDataContainer().set(axisKey, AXIS_DATA_TYPE, axis);
    }

    /**
     * @throws IllegalArgumentException if the sign is nighter a wall sign, a standing sign nor a hanging (wall) sig; basically whenever there is a new sign type.
     *                                  Don't worry to hard about it. It took minecraft 12 years to add the "new" two hanging sign variants.
     */
    private void initToggleGate(@NotNull Sign signState, @Nullable Audience audience) throws IllegalArgumentException {
        final @NotNull PersistentDataContainer container = signState.getPersistentDataContainer();

        BlockFace direction = container.get(directionKey, DIRECTION_DATA_TYPE);
        Axis axis = container.get(axisKey, AXIS_DATA_TYPE);

        if (direction != null && axis != null) {
            if (axis != Axis.Y) {
                switch (signState.getBlockData()) {
                    case WallSign wallSign -> {
                        // blockface is cartesian and sign is orthogonal to frame
                        if (direction.isCartesian() && direction != wallSign.getFacing() && direction != wallSign.getFacing().getOppositeFace() && cartesianDirections.get(wallSign.getFacing()) != axis) {
                            final @NotNull Block attachedTo = signState.getBlock().getRelative(wallSign.getFacing().getOppositeFace());
                            final @NotNull BlockPosition posInFrame = Position.block(attachedTo.getRelative(direction).getLocation());

                            FrameFloodFill frameFloodFill = new FrameFloodFill(signState.getWorld(), posInFrame, axis);
                            if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                                doToggleWork(signState, frameFloodFill, audience);
                                return;
                            } else {
                                plugin.getComponentLogger().debug("Could not find frame for gate at " + signState.getLocation() + " (wall sign) using the last known state. Ignoring.");
                            }
                        } else { // direction error
                            plugin.getComponentLogger().warn("BlockFace \"" + direction.name() + "\" from Gate at " + signState.getLocation() + " is not cartesian or facing the same axis as the wall sign. Ignoring!");
                        }
                    }
                    case org.bukkit.block.data.type.Sign standingSign -> {
                        if (direction == BlockFace.UP || direction == BlockFace.DOWN) {
                            final @NotNull BlockPosition posInFrame = Position.block(signState.getLocation().add(0, 2 * direction.getModY(), 0));

                            FrameFloodFill frameFloodFill = new FrameFloodFill(signState.getWorld(), posInFrame, axis);
                            if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                                doToggleWork(signState, frameFloodFill, audience);
                                return;
                            } else {
                                plugin.getComponentLogger().debug("Could not find frame for gate at " + signState.getLocation() + " (standing sign) using the last known state. Ignoring.");
                            }
                        } else {
                            plugin.getComponentLogger().warn("BlockFace \"" + direction.name() + "\" from Gate at " + signState.getLocation() + " is nighter up nor down for a standing sign. Ignoring!");
                        }
                    }
                    case HangingSign hangingSign -> { // the one with the long changes
                        if (direction == BlockFace.UP || direction == BlockFace.DOWN) {
                            final @NotNull BlockPosition posInFrame = Position.block(signState.getLocation().add(0, 2 * direction.getModY(), 0));

                            FrameFloodFill frameFloodFill = new FrameFloodFill(signState.getWorld(), posInFrame, axis);
                            if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                                doToggleWork(signState, frameFloodFill, audience);
                                return;
                            } else {
                                plugin.getComponentLogger().debug("Could not find frame for gate at " + signState.getLocation() + " (hanging sign) using the last known state. Ignoring.");
                            }
                        } else {
                            plugin.getComponentLogger().warn("BlockFace \"" + direction.name() + "\" from Gate at " + signState.getLocation() + " is nighter up nor down for a hanging sign. Ignoring!");
                        }
                    }
                    case WallHangingSign wallHangingSign -> { // the one with the post
                        // direction is cartesian and sign is facing orthogonal to frame
                        if (direction.isCartesian() && direction != wallHangingSign.getFacing() && direction != wallHangingSign.getFacing().getOppositeFace() && cartesianDirections.get(wallHangingSign.getFacing()) != axis) {

                            final @NotNull BlockPosition posInFrame;
                            if (direction == BlockFace.UP || direction == BlockFace.DOWN) {
                                posInFrame = Position.block(signState.getLocation().add(0, 2 * direction.getModY(), 0));
                            } else {
                                posInFrame = Position.block(signState.getBlock().getRelative(direction, 2).getLocation());
                            }

                            FrameFloodFill frameFloodFill = new FrameFloodFill(signState.getWorld(), posInFrame, axis);
                            if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                                doToggleWork(signState, frameFloodFill, audience);
                                return;
                            } else {
                                plugin.getComponentLogger().debug("Could not find frame for gate at " + signState.getLocation() + " (hanging wall sign) using the last known state. Ignoring.");
                            }
                        } else {
                            plugin.getComponentLogger().warn("BlockFace \"" + direction.name() + "\" from Gate at " + signState.getLocation() + " is not cartesian or facing the same axis as the hanging wall sign. Ignoring!");
                        }
                    }
                    default ->
                        throw new IllegalArgumentException("Unknown Sign type. The plugin is probably not up to date!");
                }
            } else { // wrong axis
                plugin.getComponentLogger().warn("Axis from Gate at " + signState.getLocation() + " is y, yet a horizontal gate is impossible. Ignoring!");
            }
        } // no or corrupt data


        switch (signState.getBlockData()) {
            case WallSign wallSign -> {
                final @NotNull Block attachedTo = signState.getBlock().getRelative(wallSign.getFacing().getOppositeFace());

                FrameFloodFill frameFloodFill;
                Block inFrame;

                // try "right"
                direction = Utils.rotate90AroundY(wallSign.getFacing());
                inFrame = attachedTo.getRelative(direction);
                axis = cardinalDirections.get(direction);
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(inFrame.getLocation()), axis);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, direction, axis);
                    return;
                }

                // try "left"
                direction = Utils.rotate90AroundY(Utils.rotate270AroundY(wallSign.getFacing()));
                inFrame = attachedTo.getRelative(direction);
                axis = cardinalDirections.get(direction);
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(inFrame.getLocation()), axis);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, direction, axis);
                    return;
                }

                // try "up"
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(attachedTo.getX(), attachedTo.getY() + 1, attachedTo.getZ()), Axis.X);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.UP, Axis.X);
                    return;
                }

                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(attachedTo.getX(), attachedTo.getY() + 1, attachedTo.getZ()), Axis.Z);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.UP, Axis.Z);
                    return;
                }

                // try "down"
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(attachedTo.getX(), attachedTo.getY() - 1, attachedTo.getZ()), Axis.X);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.DOWN, Axis.X);
                    return;
                }

                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(attachedTo.getX(), attachedTo.getY() - 1, attachedTo.getZ()), Axis.Z);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.DOWN, Axis.Z);
                    return;
                }
            }
            case org.bukkit.block.data.type.Sign standingSign -> {
                FrameFloodFill frameFloodFill;

                // try "up"
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signState.getX(), signState.getY() + 2, signState.getZ()), Axis.X);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.UP, Axis.X);
                    return;
                }

                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signState.getX(), signState.getY() + 2, signState.getZ()), Axis.Z);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.UP, Axis.Z);
                    return;
                }

                // try "down"
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signState.getX(), signState.getY() - 2, signState.getZ()), Axis.X);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.DOWN, Axis.X);
                    return;
                }

                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signState.getX(), signState.getY() - 2, signState.getZ()), Axis.Z);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.DOWN, Axis.Z);
                    return;
                }
            }
            case HangingSign hangingSign -> {
                FrameFloodFill frameFloodFill;

                // try "up"
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signState.getX(), signState.getY() + 2, signState.getZ()), Axis.X);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.UP, Axis.X);
                    return;
                }

                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signState.getX(), signState.getY() + 2, signState.getZ()), Axis.Z);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.UP, Axis.Z);
                    return;
                }

                // try "down"
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signState.getX(), signState.getY() - 2, signState.getZ()), Axis.X);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.DOWN, Axis.X);
                    return;
                }

                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signState.getX(), signState.getY() - 2, signState.getZ()), Axis.Z);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.DOWN, Axis.Z);
                    return;
                }
            }
            case WallHangingSign wallHangingSign -> {
                Block signBlock = signState.getBlock();
                FrameFloodFill frameFloodFill;
                Block inFrame;

                // try "right"
                direction = Utils.rotate90AroundY(wallHangingSign.getFacing());
                inFrame = signBlock.getRelative(direction, 2);
                axis = cardinalDirections.get(direction);
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(inFrame.getLocation()), axis);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, direction, axis);
                    return;
                }

                // try "left"
                direction = Utils.rotate90AroundY(Utils.rotate270AroundY(wallHangingSign.getFacing()));
                inFrame = signBlock.getRelative(direction, 2);
                axis = cardinalDirections.get(direction);
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(inFrame.getLocation()), axis);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, direction, axis);
                    return;
                }

                // try "up"
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signBlock.getX(), signBlock.getY() + 2, signBlock.getZ()), Axis.X);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.UP, Axis.X);
                    return;
                }

                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signBlock.getX(), signBlock.getY() + 2, signBlock.getZ()), Axis.Z);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.UP, Axis.Z);
                    return;
                }

                // try "down"
                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signBlock.getX(), signBlock.getY() - 2, signBlock.getZ()), Axis.X);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.DOWN, Axis.X);
                    return;
                }

                frameFloodFill = new FrameFloodFill(signState.getWorld(), Position.block(signBlock.getX(), signBlock.getY() - 2, signBlock.getZ()), Axis.Z);

                if (frameFloodFill.getState() == FrameFloodFill.FloodFillState.DONE) {
                    doToggleWork(signState, frameFloodFill, audience);
                    savePathToFrame(signState, BlockFace.DOWN, Axis.Z);
                    return;
                }
            }

            default -> throw new IllegalArgumentException("Unknown Sign type. The plugin is probably not up to date!");
        }

        if (audience != null) {
            plugin.getMessageManager().sendLang(audience, GateLangPath.GATE_ERROR_NO_FRAME); // no frame found
        }
    }

    // http://www.adammil.net/blog/v126_A_More_Efficient_Flood_Fill.html
    private class FrameFloodFill {
        private final @NotNull World world;
        private final @NotNull Axis axis;
        private final int fixedAxisValue;
        // access a point, consisting of two coordinates, ordered by insertion.
        // This is basically a (linkedHash)set, but with 2 int (NOT Integer to avoid (un)boxing) keys.
        // Really specific, but this enables much more speed. we have about a 50% speed increase
        // (from 15-18 micro seconds down to 9-14, both averaging around the bottom, so 15 vs 10 average)
        // against MultiKeyMap of the apache commons collections!
        // (measured before switching from materials to block data comparison.
        // While the overall time has gone up since then, the difference should have stayed the same)
        private final LinkedDoubleIntHashSet marked = new LinkedDoubleIntHashSet(100); // this is the working data - it records all valid found blocks - empty or gate
        private final LinkedDoubleIntHashSet markedFull = new LinkedDoubleIntHashSet(100); // this is a result - the working data filtered by if the block at the position is a gate block
        private SortedSet<Position> foundEmptyPositions = null;
        private SortedSet<Position> foundFullPositions = null;

        private int area = 0;
        private @NotNull GateFeature.FrameFloodFill.FloodFillState state = FloodFillState.STARTING;

        // axis has to be x or z
        public FrameFloodFill(final @NotNull World world,
                              final @NotNull BlockPosition startPositionInsideFrame,
                              final @NotNull Axis axis) throws IllegalArgumentException {

            this.world = world;
            this.axis = axis;

            switch (axis) {
                case X -> {
                    fixedAxisValue = startPositionInsideFrame.blockZ();
                    scanlineFill(startPositionInsideFrame.blockX(), startPositionInsideFrame.blockY());
                }
                case Y -> {
                    state = FloodFillState.FAILED_INVALID;
                    throw new IllegalArgumentException("Y axis is NOT allowed!");
                }
                case Z -> {
                    fixedAxisValue = startPositionInsideFrame.blockX();
                    scanlineFill(startPositionInsideFrame.blockZ(), startPositionInsideFrame.blockY());
                }
                default -> {
                    throw new IllegalArgumentException("Unknown Axis \"" + axis.name() + "\". When did I miss the addition of a new dimension?");
                }
            }
        }

        /**
         * This methode checks the block at the given coordinate and marks it as done.
         *
         * @return This methode will return true, if the block at the coordinates is not marked yet and either empty or a gate block.
         * It will return false, if the block was already visited, is at the minimum / maximum world height or neither empty nor a gate block.
         * As a hidden property this will set the {@link #getState()} to {@link FloodFillState#FAILED_WORLD_HEIGHT} if the limit of the world was hit
         */
        private boolean markIfBlockTypeAndNotVisited(int axisValue, int y) {
            // checking the set first or the BlockData is not a simple decision.
            // #contains() is surprisingly slow, but I'm not sure if there can be done much about it.
            // if much complicated data will get compared the contains will be faster, but a small set simple data will definitely be faster.
            // I did some runs with the fences of 1.20.6 and randomized data and checking #contains() first where just single micro seconds faster.
            // There is definitely an argument to be made here for simpler cases.
            // However, all in all I would prefer consistent slowness over unpredictable performance.
            // Caching every coordinate we have visited instead just marking the ones matching the criteria is the slowest option.
            // I just hat a 3 week vacation after writing all of this in one swing and I have no idea why using the set is so freaking slow!
            // it's still way faster than apaches Multi key map and pretty much fast enough - but damn why is fetching BlockData and possibly merging it together still faster?
            if (marked.contains(axisValue, y)) {
                return false;
            } else {
                final @NotNull BlockData blockData;

                // get block data
                if (axis == Axis.X) {
                    blockData = world.getBlockData(axisValue, y, fixedAxisValue);
                } else {
                    blockData = world.getBlockData(fixedAxisValue, y, axisValue);
                }

                final boolean isGateBlock = getFeatureConfig().isGateBlock(blockData);

                if (isGateBlock) {
                    markedFull.add(axisValue, y);
                }

                if (isGateBlock || getFeatureConfig().isEmptyBlock(blockData)) {
                    if (marked.add(axisValue, y)) {
                        // if the block is at world max height, there can't be a frame above, therefore it is invalid and the flood fill fails
                        // same for min height
                        if (y <= world.getMinHeight() || y >= (world.getMaxHeight() - 1)) {
                            state = FloodFillState.FAILED_WORLD_HEIGHT;

                            return false;
                        }

                        if (getFeatureConfig().getMaxArea() <= ++area) {
                            state = FloodFillState.FAILED_AREA;

                            return false;
                        } //else {
                        // here would be the place to do something immediately with the coords if we didn't need to wait for the whole process to possibly fail later
                        //}
                    }

                    return true;
                } else {
                    return false;
                }
            }
        }

        public void scanlineFill(final int axisValue, final int y) {
            state = FloodFillState.RUNNING;

            if (markIfBlockTypeAndNotVisited(axisValue, y)) {
                if (state != FloodFillState.RUNNING) {
                    return;
                }

                final ArrayDeque<@NotNull Segment> deque = new ArrayDeque<>();
                deque.push(new Segment(axisValue, axisValue + 1, y, Direction.NONE, true, true));

                do {
                    Segment segment = deque.pop();
                    int startAxisValue = segment.startAxisValue(), endAxisValue = segment.endAxisValue();
                    if (segment.scanLeft()) { // if we should extend the segment towards the left...
                        while (markIfBlockTypeAndNotVisited(startAxisValue - 1, segment.y())) {
                            startAxisValue--;
                        }

                        if (state != FloodFillState.RUNNING) {
                            return;
                        }
                    }

                    if (segment.scanRight()) {
                        while (markIfBlockTypeAndNotVisited(endAxisValue, segment.y())) {
                            endAxisValue++;
                        }

                        if (state != FloodFillState.RUNNING) {
                            return;
                        }
                    }

                    // at this point, the segment from startAxisValue (inclusive) to endAxisValue (exclusive) is filled. compute the region to ignore
                    // segment.startAxisValue-- since the segment is bounded on either side by filled cells or array edges, we can extend the size of
                    // segment.endAxisValue++ the region that we're going to ignore in the adjacent lines by one
                    // scan above and below the segment and add any new segments we find
                    if (segment.y() > world.getMinHeight()) {
                        addLine(deque, startAxisValue, endAxisValue, segment.y() - 1,
                            segment.startAxisValue() - 1, segment.endAxisValue() + 1, Direction.ABOVE, segment.dir() != Direction.BELOW);
                    } else {
                        state = FloodFillState.FAILED_WORLD_HEIGHT;
                        return;
                    }

                    addLine(deque, startAxisValue, endAxisValue, segment.y() + 1,
                        segment.startAxisValue() - 1, segment.endAxisValue() + 1, Direction.BELOW, segment.dir() != Direction.ABOVE);
                } while (!deque.isEmpty());

                // convert our data to positions and sort them by y from up to down
                SortedSet<Position> tempSetFull = new TreeSet<>((pos1, pos2) -> (int) (pos2.y() - pos1.y()));
                SortedSet<Position> tempSetEmpty = new TreeSet<>((pos1, pos2) -> (int) (pos1.y() - pos2.y()));
                // if outside of for-loop to - again - save computation time
                if (axis == Axis.X) {
                    if (markedFull.isEmpty()) {
                        for (LinkedDoubleIntHashSet.DoubleInt doubleInt : marked) {
                            tempSetEmpty.add(Position.block(doubleInt.getValue1(), doubleInt.getValue2(), fixedAxisValue));
                        }
                    } else {
                        for (LinkedDoubleIntHashSet.DoubleInt doubleInt : markedFull) {
                            tempSetFull.add(Position.block(doubleInt.getValue1(), doubleInt.getValue2(), fixedAxisValue));
                        }
                    }
                } else {
                    if (markedFull.isEmpty()) {
                        for (LinkedDoubleIntHashSet.DoubleInt doubleInt : marked) {
                            tempSetEmpty.add(Position.block(fixedAxisValue, doubleInt.getValue2(), doubleInt.getValue1()));
                        }
                    } else {
                        for (LinkedDoubleIntHashSet.DoubleInt doubleInt : markedFull) {
                            tempSetFull.add(Position.block(fixedAxisValue, doubleInt.getValue2(), doubleInt.getValue1()));
                        }
                    }
                }

                // don't let somebody mess with our results
                foundFullPositions = Collections.unmodifiableSortedSet(tempSetFull);
                foundEmptyPositions = Collections.unmodifiableSortedSet(tempSetEmpty);

                state = FloodFillState.DONE;
            }
        }

        private void addLine(final @NotNull ArrayDeque<@NotNull Segment> deque, final int startAxisValue, final int endAxisValue, final int y,
                             final int ignoreStart, final int ignoreEnd, final @NotNull Direction dir, final boolean isNextInDir) {
            @Nullable Integer regionStart = null;
            int axisValue;
            for (axisValue = startAxisValue; axisValue < endAxisValue; axisValue++) {// scan the width of the parent segment
                // if we're outside the region we should ignore and the cell is clear
                if ((isNextInDir || axisValue < ignoreStart || axisValue >= ignoreEnd) && markIfBlockTypeAndNotVisited(axisValue, y)) {
                    if (regionStart == null) {
                        regionStart = axisValue; // and start a new segment if we haven't already
                    }
                } else if (regionStart != null) {// otherwise, if we shouldn't fill this cell, and we have a current segment...
                    deque.push(new Segment(regionStart, axisValue, y, dir, regionStart == startAxisValue, false)); // push the segment
                    regionStart = null; // and end it
                }

                if (!isNextInDir && axisValue < ignoreEnd && axisValue >= ignoreStart) {
                    axisValue = ignoreEnd - 1; // skip over the ignored region
                }
            }
            if (regionStart != null) {
                deque.push(new Segment(regionStart, axisValue, y, dir, regionStart == startAxisValue, true));
            }
        }

        public @NotNull GateFeature.FrameFloodFill.FloodFillState getState() {
            return state;
        }

        /**
         * @return an unmodifiable set containing all found positions with valid gate blocks in them, if the {@link #getState()} is {@link FloodFillState#DONE};
         * or null otherwise (wrong state)
         * @see #getState()
         */
        public @Nullable SortedSet<Position> getFoundFullPositions() {
            return foundFullPositions;
        }

        /**
         * @return an unmodifiable set containing all found positions "empty" (air, maybe water,...), if the {@link #getState()} is {@link FloodFillState#DONE};
         * or null otherwise (wrong state)
         * @see #getState()
         */
        public @Nullable SortedSet<Position> getFoundEmptyPositions() {
            return foundEmptyPositions;
        }

        public boolean isOpen() {
            return markedFull.isEmpty();
        }

        private enum Direction {
            ABOVE,
            NONE,
            BELOW
        }

        protected enum FloodFillState {
            STARTING,
            RUNNING,
            DONE,
            // the reasons how the flood fill might fail
            FAILED_AREA, // area too big
            FAILED_WORLD_HEIGHT, // reached world min / max height
            FAILED_INVALID // invalid arguments
        }

        private record Segment(int startAxisValue, int endAxisValue, int y, @NotNull Direction dir, boolean scanLeft,
                               boolean scanRight) {
        }
    }
}
