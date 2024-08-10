package de.greensurvivors.greenbook.features.wireless.network;

import de.greensurvivors.greenbook.GreenBook;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.type.WallSign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public abstract class AWirelessReceiver extends AWirelessNode {

    /**
     * Creates a new wireless receiver with the given location and network.
     * The receiver will be automatically added to the network.
     *
     * @param plugin   the greenBook plugin
     * @param location the location of the receiver
     * @param network  the network the receiver belongs to
     */
    protected AWirelessReceiver(@NotNull GreenBook plugin, final @NotNull Location location, final @NotNull WirelessNetwork network) {
        super(plugin, WirelessNodeType.RECEIVER, location, network);

        network.addReceiver(this);
    }

    /**
     * Creates a new wireless receiver, if the block at the location is a wall sign.
     * The specific receiver type will be determined based on the block the sign is attached to.
     *
     * @param plugin   the greenBook plugin
     * @param location the location of the receiver sign
     * @param network  the network the receiver belongs to
     * @return the created receiver, or null if the block at the location is not a wall sign
     */
    @SuppressWarnings("UnstableApiUsage") // block type
    public static @Nullable AWirelessReceiver createReceiver(@NotNull GreenBook plugin, @NotNull Location location, @NotNull WirelessNetwork network) {
        if (location.getBlock().getBlockData() instanceof WallSign wallSign) {
            BlockFace face = wallSign.getFacing().getOppositeFace();

            if (location.add(face.getModX(), face.getModY(), face.getModZ()).getBlock().getType() == BlockType.LECTERN.asMaterial()) {
                return new FineWirelessReceiver(plugin, location, network);
            } else {
                return new BinaryWirelessReceiver(plugin, location, network);
            }
        } else {
            return null;
        }

    }

    /**
     * Sets the power state of the receiver based on the new signal strength.
     * A receiver might output an on/off signal or repeat the given signal strength,
     * depending on the receiver type.
     *
     * @param newSignalStrength the new signal strength
     */
    public abstract void setPowerState(@Range(from = 0, to = 15) byte newSignalStrength);
}
