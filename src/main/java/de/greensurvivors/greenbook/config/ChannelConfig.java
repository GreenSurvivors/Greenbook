package de.greensurvivors.greenbook.config;

import com.google.gson.*;
import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.GreenLogger;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * this class keeps track of all wireless receivers per channel.
 * Because of access time, we are using json with our own decoder
 */
public class ChannelConfig {
    //pattern to extract a string from json
    private static final Pattern quoteMarkPattern = Pattern.compile("\"(.*?)\"");
    //using json (implemented by google)
    private static final Gson gson = new Gson();
    //todo
    private HashSet<Location> locations = null;
    //path to the file of this channel
    private final String path;

    //mother folder of all channels
    public static final String FOLDER = "receiverFiles";

    /**
     * new config for the channel, uuid combination
     * @param channel wireless channel
     * @param playerUUID uuid of the player owning the channel. If no uuid was given (null), global is assumed
     */
    public ChannelConfig(@NotNull String channel, @Nullable String playerUUID) {
        if (playerUUID == null) {
            this.path = FOLDER + File.separator + channel + ".json";
        } else {
            this.path = FOLDER + File.separator + playerUUID + File.separator + channel + ".json";
        }
    }


    /**
     * Save given locations of to file.
     * Please note: You can't load a channel without saving it first
     */
    public void saveCfg(@NotNull HashSet<Location> locations) {
        File file = new File(GreenBook.inst().getDataFolder(), path);

        // try to create a file
        if (!file.isFile()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                //didn't work
                GreenLogger.log(Level.SEVERE, "couldn't create new File: " + file.getPath());
                GreenLogger.log(Level.SEVERE, e.getMessage());
                //throw new RuntimeException(e);
            }
        }

        //array holding all the json objects that end up saved to file
        JsonArray jsonArray = new JsonArray();

        //loop through all given locations and serialize them to json objects
        for (Location location : locations) {
            //json object holding a location
            JsonObject jsonLocation = new JsonObject();

            //serialize the important information
            jsonLocation.addProperty("world", gson.toJson(location.getWorld().getName()));
            jsonLocation.addProperty("x", gson.toJson(location.getBlockX()));
            jsonLocation.addProperty("y", gson.toJson(location.getBlockY()));
            jsonLocation.addProperty("z", gson.toJson(location.getBlockZ()));

            //add serialize location
            jsonArray.add(jsonLocation);
        }

        try {
            //json to string and safe to utf8 file
            FileUtils.writeStringToFile(file, gson.toJson(jsonArray), StandardCharsets.UTF_8);
        } catch (IOException e) {
            //error
            GreenLogger.log(Level.SEVERE, "couldn't write file " + file.getPath());
            GreenLogger.log(Level.SEVERE, "Data will be lost!");
            GreenLogger.log(Level.SEVERE, e.getMessage());
            //throw new RuntimeException(e);
        }
    }

    /**
     * todo
     * @return
     */
    public @Nullable HashSet<Location> getSet() {
        if (locations == null) {
            File file = new File(GreenBook.inst().getDataFolder(), path);

            if (!file.isFile()) {
                return null;
            }

            try {
                JsonElement setElement = JsonParser.parseReader(new FileReader(file));

                if (setElement.isJsonArray()) {
                    locations = new HashSet<>();

                    for (JsonElement locationElement : setElement.getAsJsonArray()) {
                        if (locationElement.isJsonObject()) {
                            JsonElement tempElement = locationElement.getAsJsonObject().get("world");

                            if (tempElement != null && tempElement.isJsonPrimitive()) {
                                Matcher matcher = quoteMarkPattern.matcher(tempElement.getAsString());
                                if (matcher.matches()) {
                                    String worldName = matcher.group(1);
                                    tempElement = locationElement.getAsJsonObject().get("x");

                                    if (tempElement != null && tempElement.isJsonPrimitive()) {
                                        int x = tempElement.getAsInt();

                                        tempElement = locationElement.getAsJsonObject().get("y");
                                        if (tempElement != null && tempElement.isJsonPrimitive()) {
                                            int y = tempElement.getAsInt();

                                            tempElement = locationElement.getAsJsonObject().get("z");
                                            if (tempElement != null && tempElement.isJsonPrimitive()) {
                                                int z = tempElement.getAsInt();

                                                World world = Bukkit.getWorld(worldName);

                                                if (world != null) {
                                                    locations.add(new Location(world, x, y, z));
                                                } else {
                                                    GreenLogger.log(Level.WARNING, "world unknown: '" + worldName + "'");
                                                }
                                            } else {
                                                GreenLogger.log(Level.WARNING, "z not primitive");
                                            }
                                        } else {
                                            GreenLogger.log(Level.WARNING, "y not primitive");
                                        }
                                    } else {
                                        GreenLogger.log(Level.WARNING, "x not primitive");
                                    }
                                }
                            } else {
                                GreenLogger.log(Level.WARNING, "world not primitive");
                            }
                        } else {
                            GreenLogger.log(Level.WARNING, "Location not Object");
                        }
                    }

                    return locations;
                } else {
                    GreenLogger.log(Level.WARNING, "not array");
                }

            } catch (FileNotFoundException e) {
                GreenLogger.log(Level.WARNING, "couldn't load file " + file.getPath(), e);
            }
        }
        return null;
    }
}
