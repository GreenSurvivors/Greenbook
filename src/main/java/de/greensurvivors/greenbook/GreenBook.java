package de.greensurvivors.greenbook;

import de.greensurvivors.greenbook.config.MainConfig;
import de.greensurvivors.greenbook.listener.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GreenBook extends JavaPlugin {
	private static GreenBook instance;

	public static GreenBook inst() {
		return instance;
	}


	// aks world guard if using is allowed if clicking on sign is doing something
	@Override
	public void onEnable() {
		// set instance
		instance = this;
		// set logger
		GreenLogger.setLogger(getLogger());

		// configuration
		MainConfig.inst().reloadMain();

		// listener
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(WirelessListener.inst(), this);
		pm.registerEvents(LiftListener.inst(), this);
		pm.registerEvents(ShelfListener.inst(), this);
		pm.registerEvents(PaintingListener.inst(), this);
		pm.registerEvents(BridgeListener.inst(), this);
		pm.registerEvents(GateListener.inst(), this);
	}

	@Override
	public void onDisable() {
	}
}
