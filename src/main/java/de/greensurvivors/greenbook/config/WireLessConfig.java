package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.listener.WirelessListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.regex.Pattern;

public class WireLessConfig {
    private static final String
            WIRELESS_NODE = "wireless",
            //these settings are only accessible via config file
            USE_PLAYER_SPECIFIC_CHANNELS = WIRELESS_NODE + ".usePlayerSpecificChannels",
            COMPATIBILITY_MODE = WIRELESS_NODE + ".compatibilityMode";

    private final Pattern FILENAME_LIMITATIONS = Pattern.compile("[-\"*/:<>?|+,.;=\\[\\]\\\\ ]");
    private final String FILENAME_REPLACEMENT_STR = "%_";

    private static final boolean
            DEFAULT_USE_PLAYER_SPECIFIC_CHANNELS = true,
            DEFAULT_COMPATIBILITY_MODE = false;


    private static WireLessConfig instance;

    public static WireLessConfig inst() {
        if (instance == null) {
            instance = new WireLessConfig();
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

    protected void load(FileConfiguration cfg) {
        // clear cache
        WirelessListener.inst().clear();

        WirelessListener.inst().setCompatibilityMode(cfg.getBoolean(COMPATIBILITY_MODE, DEFAULT_COMPATIBILITY_MODE));
        WirelessListener.inst().setUsePlayerSpecificChannels(cfg.getBoolean(USE_PLAYER_SPECIFIC_CHANNELS, DEFAULT_USE_PLAYER_SPECIFIC_CHANNELS));
    }

    protected void save(FileConfiguration cfg) {
        cfg.addDefault(USE_PLAYER_SPECIFIC_CHANNELS, DEFAULT_USE_PLAYER_SPECIFIC_CHANNELS);
        cfg.addDefault(COMPATIBILITY_MODE, DEFAULT_COMPATIBILITY_MODE);

    }
}
