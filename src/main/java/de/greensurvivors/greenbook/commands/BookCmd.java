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

/**
 * config commands for the readable bookshelf's
 */
public class BookCmd {
    public static final String SUBCOMMAND = "book";
    private static final String
            ADD = "add",
            REMOVE_SHORT = "rm", REMOVE_LONG = "remove",
            EMPTY_HAND_SHORT = "hand", EMPTY_HAND_LONG = "emptyhand",
            SNEAK = "sneak",
            LIST = "list";
    private static final int BOOKS_PER_PAGE = 5;

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * <br>/greenbook book add [quote] - add a new quote (aka book)
     * <br>/greenbook book remove [quote number] - remove a quote (aka book) by its id (important: may change if books are added or removed)
     * <br>/greenbook book list [page number - optional] - list all known quotes (books) neatly arranged in pages.
     * <br>/greenbook book emptyhand [true/false] - set if an empty hand is required for reading books
     * <br>/greenbook book sneak [true/false] - set if sneaking is required for reading books
     *
     * @param sender  Source of the command
     * @param args    Passed command arguments
     * @return        false if not enough arguments, otherwise true
     */
    public static boolean handleCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length >= 3){
            switch (args[1].toLowerCase()){
                case ADD -> {//greenbook book add [quote] - add a new quote (aka book)
                    //check permission
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_ADD)){
                        //our book was broken into an array of strings, we have to glue it back together
                        StringBuilder builder = new StringBuilder();

                        for (int i = 2; i < args.length; i++){
                            //add whitespace between arguments, but not a leading one
                            if (i != 2){
                                builder.append(" ");
                            }

                            builder.append(args[i]);
                        }

                        //save the added book to config and add it to the current active list
                        ShelfConfig.inst().addBook(builder.toString());
                        //join feedback with new line as separator and send
                        sender.sendMessage(Lang.join(List.of(Lang.build(Lang.SHELF_ADD_BOOK.get()), Lang.build(builder.toString()))));
                    } else { //no permission
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
                case REMOVE_SHORT, REMOVE_LONG -> { //greenbook book remove [quote number] - remove a quote (aka book) by its id (important: may change if books are added or removed)
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_REMOVE)){
                        //check if the given id was valid
                        if (Misc.isInt(args[2])) {
                            String book = ShelfListener.inst().getBook(Integer.parseInt(args[2]));
                            if (book != null){
                                //it is a book. now remove it from config and current active list
                                ShelfConfig.inst().removeBook(book);
                                //join feedback with new line as separator and send
                                sender.sendMessage(Lang.join(List.of(Lang.build(Lang.SHELF_REMOVED_BOOK.get()), Lang.build(book))));
                            } else {
                                //no book with this id
                                sender.sendMessage(Lang.build(Lang.SHELF_NO_BOOK.get().replace(Lang.VALUE, args[2])));
                            }
                        } else {
                            //no valid int was given
                            sender.sendMessage(Lang.build(Lang.NO_NUMBER.get().replace(Lang.VALUE, args[2])));
                        }
                    } else {
                        //no permission
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
                //NOTE: The list subbcommand without a page number is down below!
                case LIST -> { //greenbook book list [page number] - list all known quotes (books) neatly arranged in pages.
                    //check permission
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_LIST)){
                        //get all currently active books
                        List<String> books = ShelfListener.inst().getBooks();
                        //how many books are known. Needed to calculate how many pages there are and
                        //how many there should be on the given page (if the page is not full)
                        final int NUM_OF_BOOKS = books.size();
                        //how many pages of books are there? - needed in header and limit page to how many exits
                        final int NUM_OF_PAGES = (int) Math.ceil((double)NUM_OF_BOOKS / (double)BOOKS_PER_PAGE);

                        if (Misc.isInt(args[2])) {
                            //limit page to range of possible pages
                            final int PAGE = Math.max(Math.min(Integer.parseInt(args[2]), NUM_OF_PAGES), 1);
                            //don't try to access more books than exits
                            final int MAX_BOOKS_THIS_PAGE = Math.min(NUM_OF_BOOKS, PAGE * BOOKS_PER_PAGE);

                            //list of holding all the message lines
                            List<Component> tempComponents = new ArrayList<>();
                            //set header with current and max number of pages
                            tempComponents.add(Lang.build(Lang.SHELF_LIST_HEADER.get().replace(Lang.VALUE, String.valueOf(PAGE)).replace(Lang.MAX, String.valueOf(NUM_OF_PAGES))));

                            //add the books for the page
                            for (int id = (PAGE -1) * BOOKS_PER_PAGE; id < MAX_BOOKS_THIS_PAGE; id++){
                                tempComponents.add(
                                       //add leading " id - " to identify what id the book has (for remove command)
                                       Lang.build("&e " + id + " - ").
                                       //add the book itself (please notice: since we are appending, we have to reset the color beforehand)
                                       append(Lang.build("&f" + books.get(id))));
                            }

                            //add footer
                            tempComponents.add(Lang.build(Lang.SHELF_LIST_FOOTER.get())); //todo make buttons clickable

                            //join with new line as separator and send the result
                            sender.sendMessage(Lang.join(tempComponents));
                        } else {
                            //the given page number was not an int
                            sender.sendMessage(Lang.build(Lang.NO_NUMBER.get().replace(Lang.VALUE, args[2])));
                        }
                    } else {
                        //no permission
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
                case EMPTY_HAND_SHORT, EMPTY_HAND_LONG -> { //greenbook book emptyhand [true/false] - set if an empty hand is required for reading books
                    //check permission
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_EMPTYHAND)){
                        Boolean shouldQuoteRequireEmptyHand = BooleanUtils.toBooleanObject(args[2]);

                        if (shouldQuoteRequireEmptyHand != null) {
                            ShelfConfig.inst().setRequireEmptyHand(shouldQuoteRequireEmptyHand);
                            sender.sendMessage(Lang.build(Lang.SHELF_SET_EMPTYHAND.get().replace(Lang.VALUE, args[2])));
                        } else {
                            //the given argument was not a boolean
                            sender.sendMessage(Lang.build(Lang.NO_BOOL.get().replace(Lang.VALUE, args[2])));
                        }
                    } else {
                        //no permission
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
                case SNEAK -> { //greenbook book sneak [true/false] - set if sneaking is required for reading books
                    if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_SNEAK)){
                        //try to get a bool from the 3rd argument
                        Boolean shouldRequireSneak = BooleanUtils.toBooleanObject(args[2]);
                        if (shouldRequireSneak != null) {
                            //save the value to file and set the current value
                            ShelfConfig.inst().setRequireSneak(shouldRequireSneak);
                            //give feedback
                            sender.sendMessage(Lang.build(Lang.SHELF_SET_SNEAK.get().replace(Lang.VALUE, args[2])));
                        } else {
                            //3rd argument was not a bool
                            sender.sendMessage(Lang.build(Lang.NO_BOOL.get().replace(Lang.VALUE, args[2])));
                        }
                    } else {
                        //no permission
                        sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                    }
                }
            }
        } else {
            //NOTE: The list subbcommand with a page number is up above!
            if (args.length == 2 && args[1].equalsIgnoreCase(LIST)){ //no argument, defaults to first page
                //check permission
                if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_LIST)){
                    //get all currently active books
                    List<String> books = ShelfListener.inst().getBooks();
                    //how many books are known. Needed to calculate how many pages there are and
                    //how many there should be on the first page (if the page is not full)
                    final int NUM_OF_BOOKS = books.size();
                    //how many pages of books are there? - needed in header
                    final int NUM_OF_PAGES = (int) Math.ceil((double)NUM_OF_BOOKS / (double)BOOKS_PER_PAGE);
                    //don't try to access more books than exits
                    final int MAX_BOOKS_FIRST_PAGE = Math.min(NUM_OF_BOOKS, BOOKS_PER_PAGE); //don't try to access more books than exits

                    //list of holding all the message lines
                    List<Component> tempComponents = new ArrayList<>();
                    //set header with first page and max number of pages
                    tempComponents.add(Lang.build(Lang.SHELF_LIST_HEADER.get().replace(Lang.VALUE, String.valueOf(1)).replace(Lang.MAX, String.valueOf(NUM_OF_PAGES))));

                    //add the books for the first page
                    for (int id = 0; id < MAX_BOOKS_FIRST_PAGE; id++){
                        tempComponents.add(
                                //add leading " id - " to identify what id the book has (for remove command)
                                Lang.build("&e " + id + " - ").
                                        //add the book itself (please notice: since we are appending, we have to reset the color beforehand)
                                        append(Lang.build("&f" + books.get(id))));
                    }

                    //add footer
                    tempComponents.add(Lang.build(Lang.SHELF_LIST_FOOTER.get())); //todo make buttons clickable

                    //join with new line as separator and send the result
                    sender.sendMessage(Lang.join(tempComponents));
                } else {
                    //no permission
                    sender.sendMessage(Lang.build(Lang.NO_PERMISSION_COMMAND.get()));
                }
            } else {
                //not enough args
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
     *                command inside a command block, this will be the player, not
     *                the command block.
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed
     * @return A List of possible completions for the final argument, or null
     * to default to the command executor
     */
    public static @Nullable List<String> handleTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        switch (args.length){
            //greenbook shelf ?
            //return list of subcommands the sender has permission for
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
                    result.add(EMPTY_HAND_SHORT);
                    result.add(EMPTY_HAND_LONG);
                }

                return result.stream().filter(s -> s.toLowerCase().startsWith(args[1])).toList();
            }
            case 3 -> {
                switch (args[1].toLowerCase()){
                    //greenbook shelf remove ?
                    case REMOVE_SHORT, REMOVE_LONG -> {
                        //check permission
                        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_REMOVE)){
                            ArrayList<String> result = new ArrayList<>();

                            //make list of all known book ids
                            for (int id = 1; id <= ShelfListener.inst().getBooks().size(); id++){
                                result.add(String.valueOf(id));
                            }

                            //filter by already given argument
                            return result.stream().filter(s -> s.toLowerCase().startsWith(args[2])).toList();
                        }
                    }
                    //greenbook shelf list ?
                    case LIST -> {
                        //check permission
                        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_LIST)){
                            ArrayList<String> result = new ArrayList<>();

                            //cache number of pages to not recalculate every loop
                            final int PAGES = (int) Math.ceil((double)ShelfListener.inst().getBooks().size() / (double)BOOKS_PER_PAGE);

                            //make list of all known pages
                            for (int page = 1; page <= PAGES; page++){
                                result.add(String.valueOf(page));
                            }

                            //filter by already given argument
                            return result.stream().filter(s -> s.startsWith(args[2])).toList();
                        }
                    }
                    //greenbook shelf sneak ?
                    //greenbook shelf hand ?
                    //both are boolean values
                    case SNEAK, EMPTY_HAND_SHORT, EMPTY_HAND_LONG -> {
                        //check if sender has any permission of the both subcommands
                        if (PermissionUtils.hasPermission(sender, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_EMPTYHAND, PermissionUtils.GREENBOOK_SHELF_SNEAK)){
                            //filter by already given argument
                            return Stream.of(String.valueOf(true), String.valueOf(false)).filter(s -> s.toLowerCase().startsWith(args[2])).toList();
                        }
                    }
                }
            }
        }
        return null;
    }
}
