package de.greensurvivors.greenbook.features;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.config.AFeatureConfig;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Every feature should make use of {@link IPermissionHolder} to manage its permissions.
 *
 * @param <C>
 */
public abstract class AFeature<C extends AFeatureConfig> {
    protected final @NotNull C featureConfig;
    protected final @NotNull GreenBook plugin;
    private final @NotNull FeatureType featureType;

    public AFeature(@NotNull GreenBook plugin, @NotNull FeatureType featureType, @NotNull C featureConfig) {
        this.plugin = plugin;
        this.featureType = featureType;
        this.featureConfig = featureConfig;
    }

    /**
     * Returns the type of the feature.
     *
     * @return the feature type (not null)
     */
    @Contract(pure = true)
    public @NotNull FeatureType getFeatureType() {
        return featureType;
    }

    /**
     * Returns the FeatureConfig of this feature.
     */
    public @NotNull C getFeatureConfig() {
        return featureConfig;
    }

    /**
     * Registers the commands for this feature.
     * A command may be registered as stand-alone via the commands registrar, or as a subcommand of the main command.
     *
     * @param commandsRegistrar the commands registrar to register stand-alone commands with
     * @param mainCommand       the main command to register the subcommands with
     */
    @SuppressWarnings("UnstableApiUsage") // brigadier api
    public abstract void registerCommands(final @NotNull Commands commandsRegistrar, @NotNull GreenBookCmd mainCommand);

    /**
     * A method called when the feature is disabled.
     * Should call {@link AFeatureConfig#setEnabled(boolean)} with false.
     * If the feature registers EventHandlers they should be disabled via {@link org.bukkit.event.HandlerList#unregisterAll(Listener)}
     */
    public abstract void onDisable();

    /**
     * A method called when the feature is enabled.
     * Should call {@link AFeatureConfig#setEnabled(boolean)} with true.
     * If the feature registers EventHandlers they should be enabled via {@link org.bukkit.plugin.PluginManager#registerEvents(Listener, Plugin)}
     */
    public abstract void onEnable();

    /**
     * An interface for Features, to house permissions.
     * When implementing this don't forget to call Bukkit.getPluginManager().addPermission(permission)
     */
    public interface IPermissionHolder {
        @NotNull Permission getPermission();
    }
}
