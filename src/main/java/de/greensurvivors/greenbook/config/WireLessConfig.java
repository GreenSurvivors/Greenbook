package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.listener.WirelessListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.regex.Pattern;

//todo make it easier to switch between player specific channels and not
public class WireLessConfig {
    private static final String
            //config key root, never use this directly!
            WIRELESS_KEY = "wireless.",
            //if every player should have thair own channel or all player activate all other recivers
            //this means, a transmitter placed by a player only activates receivers placed by the same player.
            USE_PLAYER_SPECIFIC_CHANNELS = WIRELESS_KEY + "usePlayerSpecificChannels",
            // config key if we try to load old craftbook signs or if the channel files ever get lost
            COMPATIBILITY_MODE = WIRELESS_KEY + "compatibilityMode";

    //this pattern contains all chars that are not allowed in a filename
    private final Pattern FILENAME_LIMITATIONS = Pattern.compile("[-\"*/:<>?|+,.;=\\[\\]\\\\ ]");
    //replacement for limited chars, hopefully it's unique enough, and limited chars are seldom enough
    private final String FILENAME_REPLACEMENT_STR = "%_";

    //default values, in case no or faulty values are in config
    private static final boolean
            DEFAULT_USE_PLAYER_SPECIFIC_CHANNELS = true,
            DEFAULT_COMPATIBILITY_MODE = false;

    //mutex to save channels async
    private final Object MUTEX = new Object();

    //this class keeps track of its own instance, so it's basically static
    private static WireLessConfig instance;

    /**
     * load config the fist time and initialise with default values
     */
    private WireLessConfig() {
        GreenBook.inst().getConfig().addDefault(USE_PLAYER_SPECIFIC_CHANNELS, DEFAULT_USE_PLAYER_SPECIFIC_CHANNELS);
        GreenBook.inst().getConfig().addDefault(COMPATIBILITY_MODE, DEFAULT_COMPATIBILITY_MODE);
    }

    /**
     * static to instance translator
     */
    public static WireLessConfig inst() {
        if (instance == null) {
            instance = new WireLessConfig();
        }

        return instance;
    }

    /**
     *
     * @param channelStr channel, stripped of all color / formatting
     * <br> a channel connects a transmitter with a receiver with the same channel
     * @param playerUUIDStr UUID of the player owning this channel. If null global owns it.
     * @return the set of saved receiver locations or null if no receiver for this channel uuid combination was ever saved
     */
    public @Nullable HashSet<Location> loadReceiverLocations(@NotNull String channelStr, @Nullable String playerUUIDStr) {
        //don't allow forbidden chars or empty filenames
        if (channelStr.equals("")) {
            channelStr = FILENAME_REPLACEMENT_STR;
        } else {
            channelStr = FILENAME_LIMITATIONS.matcher(channelStr).replaceAll(FILENAME_REPLACEMENT_STR);
        }

        ChannelConfig config = new ChannelConfig(channelStr, playerUUIDStr);

        HashSet<Location> result;
        synchronized (MUTEX){
            result = config.getSet();
        }

        return result;

    }

    /**
     * save the used receiver locations to file
     * @param channelStr
     * @param locations
     * @param playerUUID
     */
    public void saveReceiverLocations(@NotNull String channelStr, @NotNull HashSet<Location> locations, @Nullable String playerUUID) {
        //don't allow forbidden chars or empty filenames
        if (channelStr.equals("")) {
            channelStr = FILENAME_REPLACEMENT_STR;
        } else {
            channelStr = FILENAME_LIMITATIONS.matcher(channelStr).replaceAll(FILENAME_REPLACEMENT_STR);
        }

        ChannelConfig config = new ChannelConfig(channelStr, playerUUID);

        synchronized (MUTEX){
            Bukkit.getScheduler().runTaskAsynchronously(GreenBook.inst(), () -> config.saveCfg(locations));
        }
    }

    public void setUsePlayerSpecificChannels(boolean newValue){
        GreenBook.inst().getConfig().set(USE_PLAYER_SPECIFIC_CHANNELS, newValue);
        WirelessListener.inst().setUsePlayerSpecificChannels(newValue);
    }

    public void setCompatiblityMode(boolean newValue){
        GreenBook.inst().getConfig().set(COMPATIBILITY_MODE, newValue);
        WirelessListener.inst().setCompatibilityMode(newValue);
    }

    protected void load() {
        // clear cache
        WirelessListener.inst().clear();

        WirelessListener.inst().setCompatibilityMode(GreenBook.inst().getConfig().getBoolean(COMPATIBILITY_MODE, DEFAULT_COMPATIBILITY_MODE));
        WirelessListener.inst().setUsePlayerSpecificChannels(GreenBook.inst().getConfig().getBoolean(USE_PLAYER_SPECIFIC_CHANNELS, DEFAULT_USE_PLAYER_SPECIFIC_CHANNELS));
    }
}
