package de.greensurvivors.greenbook;

import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.config.ConfigManager;
import de.greensurvivors.greenbook.features.AFeature;
import de.greensurvivors.greenbook.features.FeatureRegistry;
import de.greensurvivors.greenbook.language.MessageManager;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;

public final class GreenBook extends JavaPlugin {
    private @MonotonicNonNull MessageManager messageManager;
    private @MonotonicNonNull ConfigManager configManager;
    private @MonotonicNonNull FeatureRegistry featureRegistry;
    private @MonotonicNonNull DependencyManager dependencyManager;

    private void onLifeCycleCommandEvent(final @NotNull ReloadableRegistrarEvent<Commands> event) {
        final @NotNull GreenBookCmd mainCommand = new GreenBookCmd(this);

        for (AFeature<?> feature : featureRegistry.getAllFeatures()) {
            feature.registerCommands(event.registrar(), mainCommand);
        }

        mainCommand.finalizeSubCommands(event.registrar());
    }

    @Override
    public void onEnable() {
        // don't allow craftbook to run
        Plugin craftBook = Bukkit.getPluginManager().getPlugin("CraftBook");
        if (craftBook != null) {
            Bukkit.getPluginManager().disablePlugin(craftBook);

            getComponentLogger().warn("Shot CraftBook down. Dinosaurs did go extinct, please remove it from your plugins folder!");
        }

        //language
        messageManager = new MessageManager(this);

        // dependencies
        dependencyManager = new DependencyManager(this);

        // config
        configManager = new ConfigManager(this);
        // note: since we don't join here and the async tasks will start later, this means we will register commands without any of the feature config loaded yet!
        // it's ok, they can wait.
        // the message manager however will get initializied before them!
        configManager.reload();

        // features
        featureRegistry = new FeatureRegistry(this);
        featureRegistry.registerStandardFeatures();

        // register Commands
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, this::onLifeCycleCommandEvent);
    }

    @Override
    public void onDisable() {
        featureRegistry.disableAll();
    }

    public @MonotonicNonNull MessageManager getMessageManager() {
        return messageManager;
    }

    public @MonotonicNonNull ConfigManager getConfigManager() {
        return configManager;
    }

    public @MonotonicNonNull FeatureRegistry getFeatureRegistry() {
        return featureRegistry;
    }

    public @MonotonicNonNull DependencyManager getDependencyManager() {
        return dependencyManager;
    }
}
