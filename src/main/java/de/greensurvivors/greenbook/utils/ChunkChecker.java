package de.greensurvivors.greenbook.utils;

import de.greensurvivors.greenbook.GreenBook;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ChunkChecker implements Listener {
    private final @NotNull Map<@NotNull String, @NotNull Set<@NotNull ChunkGroup>> trackedWorlds = new HashMap<>();

    public ChunkChecker(@NotNull GreenBook plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public @NotNull CompletableFuture<Chunk> whenAllNeighboursLoaded(@NotNull Chunk mainChunk) {
        CompletableFuture<Chunk> future = new CompletableFuture<>();
        Set<ChunkGroup> trackedChunks = trackedWorlds.computeIfAbsent(mainChunk.getWorld().getName(), ignored -> new HashSet<>());

        trackedChunks.add(new ChunkGroup(mainChunk, future));

        return future;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkLoad(@NotNull ChunkLoadEvent event) {
        Set<ChunkGroup> trackedChunks = trackedWorlds.get(event.getWorld().getName());

        if (trackedChunks != null) {
            final long key = event.getChunk().getChunkKey();
            trackedChunks.removeIf(chunkGroup -> chunkGroup.onChunkLoad(key));

            if (trackedChunks.isEmpty()) {
                trackedWorlds.remove(event.getWorld().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onChunkUnload(@NotNull ChunkUnloadEvent event) {
        Set<ChunkGroup> trackedChunks = trackedWorlds.get(event.getWorld().getName());

        if (trackedChunks != null) {
            trackedChunks.removeIf(chunkGroup -> chunkGroup.onChunkUnload(event.getChunk().getChunkKey()));

            if (trackedChunks.isEmpty()) {
                trackedWorlds.remove(event.getWorld().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWorldUnload(@NotNull WorldUnloadEvent event) {
        Set<ChunkGroup> trackers = trackedWorlds.get(event.getWorld().getName());

        if (trackers != null) {
            for (ChunkGroup a : trackers) {
                a.onWorldUnload();
            }

            trackedWorlds.remove(event.getWorld().getName());
        }
    }

    private static class ChunkGroup {
        private final @NotNull Map<Long, Boolean> surroundingChunks = new Long2BooleanOpenHashMap(8); // should be faster than using a regular hashmap
        private final @NotNull Chunk mainChunk;
        private final @NotNull CompletableFuture<Chunk> future;

        protected ChunkGroup(@NotNull Chunk mainChunk, @NotNull CompletableFuture<Chunk> future) {
            this.mainChunk = mainChunk;
            this.future = future;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = mainChunk.getX() + dx;
                    int z = mainChunk.getZ() + dz;

                    surroundingChunks.put(Chunk.getChunkKey(x, z), mainChunk.getWorld().isChunkLoaded(x, z));
                }
            }

            checkLoaded();
        }

        private boolean checkLoaded() {
            for (Boolean isChunkLoaded : surroundingChunks.values()) {
                if (!isChunkLoaded) {
                    return false;
                }
            }

            future.complete(mainChunk);
            return true;
        }

        protected boolean onChunkLoad(long chunkKey) {
            if (surroundingChunks.containsKey(chunkKey)) {
                surroundingChunks.put(chunkKey, true);

                return checkLoaded();
            }

            return false;
        }

        protected boolean onChunkUnload(long chunkKey) {
            if (mainChunk.getChunkKey() == chunkKey) {
                future.cancel(true);
                return true;
            } else if (surroundingChunks.containsKey(chunkKey)) {
                surroundingChunks.put(chunkKey, false);
            }

            return false;
        }

        protected void onWorldUnload() {
            future.cancel(true);
        }
    }
}
