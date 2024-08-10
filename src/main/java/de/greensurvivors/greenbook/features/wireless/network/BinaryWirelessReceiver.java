package de.greensurvivors.greenbook.features.wireless.network;

import de.greensurvivors.greenbook.GreenBook;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.WallSign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * This wireless receiver will output an on/off signal,
 * if the signal strength in the network is greater than 0.
 * Will be faster than {@link FineWirelessReceiver},
 * so consider using that instead, if you don't depend on signal strength.
 */
public class BinaryWirelessReceiver extends AWirelessReceiver {
    private final @NotNull Location outputBlockLoc;
    private boolean isPowered = false;

    protected BinaryWirelessReceiver(@NotNull GreenBook plugin, @NotNull Location location, @NotNull WirelessNetwork network) {
        super(plugin, location, network);

        if (location.getBlock().getBlockData() instanceof WallSign wallSign) { // todo move this check up before super when it becomes possible
            BlockFace face = wallSign.getFacing().getOppositeFace();
            outputBlockLoc = location.add(face.getModX() * 2, 0, face.getModZ() * 2);
        } else {
            throw new IllegalStateException("Receiver was not a wallSign at : " + location);
        }
    }

    /**
     * Toggles the power state of the block behind the block the receiver is attached to.
     * If the signal strength is greater than 0, the block will be powered, otherwise it will be unpowered.
     *
     * @param newSignalStrength the new signal strength
     */
    @Override
    public void setPowerState(@Range(from = 0, to = 15) byte newSignalStrength) {
        boolean newState = newSignalStrength > 0;

        if (newState != isPowered) {
            if (outputBlockLoc.getBlock().getBlockData() instanceof Powerable powerable) {
                powerable.setPowered(newState);
            }

            isPowered = newState;
        }
    }
}
