package de.greensurvivors.greenbook;

import com.sk89q.worldedit.WorldEdit;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class DependencyManager { // todo better dependency system for Features. only load them if their dependencies are fulfilled
    private final @NotNull GreenBook plugin;

    public DependencyManager(@NotNull GreenBook plugin) {
        this.plugin = plugin;
    }

    public void registerEditSessionEvent(@NotNull Object eventHandlerClass) {
        Plugin wePlugin = Bukkit.getPluginManager().getPlugin("Wordedit");

        if (wePlugin != null && wePlugin.isEnabled()) {
            WorldEdit.getInstance().getEventBus().register(eventHandlerClass);
        }
    }
}
