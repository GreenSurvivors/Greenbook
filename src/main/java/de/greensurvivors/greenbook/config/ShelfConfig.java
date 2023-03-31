package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.GreenLogger;
import de.greensurvivors.greenbook.listener.ShelfListener;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * keeps track of books and config values of readable bookshelfs in its own file
 */
public class ShelfConfig {
    //config key root, never use this directly!
    private static final String SHELF_KEY = "shelf.";
    //config key of requiring an empty hand to read a book
    private static final String REQUIRE_EMPTY_HAND_KEY = SHELF_KEY + "require_empty_hand";
    //config key of requiring sneaking to read a book
    private static final String REQUIRE_SNEAKING = SHELF_KEY + "require_sneaking";
    //config key of the list of all books
    private static final String BOOKS_KEY = SHELF_KEY + "books";

    //default quotes (aka books)
    private static final List<String> DEFAULT_BOOKS = List.of("moo!", "&cfire crackle");
    //default you have to sneak with an empty hand to read a book
    private static final boolean
            DEFAULT_REQUIRE_SNEAK = true, DEFAULT_REQUIRE_EMPTY_HAND = true;

    //this class keeps track of its own instance, so it's basically static
    private static ShelfConfig instance;

    //the config itself
    private final File file;
    private final YamlConfiguration configuration;

    /**
     * load config the fist time and initialise with default values
     */
    private ShelfConfig() {
        this.file = new File(GreenBook.inst().getDataFolder(), "shelfConfig.yml");
        this.configuration = YamlConfiguration.loadConfiguration(this.file);

        this.configuration.addDefault(REQUIRE_EMPTY_HAND_KEY, DEFAULT_REQUIRE_SNEAK);
        this.configuration.addDefault(REQUIRE_SNEAKING, DEFAULT_REQUIRE_SNEAK);
    }

    /**
     * static to instance translator
     */
    public static ShelfConfig inst() {
        if (instance == null) {
            instance = new ShelfConfig();
        }

        return instance;
    }

    /**
     * saves the config with its settings and quotes to file (NOTE: its not the main config!)
     */
    public void saveShelfConfig() {
        //config.setDefault doesn't work for string lists.
        //so we have to set it here if no valid list was set
        if(this.configuration.getStringList(BOOKS_KEY).isEmpty()){
            this.configuration.set(BOOKS_KEY, DEFAULT_BOOKS);
        }

        // set header, keep commands and save default values to file
        this.configuration.options().setHeader(Collections.singletonList(String.format(
                "Shelf config file for %s (%s)",
                GreenBook.inst().getName(),
                GreenBook.inst().getDescription().getVersion())));
        this.configuration.options().parseComments(true);
        this.configuration.options().copyDefaults(true);

        // save modified configuration
        try {
            this.configuration.save(file);
        } catch (IOException e) {
            GreenLogger.log(Level.SEVERE, "Could not save shelf book config file.", e);
        }
    }

    /**
     * load if reading a book requires an empty hand.
     * updates the currently working value
     * might be default if no valid value was set in config
     */
    private void loadRequireEmptyHand(){
        ShelfListener.inst().setRequireEmptyHand(this.configuration.getBoolean(REQUIRE_EMPTY_HAND_KEY, DEFAULT_REQUIRE_EMPTY_HAND));
    }

    /**
     * set if reading a book requires an empty hand.
     * saves this value to file and loading the new value again to update the current working value
     * @param required if reading a book requires an empty hand.
     */
    public void setRequireEmptyHand(boolean required){
        //set value in config
        this.configuration.set(REQUIRE_EMPTY_HAND_KEY, required);
        //save config to file
        saveShelfConfig();

        //set working value
        loadRequireEmptyHand();
    }

    /**
     * load if reading a book requires sneaking.
     * updates the currently working value
     * might be default if no valid value was set in config
     */
    private void loadRequireSneak(){
        ShelfListener.inst().setRequireSneak(this.configuration.getBoolean(REQUIRE_SNEAKING, DEFAULT_REQUIRE_SNEAK));
    }

    /**
     * set if reading a book requires sneaking.
     * saves this value to file and loading the new value again to update the current working value
     * @param required if reading a book requires sneaking.
     */
    public void setRequireSneak(boolean required){
        //set value in config
        this.configuration.set(REQUIRE_SNEAKING, required);
        //save config to file
        saveShelfConfig();

        //set working value
        loadRequireSneak();
    }

    /**
     * add a new quote (book) to config. Update currently used books
     * @param newBook new quote (book) to show up right-clicking a bookshelf
     */
    public void addBook(@NotNull String newBook) {
        //load list and add the new book
        List<String> books = this.configuration.getStringList(BOOKS_KEY);
        books.add(newBook);
        this.configuration.set(BOOKS_KEY, books);

        //set currently used books
        ShelfListener.inst().setBooks(books);

        //save changed config
        saveShelfConfig();
    }

    /**
     * remove a quote (book) from config. Update currently used books
     * @param oldBook old quote (book) to not show up anymore, when right-clicking a bookshelf
     */
    public void removeBook(@NotNull String oldBook) {
        //load list and try to remove the old book
        List<String> books = this.configuration.getStringList(BOOKS_KEY);
        books.remove(oldBook);
        this.configuration.set(BOOKS_KEY, books);

        //set currently used books
        ShelfListener.inst().setBooks(books);

        //save changed config
        saveShelfConfig();
    }

    /**
     * load all config values, as well all books from file
     */
    public void loadShelfConfig() {
        loadRequireSneak();
        loadRequireEmptyHand();

        ShelfListener.inst().setBooks(this.configuration.getStringList(BOOKS_KEY));
    }
}
