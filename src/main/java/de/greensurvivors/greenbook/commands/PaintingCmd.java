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

public class PaintingCmd {
    public static final String SUBCOMMAND = "painting";

    private static final String RANGE = "range";

    /**
     * /greenbook painting range [number]
     * @param sender
     * @param args
     * @return
     */
    public static boolean handleCommand(CommandSender sender, String[] args){
        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_PAINTING_RANGE)){
            if (args.length >= 3 && args[1].equalsIgnoreCase(RANGE)){
                if (Misc.isInt(args[2])){
                    int range = Math.max(0, Integer.parseInt(args[2])); //don't allow negativ ranges

                    PaintingListener.inst().setModifyRange(range);
                    PaintingConfig.inst().saveNewRange(range);

                    sender.sendMessage(Lang.build(Lang.PAINTING_SET_RANGE.get().replace(Lang.VALUE, String.valueOf(range))));
                } else {
                    sender.sendMessage(Lang.build(Lang.NO_NUMBER.get()));
                    return false;
                }
            } else {
                sender.sendMessage(Lang.build(Lang.NOT_ENOUGH_ARGS.get()));
                return false;
            }
        } else {
            sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
        }

        return true;
    }

    public static List<String> handleTap (CommandSender sender, String[] args){
        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_PAINTING_RANGE)){
            if (args.length == 2){
                return Stream.of(RANGE).filter(s -> s.toLowerCase().startsWith(args[1])).toList();
            }
        }

        return Collections.emptyList();
    }
}
