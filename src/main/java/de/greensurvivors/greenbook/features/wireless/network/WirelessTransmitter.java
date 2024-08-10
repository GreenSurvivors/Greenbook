package de.greensurvivors.greenbook.features.wireless.network;

import de.greensurvivors.greenbook.GreenBook;
import net.minecraft.core.BlockPos;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;


public class WirelessTransmitter extends AWirelessNode {
    /**
     * Keeps track of the last known power state the transmitter was in.
     * This is ranged from 0 to 15 as it mirrors redstone power
     */
    private @Range(from = 0, to = 15) byte lastPowerState;

    /**
     * Creates a new wireless transmitter,
     * at the given location and for the given network.
     * This also registers this transmitter into it's network
     *
     * @param plugin the greenBook plugin
     * @param location the location of the transmitter
     * @param network the network the transmitter belongs to
     */
    public WirelessTransmitter(@NotNull GreenBook plugin, @NotNull Location location, @NotNull WirelessNetwork network) {
        super(plugin,WirelessNodeType.TRANSMITTER, location, network);

        /*
         * eBlock.getBlockPower() does NOT work here!
         * It will fail to get indirect power whenever a "direct" power source (just redstone dust, really) is next to it
         */
        // todo move to a version dependent class
        lastPowerState = (byte) ((CraftWorld)location.getWorld()).getHandle().getBestNeighborSignal(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        network.addTransmitter(this);
    }

    /**
     * Get the last known power state of the transmitter.
     * @return the last known power state, ranged from 0 to 15
     */
    @Contract(pure = true)
    public @Range(from = 0, to = 15) byte getLastPowerState() {
        return lastPowerState;
    }

    /**
     * Sets the last power state of the transmitter.
     * If it has changed, calculates the highest power state in the network,
     * and updates the power state of all receivers within the network.
     *
     * @param  nowPowerState the new power state to set
     * @return true if the power state was updated, false if it remained the same
     */
    @Contract(mutates = "this")
    public boolean setLastPowerState(@Range(from = 0, to = 15) byte nowPowerState) {
        if (this.lastPowerState != nowPowerState) {
            this.lastPowerState = nowPowerState;

            network.calcHighestPowerState();
            network.updatePowerStateReceivers();

            return true;
        } else {
            return false;
        }
    }
}
