package de.greensurvivors.greenbook;

import de.greensurvivors.greenbook.commands.CoinCmd;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.config.MainConfig;
import de.greensurvivors.greenbook.listener.*;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public class GreenBook extends JavaPlugin {
	private static GreenBook instance;
	private CoinCmd coinCmd = null;
	private GreenBookCmd greenBookCmd = null;

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

		//register commands (must happen before config, because it depens on non null values.)
		PluginCommand coinCommand = getCommand(CoinCmd.getCommand());
		if (coinCommand != null) {
			this.coinCmd = new CoinCmd();

			coinCommand.setExecutor(this.coinCmd);
			coinCommand.setTabCompleter(this.coinCmd);
		} else {
			GreenLogger.log(Level.SEVERE, "Couldn't register command '" + CoinCmd.getCommand() + "'!");
		}

		PluginCommand mainCommand = getCommand(GreenBookCmd.getCommand());
		if (mainCommand != null) {
			this.greenBookCmd = new GreenBookCmd();

			mainCommand.setExecutor(this.greenBookCmd);
			mainCommand.setTabCompleter(this.greenBookCmd);
		} else {
			GreenLogger.log(Level.SEVERE, "Couldn't register command '" + GreenBookCmd.getCommand() + "'!");
		}

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

	public @Nullable CoinCmd getCoinCmd() {
		return coinCmd;
	}

	@Override
	public void onDisable() {
		PaintingListener.inst().clear();
	}
}
