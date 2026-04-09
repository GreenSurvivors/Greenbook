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
import org.jetbrains.annotations.NotNull;

public final class GreenBook extends JavaPlugin {
    private MessageManager messageManager;
    private ConfigManager configManager;
    private FeatureRegistry featureRegistry;
    private DependencyManager dependencyManager;

    private void onLifeCycleCommandEvent(final @NotNull ReloadableRegistrarEvent<Commands> event) {
        GreenBookCmd mainCommand = new GreenBookCmd(this);

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

            getComponentLogger().warn("Shoot Craftbook down. Dinosaurs did go extinct, please remove it from your plugins folder!");
        }

        //language
        messageManager = new MessageManager(this);

        // dependencies
        dependencyManager = new DependencyManager(this);

        // features
        featureRegistry = new FeatureRegistry(this);
        //redstoneFeature = new WirelessRedstoneFeature(this);

        // config
        configManager = new ConfigManager(this);
        configManager.reload();

        // register Commands
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, this::onLifeCycleCommandEvent);
    }

    @Override
    public void onDisable() {
        featureRegistry.disableAll();
    }


    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public FeatureRegistry getFeatureRegistry() {
        return featureRegistry;
    }

    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }
}
