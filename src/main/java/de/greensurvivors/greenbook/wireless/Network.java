package de.greensurvivors.greenbook.wireless;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Network {
    private final @NotNull String id;
    private final @Nullable String ownerUUIDStr;
    private final @NotNull ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Network(@NotNull String id, @Nullable String ownerUUIDStr) {
        this.id = id;
        this.ownerUUIDStr = ownerUUIDStr;
    }

    @Override // power state doesn't matter
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Network) obj;
        return this.id.equalsIgnoreCase(that.id) &&
                Objects.equals(this.ownerUUIDStr, that.ownerUUIDStr);
    }

    public @NotNull String getId() {
        return id;
    }

    public @Nullable String getOwnerUUIDStr() {
        return ownerUUIDStr;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerUUIDStr);
    }

    @Override
    public String toString() {
        return "Network[" +
                "Id=" + id + ", " +
                "OwnerUUIDStr=" + ownerUUIDStr + ']';
    }

    public @NotNull ReentrantReadWriteLock getLock() {
        return lock;
    }
}
