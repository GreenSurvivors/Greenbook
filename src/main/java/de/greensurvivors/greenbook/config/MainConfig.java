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

public class MainConfig {
    private static MainConfig instance;


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
        File file = new File(GreenBook.inst().getDataFolder(), "language.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String k;
        // check Config
        for (Lang l : Lang.values()) {
            k = l.name().replaceAll("_", ".");
            // load value, if value not exist add
            if (cfg.contains(k))
                l.set(cfg.getString(k));
            else
                cfg.set(k, l.get());
        }
        // save modified configuration
        cfg.options().setHeader(Collections.singletonList(String.format(
                "Language configuration for %s (%s)",
                GreenBook.inst().getName(),
                GreenBook.inst().getDescription().getVersion())));
        cfg.options().parseComments(true);
        try {
            cfg.save(file);
        } catch (IOException e) {
            GreenLogger.log(Level.SEVERE, "Could not save language configuration.", e);
        }
    }

    public void reloadMain() {
        GreenBook.inst().reloadConfig();
        saveMain();
        loadMainSave();

        //coin item
        ItemStack coinItem = CoinConfig.inst().loadCoinItem();
        CoinConfig.inst().saveCoinItem(coinItem);

        CoinCmd coinCmd = GreenBook.inst().getCoinCmd();
        if (coinCmd != null) {
            coinCmd.setCoinItem(coinItem);
        }

        loadLanguage();
    }

    private void saveMain() {
        FileConfiguration cfg = GreenBook.inst().getConfig();
        ShelfConfig.inst().saveShelfConfig();

        cfg.options().setHeader(List.of(GreenBook.inst().getName() + " " + GreenBook.inst().getDescription().getVersion()));
        cfg.options().copyDefaults(true);
        cfg.options().parseComments(true);
        GreenBook.inst().saveConfig();
    }

    /**
     * Load all configs in another task
     */
    private void loadMainSave() {
        Bukkit.getScheduler().runTask(GreenBook.inst(), () -> {

            WireLessConfig.inst().load();
            PaintingConfig.inst().load();
            ShelfConfig.inst().loadShelfs();
        });
    }
}
