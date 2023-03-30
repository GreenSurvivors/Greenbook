package de.greensurvivors.greenbook.commands;

import de.greensurvivors.greenbook.PermissionUtils;
import de.greensurvivors.greenbook.config.ShelfConfig;
import de.greensurvivors.greenbook.language.Lang;
import de.greensurvivors.greenbook.listener.ShelfListener;
import de.greensurvivors.greenbook.utils.Misc;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.BooleanUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ShelfCmd{
    public static final String SUBCOMMAND = "book";
    private static final String
            ADD = "add",
            REMOVE_SHORT = "rm", REMOVE_LONG = "remove",
            EMPTY_HAND = "emptyhand", HAND = "hand",
            SNEAK = "sneak",
            LIST = "list";
    private static final int BOOKS_PER_PAGE = 5;

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * <br>/greenbook book add [quote]
     * <br>/greenbook book remove [quote number]
     * <br>/greenbook book list [page number - optional]
     * <br>/greenbook book sneak [true/false]
     * <br>/greenbook book emptyhand [true/false]
     * <br>
     * @param sender  Source of the command
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    public static boolean handleCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length >= 3){
            switch (args[1].toLowerCase()){
                case ADD -> {
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_ADD)){
                        StringBuilder builder = new StringBuilder();

                        for (int i = 2; i < args.length; i++){
                            if (i != 2){
                                builder.append(" ");
                            }

                            builder.append(args[i]);
                        }

                        ShelfConfig.inst().addBook(builder.toString());
                        sender.sendMessage(Lang.build(Lang.SHELF_ADD_BOOK.get()).append(Component.newline()).append(Lang.build("&f" + builder)));
                    } else {
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
                case REMOVE_SHORT, REMOVE_LONG -> {
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_REMOVE)){
                        if (Misc.isInt(args[2])) {
                            String book = ShelfListener.inst().getBook(Integer.parseInt(args[2]));

                            if (book != null){
                                ShelfConfig.inst().removeBook(book);
                                sender.sendMessage(Lang.build(Lang.SHELF_REMOVED_BOOK.get()).append(Component.newline()).append(Lang.build("&f" + book)));
                            } else {
                                sender.sendMessage(Lang.build(Lang.SHELF_NO_BOOK.get().replace(Lang.VALUE, args[2])));
                            }
                        } else {
                            sender.sendMessage(Lang.build(Lang.NO_NUMBER.get().replace(Lang.VALUE, args[2])));
                        }
                    } else {
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
                case LIST -> {
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_LIST)){
                        List<String> books = ShelfListener.inst().getBooks();
                        final int NUM_OF_BOOKS = books.size();
                        final int NUM_OF_PAGES = (int) Math.ceil((double)NUM_OF_BOOKS / (double)BOOKS_PER_PAGE);

                        if (Misc.isInt(args[2])) {
                            final int PAGE = Math.max(Math.min(Integer.parseInt(args[2]), NUM_OF_PAGES), 1);
                            final int MAX_BOOKS_THIS_PAGE = Math.min(NUM_OF_BOOKS, PAGE * BOOKS_PER_PAGE); //don't try to access more books than exits

                            Component result = Lang.build(Lang.SHELF_LIST_HEADER.get().replace(Lang.VALUE, String.valueOf(PAGE)).replace(Lang.MAX, String.valueOf(NUM_OF_PAGES)));

                            for (int i = (PAGE -1) * BOOKS_PER_PAGE; i < MAX_BOOKS_THIS_PAGE; i++){
                                result = result.append(Component.newline()).
                                       append(Lang.build("&e " + i + " - ")).
                                       append(Lang.build("&f" + books.get(i)));
                            }

                            result = result.append(Component.newline()).append(Lang.build(Lang.SHELF_LIST_FOOTER.get())); //todo make buttons clickable

                            sender.sendMessage(result);
                        } else {
                            sender.sendMessage(Lang.build(Lang.NO_NUMBER.get().replace(Lang.VALUE, args[2])));
                        }
                    } else {
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
                case HAND, EMPTY_HAND -> {
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_EMPTYHAND)){
                        Boolean shouldQuoteRequireEmptyHand = BooleanUtils.toBooleanObject(args[2]);

                        if (shouldQuoteRequireEmptyHand != null) {
                            ShelfConfig.inst().setRequireEmptyHand(shouldQuoteRequireEmptyHand);
                            sender.sendMessage(Lang.build(Lang.SHELF_SET_EMPTYHAND.get().replace(Lang.VALUE, args[2])));
                        } else {
                            sender.sendMessage(Lang.build(Lang.NO_BOOL.get().replace(Lang.VALUE, args[2])));
                        }
                    } else {
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
                case SNEAK -> {
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_SNEAK)){
                        Boolean shouldRequireSneak = BooleanUtils.toBooleanObject(args[2]);

                        if (shouldRequireSneak != null) {
                            ShelfConfig.inst().setRequireSneak(shouldRequireSneak);
                            sender.sendMessage(Lang.build(Lang.SHELF_SET_SNEAK.get().replace(Lang.VALUE, args[2])));
                        } else {
                            sender.sendMessage(Lang.build(Lang.NO_BOOL.get().replace(Lang.VALUE, args[2])));
                        }
                    } else {
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
            }
        } else {
            if (args.length == 2 && args[1].equalsIgnoreCase(LIST)){ //no argumentdefaults to first page
                if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_LIST)){
                    List<String> books = ShelfListener.inst().getBooks();
                    final int NUM_OF_BOOKS = books.size();
                    final int NUM_OF_PAGES = (int) Math.ceil((double)NUM_OF_BOOKS / (double)BOOKS_PER_PAGE);
                    final int MAX_BOOKS_THIS_PAGE = Math.min(NUM_OF_BOOKS, BOOKS_PER_PAGE); //don't try to access more books than exits

                    Component result = Lang.build(Lang.SHELF_LIST_HEADER.get().replace(Lang.VALUE, String.valueOf(1)).replace(Lang.MAX, String.valueOf(NUM_OF_PAGES)));
                    for (int i = 0; i < MAX_BOOKS_THIS_PAGE; i++){
                        result = result.append(Component.newline()).
                               append(Lang.build("&e " + i + " - ")).
                               append(Lang.build("&f" + books.get(i)));
                    }
                    result = result.append(Component.newline()).append(Lang.build(Lang.SHELF_LIST_FOOTER.get())); //todo make buttons clickable

                    sender.sendMessage(result);

                } else {
                    sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                }
            } else {
                sender.sendMessage(Lang.build(Lang.NOT_ENOUGH_ARGS.get()));
                return false;
            }
        }

        return true;
    }

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender  Source of the command.  For players tab-completing a
     *                command inside of a command block, this will be the player, not
     *                the command block.
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed
     * @return A List of possible completions for the final argument, or null
     * to default to the command executor
     */
    public static @Nullable List<String> handleTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        switch (args.length){
            case 2 -> {
                ArrayList<String> result = new ArrayList<>();

                if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_ADD)){
                    result.add(ADD);
                }
                if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_REMOVE)){
                    result.add(REMOVE_SHORT);
                    result.add(REMOVE_LONG);
                }
                if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_LIST)){
                    result.add(LIST);
                }
                if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_SNEAK)){
                    result.add(SNEAK);
                }
                if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_EMPTYHAND)){
                    result.add(HAND);
                    result.add(EMPTY_HAND);
                }

                return result.stream().filter(s -> s.toLowerCase().startsWith(args[1])).toList();
            }
            case 3 -> {
                switch (args[1]){
                    case REMOVE_SHORT, REMOVE_LONG -> {
                        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_REMOVE)){
                            ArrayList<String> result = new ArrayList<>();

                            for (int i = 1; i <= ShelfListener.inst().getBooks().size(); i++){
                                result.add(String.valueOf(i));
                            }

                            return result.stream().filter(s -> s.toLowerCase().startsWith(args[1])).toList();
                        }
                    }
                    case LIST -> {
                        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_LIST)){
                            ArrayList<String> result = new ArrayList<>();

                            for (int i = 1; i <= ShelfListener.inst().getBooks().size() / BOOKS_PER_PAGE; i++){
                                result.add(String.valueOf(i));
                            }

                            return result.stream().filter(s -> s.toLowerCase().startsWith(args[1])).toList();
                        }
                    }
                    case SNEAK, HAND, EMPTY_HAND -> {
                        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_EMPTYHAND, PermissionUtils.GREENBOOK_SHELF_SNEAK)){
                            return Stream.of(String.valueOf(true), String.valueOf(false)).filter(s -> s.toLowerCase().startsWith(args[2])).toList();
                        }
                    }
                }
            }
        }
        return null;
    }
}
