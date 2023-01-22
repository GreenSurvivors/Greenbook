package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.GreenLogger;
import de.greensurvivors.greenbook.language.Lang;
import de.greensurvivors.greenbook.listener.WirelessListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class MainConfig {
    private final String
            USE_PLAYER_SPECIFIC_CHANNELS = "usePlayerSpecificChannels",
            COMPATIBILITY_MODE = "compatibilityMode";

    private final boolean
            DEFAULT_USE_PLAYER_SPECIFIC_CHANNELS = true,
            DEFAULT_COMPATIBILITY_MODE = false;

    private final Pattern FILENAME_LIMITATIONS = Pattern.compile("[-\"*/:<>?|+,.;=\\[\\]\\\\ ]");
    private final String FILENAME_REPLACEMENT_STR = "%_";

    private static MainConfig instance;

    public static MainConfig inst() {
        if (instance == null) {
            instance = new MainConfig();
        }
        return instance;
    }

    public @Nullable HashSet<Location> loadReceiverLocations(@NotNull Component channelComponent, @Nullable String playerUUIDStr) {
        //don't allow forbidden chars in filenames
        String channelStr = LegacyComponentSerializer.legacyAmpersand().serialize(channelComponent);
        if (channelStr.equals("")) {
            channelStr = FILENAME_REPLACEMENT_STR;
        } else {
            channelStr = FILENAME_LIMITATIONS.matcher(channelStr).replaceAll(FILENAME_REPLACEMENT_STR);
        }

        ChannelConfig config = new ChannelConfig(channelStr, playerUUIDStr);

        return config.getSet();
    }

    public void saveReceiverLocations(@NotNull Component channelComponent, @NotNull HashSet<Location> locations, @Nullable String playerUUID) {
        //don't allow forbidden chars in filenames
        String channelStr = LegacyComponentSerializer.legacyAmpersand().serialize(channelComponent);
        if (channelStr.equals("")) {
            channelStr = FILENAME_REPLACEMENT_STR;
        } else {
            channelStr = FILENAME_LIMITATIONS.matcher(channelStr).replaceAll(FILENAME_REPLACEMENT_STR);
        }

        ChannelConfig config = new ChannelConfig(channelStr, playerUUID);

        config.saveCfg(locations);
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

        loadLanguage();
    }

    private void saveMain() {
        FileConfiguration cfg = GreenBook.inst().getConfig();
        cfg.addDefault(USE_PLAYER_SPECIFIC_CHANNELS, DEFAULT_USE_PLAYER_SPECIFIC_CHANNELS);
        cfg.addDefault(COMPATIBILITY_MODE, DEFAULT_COMPATIBILITY_MODE);

        cfg.options().setHeader(List.of(GreenBook.inst().getName() + " " + GreenBook.inst().getDescription().getVersion()));
        cfg.options().copyDefaults(true);
        cfg.options().parseComments(true);
        GreenBook.inst().saveConfig();
    }

    private void loadMain() {
        FileConfiguration cfg = GreenBook.inst().getConfig();

        WirelessListener.inst().setUsePlayerSpecificChannels(cfg.getBoolean(USE_PLAYER_SPECIFIC_CHANNELS, DEFAULT_USE_PLAYER_SPECIFIC_CHANNELS));
        WirelessListener.inst().setCompatibilityMode(cfg.getBoolean(COMPATIBILITY_MODE, DEFAULT_COMPATIBILITY_MODE));
    }

    /**
     * Load region restock and clears cached ones.
     */
    public void loadMainSave() {
        Bukkit.getScheduler().runTask(GreenBook.inst(), () -> {
            // clear cache
            WirelessListener.inst().clear();
            Bukkit.getScheduler().runTaskAsynchronously(GreenBook.inst(), this::loadMain);
        });
    }
}
