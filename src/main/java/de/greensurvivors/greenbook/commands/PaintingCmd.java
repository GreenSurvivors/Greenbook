package de.greensurvivors.greenbook.commands;

import de.greensurvivors.greenbook.PermissionUtils;
import de.greensurvivors.greenbook.config.PaintingConfig;
import de.greensurvivors.greenbook.language.Lang;
import de.greensurvivors.greenbook.listener.PaintingListener;
import de.greensurvivors.greenbook.utils.Misc;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * subcommand to set the mod range of the painting switcher
 */
public class PaintingCmd {
    public static final String SUBCOMMAND = "painting";

    private static final String RANGE = "range";

    /**
     * execute painting subcommand
     * <br>/greenbook painting range [number] - set the range a painting can still be modified by a player
     * @param sender  Source of the command
     * @param args    Passed command arguments
     * @return        true, if enough args where given.
     */
    protected static boolean handleCommand(CommandSender sender, String[] args){
        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_PAINTING_RANGE)){
            if (args.length >= 3 && args[1].equalsIgnoreCase(RANGE)){
                if (Misc.isInt(args[2])){
                    int range = Math.max(0, Integer.parseInt(args[2])); //don't allow negativ ranges

                    //set the working value
                    PaintingListener.inst().setModifyRange(range);
                    //save to file
                    PaintingConfig.inst().saveNewRange(range);

                    sender.sendMessage(Lang.build(Lang.PAINTING_SET_RANGE.get().replace(Lang.VALUE, String.valueOf(range))));
                } else { //the given range was not an integer
                    sender.sendMessage(Lang.build(Lang.NO_NUMBER.get()));
                    return false;
                }
            } else { //not enough arguments
                sender.sendMessage(Lang.build(Lang.NOT_ENOUGH_ARGS.get()));
                return false;
            }
        } else { //no permission
            sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
        }

        return true;
    }

    /**
     * Requests a list of possible completions for a command argument.
     * @param sender  Source of the command
     * @param args    Passed command arguments
     * @return        "range" if second argument, else empty list
     */
    protected static List<String> handleTab(CommandSender sender, String[] args){
        //check permission
        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_PAINTING_RANGE)){
            if (args.length == 2){
                //return the subcommands name
                return Stream.of(RANGE).filter(s -> s.toLowerCase().startsWith(args[1])).toList();
            }
        }

        return Collections.emptyList();
    }
}
