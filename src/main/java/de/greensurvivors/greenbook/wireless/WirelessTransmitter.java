package de.greensurvivors.greenbook.wireless;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WirelessTransmitter extends WirelessSign {
    private Byte lastPowerState = null;

    public WirelessTransmitter(@NotNull Location location, @Nullable Network network) {
        super(location, network);
    }

    public synchronized Byte getLastPowerState() {
        return lastPowerState;
    }

    public synchronized void setLastPowerState(byte lastPowerState) {
        this.lastPowerState = lastPowerState;
    }
}
