package de.greensurvivors.greenbook.features.wireless.network;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum WirelessNodeType {
    /**
     * A Node that can transmit signals and update the {@link WirelessNetwork} its belongs to.
     *
     * @see WirelessTransmitter
     */
    TRANSMITTER(),
    /**
     * A Node that receives signals, whenever the {@link WirelessNetwork} it belongs to gets updated.
     *
     * @see AWirelessReceiver
     * @see BinaryWirelessReceiver
     * @see FineWirelessReceiver
     */
    RECEIVER(),
    /**
     * Invalid Node type, does not match any pattern, has no ID but a display name.
     */
    NONE(); // this always fails to match

    private final static @NotNull Set<@NotNull WirelessNodeType> validValues = Collections.unmodifiableSet(EnumSet.of(TRANSMITTER, RECEIVER));

    public static @NotNull Set<@NotNull WirelessNodeType> getValidTypes() {
        return validValues;
    }
}
