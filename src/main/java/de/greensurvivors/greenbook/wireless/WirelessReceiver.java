package de.greensurvivors.greenbook.wireless;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public class WirelessReceiver extends WirelessSign {
    private final long timeUntilExpiration = TimeUnit.MINUTES.toNanos(5);
    private long startNanos = System.nanoTime();

    public WirelessReceiver(@NotNull Location location, @Nullable Network network) {
        super(location, network);
    }

    public boolean isExpired() {
        return System.nanoTime() - startNanos >= timeUntilExpiration;
    }

    public void refresh() {
        startNanos = System.nanoTime();
    }
}
