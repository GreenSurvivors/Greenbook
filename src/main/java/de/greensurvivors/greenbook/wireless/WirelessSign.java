package de.greensurvivors.greenbook.wireless;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WirelessSign {
    protected final @NotNull Location location;
    protected final @Nullable Network network;

    public WirelessSign(@NotNull Location location, @Nullable Network network) {
        this.location = location;
        this.network = network;
    }

    public boolean isValid() {
        return network != null;
    }

    public @Nullable String getOwnerUUIDStr() {
        return isValid() ? network.getOwnerUUIDStr() : null;
    }

    public @Nullable String getChannel() {
        return isValid() ? network.getId() : null;
    }

    public @Nullable Network getNetwork() {
        return network;
    }

    public @NotNull Location getLocation() {
        return location;
    }
}
