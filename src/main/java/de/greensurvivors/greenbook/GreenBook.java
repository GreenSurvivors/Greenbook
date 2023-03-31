package de.greensurvivors.greenbook;

import de.greensurvivors.greenbook.commands.CoinCmd;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.config.MainConfig;
import de.greensurvivors.greenbook.listener.LiftListener;
import de.greensurvivors.greenbook.listener.PaintingListener;
import de.greensurvivors.greenbook.listener.ShelfListener;
import de.greensurvivors.greenbook.listener.WirelessListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public class GreenBook extends JavaPlugin {
	private static GreenBook instance;
	private CoinCmd coinCmd = null;

	public static GreenBook inst() {
		return instance;
	}

	@Override
	public void onEnable() {
		// set instance
		instance = this;
		// set logger
		GreenLogger.setLogger(getLogger());

		//register commands (must happen before config, because config depend on non-null values.)
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
			GreenBookCmd greenBookCmd = new GreenBookCmd();

			mainCommand.setExecutor(greenBookCmd);
			mainCommand.setTabCompleter(greenBookCmd);
		} else {
			GreenLogger.log(Level.SEVERE, "Couldn't register command '" + GreenBookCmd.getCommand() + "'!");
		}

		// configuration
		MainConfig.inst().reloadMain();

		// listener
		//todo (dis)able Listeners, depending on enabled config value
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(WirelessListener.inst(), this);
		pm.registerEvents(LiftListener.inst(), this);
		pm.registerEvents(ShelfListener.inst(), this);
		pm.registerEvents(PaintingListener.inst(), this);
	}

	/**
	 * @return handler of the /coin command
	 */
	public @Nullable CoinCmd getCoinCmd() {
		return coinCmd;
	}

	@Override
	public void onDisable() {
		PaintingListener.inst().clear();
		WirelessListener.inst().clear();
	}
}
