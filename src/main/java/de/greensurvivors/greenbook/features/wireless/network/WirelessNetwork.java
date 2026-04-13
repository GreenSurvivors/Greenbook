package de.greensurvivors.greenbook.features.wireless.network;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.features.wireless.WirelessConfigManager;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WirelessNetwork {
    private final @NotNull GreenBook plugin;
    private final @NotNull String channel;
    private final @Nullable String ownerUUIDStr;
    private final @NotNull Map<@NotNull Location, @NotNull WirelessTransmitter> transmitters = new HashMap<>();
    private final @NotNull Set<@NotNull AWirelessReceiver> receivers = new HashSet<>();
    private byte cachedHighestPowerState = 0;

    public WirelessNetwork(@NotNull GreenBook plugin, @NotNull String channel, @Nullable String ownerUUIDStr) {
        this.plugin = plugin;
        this.channel = channel;
        this.ownerUUIDStr = ownerUUIDStr;
    }

    public @NotNull String getChannel() {
        return channel;
    }

    public @Nullable String getOwnerUUIDStr() {
        return ownerUUIDStr;
    }

    public @NotNull Collection<@NotNull AWirelessReceiver> getReceivers() {
        return receivers;
    }

    public void addReceiver(@NotNull AWirelessReceiver newReceiver) {
        receivers.add(newReceiver);
    }

    public void removeReceiver(@NotNull AWirelessReceiver receiver) {
        receivers.remove(receiver);
    }

    public void addTransmitter(@NotNull WirelessTransmitter newTransmitter) {
        transmitters.put(newTransmitter.getLocation(), newTransmitter);

        if (newTransmitter.getLastPowerState() > cachedHighestPowerState) {
            cachedHighestPowerState = newTransmitter.getLastPowerState();

            updatePowerStateReceivers();
        }
    }

    public void removeTransmitter(@NotNull WirelessTransmitter transmitter) {
        transmitters.remove(transmitter.getLocation().toBlockLocation());

        if (transmitter.getLastPowerState() >= cachedHighestPowerState) {
            calcHighestPowerState();
            updatePowerStateReceivers();
        }
    }

    public void removeTransmitter(@NotNull Location transmitterLocation) {
        WirelessTransmitter removed = transmitters.remove(transmitterLocation.toBlockLocation());

        if (removed != null && removed.getLastPowerState() >= cachedHighestPowerState) {
            calcHighestPowerState();
            updatePowerStateReceivers();
        }
    }

    public @Nullable WirelessTransmitter getTransmitter(@NotNull Location transmitterLocation) {
        return transmitters.get(transmitterLocation.toBlockLocation());
    }

    protected void updatePowerStateReceivers() {
        for (AWirelessReceiver receiver : getReceivers()) {
            receiver.setPowerState(cachedHighestPowerState);
        }
    }

    public byte calcHighestPowerState() {
        cachedHighestPowerState = 0;

        for (WirelessTransmitter transmitter : transmitters.values()) {
            byte powerState = transmitter.getLastPowerState();

            if (powerState > cachedHighestPowerState) {
                if (powerState >= 15) {
                    cachedHighestPowerState = 15;
                    return 15;
                } else {
                    cachedHighestPowerState = powerState;
                }
            }
        }

        return cachedHighestPowerState;
    }

    @Override
    public boolean equals(Object obj) { // todo can we relay on that nodes are irrelevant?
        if (this == obj) {
            return true;
        } else {
            if (obj instanceof WirelessNetwork that) {
                if (this.getChannel().equalsIgnoreCase(that.getChannel())) {
                    if (!((WirelessConfigManager) plugin.getFeatureRegistry().getFeature(FeatureType.WIRELESS).getFeatureConfig()).usePlayerSpecificChannels()) {
                        return true;
                    } else {
                        return (Objects.equals(this.getOwnerUUIDStr(), that.getOwnerUUIDStr()) ||
                            (this.getOwnerUUIDStr() != null && this.getOwnerUUIDStr().equalsIgnoreCase(that.getOwnerUUIDStr())));
                    }
                }
            }
            return false;
        }
    }
}
