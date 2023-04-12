package de.greensurvivors.greenbook.commands;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.GreenLogger;
import de.greensurvivors.greenbook.config.WireLessConfig;
import de.greensurvivors.greenbook.language.Lang;
import de.greensurvivors.greenbook.listener.WirelessListener;
import de.greensurvivors.greenbook.utils.PermissionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class WireLessCmd {
    public static final String SUBCOMMAND = "wire";

    private static final String
            UPDATE_SIGNS = "updatesigns",
            PLAYER_SPECIFIC_CHANNELS_LONG = "playerspecificchannels",PLAYER_SPECIFIC_CHANNELS_SHORT = "psc",
            COMPATIBILITY_MODE_LONG = "compatibilitymode", COMPATIBILITY_MODE_SHORT = "compa";

    /**
     * /greenbook wire updateSigns - updates signs legacy signs around the player
     * /greenbook wire playerspecificchannels [true / false] - set if player specific channels should be used.
     * @param sender source of command, for permission check
     * @param args   given arguments (ignored)
     * @return       true, you can't mess up
     */
    protected static boolean handleCommand(@NotNull CommandSender sender, @Nullable String[] args){
        if (args.length > 1){
            switch (args[1].toLowerCase()){
                case UPDATE_SIGNS -> {
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_WIRELESS_UPDATE_SIGNS_CMD)){
                        if (sender instanceof Player player){
                            final World WORLD = player.getWorld();
                            final int CHUNK_SIM_DIST = WORLD.getSimulationDistance();
                            final int CHUNK_START_X = player.getChunk().getX();
                            final int CHUNK_START_Z = player.getChunk().getZ();

                            Bukkit.getScheduler().runTaskAsynchronously(GreenBook.inst(), () -> {
                                //note: All chunks should be loaded, getChunkAtAsync should report back immediately, since we are operating around a player.
                                // However, since we are working async there is no guarantee.
                                for (int dist = 0; dist <= CHUNK_SIM_DIST; dist++){
                                    try {
                                        //north
                                        for (int dx = CHUNK_START_X - dist; dx <= CHUNK_START_X + dist; dx++){
                                            WirelessListener.inst().parseThroughChunk(WORLD.getChunkAtAsync(dx, CHUNK_START_Z + dist).get());
                                        }

                                        //east, it's one chunk smaller in north and south direction since we already checked the chunks there
                                        for (int dz = CHUNK_START_Z - dist +1; dz <= CHUNK_START_Z + dist -1; dz++){
                                            WirelessListener.inst().parseThroughChunk(WORLD.getChunkAtAsync(CHUNK_START_X + dist, dz).get());
                                        }

                                        //south
                                        for (int dx = CHUNK_START_X + dist; dx >= CHUNK_START_X - dist; dx--){
                                            WirelessListener.inst().parseThroughChunk(WORLD.getChunkAtAsync(dx, CHUNK_START_Z - dist).get());
                                        }

                                        //west, it's one chunk smaller in north and south direction since we already checked the chunks there
                                        for (int dz = CHUNK_START_Z + dist -1; dz >= CHUNK_START_Z - dist +1; dz--){
                                            WirelessListener.inst().parseThroughChunk(WORLD.getChunkAtAsync(CHUNK_START_X - dist, dz).get());
                                        }
                                    } catch (CancellationException | InterruptedException exception){
                                        GreenLogger.log(Level.WARNING, "Couldn't update signs, ether interrupted or canceled.", exception);
                                    } catch (ExecutionException exception){
                                        GreenLogger.log(Level.SEVERE, "Couldn't update signs, error:", exception);
                                        GreenLogger.log(Level.SEVERE, "Cause: ", exception.getCause());
                                    }
                                }
                            });
                        } else {
                            sender.sendMessage(Lang.build(Lang.NOT_PLAYER_SELF.get()));
                        }
                    } else {
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
                case PLAYER_SPECIFIC_CHANNELS_LONG, PLAYER_SPECIFIC_CHANNELS_SHORT -> {
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_WIRELESS_SET_PLAYER_SPECIFIC_CHANNELS)){
                        if (args.length > 2){
                            Boolean newValue = BooleanUtils.toBooleanObject(args[2]);

                            if (newValue != null){
                                WireLessConfig.inst().setUsePlayerSpecificChannels(newValue);
                            } else {
                                sender.sendMessage(Lang.build(Lang.NO_BOOL.get().replace(Lang.VALUE, args[2])));
                            }
                        } else {
                            sender.sendMessage(Lang.build(Lang.NOT_ENOUGH_ARGS.get()));
                        }
                    } else {
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
                case COMPATIBILITY_MODE_LONG, COMPATIBILITY_MODE_SHORT-> {
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_WIRELESS_SET_COMPATIBILITY_MODE)){
                        if (args.length > 2){
                            Boolean newValue = BooleanUtils.toBooleanObject(args[2]);

                            if (newValue != null){
                                WireLessConfig.inst().setCompatiblityMode(newValue);
                            } else {
                                sender.sendMessage(Lang.build(Lang.NO_BOOL.get().replace(Lang.VALUE, args[2])));
                            }
                        } else {
                            sender.sendMessage(Lang.build(Lang.NOT_ENOUGH_ARGS.get()));
                        }
                    } else {
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
                default -> {
                    sender.sendMessage(Lang.build(Lang.UNKNOWN_ARGUMENT.get()));
                    return false;
                }
            }
        } else {
            sender.sendMessage(Lang.build(Lang.NOT_ENOUGH_ARGS.get()));
            return false;
        }

        return true;
    }

    /**
     * Requests a list of possible completions for a command argument.
     * /greenbook wireless(0) updatesigns
     * @param sender  Source of the command
     * @param args    Passed command arguments
     * @return        "range" if second argument, else empty list
     */
    protected static List<String> handleTab(CommandSender sender, String[] args){
        switch (args.length){
            case 2 -> {
                ArrayList<String> result = new ArrayList<>();
                if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_WIRELESS_UPDATE_SIGNS_CMD)){
                    result.add(UPDATE_SIGNS);
                }
                if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_WIRELESS_SET_PLAYER_SPECIFIC_CHANNELS)){
                    result.add(PLAYER_SPECIFIC_CHANNELS_LONG);
                    result.add(PLAYER_SPECIFIC_CHANNELS_SHORT);
                }
                if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_WIRELESS_SET_COMPATIBILITY_MODE)){
                    result.add(COMPATIBILITY_MODE_LONG);
                    result.add(COMPATIBILITY_MODE_SHORT);
                }

                return result.stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
            }
            case 3 -> {
                ArrayList<String> result = new ArrayList<>();

                if (args[1].equalsIgnoreCase(PLAYER_SPECIFIC_CHANNELS_LONG) || args[1].equalsIgnoreCase(PLAYER_SPECIFIC_CHANNELS_SHORT) &&
                        PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_WIRELESS_SET_PLAYER_SPECIFIC_CHANNELS)){
                    result.add(String.valueOf(true));
                    result.add(String.valueOf(false));
                } else if (args[1].equalsIgnoreCase(COMPATIBILITY_MODE_LONG) || args[1].equalsIgnoreCase(COMPATIBILITY_MODE_SHORT) &&
                        PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_WIRELESS_SET_COMPATIBILITY_MODE)){
                    result.add(String.valueOf(true));
                    result.add(String.valueOf(false));
                }

                return result.stream().filter(s -> s.startsWith(args[2].toLowerCase())).toList();
            }
        }

        return List.of();
    }


}
