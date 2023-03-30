package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.GreenLogger;
import de.greensurvivors.greenbook.listener.ShelfListener;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class ShelfConfig {
    private static final String SHELF_KEY = "shelf.";
    private static final String Books_KEY = SHELF_KEY + "books";
    private static final String REQUIRE_EMPTY_HAND_KEY = SHELF_KEY + "require_empty_hand";
    private static final String REQUIRE_SNEAKING = SHELF_KEY + "require_sneaking";

    private static final List<String> DEFAULT_BOOKS = List.of("moo!", "fire crackle");
    private static final boolean
            DEFAULT_REQUIRE_SNEAK = true, DEFAULT_REQUIRE_EMPTY_HAND = false;

    private static ShelfConfig instance;

    private final File file;
    private final YamlConfiguration configuration;

    private ShelfConfig() {
        this.file = new File(GreenBook.inst().getDataFolder(), "shelfConfig.yml");
        this.configuration = YamlConfiguration.loadConfiguration(this.file);

        this.configuration.addDefault(REQUIRE_EMPTY_HAND_KEY, DEFAULT_REQUIRE_SNEAK);
        this.configuration.addDefault(REQUIRE_SNEAKING, DEFAULT_REQUIRE_SNEAK);
    }

    public static ShelfConfig inst() {
        if (instance == null) {
            instance = new ShelfConfig();
        }

        return instance;
    }

    public void setRequireEmptyHand(boolean required){
        this.configuration.set(REQUIRE_EMPTY_HAND_KEY, required);
        saveShelfConfig();

        loadRequireEmptyHand();
    }

    private void loadRequireEmptyHand(){
        ShelfListener.inst().setRequireEmptyHand(this.configuration.getBoolean(REQUIRE_EMPTY_HAND_KEY, DEFAULT_REQUIRE_EMPTY_HAND));
    }

    public void setRequireSneak(boolean required){
        this.configuration.set(REQUIRE_SNEAKING, required);
        saveShelfConfig();

        loadRequireSneak();
    }

    private void loadRequireSneak(){
        ShelfListener.inst().setRequireSneak(this.configuration.getBoolean(REQUIRE_SNEAKING, DEFAULT_REQUIRE_SNEAK));
    }

    public void saveShelfConfig() {
        if(this.configuration.getStringList(Books_KEY).isEmpty()){
            this.configuration.set(Books_KEY, DEFAULT_BOOKS);
        }

        // save modified configuration
        this.configuration.options().setHeader(Collections.singletonList(String.format(
                "Shelf config file for %s (%s)",
                GreenBook.inst().getName(),
                GreenBook.inst().getDescription().getVersion())));
        this.configuration.options().parseComments(true);
        this.configuration.options().copyDefaults(true);

        try {
            this.configuration.save(file);
        } catch (IOException e) {
            GreenLogger.log(Level.SEVERE, "Could not save shelf book config file.", e);
        }
    }

    public void addBook(String newBook) {
        List<String> books = this.configuration.getStringList(Books_KEY);
        books.add(newBook);
        this.configuration.set(Books_KEY, books);
        ShelfListener.inst().setBooks(books);

        saveShelfConfig();
    }

    public void removeBook(String oldBook) {
        List<String> books = this.configuration.getStringList(Books_KEY);
        books.remove(oldBook);
        this.configuration.set(Books_KEY, books);
        ShelfListener.inst().setBooks(books);

        saveShelfConfig();
    }

    public void loadShelfs() {
        ShelfListener.inst().setBooks(this.configuration.getStringList(Books_KEY));

        loadRequireSneak();
        loadRequireEmptyHand();
    }
}
