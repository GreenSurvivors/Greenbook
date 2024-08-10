package de.greensurvivors.greenbook.features.wireless.network;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.utils.Utils;
import org.bukkit.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AWirelessNode {
    protected final @NotNull Location location;
    protected final @NotNull WirelessNetwork network;
    protected final @NotNull WirelessNodeType nodeType;
    protected final @NotNull GreenBook plugin;

    protected AWirelessNode(@NotNull GreenBook plugin, @NotNull WirelessNodeType nodeType,
                            @NotNull Location location, @NotNull WirelessNetwork network) {
        this.plugin = plugin;
        this.nodeType = nodeType;
        this.location = location;
        this.network = network;
    }

    public @NotNull WirelessNodeType getNodeType() {
        return nodeType;
    }

    @Contract(pure = true)
    public @Nullable String getOwnerUUIDStr() {
        return network.getOwnerUUIDStr();
    }

    @Contract(pure = true)
    public @Nullable String getChannel() {
        return network.getChannel();
    }

    @Contract(pure = true)
    public @NotNull WirelessNetwork getNetwork() {
        return network;
    }

    @Contract(pure = true)
    public @NotNull org.bukkit.Location getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof AWirelessNode that) {
            return Utils.isSameBlockLocation(this.getLocation(), that.getLocation()) &&
                this.getNetwork().equals(that.getNetwork());
        } else {
            return false;
        }
    }
}
