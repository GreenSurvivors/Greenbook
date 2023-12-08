package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.GreenLogger;
import de.greensurvivors.greenbook.commands.CoinCmd;
import de.greensurvivors.greenbook.language.Lang;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * mother class of all configuration of this plugin
 */
public class MainConfig {
    //this class keeps track of its own instance, so it's basically static
    private static MainConfig instance;

    private MainConfig() {
    }

    /**
     * static to instance translator
     */
    public static MainConfig inst() {
        if (instance == null) {
            instance = new MainConfig();
        }
        return instance;
    }

    /**
     * Load language configuration.
     */
    private void loadLanguage() {
        // translations are located in its own file
        File file = new File(GreenBook.inst().getDataFolder(), "language.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String key;
        // check Config
        for (Lang langValue : Lang.values()) {
            //from name to key
            key = langValue.name().replaceAll("_", ".");
            // load value, if value not exist add default to config
            if (cfg.contains(key))
                langValue.set(cfg.getString(key));
            else
                cfg.set(key, langValue.get());
        }

        // set header and keep commands
        cfg.options().setHeader(Collections.singletonList(String.format(
                "Language configuration for %s (%s)",
                GreenBook.inst().getName(),
                GreenBook.inst().getDescription().getVersion())));
        cfg.options().parseComments(true);

        // save modified configuration
        try {
            cfg.save(file);
        } catch (IOException e) {
            GreenLogger.log(Level.SEVERE, "Could not save language configuration.", e);
        }
    }

    /**
     * reload all config files
     */
    public void reloadMain() {
        //reload file
        GreenBook.inst().reloadConfig();

        //load config files, first time, when no files exist we will set default values
        loadMainSave();
        //save config files, will write default values
        saveMain();
    }

    /**
     * save all config files that not got saved immediately after setting them
     * Note, that wireless channels are managed mid-game
     */
    private void saveMain() {
        FileConfiguration cfg = GreenBook.inst().getConfig();
        ShelfConfig.inst().saveShelfConfig();

        //set header, defaults and keep commands
        cfg.options().setHeader(List.of(GreenBook.inst().getName() + " " + GreenBook.inst().getDescription().getVersion()));
        cfg.options().copyDefaults(true);
        cfg.options().parseComments(true);

        //save file
        GreenBook.inst().saveConfig();
    }

    /**
     * Load all configs in another sync task
     * Note, that wireless channels are managed mid-game
     */
    private void loadMainSave() {
        Bukkit.getScheduler().runTask(GreenBook.inst(), () -> {

            WireLessConfig.inst().load();
            PaintingConfig.inst().load();
            ShelfConfig.inst().loadShelfConfig();

            //coin item
            ItemStack coinItem = CoinConfig.inst().loadCoinItem();
            CoinConfig.inst().saveCoinItem(coinItem);

            CoinCmd coinCmd = GreenBook.inst().getCoinCmd();
            if (coinCmd != null) {
                coinCmd.setCoinItem(coinItem);
            }

            //reload language
            loadLanguage();
        });
    }
}
